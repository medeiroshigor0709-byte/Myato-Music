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
 * Guarantees strict artist attribution based on real track metadata (Artist, Album Artist,
 * Title feat. credits, Album, Genre), normalizes case/accents/whitespace, separates solo
 * tracks from collaborations, and never substitutes similar genres or unrelated artists.
 */
object MusicSearchEngine {

    const val UNKNOWN_ARTIST_LABEL = "Artista desconhecido"

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

    fun normalizeText(raw: String): String {
        val collapsed = raw.trim().replace(multiSpaceRegex, " ")
        if (collapsed.isEmpty()) return ""
        val decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
        return combiningMarksRegex.replace(decomposed, "").lowercase(Locale.ROOT)
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

    fun matchesArtist(
        track: MusicTrack,
        targetArtist: String,
        includeCollaborations: Boolean,
    ): Boolean {
        val normTarget = normalizeText(targetArtist)
        if (normTarget.isBlank()) return false

        val parsed = parseTrackArtists(track)
        if (parsed.isUnknownArtist) {
            return normTarget == normalizeText(UNKNOWN_ARTIST_LABEL)
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
        return tracks
            .filter { matchesArtist(it, artistName, includeCollaborations) }
            .distinctBy { it.id }
    }

    fun extractArtistCandidates(
        tracks: List<MusicTrack>,
        query: String = "",
    ): List<ArtistFilterCandidate> {
        val normQuery = normalizeText(query)

        data class Acc(
            var displayName: String,
            val soloIds: MutableSet<Long> = linkedSetOf(),
            val collabIds: MutableSet<Long> = linkedSetOf(),
        )

        val byNorm = linkedMapOf<String, Acc>()

        tracks.forEach { track ->
            val parsed = parseTrackArtists(track)
            if (parsed.isUnknownArtist) return@forEach

            if (!parsed.isCollaboration && parsed.primaryArtists.size == 1) {
                val name = parsed.primaryArtists.first()
                val key = normalizeText(name)
                val acc = byNorm.getOrPut(key) { Acc(displayName = name) }
                acc.soloIds.add(track.id)
            } else {
                parsed.allArtists.forEach { artistName ->
                    val key = normalizeText(artistName)
                    val acc = byNorm.getOrPut(key) { Acc(displayName = artistName) }
                    acc.collabIds.add(track.id)
                }
            }
        }

        val candidates = byNorm.entries.mapNotNull { (normName, acc) ->
            val totalIds = (acc.soloIds + acc.collabIds).size
            if (normQuery.isNotBlank()) {
                val matches = normName == normQuery ||
                    normName.startsWith(normQuery) ||
                    containsWordPrefix(normName, normQuery) ||
                    (normQuery.length >= 3 && normName.contains(normQuery))
                if (!matches) return@mapNotNull null
            }
            Triple(
                normName,
                scoreArtistCandidate(normName, normQuery),
                ArtistFilterCandidate(
                    name = acc.displayName,
                    soloTrackCount = acc.soloIds.size,
                    collabTrackCount = acc.collabIds.size,
                    totalTrackCount = totalIds,
                )
            )
        }

        return candidates
            .sortedWith(
                compareByDescending<Triple<String, Int, ArtistFilterCandidate>> { it.second }
                    .thenByDescending { it.third.soloTrackCount }
                    .thenByDescending { it.third.totalTrackCount }
                    .thenBy { it.first }
            )
            .map { it.third }
    }

    fun searchTracks(
        tracks: List<MusicTrack>,
        query: String,
        filterMode: SearchFilterMode = SearchFilterMode.ALL,
        selectedArtist: String? = null,
        sortOrder: TrackSortOrder = TrackSortOrder.TITLE_ASC,
    ): SmartSearchResult {
        val normQuery = normalizeText(query)
        val matchingArtists = extractArtistCandidates(tracks, query)

        val resolvedArtistFilter: String? = when {
            !selectedArtist.isNullOrBlank() -> selectedArtist.trim()
            filterMode != SearchFilterMode.ALL && normQuery.isNotBlank() -> {
                matchingArtists.firstOrNull { normalizeText(it.name) == normQuery }?.name
                    ?: query.trim()
            }
            else -> null
        }

        val rawHits: List<TrackSearchHit> = when (filterMode) {
            SearchFilterMode.EXACT_ARTIST -> {
                if (resolvedArtistFilter == null) {
                    tracks.map { track ->
                        val parsed = parseTrackArtists(track)
                        TrackSearchHit(track, 0, SearchMatchSource.EXACT_ARTIST, parsed)
                    }
                } else {
                    tracks.mapNotNull { track ->
                        if (!matchesArtist(track, resolvedArtistFilter, includeCollaborations = false)) {
                            return@mapNotNull null
                        }
                        TrackSearchHit(
                            track = track,
                            relevanceScore = 1000,
                            matchSource = SearchMatchSource.EXACT_ARTIST,
                            parsedArtists = parseTrackArtists(track),
                        )
                    }
                }
            }

            SearchFilterMode.ARTIST_AND_COLLABS -> {
                if (resolvedArtistFilter == null) {
                    tracks.map { track ->
                        val parsed = parseTrackArtists(track)
                        val src = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                        TrackSearchHit(track, 0, src, parsed)
                    }
                } else {
                    tracks.mapNotNull { track ->
                        if (!matchesArtist(track, resolvedArtistFilter, includeCollaborations = true)) {
                            return@mapNotNull null
                        }
                        val parsed = parseTrackArtists(track)
                        val isSoloExact = !parsed.isCollaboration &&
                            parsed.primaryArtists.size == 1 &&
                            normalizeText(parsed.primaryArtists.first()) == normalizeText(resolvedArtistFilter)
                        TrackSearchHit(
                            track = track,
                            relevanceScore = if (isSoloExact) 1000 else 850,
                            matchSource = if (isSoloExact) SearchMatchSource.EXACT_ARTIST else SearchMatchSource.COLLAB_ARTIST,
                            parsedArtists = parsed,
                        )
                    }
                }
            }

            SearchFilterMode.ALL -> {
                if (resolvedArtistFilter != null && normQuery.isBlank()) {
                    // Explicit artist chip selected without extra query text
                    tracks.mapNotNull { track ->
                        if (!matchesArtist(track, resolvedArtistFilter, includeCollaborations = true)) {
                            return@mapNotNull null
                        }
                        val parsed = parseTrackArtists(track)
                        val isSoloExact = !parsed.isCollaboration &&
                            parsed.primaryArtists.size == 1 &&
                            normalizeText(parsed.primaryArtists.first()) == normalizeText(resolvedArtistFilter)
                        TrackSearchHit(
                            track = track,
                            relevanceScore = if (isSoloExact) 1000 else 850,
                            matchSource = if (isSoloExact) SearchMatchSource.EXACT_ARTIST else SearchMatchSource.COLLAB_ARTIST,
                            parsedArtists = parsed,
                        )
                    }
                } else if (normQuery.isBlank()) {
                    tracks.map { track ->
                        TrackSearchHit(track, 0, SearchMatchSource.TITLE, parseTrackArtists(track))
                    }
                } else {
                    tracks.mapNotNull { track ->
                        if (resolvedArtistFilter != null && !matchesArtist(track, resolvedArtistFilter, includeCollaborations = true)) {
                            return@mapNotNull null
                        }
                        scoreTrackAgainstQuery(track, normQuery)
                    }
                }
            }
        }

        val sortedHits = sortSearchHits(
            hits = rawHits,
            sortOrder = sortOrder,
            prioritizeRelevance = normQuery.isNotBlank() || resolvedArtistFilter != null,
        )

        return SmartSearchResult(
            hits = sortedHits,
            matchingArtists = matchingArtists,
            activeArtistFilter = resolvedArtistFilter,
            filterMode = filterMode,
        )
    }

    private fun scoreTrackAgainstQuery(track: MusicTrack, normQuery: String): TrackSearchHit? {
        val parsed = parseTrackArtists(track)
        val normTitle = normalizeText(track.title)
        val normAlbum = normalizeText(track.album)
        val normAlbumArtist = normalizeText(track.albumArtist)
        val normGenre = normalizeText(track.genre)
        val normRawArtist = normalizeText(track.artist)

        var bestScore = 0
        var bestSource = SearchMatchSource.TITLE

        // 1. Artist exact / primary / collaboration matching (highest priority)
        if (!parsed.isUnknownArtist) {
            val isSoloExact = !parsed.isCollaboration &&
                parsed.primaryArtists.size == 1 &&
                normalizeText(parsed.primaryArtists.first()) == normQuery

            val isExactPrimaryOrCollab = parsed.allArtists.any { normalizeText(it) == normQuery }

            when {
                isSoloExact -> {
                    bestScore = 1000
                    bestSource = SearchMatchSource.EXACT_ARTIST
                }
                isExactPrimaryOrCollab -> {
                    bestScore = 850
                    bestSource = SearchMatchSource.COLLAB_ARTIST
                }
                parsed.primaryArtists.any { normalizeText(it).startsWith(normQuery) } -> {
                    bestScore = 720
                    bestSource = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                }
                parsed.featuredArtists.any { normalizeText(it).startsWith(normQuery) } -> {
                    bestScore = 680
                    bestSource = SearchMatchSource.COLLAB_ARTIST
                }
                parsed.allArtists.any { containsWordPrefix(normalizeText(it), normQuery) } -> {
                    bestScore = 640
                    bestSource = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                }
                normQuery.length >= 3 && normRawArtist.contains(normQuery) -> {
                    bestScore = 590
                    bestSource = if (parsed.isCollaboration) SearchMatchSource.COLLAB_ARTIST else SearchMatchSource.EXACT_ARTIST
                }
            }
        } else if (normalizeText(UNKNOWN_ARTIST_LABEL).contains(normQuery) && normQuery.length >= 3) {
            bestScore = 520
            bestSource = SearchMatchSource.EXACT_ARTIST
        }

        // 2. Album artist metadata
        if (bestScore < 560 && normAlbumArtist.isNotBlank() && !isGenericCompilationAlbumArtist(track.albumArtist)) {
            val albumArtistScore = when {
                normAlbumArtist == normQuery -> 560
                normAlbumArtist.startsWith(normQuery) -> 520
                containsWordPrefix(normAlbumArtist, normQuery) -> 490
                normQuery.length >= 3 && normAlbumArtist.contains(normQuery) -> 460
                else -> 0
            }
            if (albumArtistScore > bestScore) {
                bestScore = albumArtistScore
                bestSource = SearchMatchSource.ALBUM_ARTIST
            }
        }

        // 3. Track Title metadata
        if (bestScore < 450 && normTitle.isNotBlank()) {
            val titleScore = when {
                normTitle == normQuery -> 450
                normTitle.startsWith(normQuery) -> 410
                containsWordPrefix(normTitle, normQuery) -> 370
                normQuery.length >= 3 && normTitle.contains(normQuery) -> 320
                else -> 0
            }
            if (titleScore > bestScore) {
                bestScore = titleScore
                bestSource = SearchMatchSource.TITLE
            }
        }

        // 4. Album metadata
        if (bestScore < 250 && normAlbum.isNotBlank()) {
            val albumScore = when {
                normAlbum == normQuery -> 250
                normAlbum.startsWith(normQuery) -> 220
                containsWordPrefix(normAlbum, normQuery) -> 190
                normQuery.length >= 3 && normAlbum.contains(normQuery) -> 160
                else -> 0
            }
            if (albumScore > bestScore) {
                bestScore = albumScore
                bestSource = SearchMatchSource.ALBUM
            }
        }

        // 5. Genre metadata
        if (bestScore < 130 && normGenre.isNotBlank()) {
            val genreScore = when {
                normGenre == normQuery -> 130
                normGenre.startsWith(normQuery) -> 110
                containsWordPrefix(normGenre, normQuery) -> 95
                normQuery.length >= 3 && normGenre.contains(normQuery) -> 80
                else -> 0
            }
            if (genreScore > bestScore) {
                bestScore = genreScore
                bestSource = SearchMatchSource.GENRE
            }
        }

        return if (bestScore > 0) {
            TrackSearchHit(
                track = track,
                relevanceScore = bestScore,
                matchSource = bestSource,
                parsedArtists = parsed,
            )
        } else {
            null
        }
    }

    private fun scoreArtistCandidate(normArtist: String, normQuery: String): Int {
        if (normQuery.isBlank()) return 0
        return when {
            normArtist == normQuery -> 1000
            normArtist.startsWith(normQuery) -> 800
            containsWordPrefix(normArtist, normQuery) -> 600
            normArtist.contains(normQuery) -> 400
            else -> 0
        }
    }

    private fun containsWordPrefix(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        val words = haystack.split(' ', '-', '_', '(', ')', '[', ']', '.', '/', '&')
        return words.any { it.startsWith(needle) }
    }

    private fun sortSearchHits(
        hits: List<TrackSearchHit>,
        sortOrder: TrackSortOrder,
        prioritizeRelevance: Boolean,
    ): List<TrackSearchHit> {
        val secondaryComparator: Comparator<TrackSearchHit> = when (sortOrder) {
            TrackSortOrder.TITLE_ASC -> compareBy { normalizeText(it.track.title) }
            TrackSortOrder.TITLE_DESC -> compareByDescending { normalizeText(it.track.title) }
            TrackSortOrder.ARTIST_ASC -> compareBy { normalizeText(it.parsedArtists.displayArtist) }
            TrackSortOrder.ALBUM_ASC -> compareBy { normalizeText(it.track.album) }
            TrackSortOrder.DURATION_DESC -> compareByDescending { it.track.durationMs }
            TrackSortOrder.DATE_ADDED_DESC, TrackSortOrder.RECENTLY_ADDED -> compareByDescending { it.track.dateAddedSec }
            TrackSortOrder.DATE_ADDED_ASC -> compareBy { it.track.dateAddedSec }
            TrackSortOrder.MOST_PLAYED -> compareByDescending { it.track.playCount }
            TrackSortOrder.LEAST_PLAYED -> compareBy { it.track.playCount }
        }
        return if (prioritizeRelevance) {
            hits.sortedWith(
                compareByDescending<TrackSearchHit> { it.relevanceScore }
                    .then(secondaryComparator)
            )
        } else {
            hits.sortedWith(secondaryComparator)
        }
    }
}
