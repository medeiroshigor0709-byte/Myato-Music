package com.example.core.metadata

import com.example.domain.models.ArtistFilterCandidate
import com.example.domain.models.MusicTrack
import com.example.domain.models.ParsedTrackArtists
import com.example.domain.models.SearchFilterMode
import com.example.domain.models.SearchMatchSource
import com.example.domain.models.SmartSearchResult
import com.example.domain.models.TrackSearchHit
import com.example.domain.models.TrackSortOrder
import java.text.Normalizer
import java.util.Locale

/**
 * Intelligent offline music search and artist-identity engine.
 *
 * Performance & architecture highlights:
 * - ASCII fast-path text normalization (avoids java.text.Normalizer & Regex overhead on ASCII strings)
 * - Pre-computed TrackSearchIndex caching parsed artists, normalized metadata fields, word tokens,
 *   and artist lookup maps per library snapshot (eliminating repeated regex parsing on every keystroke)
 * - Bounded LRU query cache + safe prefix candidate narrowing for long queries
 * - Zero redundant normalization inside sort comparators
 * - Guarantees strict artist attribution based on real metadata (no unrelated genre/artist hallucination)
 */
object MusicSearchEngine {

    const val UNKNOWN_ARTIST_LABEL = "Artista desconhecido"
    const val DEFAULT_SEARCH_DEBOUNCE_MS = 280L
    const val MIN_SEARCH_DEBOUNCE_MS = 250L
    const val MAX_SEARCH_DEBOUNCE_MS = 350L

    private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val multiSpaceRegex = Regex("\\s+")

    private val unknownArtistTokens = setOf(
        "<unknown>",
        "unknown",
        "unknown artist",
        "artista desconhecido",
        "desconhecido",
        "sem artista",
        "n/a",
    )

    private val compilationAlbumArtists = setOf(
        "various artists",
        "various",
        "varios artistas",
        "varios",
        "va",
        "v.a.",
        "compilation",
        "coletanea",
        "soundtrack",
    )

    // Split primary vs featured artists inside artist string (e.g. "Haikaiss feat. Rashid", "ADL (part. Haikaiss)")
    private val featSplitRegex = Regex(
        pattern = """(?i)(?:\s*\(\s*|\s*\[\s*|\s+)(?:feat\.?|ft\.?|featuring|part\.?|participacao|participação|with)\b\.?\s*""",
    )

    // Extract featured artists inside title like "Song Name (feat. Haikaiss & Rashid)"
    private val titleFeatRegex = Regex(
        pattern = """(?i)[\(\[]\s*(?:feat\.?|ft\.?|featuring|part\.?|participacao|participação)\b\.?\s*([^\)\]]+)[\)\]]""",
    )

    // Split multiple artists in a single segment (commas, semicolons, &, /, +, x)
    private val collabSeparatorRegex = Regex(
        pattern = """(?i)\s*(?:,|;|/|\+|\s+&\s+|\s+x\s+|\s+e\s+)\s*""",
    )

    private val normUnknownLabel by lazy { normalizeText(UNKNOWN_ARTIST_LABEL) }

    data class IndexedTrackEntry(
        val track: MusicTrack,
        val parsedArtists: ParsedTrackArtists,
        val normTitle: String,
        val titleWords: List<String>,
        val normAlbum: String,
        val albumWords: List<String>,
        val normAlbumArtist: String,
        val albumArtistWords: List<String>,
        val isGenericAlbumArtist: Boolean,
        val normGenre: String,
        val genreWords: List<String>,
        val normRawArtist: String,
        val normDisplayArtist: String,
        val normPrimaryArtists: List<String>,
        val primaryArtistWords: List<List<String>>,
        val normFeaturedArtists: List<String>,
        val featuredArtistWords: List<List<String>>,
        val normAllArtists: List<String>,
        val allArtistWords: List<List<String>>,
        val isSinglePrimarySolo: Boolean,
    )

    internal data class IndexedArtistEntry(
        val normName: String,
        val words: List<String>,
        val candidate: ArtistFilterCandidate,
    )

    private data class SearchCacheKey(
        val normQuery: String,
        val filterMode: SearchFilterMode,
        val normSelectedArtist: String?,
        val sortOrder: TrackSortOrder,
    )

    private data class ScoredIndexedHit(
        val entry: IndexedTrackEntry,
        val hit: TrackSearchHit,
    )

