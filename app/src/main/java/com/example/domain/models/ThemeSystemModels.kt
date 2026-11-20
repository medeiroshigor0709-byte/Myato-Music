package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeCategory(
    val displayName: String,
    val subtitle: String,
) {
    KAWAII_CATS("Kawaii & Cats", "Gatinhos, corações e tons fofos"),
    ANIME_JAPAN("Anime & Japão", "Sakura, chuva urbana e pôr do sol"),
    UNIVERSE_SPACE("Universo & Espaço", "Nebulosas, saturno, auroras e estrelas"),
    GAMER_PIXEL("Gamer & Pixel", "Pixel art, arcade neon e retrô 8-bit"),
    CUTE_AESTHETIC("Cute & Aesthetic", "Laços, nuvens, lavanda e tons pastel"),
    DARK_ELEGANCE("Dark & Elegance", "Ouro, esmeralda, prata e luxo sombrio"),
    NATURE_RELAX("Nature & Relax", "Florestas, oceano, jardim zen e montanhas"),
    NEON_FUTURISM("Neon & Futurism", "Synthwave, holográfico e ciber-luz"),
    MINIMAL_MODERN("Minimal & Modern", "Limpo, monocromático e sofisticado"),
    MYATO_EXCLUSIVES("Exclusivos Myato", "Identidade oficial e edições especiais Myato"),
    CUSTOM("Personalizados", "Temas criados no Studio e importados");

    companion object {
        fun fromName(raw: String?): ThemeCategory {
            return entries.firstOrNull { it.name == raw || it.displayName == raw } ?: MYATO_EXCLUSIVES
        }
    }
}

@Serializable
enum class ThemeAnimationType(val label: String) {
    NONE("Sem partículas"),
    HEARTS("Corações flutuantes"),
    MOONS_AND_STARS("Luas e estrelas"),
    PAWS_AND_SPARKLES("Patinhas e brilhos"),
    SNOWFLAKES("Flocos de neve"),
    COSMIC_NEBULA("Nebulosa estelar"),
    SAKURA_PETALS("Pétalas de cerejeira"),
    CYBER_GRID("Grade cibernética"),
    RAIN_DROPS("Chuva noturna"),
    MAGIC_STARS("Partículas mágicas"),
    SUNSET_GLOW("Brilho do pôr do sol"),
    GALAXY_SWIRL("Espiral galáctica"),
    STARFIELD("Campo de estrelas"),
    SATURN_RINGS("Anéis planetários"),
    AURORA_LIGHTS("Luzes da aurora"),
    MOONLIGHT_DRIFT("Reflexos lunares"),
    PIXEL_BLOCKS("Blocos pixelados"),
    ARCADE_SCANLINES("Linhas de fliperama"),
    PIXEL_LEAVES("Folhas 8-bit"),
    CYBER_CIRCUIT("Pulsos de circuito"),
    RETRO_WAVE("Ondas retrô"),
    RIBBONS("Laços e brilhos suaves"),
    FLOATING_CLOUDS("Nuvens em movimento"),
    LAVENDER_MIST("Brisa de lavanda"),
    CREAM_BOKEH("Bokeh acolhedor"),
    PASTEL_PRISM("Prismas arco-íris"),
    GOLD_SHIMMER("Poeira dourada"),
    BLOOD_ECLIPSE("Eclipse carmesim"),
    ROYAL_SHIMMER("Aura púrpura real"),
    SILVER_STREAKS("Fios prateados"),
    EMERALD_DUST("Fragmentos esmeralda"),
    FOREST_LEAVES("Folhas da floresta"),
    ZEN_RIPPLES("Ondulações zen"),
    OCEAN_WAVES("Espuma e ondas do mar"),
    MOUNTAIN_MIST("Neblina alpina"),
    BOTANICAL_VINES("Sementes e folhas"),
    NEON_WAVES("Ondas neon"),
    CITY_LIGHTS("Luzes urbanas"),
    ELECTRIC_BOLTS("Faíscas elétricas"),
    SYNTHWAVE_SUN("Sol synthwave"),
    HOLOGRAPHIC_PRISM("Reflexos iridescentes"),
    CLEAN_MINIMAL("Pulsação minimalista"),
    SOUND_WAVES("Ondas musicais"),
    CRYSTAL_SHARDS("Cristais luminosos"),
    DREAM_CLOUDS("Nuvens e gradientes"),
    MYATO_GALAXY("Constelação Myato");

    companion object {
        fun fromName(raw: String?): ThemeAnimationType {
            return entries.firstOrNull { it.name == raw } ?: SOUND_WAVES
        }
    }
}

@Serializable
enum class ThemeAnimationIntensity(
    val label: String,
    val factor: Float,
) {
    DISABLED("Desativadas", 0f),
    SOFT("Suaves", 0.5f),
    NORMAL("Normais", 1.0f),
    INTENSE("Intensas", 1.5f);

    companion object {
        fun fromFactor(factor: Float, disabled: Boolean): ThemeAnimationIntensity {
            return when {
                disabled || factor <= 0.05f -> DISABLED
                factor < 0.78f -> SOFT
                factor > 1.22f -> INTENSE
                else -> NORMAL
            }
        }
    }
}

@Serializable
enum class ThemeBorderStyle(val label: String) {
    NONE("Sem borda"),
    SUBTLE("Sutil"),
    SOLID("Contorno nítido"),
    GLOW("Brilho luminoso"),
    DOUBLE("Moldura dupla"),
    PIXEL("Borda pixelada");

    companion object {
        fun fromName(raw: String?): ThemeBorderStyle {
            return entries.firstOrNull { it.name == raw } ?: SUBTLE
        }
    }
}

