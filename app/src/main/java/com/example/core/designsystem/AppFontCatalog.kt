package com.example.core.designsystem

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.domain.models.AppFontOption
import com.example.domain.models.FontCategory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized catalog and memory-cached resolver for MYATO MUSIC's offline
 * DaFont-inspired curated font library and user-imported TTF/OTF fonts.
 */
object AppFontCatalog {

    const val DEFAULT_FONT_ID = "default_myato"

    val builtInFonts: List<AppFontOption> = listOf(
        // Padrão Original
        AppFontOption(
            id = DEFAULT_FONT_ID,
            name = "MYATO Original",
            creator = "Rodrigo Fuenzalida & Tokotype",
            license = "SIL OFL 1.1",
            description = "Tipografia padrão original (Outfit + Plus Jakarta Sans)",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),

        // 1. Góticas (Inked God / Death Font inspirations)
        AppFontOption(
            id = "unifraktur_maguntia",
            name = "UnifrakturMaguntia",
            creator = "J. 'mach' Wust",
            license = "SIL OFL 1.1",
            description = "Gótica tradicional com traços pontiagudos e ornamentais",
            category = FontCategory.GOTICAS.label,
            fontCategory = FontCategory.GOTICAS,
            styleReference = "Estilo gótico ornamental (Inked God)",
        ),
        AppFontOption(
            id = "pirata_one",
            name = "Pirata One",
            creator = "Rodrigo Fuenzalida & Nicolás Massi",
            license = "SIL OFL 1.1",
            description = "Gótica sombria e expressiva com presença marcante",
            category = FontCategory.GOTICAS.label,
            fontCategory = FontCategory.GOTICAS,
            styleReference = "Estilo gótico sombrio (Death Font)",
        ),
        AppFontOption(
            id = "new_rocker",
            name = "New Rocker",
            creator = "Pablo Impallari",
            license = "SIL OFL 1.1",
            description = "Gótica pesada com ângulos agressivos e ornamentados",
            category = FontCategory.GOTICAS.label,
            fontCategory = FontCategory.GOTICAS,
            styleReference = "Estilo gótico pesado (Death Font)",
        ),

        // 2. Horror (Ghost Factory inspiration)
        AppFontOption(
            id = "creepster",
            name = "Creepster",
            creator = "Sideshow",
            license = "Apache 2.0",
            description = "Tipografia assustadora clássica com contornos sinistros",
            category = FontCategory.HORROR.label,
            fontCategory = FontCategory.HORROR,
            styleReference = "Estilo terror clássico (Ghost Factory)",
        ),
        AppFontOption(
            id = "nosifer",
            name = "Nosifer",
            creator = "Typomondo",
            license = "SIL OFL 1.1",
            description = "Letras escorrendo com estética de terror intenso",
            category = FontCategory.HORROR.label,
            fontCategory = FontCategory.HORROR,
            styleReference = "Estilo horror desgastado (Ghost Factory)",
        ),
        AppFontOption(
            id = "butcherman",
            name = "Butcherman",
            creator = "Typomondo",
            license = "SIL OFL 1.1",
            description = "Tipografia de terror áspera, rasgada e desgastada",
            category = FontCategory.HORROR.label,
            fontCategory = FontCategory.HORROR,
            styleReference = "Estilo horror pesado",
        ),

        // 3. Grunge (Dirty Stains / Finger Printed inspirations)
        AppFontOption(
            id = "rubik_wet_paint",
            name = "Rubik Wet Paint",
            creator = "Hubert & Fischer",
            license = "SIL OFL 1.1",
            description = "Textura manchada com efeito de tinta desgastada",
            category = FontCategory.GRUNGE.label,
            fontCategory = FontCategory.GRUNGE,
            styleReference = "Estilo grunge manchado (Dirty Stains)",
        ),
        AppFontOption(
            id = "rubik_glitch",
            name = "Rubik Glitch",
            creator = "Hubert & Fischer",
            license = "SIL OFL 1.1",
            description = "Aparência distorcida e riscada de interferência visual",
            category = FontCategory.GRUNGE.label,
            fontCategory = FontCategory.GRUNGE,
            styleReference = "Estilo arranhado e distorcido (Finger Printed)",
        ),
        AppFontOption(
            id = "syne_tactile",
            name = "Syne Tactile",
            creator = "Lucas Descroix & Bonjour Monde",
            license = "SIL OFL 1.1",
            description = "Contorno áspero e orgânico com textura artesanal",
            category = FontCategory.GRUNGE.label,
            fontCategory = FontCategory.GRUNGE,
            styleReference = "Estilo texturizado experimental",
        ),

        // 4. Vintage (War Is Over / Personal Services inspirations)
        AppFontOption(
            id = "black_ops_one",
            name = "Black Ops One",
            creator = "James Grieshaber",
            license = "SIL OFL 1.1",
            description = "Estêncil militar robusto e vintage de alto impacto",
            category = FontCategory.VINTAGE.label,
            fontCategory = FontCategory.VINTAGE,
            styleReference = "Estilo militar vintage (War Is Over)",
        ),
        AppFontOption(
            id = "special_elite",
            name = "Special Elite",
            creator = "Astigmatic",
            license = "Apache 2.0",
            description = "Máquina de escrever antiga com tinta desgastada",
            category = FontCategory.VINTAGE.label,
            fontCategory = FontCategory.VINTAGE,
            styleReference = "Estilo industrial desgastado (Personal Services)",
        ),
        AppFontOption(
            id = "rye",
            name = "Rye",
            creator = "Nicole Fally",
            license = "SIL OFL 1.1",
            description = "Cartaz retrô estilo xilogravura com ornamentos antigos",
            category = FontCategory.VINTAGE.label,
            fontCategory = FontCategory.VINTAGE,
            styleReference = "Estilo pôster retrô vintage",
        ),

        // 5. Rock e Metal (White On Black / Headliner No. 45 inspirations)
        AppFontOption(
            id = "metal_mania",
            name = "Metal Mania",
            creator = "Open Window",
            license = "SIL OFL 1.1",
            description = "Letras pontiagudas inspiradas em bandas de heavy metal",
            category = FontCategory.ROCK_METAL.label,
            fontCategory = FontCategory.ROCK_METAL,
            styleReference = "Estilo metal agressivo (White On Black)",
        ),
        AppFontOption(
            id = "anton",
            name = "Anton",
            creator = "Vernon Adams",
            license = "SIL OFL 1.1",
            description = "Condensada e ultra pesada para títulos de impacto",
            category = FontCategory.ROCK_METAL.label,
            fontCategory = FontCategory.ROCK_METAL,
            styleReference = "Estilo condensado pesado (Headliner No. 45)",
        ),
        AppFontOption(
            id = "oswald",
            name = "Oswald",
            creator = "Vernon Adams",
            license = "SIL OFL 1.1",
            description = "Condensada firme para manchetes musicais e pôsteres",
            category = FontCategory.ROCK_METAL.label,
            fontCategory = FontCategory.ROCK_METAL,
            styleReference = "Estilo manchete condensada (Headliner No. 45)",
        ),

        // 6. Urbanas (Urban Jungle inspiration)
        AppFontOption(
            id = "permanent_marker",
            name = "Permanent Marker",
            creator = "Font Diner",
            license = "Apache 2.0",
            description = "Traço de pincel atômico e tag urbana de rua",
            category = FontCategory.URBANAS.label,
            fontCategory = FontCategory.URBANAS,
            styleReference = "Estilo marcador street (Urban Jungle)",
        ),
        AppFontOption(
            id = "bungee",
            name = "Bungee",
            creator = "David Jonathan Ross",
            license = "SIL OFL 1.1",
            description = "Letras urbanas em bloco inspiradas em letreiros de rua",
            category = FontCategory.URBANAS.label,
            fontCategory = FontCategory.URBANAS,
            styleReference = "Estilo urbano geométrico (Urban Jungle)",
        ),
        AppFontOption(
            id = "rubik_spray_paint",
            name = "Rubik Spray Paint",
            creator = "Hubert & Fischer",
            license = "SIL OFL 1.1",
            description = "Efeito de grafite com spray em muro urbano",
            category = FontCategory.URBANAS.label,
            fontCategory = FontCategory.URBANAS,
            styleReference = "Estilo grafite spray underground",
        ),

        // 7. Medievais
        AppFontOption(
            id = "cinzel_decorative",
            name = "Cinzel Decorative",
            creator = "Natanael Gama",
            license = "SIL OFL 1.1",
            description = "Letras épicas com floreios inspirados em inscrições antigas",
            category = FontCategory.MEDIEVAIS.label,
            fontCategory = FontCategory.MEDIEVAIS,
        ),
        AppFontOption(
            id = "almendra_display",
            name = "Almendra Display",
            creator = "Ana Sanfilippo",
            license = "SIL OFL 1.1",
            description = "Caligrafia medieval expressiva de manuscritos antigos",
            category = FontCategory.MEDIEVAIS.label,
            fontCategory = FontCategory.MEDIEVAIS,
        ),
        AppFontOption(
            id = "medievalsharp",
            name = "MedievalSharp",
            creator = "Wojciech Kalinowski",
            license = "SIL OFL 1.1",
            description = "Escrita medieval esculpida com cantos angulares",
            category = FontCategory.MEDIEVAIS.label,
            fontCategory = FontCategory.MEDIEVAIS,
        ),

        // 8. Manuscritas
        AppFontOption(
            id = "caveat",
            name = "Caveat",
            creator = "Pablo Impallari",
            license = "SIL OFL 1.1",
            description = "Letra cursiva natural feita à mão com leveza",
            category = FontCategory.MANUSCRITAS.label,
            fontCategory = FontCategory.MANUSCRITAS,
        ),
        AppFontOption(
            id = "pacifico",
            name = "Pacifico",
            creator = "Vernon Adams",
            license = "SIL OFL 1.1",
            description = "Escrita à mão estilo pincel fluido e retrô",
            category = FontCategory.MANUSCRITAS.label,
            fontCategory = FontCategory.MANUSCRITAS,
        ),
        AppFontOption(
            id = "dancing_script",
            name = "Dancing Script",
            creator = "Impallari Type",
            license = "SIL OFL 1.1",
            description = "Caligrafia manuscrita fluida e musical",
            category = FontCategory.MANUSCRITAS.label,
            fontCategory = FontCategory.MANUSCRITAS,
        ),

        // 9. Decorativas (DeFonte / Prevalent inspirations)
        AppFontOption(
            id = "righteous",
            name = "Righteous",
            creator = "Astigmatic",
            license = "SIL OFL 1.1",
            description = "Decorativa arredondada e geométrica com estilo único",
            category = FontCategory.DECORATIVAS.label,
            fontCategory = FontCategory.DECORATIVAS,
            styleReference = "Estilo decorativo arredondado (DeFonte)",
        ),
        AppFontOption(
            id = "monoton",
            name = "Monoton",
            creator = "Vernon Adams",
            license = "SIL OFL 1.1",
            description = "Estilo neon multilinha artístico para grandes títulos",
            category = FontCategory.DECORATIVAS.label,
            fontCategory = FontCategory.DECORATIVAS,
            styleReference = "Estilo artístico de linhas paralelas",
        ),
        AppFontOption(
            id = "syne",
            name = "Syne",
            creator = "Bonjour Monde",
            license = "SIL OFL 1.1",
            description = "Decorativa larga e contemporânea com personalidade",
            category = FontCategory.DECORATIVAS.label,
            fontCategory = FontCategory.DECORATIVAS,
            styleReference = "Estilo decorativo editorial (Prevalent)",
        ),

        // 10. Minimalistas
        AppFontOption(
            id = "inter",
            name = "Inter",
            creator = "Rasmus Andersson",
            license = "SIL OFL 1.1",
            description = "Moderna, limpa e projetada para leitura em interfaces",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "roboto",
            name = "Roboto",
            creator = "Christian Robertson",
            license = "Apache 2.0",
            description = "Padrão moderno, limpo e altamente legível do Android",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "plus_jakarta_sans",
            name = "Plus Jakarta Sans",
            creator = "Tokotype",
            license = "SIL OFL 1.1",
            description = "Geométrica humanista equilibrada e moderna",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "poppins",
            name = "Poppins",
            creator = "Indian Type Foundry",
            license = "SIL OFL 1.1",
            description = "Geométrica limpa com formas suaves e equilibradas",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "montserrat",
            name = "Montserrat",
            creator = "Julieta Ulanovsky",
            license = "SIL OFL 1.1",
            description = "Urbana, moderna e excelente para cabeçalhos limpos",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "dm_sans",
            name = "DM Sans",
            creator = "Colophon Foundry",
            license = "SIL OFL 1.1",
            description = "Minimalista de baixo contraste para players modernos",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "manrope",
            name = "Manrope",
            creator = "Mikhail Sharanda",
            license = "SIL OFL 1.1",
            description = "Minimalista contemporânea de acabamento premium",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "urbanist",
            name = "Urbanist",
            creator = "Corey Hu",
            license = "SIL OFL 1.1",
            description = "Minimalista geométrica com estética limpa",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "nunito_sans",
            name = "Nunito Sans",
            creator = "Vernon Adams",
            license = "SIL OFL 1.1",
            description = "Confortável, balanceada e fácil de ler em listas longas",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "lato",
            name = "Lato",
            creator = "Łukasz Dziedzic",
            license = "SIL OFL 1.1",
            description = "Humanista limpa com ótima clareza em menus",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "open_sans",
            name = "Open Sans",
            creator = "Steve Matteson",
            license = "SIL OFL 1.1",
            description = "Neutra e aberta com excelente legibilidade",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "rubik",
            name = "Rubik",
            creator = "Hubert & Fischer",
            license = "SIL OFL 1.1",
            description = "Moderna com cantos sutilmente arredondados",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),
        AppFontOption(
            id = "outfit",
            name = "Outfit",
            creator = "Rodrigo Fuenzalida",
            license = "SIL OFL 1.1",
            description = "Geométrica contemporânea usada nos títulos MYATO",
            category = FontCategory.MINIMALISTAS.label,
            fontCategory = FontCategory.MINIMALISTAS,
        ),

        // 11. Futuristas
        AppFontOption(
            id = "orbitron",
            name = "Orbitron",
            creator = "Matt McInerney",
            license = "SIL OFL 1.1",
            description = "Geométrica sci-fi inspirada em interfaces digitais",
            category = FontCategory.FUTURISTAS.label,
            fontCategory = FontCategory.FUTURISTAS,
        ),
        AppFontOption(
            id = "audiowide",
            name = "Audiowide",
            creator = "Astigmatic",
            license = "SIL OFL 1.1",
            description = "Tecnológica e larga com estética de sintetizador",
            category = FontCategory.FUTURISTAS.label,
            fontCategory = FontCategory.FUTURISTAS,
        ),
        AppFontOption(
            id = "rajdhani",
            name = "Rajdhani",
            creator = "Indian Type Foundry",
            license = "SIL OFL 1.1",
            description = "Técnica e quadrada com excelente leitura numérica",
            category = FontCategory.FUTURISTAS.label,
            fontCategory = FontCategory.FUTURISTAS,
        ),
        AppFontOption(
            id = "jetbrains_mono",
            name = "JetBrains Mono",
            creator = "JetBrains",
            license = "SIL OFL 1.1",
            description = "Monoespaçada de precisão técnica e leitura nítida",
            category = FontCategory.FUTURISTAS.label,
            fontCategory = FontCategory.FUTURISTAS,
        ),

        // 12. Clássicas (Aquifer / Roman New Times inspirations)
        AppFontOption(
            id = "cinzel",
            name = "Cinzel",
            creator = "Natanael Gama",
            license = "SIL OFL 1.1",
            description = "Proporções romanas clássicas com elegância atemporal",
            category = FontCategory.CLASSICAS.label,
            fontCategory = FontCategory.CLASSICAS,
            styleReference = "Estilo romano clássico (Roman New Times)",
        ),
        AppFontOption(
            id = "playfair_display",
            name = "Playfair Display",
            creator = "Claus Eggers Sørensen",
            license = "SIL OFL 1.1",
            description = "Serifada de alto contraste refinada para títulos elegantes",
            category = FontCategory.CLASSICAS.label,
            fontCategory = FontCategory.CLASSICAS,
            styleReference = "Estilo clássico elegante (Aquifer)",
        ),
        AppFontOption(
            id = "cormorant_garamond",
            name = "Cormorant Garamond",
            creator = "Christian Thalmann",
            license = "SIL OFL 1.1",
            description = "Serifada editorial clássica com traços delicados",
            category = FontCategory.CLASSICAS.label,
            fontCategory = FontCategory.CLASSICAS,
            styleReference = "Estilo clássico refinado (Aquifer)",
        ),
        AppFontOption(
            id = "eb_garamond",
            name = "EB Garamond",
            creator = "Georg Duffner",
            license = "SIL OFL 1.1",
            description = "Tipografia tradicional literária inspirada no século XVI",
            category = FontCategory.CLASSICAS.label,
            fontCategory = FontCategory.CLASSICAS,
            styleReference = "Estilo clássico tradicional",
        ),
    )

