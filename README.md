# MYATO MUSIC

> **Sua música. Seu estilo. Sua experiência.**

Aplicativo Android reprodutor de música **100% offline**, construído nativamente em **Kotlin**, **Jetpack Compose**, **AndroidX Media3 (ExoPlayer + MediaLibraryService)** e **Room Database**, seguindo arquitetura **Clean Architecture + MVVM**.

---

## Privacidade Offline Obrigatória

- **Zero permissão `INTERNET`** declarada no `AndroidManifest.xml`.
- **Zero streaming, zero downloads, zero telemetria, zero anúncios e zero busca automática de letras online.**
- Todos os arquivos de áudio, letras `.lrc` / embutidas, capas, playlists, múltiplas filas, equalizador, temas, estatísticas e backups permanecem exclusivamente no dispositivo do usuário.

---

## Principais Recursos Implementados

- **Motor de Áudio Media3 / ExoPlayer (`MyatoPlaybackService`)**
  - Reprodução contínua em segundo plano com notificação de mídia (`MediaLibraryService` + `MediaSession`), reprodução *gapless*, controle de áudio com recuperação de foco, botões de mídia/fones com múltiplo clique configurável e suporte ao Android Auto (`automotive_app_desc.xml`).
  - Detecção real de codecs do dispositivo (`MediaCodecList`) cobrindo MP3, M4A, WMA, FLAC, OPUS, AAC, ALAC, APE, DSF, WAV e OGG — exibindo apenas status real do hardware/SO.
- **Biblioteca Musical Completa (MediaStore + Storage Access Framework)**
  - Todas as Músicas, Álbuns, Artistas, Gêneros, Pastas, Favoritos, Adicionadas Recentemente, Mais Reproduzidas, Histórico, Busca em Tempo Real, Duplicatas por tamanho/título e filtros por duração mínima / pastas ignoradas.
- **Sistema Avançado de Filas Múltiplas & Playlists**
  - Múltiplas filas simultâneas independentes (inclusive com nomes idênticos), fila ativa isolada, duplicação, mesclagem, reordenação, importação/exportação `.m3u` e sincronização via Room.
- **Letras Sincronizadas (LRC) & Editor de Tags ID3**
  - Leitura/gravação de letras `.lrc`, ajuste de atraso/adiantamento em ms, criação/edição linha a linha, e gravador ID3v2.3 real em Kotlin puro (`TagEditorEngine`) com troca de capa via Android Photo Picker (`PickVisualMedia`).
- **Equalizador Real & Efeitos de Áudio (`EqualizerEngine`)**
  - Bandas de frequência reais (`android.media.audiofx.Equalizer`), Bass Boost, Virtualizer, Loudness Enhancer, Visualizer em tempo real e perfis automáticos por saída (alto-falante, fone com fio, Bluetooth).
- **Motor Visual, Temas & Extração de Paleta**
  - Tela de abertura laranja `#FF8500` com emblema exclusivo `MYATO`, tema padrão *Midnight Orange* (`#FF8500`, `#000000`, `#1C1C1C`), 12 temas integrados, editor completo, extração offline de cores da capa do álbum (`Palette`) e importação/exportação JSON.
- **Produtividade, Widgets, Backup & Estatísticas**
  - Marcadores com timestamp, notas por música, 3 widgets (Pequeno, Médio e Grande), atalhos fixados na tela inicial, Timer de Sono (tempo, fim da faixa ou contagem de faixas), estatísticas com exportação CSV e backup/restauração JSON (incluindo agendamento via `WorkManager`).

---

## Como Gerar o APK Automaticamente pelo GitHub Actions

Este projeto já inclui o fluxo completo em `.github/workflows/android-build.yml` com Gradle Wrapper (`gradlew`).

### 1. Enviar o projeto para o GitHub

```bash
git init
git add .
git commit -m "feat: MYATO MUSIC initial release"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/myato-music.git
git push -u origin main
```

### 2. Como baixar o APK gerado em qualquer push (sem configurar chaves)

1. Acesse seu repositório no GitHub e abra a aba **Actions**.
2. Clique na execução mais recente do workflow **Android CI & APK Release**.
3. Role até a seção **Artifacts** no rodapé da execução.
4. Baixe **`MYATO-MUSIC-debug-apk`** (contém `MYATO-MUSIC-debug.apk` e `MYATO-MUSIC.apk`, prontos para instalação direta em dispositivos Android).

### 3. Como configurar assinatura de Release APK (produção)

Para gerar o **`MYATO-MUSIC-release.apk`** assinado com sua própria chave e habilitar **GitHub Releases** automáticas:

1. Gere uma keystore de produção localmente (guarde o arquivo `.jks` e as senhas em local seguro — **nunca faça commit da keystore no Git**):
   ```bash
   keytool -genkey -v \
     -keystore myato-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias upload
   ```
2. Converta a keystore para Base64 (sem quebras de linha):
   - **Linux:** `base64 -w 0 myato-release.jks`
   - **macOS:** `base64 -i myato-release.jks | tr -d '\n'`
3. No GitHub, acesse **Settings → Secrets and variables → Actions → New repository secret** e crie exatamente estes 4 segredos:
   - `ANDROID_KEYSTORE_BASE64`: conteúdo Base64 do passo 2.
   - `ANDROID_KEYSTORE_PASSWORD`: senha da keystore (`storepass`).
   - `ANDROID_KEY_ALIAS`: alias da chave (ex.: `upload`).
   - `ANDROID_KEY_PASSWORD`: senha da chave (`keypass`).

### 4. Como criar uma Release oficial por tag (`v*`)

Com os 4 secrets configurados acima:

```bash
git tag v1.0.0
git push origin v1.0.0
```

O GitHub Actions executará os testes (`testDebugUnitTest`), compilará e assinará o `MYATO-MUSIC-release.apk`, validará a assinatura com `apksigner verify` e criará automaticamente a **Release** no GitHub com os arquivos `MYATO-MUSIC-release.apk` e `MYATO-MUSIC-debug.apk`.

### 5. Publicando atualizações preservando dados do usuário

- Mantenha sempre o mesmo `applicationId` (`com.aistudio.myatomusic.kxmpzq`) e a **mesma keystore** (`ANDROID_KEYSTORE_BASE64`).
- Ao lançar novas versões, incremente `versionCode` e `versionName` em `app/build.gradle.kts` e crie uma nova tag (ex.: `v1.1.0`).

---

## Compilação Local

Requisitos: JDK 17 e Android SDK (compileSdk 36).

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