@Serializable
enum class ThemeGradientStyle(val label: String) {
    SOLID("Sólido"),
    VERTICAL("Vertical"),
    DIAGONAL("Diagonal"),
    RADIAL("Radial luminoso");

    companion object {
        fun fromName(raw: String?): ThemeGradientStyle {
            return entries.firstOrNull { it.name == raw } ?: VERTICAL
        }
    }
}

@Serializable
enum class ThemeIconStyle(val label: String) {
    ROUNDED("Arredondados"),
    FILLED("Preenchidos"),
    OUTLINED("Contorno fino"),
    DUOTONE("Duotone destacado");

    companion object {
        fun fromName(raw: String?): ThemeIconStyle {
            return entries.firstOrNull { it.name == raw } ?: ROUNDED
        }
    }
}

@Serializable
enum class ThemeEffectPreset(val label: String) {
    CLEAN("Limpo"),
    SOFT_GLOW("Brilho suave"),
    GLASSMORPHIC("Vidro translúcido"),
    NEON_EDGE("Borda neon"),
    VIGNETTE("Vinheta cinematográfica"),
    RETRO_CRT("Textura retrô");

    companion object {
        fun fromName(raw: String?): ThemeEffectPreset {
            return entries.firstOrNull { it.name == raw } ?: SOFT_GLOW
        }
    }
}

@Serializable
enum class WallpaperStyle(val label: String) {
    SOLID("Fundo sólido"),
    GRADIENT("Gradiente fluido"),
    CAT_PAWS("Patinhas & Gatinhos"),
    HEARTS("Corações delicados"),
    CHERRY_BLOSSOMS("Flores de cerejeira"),
    STARFIELD("Céu estrelado"),
    NEBULA("Nebulosa espacial"),
    SATURN("Órbita de Saturno"),
    AURORA("Aurora boreal"),
    PIXEL_GRID("Grade Pixel Art"),
    SCANLINES("Arcade Retrô"),
    CLOUDS("Nuvens suaves"),
    RIBBONS("Laços Coquette"),
    RAINDROPS("Chuva na cidade"),
    GOLD_GEOMETRY("Geometria Luxuosa"),
    ZEN_WAVES("Jardim Zen"),
    OCEAN_WAVES("Ondas do Oceano"),
    BOTANICAL("Folhagem Botânica"),
    CITY_SKYLINE("Horizonte Cyberpunk"),
    SYNTHWAVE("Sol & Grade Synthwave"),
    HOLOGRAPHIC("Reflexo Holográfico"),
    CRYSTALS("Facetas de Cristal"),
    CUSTOM_IMAGE("Imagem da Galeria");

    companion object {
        fun fromName(raw: String?): WallpaperStyle {
            return entries.firstOrNull { it.name == raw } ?: GRADIENT
        }
    }
}

@Serializable
enum class ThemeAutoScheduleMode(val label: String, val description: String) {
    MANUAL("Manual", "Mantém o tema selecionado até você alterar."),
    DAY_NIGHT("Dia e Noite", "Alterna automaticamente entre tema diurno (06h–18h) e noturno."),
    DAILY_RANDOM("Aleatório Diário", "Sorteia um novo tema automaticamente uma vez por dia."),
    HOURLY_RANDOM("Aleatório por Hora", "Sorteia um novo tema a cada hora de uso."),
    SEASONAL("Sazonal Automático", "Escolhe automaticamente um tema alinhado à estação atual.");

    companion object {
        fun fromName(raw: String?): ThemeAutoScheduleMode {
            return entries.firstOrNull { it.name == raw } ?: MANUAL
        }
    }
}

enum class ThemeCreationMode(val label: String) {
    SCRATCH("Criar do Zero"),
    FROM_THEME("Editar Existente"),
    FROM_IMAGE("Paleta de Imagem"),
}

enum class ThemePreviewScreenTab(val label: String) {
    HOME("Tela inicial"),
    PLAYER("Player"),
    PLAYLISTS("Playlists"),
    LIBRARY("Biblioteca"),
    SEARCH("Pesquisa"),
    SETTINGS("Configurações"),
}

@Serializable
data class ThemeUsageHistoryEntry(
    val themeId: String,
    val themeName: String,
    val appliedAt: Long,
)

@Serializable
data class ThemeExtendedConfig(
    val category: String = ThemeCategory.MYATO_EXCLUSIVES.name,
    val description: String = "",
    val accentColorHex: Long? = null,
    val buttonColorHex: Long? = null,
    val iconColorHex: Long? = null,
    val borderColorHex: Long? = null,
    val gradientEndColorHex: Long? = null,
    val surfaceAlpha: Float = 0.94f,
    val backgroundAlpha: Float = 1.0f,
    val shadowElevationDp: Float = 4f,
    val borderWidthDp: Float = 1f,
    val gradientStyle: String = ThemeGradientStyle.VERTICAL.name,
    val borderStyle: String = ThemeBorderStyle.SUBTLE.name,
    val iconStyle: String = ThemeIconStyle.ROUNDED.name,
    val effectPreset: String = ThemeEffectPreset.SOFT_GLOW.name,
    val wallpaperStyle: String = WallpaperStyle.GRADIENT.name,
    val wallpaperPatternSeed: Int = 1,
    val wallpaperImagePath: String? = null,
    val wallpaperDimAlpha: Float = 0.42f,
    val animationType: String = ThemeAnimationType.SOUND_WAVES.name,
    val isFeatured: Boolean = false,
    val seasonalTag: String? = null,
)
