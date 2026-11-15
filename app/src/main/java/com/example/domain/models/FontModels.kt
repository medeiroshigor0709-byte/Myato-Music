package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class FontCategory(
    val label: String,
    val description: String,
) {
    GOTICAS("Góticas", "Letras medievais, sombrias e ornamentadas"),
    HORROR("Horror", "Fontes assustadoras e com aparência desgastada"),
    GRUNGE("Grunge", "Fontes com textura de sujeira, arranhões e desgaste"),
    VINTAGE("Vintage", "Tipografias retrô e antigas"),
    ROCK_METAL("Rock e Metal", "Fontes pesadas, agressivas e impactantes"),
    URBANAS("Urbanas", "Estilos de grafite, street e underground"),
    MEDIEVAIS("Medievais", "Fontes inspiradas em manuscritos antigos"),
    MANUSCRITAS("Manuscritas", "Letras que imitam escrita à mão"),
    DECORATIVAS("Decorativas", "Estilos criativos e diferentes"),
    MINIMALISTAS("Minimalistas", "Fontes modernas e limpas"),
    FUTURISTAS("Futuristas", "Letras tecnológicas e geométricas"),
    CLASSICAS("Clássicas", "Fontes tradicionais e elegantes"),
}

enum class FontApplyTarget {
    GENERAL,
    DECORATIVE,
    BOTH,
}

enum class FontSortOption(val label: String) {
    CATALOG("Catálogo"),
    NAME_ASC("A–Z"),
    NAME_DESC("Z–A"),
}

enum class FontInstallFilter(val label: String) {
    ALL("Todas"),
    BUILT_IN("Nativas"),
    IMPORTED("Importadas"),
}

@Serializable
data class AppFontOption(
    val id: String,
    val name: String,
    val creator: String = "MYATO Studio",
    val license: String = "SIL OFL 1.1",
    val description: String,
    val category: String,
    val fontCategory: FontCategory = FontCategory.MINIMALISTAS,
    val styleReference: String = "",
    val isCustom: Boolean = false,
    val customFilePath: String? = null,
    val fileFormat: String = "APP",
    val sizeBytes: Long = 0L,
    val addedAtMs: Long = 0L,
)

sealed interface FontImportResult {
    data class Success(val font: AppFontOption) : FontImportResult
    data class Error(val message: String) : FontImportResult
}
