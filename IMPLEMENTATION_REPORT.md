# Relatório de Implementação — MYATO MUSIC

## 1. Visão Geral e Identidade

- **Nome:** MYATO MUSIC (`applicationId = "com.aistudio.myatomusic.kxmpzq"`, `namespace = "com.example"`)
- **Slogan:** *Sua música. Seu estilo. Sua experiência.*
- **Versão:** `1.0.0` (`versionCode = 1`), `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36.1`
- **Arquitetura:** Clean Architecture + MVVM em módulo único organizado por camadas (`core`, `domain`, `data`, `feature`, `presentation`, `widgets`).
- **Conformidade Offline:** `AndroidManifest.xml` não solicita permissão `INTERNET`; nenhum cliente HTTP, SDK analítico, anúncio ou serviço de nuvem é utilizado.

---

## 2. Mapa de Módulos e Principais Arquivos

| Camada | Pacote / Arquivos | Responsabilidade |
| :--- | :--- | :--- |
| **App Entry** | `MyatoApplication.kt`, `MainActivity.kt` | Inicialização de tema padrão, notificações, canais, Edge-to-Edge (`enableEdgeToEdge`) e resolução de atalhos/widgets. |
| **Domain** | `domain/models/MusicModels.kt` | Modelos puros de faixas, álbuns, artistas, playlists, filas, letras, tags, equalizador, timers, temas, estatísticas e backup. |
| **Core Audio** | `core/audio/MyatoPlaybackService.kt`, `PlaybackController.kt`, `EqualizerEngine.kt`, `SleepTimerManager.kt` | Serviço `MediaLibraryService` + `ExoPlayer` *gapless*, `MediaSession`, Android Auto browse/search tree, foco de áudio, fones multi-clique, efeitos `AudioEffect` reais e timers. |
| **Core Database** | `core/database/Entities.kt`, `Daos.kt`, `MyatoDatabase.kt`, `EntityMappers.kt`, `AppPreferencesStore.kt` | Banco Room com 16 entidades (faixas, playlists, filas múltiplas, favoritos, histórico, marcadores, notas, tags, estatísticas, temas, perfis EQ, pastas ignoradas, backups) + DataStore. |
| **Core Storage** | `core/storage/MediaStoreScanner.kt`, `FileManagerRepository.kt`, `BackupManager.kt` | Indexação `MediaStore` + SAF (`DocumentFile`), detecção de duplicatas, gerenciador de pastas/arquivos, importação/exportação JSON e CSV. |
| **Core Metadata** | `core/metadata/LrcLyricsEngine.kt`, `TagEditorEngine.kt` | Parser/editor `.lrc` com deslocamento temporal e gravador ID3v2.3 binário em Kotlin puro + sincronização `MediaStore`/Room. |
| **Core Common** | `core/common/AudioFormatSupport.kt`, `TimeFormatter.kt`, `BackupWorker.kt` | Inspeção real de codecs via `MediaCodecList` (MP3, M4A, WMA, FLAC, OPUS, AAC, ALAC, APE, DSF, WAV, OGG) e worker periódico de backup. |
| **Design System** | `core/designsystem/Color.kt`, `Type.kt`, `BuiltInThemes.kt`, `Theme.kt`, `AlbumArtPaletteExtractor.kt`, `MyatoComponents.kt` | Paleta oficial `#FF8500` / `#000000` / `#1C1C1C`, tipografia Outfit / Plus Jakarta Sans / JetBrains Mono, 12 temas integrados, `Palette` offline e componentes Compose customizados. |
| **Data** | `data/repository/MyatoRepositories.kt` | Repositórios de Biblioteca, Playlists (M3U), Filas Múltiplas, Temas e Estatísticas/Engajamento. |
| **Widgets** | `widgets/WidgetProviders.kt`, `res/layout/widget_*.xml`, `res/xml/widget_*_info.xml` | Widgets Pequeno (4x1), Médio (4x2) e Grande (4x4), receiver de ações de mídia e fixação de atalhos (`ShortcutManagerCompat`). |
| **Presentation & Features** | `presentation/MyatoRootApp.kt`, `MyatoMainViewModel.kt`, `navigation/MyatoRoutes.kt` e `feature/*` | 25 telas e fluxos em Jetpack Compose (Splash, Onboarding, Home, Biblioteca, Player, Letras, Filas, Playlists, Tag Editor, File Manager, Equalizador, Timers, Temas, Estatísticas, Backup, Configurações, Formatos). |

---

## 3. Limitações Reais de Plataforma e Hardware

Para manter transparência técnica e evitar qualquer comportamento simulado:

1. **Decodificação de formatos exóticos (WMA, APE, DSF, ALAC):** O aplicativo verifica em tempo de execução os decodificadores disponíveis no `MediaCodecList` do dispositivo. Formatos sem decodificador nativo no aparelho são exibidos honestamente como não suportados na tela de Diagnóstico de Formatos.
2. **Equalizador e Visualizer (`android.media.audiofx.*`):** Dependem da implementação HAL `AudioEffect` do fabricante e do `audioSessionId` do player ativo; alguns dispositivos ou sessões podem não expor `Visualizer` sem `RECORD_AUDIO` ou limitar ganhos de bandas. O app trata exceções sem travar a reprodução.
3. **Escrita em arquivos e tags ID3 (`TagEditorEngine` / `FileManagerRepository`):** Em Android 10+ (Scoped Storage), alterações diretas em arquivos fora do escopo concedido exigem acesso via Storage Access Framework (`SAF`) ou `MediaStore.createWriteRequest` quando aplicável (cartões SD removíveis ou pastas restritas pelo sistema).
4. **Android Auto e controles físicos:** A árvore de mídia `MediaLibraryService` está registrada e integrada no `AndroidManifest.xml`, porém a validação completa na interface do Android Auto e em headsets físicos Bluetooth requer um dispositivo real ou Desktop Head Unit (não disponível no ambiente de nuvem sem emulador).