    private val customFontCache = ConcurrentHashMap<String, FontFamily>()
    private val typographyCache = ConcurrentHashMap<String, Typography>()

    fun allFonts(customFonts: List<AppFontOption>): List<AppFontOption> = customFonts + builtInFonts

    fun findFontOption(fontId: String, customFonts: List<AppFontOption>): AppFontOption {
        return customFonts.firstOrNull { it.id == fontId }
            ?: builtInFonts.firstOrNull { it.id == fontId }
            ?: builtInFonts.first()
    }

    fun resolveFontFamily(fontId: String, customFonts: List<AppFontOption>): FontFamily {
        return when (fontId) {
            DEFAULT_FONT_ID -> PlusJakartaFontFamily
            "unifraktur_maguntia" -> UnifrakturMaguntiaFontFamily
            "pirata_one" -> PirataOneFontFamily
            "new_rocker" -> NewRockerFontFamily
            "creepster" -> CreepsterFontFamily
            "nosifer" -> NosiferFontFamily
            "butcherman" -> ButchermanFontFamily
            "rubik_wet_paint" -> RubikWetPaintFontFamily
            "rubik_glitch" -> RubikGlitchFontFamily
            "syne_tactile" -> SyneTactileFontFamily
            "black_ops_one" -> BlackOpsOneFontFamily
            "special_elite" -> SpecialEliteFontFamily
            "rye" -> RyeFontFamily
            "metal_mania" -> MetalManiaFontFamily
            "anton" -> AntonFontFamily
            "oswald" -> OswaldFontFamily
            "permanent_marker" -> PermanentMarkerFontFamily
            "bungee" -> BungeeFontFamily
            "rubik_spray_paint" -> RubikSprayPaintFontFamily
            "cinzel_decorative" -> CinzelDecorativeFontFamily
            "almendra_display" -> AlmendraDisplayFontFamily
            "medievalsharp" -> MedievalSharpFontFamily
            "caveat" -> CaveatFontFamily
            "pacifico" -> PacificoFontFamily
            "dancing_script" -> DancingScriptFontFamily
            "righteous" -> RighteousFontFamily
            "monoton" -> MonotonFontFamily
            "syne" -> SyneFontFamily
            "inter" -> InterFontFamily
            "roboto" -> RobotoFontFamily
            "plus_jakarta_sans" -> PlusJakartaFontFamily
            "poppins" -> PoppinsFontFamily
            "montserrat" -> MontserratFontFamily
            "dm_sans" -> DmSansFontFamily
            "manrope" -> ManropeFontFamily
            "urbanist" -> UrbanistFontFamily
            "nunito_sans" -> NunitoSansFontFamily
            "lato" -> LatoFontFamily
            "open_sans" -> OpenSansFontFamily
            "rubik" -> RubikFontFamily
            "outfit" -> OutfitFontFamily
            "orbitron" -> OrbitronFontFamily
            "audiowide" -> AudiowideFontFamily
            "rajdhani" -> RajdhaniFontFamily
            "jetbrains_mono" -> JetBrainsMonoFontFamily
            "cinzel" -> CinzelFontFamily
            "playfair_display" -> PlayfairDisplayFontFamily
            "cormorant_garamond" -> CormorantGaramondFontFamily
            "eb_garamond" -> EbGaramondFontFamily
            else -> {
                val custom = customFonts.firstOrNull { it.id == fontId }
                resolveCustomFontFamily(custom) ?: PlusJakartaFontFamily
            }
        }
    }

