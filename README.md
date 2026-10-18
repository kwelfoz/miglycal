# Miglycal

Leitor de música para Android (FLAC + MP3) com estética de terminal.
Kotlin · Jetpack Compose · Media3 (ExoPlayer) · jaudiotagger.

## Como obter o APK

### Opção A: GitHub Actions (sem instalar nada)
1. Cria um repositório no GitHub e faz push desta pasta (ramo `main`).
2. Separador **Actions** → workflow **build-apk** (corre sozinho no push; também podes usar *Run workflow*).
3. No fim da execução, descarrega o artefacto **Miglycal-apk** → dentro está o `.apk`.
4. Passa o APK para o telemóvel e instala (permite "instalar apps de fontes desconhecidas").

### Opção B: Android Studio
1. *File → Open* nesta pasta (deixa o Gradle sincronizar).
2. *Build → Build App Bundle(s) / APK(s) → Build APK(s)*, ou na linha de comandos: `./gradlew assembleRelease`
3. APK em `app/build/outputs/apk/release/`.

O APK sai assinado com a chave de debug do Android (serve para instalar diretamente; não serve para a Play Store).

## Primeira utilização
- Dá permissão de áudio quando a app pedir.
- A app **não** pede acesso a todos os ficheiros. Ao guardar etiquetas, capas ou letras de uma faixa, o Android mostra um pedido para permitir que o Miglycal modifique *esse* ficheiro; é só confirmar.

## Funcionalidades
- Leitura de **FLAC** e **MP3** (biblioteca por pastas ou lista completa).
- **Playlists** com faixas escolhidas das pastas locais (pasta → *selecionar* → *+playlist*); modo aleatório e repetição guardados por playlist.
- **Bloquear pastas** (biblioteca ou pasta); desbloqueio em Definições.
- **Editor de etiquetas**: título, artista, álbum, artista do álbum, ano, faixa, disco, género, comentário.
- **Capas online**: iTunes (catálogo digital) e MusicBrainz com formato *Digital Media* + Cover Art Archive; só recorre a releases não digitais se não houver nenhum digital. Escolhes a capa e ela é gravada no ficheiro.
- **Letras** (LRCLIB): sincronizadas (texto LRC gravado na etiqueta do ficheiro) ou normais (também na etiqueta). No leitor grande, a letra sincronizada acompanha a música e podes tocar numa linha para saltar para ela.
- **Mini leitor** em baixo; toque abre o **leitor grande**, que termina com `flac // 44.1 khz` ou `mp3 // 320 kbps`.
- Notificação com controlos e reprodução em segundo plano.
- Paleta: verde, amarelo, laranja ou vermelho (Definições).

## Estrutura
```
app/src/main/java/app/miglycal/
  data/    Library (MediaStore), Store (definições/playlists), TagIO (etiquetas),
           Online (capas e letras), LyricsStore (.lrc), Art (capas embebidas)
  player/  PlaybackService (MediaSessionService), PlayerController
  ui/      Theme, Components, Screens, PlayerUi, TagEditor, App
```
