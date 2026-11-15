# Relatório de Testes — MYATO MUSIC

## 1. Resumo Executivo

- **Comando executado:** `gradle :app:testDebugUnitTest :app:assembleDebug`
- **Resultado da suíte JVM + Robolectric:** **16 testes executados, 16 aprovados, 0 falhas, 0 erros, 0 ignorados.**
- **Build Debug APK:** Concluído com sucesso (`app/build/outputs/apk/debug/app-debug.apk`, 25 MB).
- **Testes Instrumentados (`androidTest`) / Emulador / Dispositivo Físico:** **Não executados** neste ambiente de nuvem (não há emulador Android ou `adb` disponível no container de execução).

---

## 2. Detalhamento dos Testes Executados

### `ExampleUnitTest` (1/1 aprovado)
- `addition_isCorrect` — Verificação base de runner JUnit.

### `MyatoUnitTests` (6/6 aprovados)
1. `time formatter formats durations and lrc timestamps accurately` — Formatação `mm:ss`, `h:mm:ss`, `[mm:ss.xx]` e horas/minutos.
2. `lrc parser handles synced lines, time shifting, and active line index` — Parsing de letras `.lrc`, deslocamento temporal positivo/negativo, cálculo da linha ativa e serialização.
3. `built in themes include all 12 mandatory themes with official Midnight Orange palette` — Validação dos 12 temas integrados e dos códigos hexadecimais oficiais (`#FF8500`, `#FF9D24`, `#000000`, `#1C1C1C`, `#FFFFFF`, `#A5A5A5`).
4. `sleep timer track count decrements and completes` — Contagem de faixas, pausa/retomada e disparo de conclusão no `SleepTimerManager`.
5. `audio format support infers extensions cleanly` — Inferência de extensão por caminho e MIME type (`FLAC`, `DSF`, `OPUS`).
6. `tag edit payload preserves metadata fields accurately` — Estrutura `TagEditPayload`.

### `ExampleRobolectricTest` (1/1 aprovado)
1. `app name matches MYATO MUSIC` — Confirmação de `R.string.app_name == "MYATO MUSIC"` via recursos Android reais (Robolectric SDK 36).

### `MyatoRobolectricTests` (8/8 aprovados)
1. `manifest declares zero internet permission for strict offline privacy` — Verifica via `PackageManager` que `android.permission.INTERNET` está ausente e `FOREGROUND_SERVICE_MEDIA_PLAYBACK` está presente.
2. `database persists tracks, playlists, multi-queues, bookmarks, notes, stats, and themes` — Ciclo completo Room em memória: faixas, playlists, múltiplas filas independentes (incluindo filas com nomes idênticos e mesclagem), favoritos, marcadores, notas, estatísticas, temas customizados, backup JSON com pré-visualização e restauração, e exportação CSV.
3. `tag editor writes real id3v23 tag into mp3 file` — Escrita real de cabeçalho binário ID3v2.3 (`ID3` v2.3) em arquivo `.mp3` temporário mantendo o payload de áudio.
4. `theme repository exports and imports custom themes` — Exportação e reimportação JSON de temas preservando `isBuiltIn = false`.
5. `device audio format capability check covers all 11 formats` — Inspeção via `MediaCodecList` cobrindo MP3, M4A, WMA, FLAC, OPUS, AAC, ALAC, APE, DSF, WAV e OGG.
6. `theme center ui shows themes and opens live preview dialog before applying` — Teste Compose UI (`ThemeCenterScreen`): rolagem no grid de temas, abertura do diálogo de pré-visualização ao vivo e aplicação do tema.
7. `full player screen renders track controls and triggers playback actions` — Teste Compose UI (`FullPlayerScreen`): renderização de capa/faixa/controles e cliques reais em Play/Pause, Próxima e Favoritar.
8. `queues screen displays independent queues including identical names` — Teste Compose UI (`QueuesScreen`): exibição de múltiplas filas independentes com nomes iguais e botão de criação.

---

## 3. Cobertura que Requer Dispositivo Real / Emulador

Os seguintes comportamentos foram implementados com APIs reais do Android e compilados com sucesso, mas dependem de hardware ou sistema Android físico/emulado fora deste container:
- Reprodução contínua de áudio em hardware via `ExoPlayer` / `AudioTrack` e controles na tela de bloqueio / Android Auto (`MyatoPlaybackService`).
- Resposta real de `android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer` e `Visualizer` contra o `audioSessionId` do HAL do fabricante.
- Permissões em tempo de execução (`READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`), Photo Picker e Storage Access Framework (`OPEN_DOCUMENT_TREE`) com arquivos reais do usuário.
- Botões físicos de fones Bluetooth/com fio (1x, 2x, 3x, 4x+ cliques) e widgets na launcher real.