    fun resolveDisplayFamilyForId(fontId: String, customFonts: List<AppFontOption>): FontFamily {
        return if (fontId == DEFAULT_FONT_ID) OutfitFontFamily else resolveFontFamily(fontId, customFonts)
    }

    fun resolveBodyFamilyForId(fontId: String, customFonts: List<AppFontOption>): FontFamily {
        return if (fontId == DEFAULT_FONT_ID) PlusJakartaFontFamily else resolveFontFamily(fontId, customFonts)
    }

    fun resolveMonoFamilyForId(fontId: String, customFonts: List<AppFontOption>): FontFamily {
        return if (fontId == DEFAULT_FONT_ID) JetBrainsMonoFontFamily else resolveFontFamily(fontId, customFonts)
    }

    private fun resolveCustomFontFamily(custom: AppFontOption?): FontFamily? {
        val path = custom?.customFilePath ?: return null
        val file = File(path)
        if (!file.exists() || !file.isFile || !file.canRead() || file.length() <= 0L) {
            customFontCache.remove(custom.id)
            return null
        }
        return customFontCache.getOrPut(custom.id) {
            runCatching {
                val parsed = Typeface.createFromFile(file)
                if (parsed == null) {
                    PlusJakartaFontFamily
                } else {
                    FontFamily(
                        Font(file, FontWeight.Light),
                        Font(file, FontWeight.Normal),
                        Font(file, FontWeight.Medium),
                        Font(file, FontWeight.SemiBold),
                        Font(file, FontWeight.Bold),
                    )
                }
            }.getOrElse { PlusJakartaFontFamily }
        }
    }