    class TrackSearchIndex internal constructor(
        val datasetFingerprint: Long,
        val trackCount: Int,
        val entries: List<IndexedTrackEntry>,
        private val artistEntries: List<IndexedArtistEntry>,
        private val soloByArtistNorm: Map<String, List<IndexedTrackEntry>>,
        private val allByArtistNorm: Map<String, List<IndexedTrackEntry>>,
        val allArtistCandidates: List<ArtistFilterCandidate>,
    ) {
        private val cacheLock = Any()

        private val searchCache = object : LinkedHashMap<SearchCacheKey, SmartSearchResult>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SearchCacheKey, SmartSearchResult>?): Boolean =
                size > 64
        }

        private val artistCache = object : LinkedHashMap<String, List<ArtistFilterCandidate>>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ArtistFilterCandidate>>?): Boolean =
                size > 64
        }

        private val prefixSubsetCache = object : LinkedHashMap<Pair<String, String?>, List<IndexedTrackEntry>>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, String?>, List<IndexedTrackEntry>>?): Boolean =
                size > 32
        }

        fun extractArtistCandidates(query: String): List<ArtistFilterCandidate> {
            val normQuery = normalizeText(query)
            if (normQuery.isBlank()) return allArtistCandidates

            synchronized(cacheLock) {
                artistCache[normQuery]?.let { return it }
            }

            val scored = ArrayList<Triple<String, Int, ArtistFilterCandidate>>(artistEntries.size.coerceAtMost(32))
            for (i in artistEntries.indices) {
                val item = artistEntries[i]
                val normName = item.normName
                val matches = normName == normQuery ||
                    normName.startsWith(normQuery) ||
                    containsWordPrefix(item.words, normQuery) ||
                    (normQuery.length >= 3 && normName.contains(normQuery))
                if (matches) {
                    scored.add(
                        Triple(
                            normName,
                            scoreArtistCandidate(normName, item.words, normQuery),
                            item.candidate,
                        )
                    )
                }
            }

            val sorted = scored.sortedWith(
                compareByDescending<Triple<String, Int, ArtistFilterCandidate>> { it.second }
                    .thenByDescending { it.third.soloTrackCount }
                    .thenByDescending { it.third.totalTrackCount }
                    .thenBy { it.first }
            ).map { it.third }

            synchronized(cacheLock) {
                artistCache[normQuery] = sorted
            }
            return sorted
        }

        fun searchTracks(
            query: String,
            filterMode: SearchFilterMode,
            selectedArtist: String?,
            sortOrder: TrackSortOrder,
        ): SmartSearchResult {
            val normQuery = normalizeText(query)
            val normSelectedArtist = selectedArtist?.trim()?.takeIf { it.isNotBlank() }?.let { normalizeText(it) }
            val cacheKey = SearchCacheKey(normQuery, filterMode, normSelectedArtist, sortOrder)

            synchronized(cacheLock) {
                searchCache[cacheKey]?.let { return it }
            }

            val matchingArtists = extractArtistCandidates(query)

            val resolvedArtistFilter: String? = when {
                !selectedArtist.isNullOrBlank() -> selectedArtist.trim()
                filterMode != SearchFilterMode.ALL && normQuery.isNotBlank() -> {
                    matchingArtists.firstOrNull { normalizeText(it.name) == normQuery }?.name
                        ?: query.trim()
                }
                else -> null
            }
            val normResolvedArtist = resolvedArtistFilter?.let { normalizeText(it) }

            val scoredHits: List<ScoredIndexedHit> = when (filterMode) {
                SearchFilterMode.EXACT_ARTIST -> {
                    if (normResolvedArtist == null) {
                        entries.map { entry ->
                            ScoredIndexedHit(
                                entry,
                                TrackSearchHit(entry.track, 0, SearchMatchSource.EXACT_ARTIST, entry.parsedArtists),
                            )
                        }
                    } else {
                        val subset = soloByArtistNorm[normResolvedArtist].orEmpty()
                        subset.map { entry ->
                            ScoredIndexedHit(
                                entry,
                                TrackSearchHit(entry.track, 1000, SearchMatchSource.EXACT_ARTIST, entry.parsedArtists),
                            )
                        }
                    }
                }

                SearchFilterMode.ARTIST_AND_COLLABS -> {
                    if (normResolvedArtist == null) {
                        entries.map { entry ->
                            val src = if (entry.parsedArtists.isCollaboration) {
                                SearchMatchSource.COLLAB_ARTIST
                            } else {
                                SearchMatchSource.EXACT_ARTIST
                            }
                            ScoredIndexedHit(
                                entry,
                                TrackSearchHit(entry.track, 0, src, entry.parsedArtists),
                            )
                        }
                    } else {
                        val subset = allByArtistNorm[normResolvedArtist].orEmpty()
                        subset.map { entry ->
                            val isSoloExact = entry.isSinglePrimarySolo &&
                                entry.normPrimaryArtists.firstOrNull() == normResolvedArtist
                            ScoredIndexedHit(
                                entry,
                                TrackSearchHit(
                                    track = entry.track,
                                    relevanceScore = if (isSoloExact) 1000 else 850,
                                    matchSource = if (isSoloExact) SearchMatchSource.EXACT_ARTIST else SearchMatchSource.COLLAB_ARTIST,
                                    parsedArtists = entry.parsedArtists,
                                ),
                            )
                        }
                    }
                }

                SearchFilterMode.ALL -> {
                    val baseCandidates: List<IndexedTrackEntry> = if (normResolvedArtist != null) {
                        allByArtistNorm[normResolvedArtist].orEmpty()
                    } else {
                        entries
                    }

                    if (normResolvedArtist != null && normQuery.isBlank()) {
                        baseCandidates.map { entry ->
                            val isSoloExact = entry.isSinglePrimarySolo &&
                                entry.normPrimaryArtists.firstOrNull() == normResolvedArtist
                            ScoredIndexedHit(
                                entry,
                                TrackSearchHit(
                                    track = entry.track,
                                    relevanceScore = if (isSoloExact) 1000 else 850,
                                    matchSource = if (isSoloExact) SearchMatchSource.EXACT_ARTIST else SearchMatchSource.COLLAB_ARTIST,
                                    parsedArtists = entry.parsedArtists,
                                ),
                            )
                        }
                    } else if (normQuery.isBlank()) {
                        baseCandidates.map { entry ->
                            ScoredIndexedHit(
                                entry,
                                TrackSearchHit(entry.track, 0, SearchMatchSource.TITLE, entry.parsedArtists),
                            )
                        }
                    } else {
                        // Safe incremental prefix narrowing: only reuse cached subset if prefix length >= 3
                        // so substring matching semantics (length >= 3) guarantee 100% superset soundness.
                        val narrowedCandidates = findPrefixSubset(normQuery, normResolvedArtist) ?: baseCandidates
                        val queryTokens = if (' ' in normQuery) extractSearchWords(normQuery) else emptyList()
                        val results = ArrayList<ScoredIndexedHit>(narrowedCandidates.size.coerceAtMost(64))
                        val matchedEntries = ArrayList<IndexedTrackEntry>(narrowedCandidates.size.coerceAtMost(64))

                        for (i in narrowedCandidates.indices) {
                            val entry = narrowedCandidates[i]
                            val hit = scoreIndexedTrack(entry, normQuery, queryTokens)
                            if (hit != null) {
                                results.add(ScoredIndexedHit(entry, hit))
                                matchedEntries.add(entry)
                            }
                        }
                        if (normQuery.length >= 3) {
                            synchronized(cacheLock) {
                                prefixSubsetCache[normQuery to normResolvedArtist] = matchedEntries
                            }
                        }
                        results
                    }
                }
            }

            val sortedHits = sortIndexedHits(
                hits = scoredHits,
                sortOrder = sortOrder,
                prioritizeRelevance = normQuery.isNotBlank() || resolvedArtistFilter != null,
            )

            val result = SmartSearchResult(
                hits = sortedHits,
                matchingArtists = matchingArtists,
                activeArtistFilter = resolvedArtistFilter,
                filterMode = filterMode,
            )

            synchronized(cacheLock) {
                searchCache[cacheKey] = result
            }
            return result
        }

        private fun findPrefixSubset(normQuery: String, normArtist: String?): List<IndexedTrackEntry>? {
            if (normQuery.length < 4 || ' ' in normQuery) return null
            synchronized(cacheLock) {
                for (len in (normQuery.length - 1) downTo 3) {
                    val prefix = normQuery.substring(0, len)
                    prefixSubsetCache[prefix to normArtist]?.let { return it }
                }
            }
            return null
        }

        fun clearCaches() {
            synchronized(cacheLock) {
                searchCache.clear()
                artistCache.clear()
                prefixSubsetCache.clear()
            }
        }
    }

    private val indexLock = Any()

    @Volatile
    private var lastListRef: List<MusicTrack>? = null

    @Volatile
    private var lastBuiltIndex: TrackSearchIndex? = null

    private val indexLru = object : LinkedHashMap<Long, TrackSearchIndex>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, TrackSearchIndex>?): Boolean =
            size > 4
    }

    fun clampDebounceMs(value: Long): Long = value.coerceIn(MIN_SEARCH_DEBOUNCE_MS, MAX_SEARCH_DEBOUNCE_MS)

    /**
     * Fast, allocation-light normalization.
     * Skips java.text.Normalizer and Regex entirely when input is pure ASCII.
     */
    fun normalizeText(raw: String): String {
        if (raw.isEmpty()) return ""
        val len = raw.length
        var start = 0
        while (start < len && raw[start].isWhitespace()) start++
        if (start == len) return ""
        var end = len - 1
        while (end > start && raw[end].isWhitespace()) end--

        var isAsciiOnly = true
        var needsTransform = false
        var prevSpace = false
        for (i in start..end) {
            val c = raw[i]
            val code = c.code
            if (code >= 128) {
                isAsciiOnly = false
                break
            }
            if (c in 'A'..'Z' || c == '\t' || c == '\n' || c == '\r') {
                needsTransform = true
            } else if (c == ' ') {
                if (prevSpace) needsTransform = true
                prevSpace = true
            } else {
                prevSpace = false
            }
        }

        if (isAsciiOnly) {
            if (!needsTransform && start == 0 && end == len - 1) {
                return raw
            }
            val sb = StringBuilder(end - start + 1)
            var lastWasSpace = false
            for (i in start..end) {
                val c = raw[i]
                if (c.isWhitespace()) {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                } else {
                    lastWasSpace = false
                    sb.append(if (c in 'A'..'Z') (c.code + 32).toChar() else c)
                }
            }
            return sb.toString()
        }

        val sliced = if (start == 0 && end == len - 1) raw else raw.substring(start, end + 1)
        val collapsed = sliced.replace(multiSpaceRegex, " ")
        val decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
        return combiningMarksRegex.replace(decomposed, "").lowercase(Locale.ROOT)
    }

    private fun isWordDelimiter(c: Char): Boolean {
        return c == ' ' || c == '-' || c == '_' || c == '(' || c == ')' ||
            c == '[' || c == ']' || c == '.' || c == '/' || c == '&' ||
            c == ',' || c == ':' || c == ';' || c == '+' || c == '\'' || c == '"'
    }

    internal fun extractSearchWords(normalized: String): List<String> {
        if (normalized.isEmpty()) return emptyList()
        val words = ArrayList<String>(4)
        var tokenStart = -1
        for (i in 0 until normalized.length) {
            if (isWordDelimiter(normalized[i])) {
                if (tokenStart >= 0) {
                    words.add(normalized.substring(tokenStart, i))
                    tokenStart = -1
                }
            } else if (tokenStart < 0) {
                tokenStart = i
            }
        }
        if (tokenStart >= 0) {
            words.add(normalized.substring(tokenStart))
        }
        return words
    }

    private fun containsWordPrefix(words: List<String>, needle: String): Boolean {
        if (needle.isBlank()) return false
        for (i in words.indices) {
            if (words[i].startsWith(needle)) return true
        }
        return false
    }

    private fun containsWordPrefix(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        return containsWordPrefix(extractSearchWords(haystack), needle)
    }

    fun isUnknownArtist(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        return normalizeText(raw) in unknownArtistTokens
    }

    fun sanitizeArtistName(raw: String?): String {
        if (isUnknownArtist(raw)) return UNKNOWN_ARTIST_LABEL
        return raw!!.trim().replace(multiSpaceRegex, " ")
    }

    private fun isGenericCompilationAlbumArtist(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        val norm = normalizeText(raw)
        return norm in unknownArtistTokens || norm in compilationAlbumArtists
    }

    private fun cleanArtistSegment(segment: String): String {
        return segment
            .trim()
            .trimEnd(')', ']', '.', '-')
            .trimStart('(', '[')
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    private fun splitArtistList(segment: String, albumArtistRaw: String): List<String> {
        val cleaned = cleanArtistSegment(segment)
        if (cleaned.isBlank()) return emptyList()

        // Preserve duo/band names containing "&" when Album Artist explicitly equals the full string (e.g. "Simon & Garfunkel")
        val normCleaned = normalizeText(cleaned)
        val normAlbumArtist = normalizeText(albumArtistRaw)
        if (normAlbumArtist.isNotBlank() && normAlbumArtist == normCleaned && !cleaned.contains(",")) {
            return listOf(cleaned)
        }

        return collabSeparatorRegex.split(cleaned)
            .map { cleanArtistSegment(it) }
            .filter { it.isNotBlank() && !isUnknownArtist(it) }
            .distinctBy { normalizeText(it) }
    }

    fun parseTrackArtists(track: MusicTrack): ParsedTrackArtists {
        val rawArtist = track.artist.trim()
        val rawAlbumArtist = track.albumArtist.trim()

        val artistUnknown = isUnknownArtist(rawArtist)
        val effectiveArtistSource = when {
            !artistUnknown -> rawArtist
            !isGenericCompilationAlbumArtist(rawAlbumArtist) -> rawAlbumArtist
            else -> ""
        }

        if (effectiveArtistSource.isBlank()) {
            return ParsedTrackArtists(
                displayArtist = UNKNOWN_ARTIST_LABEL,
                primaryArtists = emptyList(),
                featuredArtists = emptyList(),
                allArtists = emptyList(),
                isCollaboration = false,
                isUnknownArtist = true,
            )
        }

        val featSplit = featSplitRegex.split(effectiveArtistSource, limit = 2)
        val primaryPart = featSplit[0]
        val featuredPartFromArtist = if (featSplit.size > 1) featSplit[1] else ""

        val primaryList = splitArtistList(primaryPart, rawAlbumArtist)
        val featuredFromArtist = if (featuredPartFromArtist.isNotBlank()) {
            splitArtistList(featuredPartFromArtist, "")
        } else {
            emptyList()
        }

        val featuredFromTitle = titleFeatRegex.findAll(track.title)
            .flatMap { match -> splitArtistList(match.groupValues[1], "") }
            .toList()

        val primaryNorms = primaryList.map { normalizeText(it) }.toSet()
        val featuredList = (featuredFromArtist + featuredFromTitle)
            .filter { normalizeText(it) !in primaryNorms }
            .distinctBy { normalizeText(it) }

        val allArtists = (primaryList + featuredList).distinctBy { normalizeText(it) }
        val isCollab = primaryList.size > 1 || featuredList.isNotEmpty()

        return ParsedTrackArtists(
            displayArtist = sanitizeArtistName(effectiveArtistSource),
            primaryArtists = primaryList,
            featuredArtists = featuredList,
            allArtists = allArtists,
            isCollaboration = isCollab,
            isUnknownArtist = false,
        )
    }

    private fun computeDatasetFingerprint(tracks: List<MusicTrack>): Long {
        var h = tracks.size.toLong() * 0x9E3779B97F4A7C15UL.toLong()
        for (i in tracks.indices) {
            val t = tracks[i]
            h = h * 31L + t.id
            h = h xor (t.title.hashCode().toLong() shl 17)
            h = h xor (t.artist.hashCode().toLong() shl 7)
            h = h xor t.album.hashCode().toLong()
            h = h xor (t.genre.hashCode().toLong() shl 3)
            h = h xor (t.playCount.toLong() * 131L)
        }
        return h
    }

    fun getOrBuildIndex(tracks: List<MusicTrack>): TrackSearchIndex {
        if (tracks === lastListRef) {
            lastBuiltIndex?.let { cached ->
                if (cached.trackCount == tracks.size) return cached
            }
        }
        val fp = computeDatasetFingerprint(tracks)
        synchronized(indexLock) {
            indexLru[fp]?.let { cached ->
                if (cached.trackCount == tracks.size) {
                    lastListRef = tracks
                    lastBuiltIndex = cached
                    return cached
                }
            }
        }
        val built = buildIndexInternal(tracks, fp)
        synchronized(indexLock) {
            indexLru[fp] = built
            lastListRef = tracks
            lastBuiltIndex = built
        }
        return built
    }

    fun prewarmIndex(tracks: List<MusicTrack>): TrackSearchIndex = getOrBuildIndex(tracks)

    fun clearCaches() {
        synchronized(indexLock) {
            lastListRef = null
            lastBuiltIndex = null
            indexLru.clear()
        }
    }

    private fun buildIndexInternal(tracks: List<MusicTrack>, fingerprint: Long): TrackSearchIndex {
        val entries = ArrayList<IndexedTrackEntry>(tracks.size)
        val soloByArtistNorm = HashMap<String, MutableList<IndexedTrackEntry>>()
        val allByArtistNorm = HashMap<String, MutableList<IndexedTrackEntry>>()

        data class Acc(
            var displayName: String,
            val soloIds: MutableSet<Long> = linkedSetOf(),
            val collabIds: MutableSet<Long> = linkedSetOf(),
        )
        val byNormArtist = linkedMapOf<String, Acc>()

        for (i in tracks.indices) {
            val track = tracks[i]
            val parsed = parseTrackArtists(track)
            val normTitle = normalizeText(track.title)
            val normAlbum = normalizeText(track.album)
            val normAlbumArtist = normalizeText(track.albumArtist)
            val normGenre = normalizeText(track.genre)
            val normRawArtist = normalizeText(track.artist)
            val normDisplayArtist = normalizeText(parsed.displayArtist)

            val normPrimary = parsed.primaryArtists.map { normalizeText(it) }
            val primaryWords = normPrimary.map { extractSearchWords(it) }
            val normFeatured = parsed.featuredArtists.map { normalizeText(it) }
            val featuredWords = normFeatured.map { extractSearchWords(it) }
            val normAll = parsed.allArtists.map { normalizeText(it) }
            val allWords = normAll.map { extractSearchWords(it) }
            val isSingleSolo = !parsed.isCollaboration && normPrimary.size == 1

            val entry = IndexedTrackEntry(
                track = track,
                parsedArtists = parsed,
                normTitle = normTitle,
                titleWords = extractSearchWords(normTitle),
                normAlbum = normAlbum,
                albumWords = extractSearchWords(normAlbum),
                normAlbumArtist = normAlbumArtist,
                albumArtistWords = extractSearchWords(normAlbumArtist),
                isGenericAlbumArtist = isGenericCompilationAlbumArtist(track.albumArtist),
                normGenre = normGenre,
                genreWords = extractSearchWords(normGenre),
                normRawArtist = normRawArtist,
                normDisplayArtist = normDisplayArtist,
                normPrimaryArtists = normPrimary,
                primaryArtistWords = primaryWords,
                normFeaturedArtists = normFeatured,
                featuredArtistWords = featuredWords,
                normAllArtists = normAll,
                allArtistWords = allWords,
                isSinglePrimarySolo = isSingleSolo,
            )
            entries.add(entry)

            if (parsed.isUnknownArtist) {
                allByArtistNorm.getOrPut(normUnknownLabel) { ArrayList() }.add(entry)
                soloByArtistNorm.getOrPut(normUnknownLabel) { ArrayList() }.add(entry)
            } else {
                if (isSingleSolo) {
                    val key = normPrimary[0]
                    soloByArtistNorm.getOrPut(key) { ArrayList() }.add(entry)
                    val acc = byNormArtist.getOrPut(key) { Acc(displayName = parsed.primaryArtists[0]) }
                    acc.soloIds.add(track.id)
                } else {
                    for (j in normAll.indices) {
                        val key = normAll[j]
                        val displayName = parsed.allArtists[j]
                        val acc = byNormArtist.getOrPut(key) { Acc(displayName = displayName) }
                        acc.collabIds.add(track.id)
                    }
                }
                for (j in normAll.indices) {
                    allByArtistNorm.getOrPut(normAll[j]) { ArrayList() }.add(entry)
                }
            }
        }

        val artistEntries = byNormArtist.entries.map { (normName, acc) ->
            val totalIds = (acc.soloIds + acc.collabIds).size
            IndexedArtistEntry(
                normName = normName,
                words = extractSearchWords(normName),
                candidate = ArtistFilterCandidate(
                    name = acc.displayName,
                    soloTrackCount = acc.soloIds.size,
                    collabTrackCount = acc.collabIds.size,
                    totalTrackCount = totalIds,
                ),
            )
        }.sortedWith(
            compareByDescending<IndexedArtistEntry> { it.candidate.soloTrackCount }
                .thenByDescending { it.candidate.totalTrackCount }
                .thenBy { it.normName }
        )

        return TrackSearchIndex(
            datasetFingerprint = fingerprint,
            trackCount = tracks.size,
            entries = entries,
            artistEntries = artistEntries,
            soloByArtistNorm = soloByArtistNorm,
            allByArtistNorm = allByArtistNorm,
            allArtistCandidates = artistEntries.map { it.candidate },
        )
    }

    fun matchesArtist(
        track: MusicTrack,
        targetArtist: String,
        includeCollaborations: Boolean,
    ): Boolean {
        val normTarget = normalizeText(targetArtist)
        if (normTarget.isBlank()) return false

        val parsed = parseTrackArtists(track)
        if (parsed.isUnknownArtist) {
            return normTarget == normUnknownLabel
        }

        return if (!includeCollaborations) {
            !parsed.isCollaboration &&
                parsed.primaryArtists.size == 1 &&
                normalizeText(parsed.primaryArtists.first()) == normTarget
        } else {
            parsed.allArtists.any { normalizeText(it) == normTarget }
        }
    }

    fun tracksForArtist(
        tracks: List<MusicTrack>,
        artistName: String,
        includeCollaborations: Boolean,
    ): List<MusicTrack> {
        val normTarget = normalizeText(artistName)
        if (normTarget.isBlank()) return emptyList()
        val index = getOrBuildIndex(tracks)
        val matched = index.searchTracks(
            query = "",
            filterMode = if (includeCollaborations) SearchFilterMode.ARTIST_AND_COLLABS else SearchFilterMode.EXACT_ARTIST,
            selectedArtist = artistName,
            sortOrder = TrackSortOrder.TITLE_ASC,
        ).hits.map { it.track }
        return matched.distinctBy { it.id }
    }

    fun extractArtistCandidates(
        tracks: List<MusicTrack>,
        query: String = "",
    ): List<ArtistFilterCandidate> {
        return getOrBuildIndex(tracks).extractArtistCandidates(query)
    }

    fun searchTracks(
        tracks: List<MusicTrack>,
        query: String,
        filterMode: SearchFilterMode = SearchFilterMode.ALL,
        selectedArtist: String? = null,
        sortOrder: TrackSortOrder = TrackSortOrder.TITLE_ASC,
    ): SmartSearchResult {
        return getOrBuildIndex(tracks).searchTracks(
            query = query,
            filterMode = filterMode,
            selectedArtist = selectedArtist,
            sortOrder = sortOrder,
        )
    }

    private fun scoreIndexedTrack(
        entry: IndexedTrackEntry,
        normQuery: String,
        queryTokens: List<String>,
    ): TrackSearchHit? {
        val parsed = entry.parsedArtists
        var bestScore = 0
        var bestSource = SearchMatchSource.TITLE

        // 1. Artist exact / primary / collaboration matching (highest priority)
        if (!parsed.isUnknownArtist) {
            val isSoloExact = entry.isSinglePrimarySolo &&
                entry.normPrimaryArtists.firstOrNull() == normQuery

            val isExactPrimaryOrCollab = entry.normAllArtists.any { it == normQuery }

            when {
                isSoloExact -> {
                    bestScore = 1000
                    bestSource = SearchMatchSource.EXACT_ARTIST
                }
                isExactPrimaryOrCollab -> {
                    bestScore = 850
                    bestSource = SearchMatchSource.COLLAB_ARTIST
                }
                entry.normPrimaryArtists.any { it.startsWith(normQuery) } -> {
                    bestScore = 720
                    bestSource = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                }
                entry.normFeaturedArtists.any { it.startsWith(normQuery) } -> {
                    bestScore = 680
                    bestSource = SearchMatchSource.COLLAB_ARTIST
                }
                entry.allArtistWords.any { containsWordPrefix(it, normQuery) } -> {
                    bestScore = 640
                    bestSource = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                }
                normQuery.length >= 3 && entry.normRawArtist.contains(normQuery) -> {
                    bestScore = 590
                    bestSource = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                }
            }
        } else if (normQuery.length >= 3 && normUnknownLabel.contains(normQuery)) {
            bestScore = 520
            bestSource = SearchMatchSource.EXACT_ARTIST
        }

        // 2. Album artist metadata
        if (bestScore < 560 && entry.normAlbumArtist.isNotBlank() && !entry.isGenericAlbumArtist) {
            val albumArtistScore = when {
                entry.normAlbumArtist == normQuery -> 560
                entry.normAlbumArtist.startsWith(normQuery) -> 520
                containsWordPrefix(entry.albumArtistWords, normQuery) -> 490
                normQuery.length >= 3 && entry.normAlbumArtist.contains(normQuery) -> 460
                else -> 0
            }
            if (albumArtistScore > bestScore) {
                bestScore = albumArtistScore
                bestSource = SearchMatchSource.ALBUM_ARTIST
            }
        }

        // 3. Track Title metadata
        if (bestScore < 450 && entry.normTitle.isNotBlank()) {
            val titleScore = when {
                entry.normTitle == normQuery -> 450
                entry.normTitle.startsWith(normQuery) -> 410
                containsWordPrefix(entry.titleWords, normQuery) -> 370
                normQuery.length >= 3 && entry.normTitle.contains(normQuery) -> 320
                else -> 0
            }
            if (titleScore > bestScore) {
                bestScore = titleScore
                bestSource = SearchMatchSource.TITLE
            }
        }

        // 4. Album metadata
        if (bestScore < 250 && entry.normAlbum.isNotBlank()) {
            val albumScore = when {
                entry.normAlbum == normQuery -> 250
                entry.normAlbum.startsWith(normQuery) -> 220
                containsWordPrefix(entry.albumWords, normQuery) -> 190
                normQuery.length >= 3 && entry.normAlbum.contains(normQuery) -> 160
                else -> 0
            }
            if (albumScore > bestScore) {
                bestScore = albumScore
                bestSource = SearchMatchSource.ALBUM
            }
        }

        // 5. Genre metadata
        if (bestScore < 130 && entry.normGenre.isNotBlank()) {
            val genreScore = when {
                entry.normGenre == normQuery -> 130
                entry.normGenre.startsWith(normQuery) -> 110
                containsWordPrefix(entry.genreWords, normQuery) -> 95
                normQuery.length >= 3 && entry.normGenre.contains(normQuery) -> 80
                else -> 0
            }
            if (genreScore > bestScore) {
                bestScore = genreScore
                bestSource = SearchMatchSource.GENRE
            }
        }

        // 6. Multi-word partial / special-character fallback (e.g. "ac dc" for "AC/DC", or "praga haikaiss")
        if (bestScore == 0 && queryTokens.size >= 2) {
            val allTokensMatched = queryTokens.all { token ->
                tokenMatchesEntry(entry, token)
            }
            if (allTokensMatched) {
                bestScore = 180
                bestSource = if (entry.allArtistWords.any { containsWordPrefix(it, queryTokens.first()) }) {
                    if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                } else {
                    SearchMatchSource.TITLE
                }
            }
        }

        return if (bestScore > 0) {
            TrackSearchHit(
                track = entry.track,
                relevanceScore = bestScore,
                matchSource = bestSource,
                parsedArtists = parsed,
            )
        } else {
            null
        }
    }

    private fun tokenMatchesEntry(entry: IndexedTrackEntry, token: String): Boolean {
        if (containsWordPrefix(entry.titleWords, token)) return true
        if (entry.allArtistWords.any { containsWordPrefix(it, token) }) return true
        if (containsWordPrefix(entry.albumWords, token)) return true
        if (containsWordPrefix(entry.genreWords, token)) return true
        if (!entry.isGenericAlbumArtist && containsWordPrefix(entry.albumArtistWords, token)) return true
        if (token.length >= 3) {
            if (entry.normTitle.contains(token) ||
                entry.normRawArtist.contains(token) ||
                entry.normAlbum.contains(token) ||
                entry.normGenre.contains(token)
            ) return true
        }
        return false
    }

    private fun scoreArtistCandidate(normArtist: String, words: List<String>, normQuery: String): Int {
        if (normQuery.isBlank()) return 0
        return when {
            normArtist == normQuery -> 1000
            normArtist.startsWith(normQuery) -> 800
            containsWordPrefix(words, normQuery) -> 600
            normArtist.contains(normQuery) -> 400
            else -> 0
        }
    }

    private fun sortIndexedHits(
        hits: List<ScoredIndexedHit>,
        sortOrder: TrackSortOrder,
        prioritizeRelevance: Boolean,
    ): List<TrackSearchHit> {
        val secondaryComparator: Comparator<ScoredIndexedHit> = when (sortOrder) {
            TrackSortOrder.TITLE_ASC -> compareBy { it.entry.normTitle }
            TrackSortOrder.TITLE_DESC -> compareByDescending { it.entry.normTitle }
            TrackSortOrder.ARTIST_ASC -> compareBy { it.entry.normDisplayArtist }
            TrackSortOrder.ALBUM_ASC -> compareBy { it.entry.normAlbum }
            TrackSortOrder.DURATION_DESC -> compareByDescending { it.entry.track.durationMs }
            TrackSortOrder.DATE_ADDED_DESC, TrackSortOrder.RECENTLY_ADDED -> compareByDescending { it.entry.track.dateAddedSec }
            TrackSortOrder.DATE_ADDED_ASC -> compareBy { it.entry.track.dateAddedSec }
            TrackSortOrder.MOST_PLAYED -> compareByDescending { it.entry.track.playCount }
            TrackSortOrder.LEAST_PLAYED -> compareBy { it.entry.track.playCount }
        }
        val sorted = if (prioritizeRelevance) {
            hits.sortedWith(
                compareByDescending<ScoredIndexedHit> { it.hit.relevanceScore }
                    .then(secondaryComparator)
            )
        } else {
            hits.sortedWith(secondaryComparator)
        }
        return sorted.map { it.hit }
    }
}
