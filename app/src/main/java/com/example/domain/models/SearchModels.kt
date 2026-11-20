package com.example.domain.models

enum class SearchFilterMode(val label: String) {
    ALL("Todos os resultados"),
    EXACT_ARTIST("Artista exato"),
    ARTIST_AND_COLLABS("Artista e colaborações"),
}

enum class SearchMatchSource(val label: String) {
    EXACT_ARTIST("Artista"),
    COLLAB_ARTIST("Participação"),
    ALBUM_ARTIST("Artista do álbum"),
    TITLE("Título"),
    ALBUM("Álbum"),
    GENRE("Gênero"),
}

data class ParsedTrackArtists(
    val displayArtist: String,
    val primaryArtists: List<String>,
    val featuredArtists: List<String>,
    val allArtists: List<String>,
    val isCollaboration: Boolean,
    val isUnknownArtist: Boolean,
)

data class TrackSearchHit(
    val track: MusicTrack,
    val relevanceScore: Int,
    val matchSource: SearchMatchSource,
    val parsedArtists: ParsedTrackArtists,
)

data class ArtistFilterCandidate(
    val name: String,
    val soloTrackCount: Int,
    val collabTrackCount: Int,
    val totalTrackCount: Int,
)

data class SmartSearchResult(
    val hits: List<TrackSearchHit> = emptyList(),
    val matchingArtists: List<ArtistFilterCandidate> = emptyList(),
    val activeArtistFilter: String? = null,
    val filterMode: SearchFilterMode = SearchFilterMode.ALL,
)