    /**
     * Resolves Material 3 Typography combining the General interface font
     * and the Decorative font (titles, song names, screen headers).
     */
    fun resolveDualTypography(
        generalFontId: String,
        decorativeFontId: String,
        customFonts: List<AppFontOption>,
    ): Typography {
        if (generalFontId == DEFAULT_FONT_ID && decorativeFontId == DEFAULT_FONT_ID) {
            return MyatoTypography
        }
        val genCustom = customFonts.firstOrNull { it.id == generalFontId }
        val decCustom = customFonts.firstOrNull { it.id == decorativeFontId }
        val cacheKey = "${generalFontId}:${genCustom?.customFilePath.orEmpty()}|${decorativeFontId}:${decCustom?.customFilePath.orEmpty()}"

        return typographyCache.getOrPut(cacheKey) {
            val displayFamily = resolveDisplayFamilyForId(decorativeFontId, customFonts)
            val bodyFamily = resolveBodyFamilyForId(generalFontId, customFonts)
            val monoFamily = resolveMonoFamilyForId(generalFontId, customFonts)
            createAppTypography(
                displayFamily = displayFamily,
                bodyFamily = bodyFamily,
                monoFamily = monoFamily,
            )
        }
    }

    /**
     * Single-font preview helper (renders all typography slots using the target font).
     */
    fun resolveAppTypography(fontId: String, customFonts: List<AppFontOption>): Typography {
        return resolveDualTypography(
            generalFontId = fontId,
            decorativeFontId = fontId,
            customFonts = customFonts,
        )
    }

    fun invalidateCustomFontCache(fontId: String) {
        customFontCache.remove(fontId)
        typographyCache.keys
            .filter { it.contains(fontId) }
            .forEach { typographyCache.remove(it) }
    }
}
