package app.miglycal.ui

/**
 * Interface text translations. Music metadata, file names, and artist biographies remain untouched.
 * The app deliberately keeps this small dictionary central so Portuguese/English stays consistent.
 */
fun localizeText(raw: String, language: String): String {
    val english = language == "en"
    val bracketed = raw.startsWith("[") && raw.endsWith("]") && raw.length >= 2
    val marked = raw.startsWith("# ") || raw.startsWith("> ") || raw.startsWith("! ")
    val marker = when {
        raw.startsWith("# ") -> "# "
        raw.startsWith("> ") -> "> "
        raw.startsWith("! ") -> "! "
        else -> ""
    }
    val value = when {
        bracketed -> raw.substring(1, raw.length - 1)
        marked -> raw.removePrefix(marker)
        else -> raw
    }

    val pairs = listOf(
        "biblioteca" to "library",
        "pastas" to "folders",
        "tudo" to "all",
        "rescan" to "scan again",
        "bloquear" to "block",
        "adicionar à playlist" to "add to playlist",
        "editar etiquetas" to "edit tags",
        "bloquear pasta" to "block folder",
        "voltar" to "back",
        "tocar" to "play",
        "aleat." to "shuffle",
        "selecionar" to "select",
        "todas" to "all",
        "fila" to "queue",
        "+playlist" to "+playlist",
        "cancelar" to "cancel",
        "playlists" to "playlists",
        "+ nova" to "+ new",
        "nova playlist" to "new playlist",
        "criar" to "create",
        "renomear" to "rename",
        "apagar" to "delete",
        "remover da playlist" to "remove from playlist",
        "guardar" to "save",
        "apagar playlist" to "delete playlist",
        "nova" to "new",
        "criar e adicionar" to "create and add",
        "definições" to "settings",
        "paleta" to "accent colour",
        "pastas bloqueadas" to "blocked folders",
        "nenhuma. bloqueia pastas na biblioteca." to "none. block folders from the library.",
        "desbloquear" to "unblock",
        "sobre" to "about",
        "flac // mp3" to "flac // mp3",
        "dar acesso" to "grant access",
        "o miglycal precisa de acesso ao áudio do telemóvel para listar a tua música (flac e mp3)." to "Miglycal needs audio access to list the music on your phone (FLAC and MP3).",
        "a procurar música..." to "scanning for music...",
        "nenhuma faixa flac/mp3 encontrada." to "no FLAC/MP3 tracks found.",
        "a guardar..." to "saving...",
        "guardado." to "saved.",
        "etiquetas" to "tags",
        "título" to "title",
        "artista" to "artist",
        "álbum" to "album",
        "artista álb." to "album artist",
        "ano" to "year",
        "faixa nº" to "track no.",
        "disco nº" to "disc no.",
        "género" to "genre",
        "comentário" to "comment",
        "tags online" to "online tags",
        "Pesquisa etiquetas completas através do MusicBrainz. As versões digitais são preferidas; os restantes lançamentos só aparecem se não houver correspondências digitais." to "Search MusicBrainz for full tags. Digital releases are preferred; other releases appear only when no digital matches are found.",
        "procurar tags completas" to "find full tags",
        "usar" to "use",
        "capa" to "artwork",
        "atual" to "current",
        "nova" to "new",
        "escolher ficheiro local" to "choose local file",
        "procurar capa online" to "search artwork online",
        "pesquisa manual" to "manual search",
        "pesquisar capa" to "search artwork",
        "letra" to "lyrics",
        "procurar letra online" to "search lyrics online",
        "música" to "music",
        "pesquisar letras" to "search lyrics",
        "tocar para selecionar" to "tap to select",
        "sincronizada" to "synced",
        "normal" to "plain",
        "ao guardar: grava a letra sincronizada (formato LRC) no campo de letra do ficheiro." to "when saving: writes synced lyrics (LRC format) to the file's lyrics field.",
        "ao guardar: grava a letra normal no campo de letra do ficheiro." to "when saving: writes plain lyrics to the file's lyrics field.",
        "log" to "log",
        "pause" to "pause",
        "play" to "play",
        "a tocar" to "now playing",
        "shuf" to "shuffle",
        "Artist photo" to "Artist photo",
        "about" to "about",
        "show less" to "show less",
        "read more" to "read more",
        "in your library" to "in your library",
        "álbum desconhecido" to "unknown album",
        "loading artist information..." to "loading artist information...",
        "no biography available." to "no biography available.",
        "no music by this artist was found in your selected folders." to "no music by this artist was found in your selected folders.",
        "track(s)" to "track(s)",
        "Set up your music library" to "Set up your music library",
        "Choose the folders where your music is stored and pick an accent colour. You can change these later in Settings." to "Choose the folders where your music is stored and pick an accent colour. You can change these later in Settings.",
        "accent colour" to "accent colour",
        "start using miglycal" to "start using miglycal",
        "choose at least one music folder to continue." to "choose at least one music folder to continue.",
        "This folder location is not supported. Choose a folder in shared internal storage or an SD card." to "This folder location is not supported. Choose a folder in shared internal storage or an SD card.",
        "music folders" to "music folders",
        "Only audio files inside these folders will appear in your library. Android opens its own folder picker; no broad storage permission is needed for selecting a folder." to "Only audio files inside these folders will appear in your library. Android opens its own folder picker; no broad storage permission is needed for selecting a folder.",
        "add folder" to "add folder",
        "no folders selected." to "no folders selected.",
        "remove" to "remove",
        "Keep at least one folder selected." to "Keep at least one folder selected.",
        "Começar agora" to "Get started",
        "start now" to "Get started",
        "Escolhe a língua, a cor e as pastas onde guardas a tua música." to "Choose the language, accent colour and folders where your music is stored.",
        "Idioma" to "Language",
        "Português" to "Português",
        "English" to "English",
        "Cor de destaque" to "Accent colour",
        "Pastas de música" to "Music folders",
        "Apenas as músicas destas pastas aparecem na biblioteca. Podes alterar isto mais tarde nas definições." to "Only music in these folders appears in your library. You can change this later in Settings.",
        "Começar a usar o Miglycal" to "Start using Miglycal",
        "Escolhe pelo menos uma pasta de música para continuar." to "Choose at least one music folder to continue.",
        "Configuração inicial" to "Initial setup",
        "Português" to "Portuguese",
        "English" to "English",
        "no cover" to "no cover",
        "read more" to "read more",
        "show less" to "show less"
    )

    var translated = if (english) pairs.firstOrNull { it.first == value }?.second
        else pairs.firstOrNull { it.second == value }?.first
    var out = translated ?: value

    if (english) {
        out = out
            .replace(Regex("^(\\d+) faixas · (\\d+) pastas$"), "$1 tracks · $2 folders")
            .replace(Regex("^(\\d+) faixas$"), "$1 tracks")
            .replace(Regex("^(\\d+) track\\(s\\) · (\\d+) album\\(s\\)$"), "$1 tracks · $2 albums")
            .replace(Regex("^(\\d+) track\\(s\\)$"), "$1 tracks")
            .replace(Regex("^adicionar (\\d+) faixa\\(s\\)$"), "add $1 track(s)")
            .replace(Regex("^(\\d+) correspondência\\(s\\) de tags\\.$"), "$1 tag match(es).")
            .replace(Regex("^(\\d+) resultado\\(s\\)\\."), "$1 result(s).")
            .replace(Regex("^(\\d+) opção\\(ões\\) encontradas\\.$"), "$1 options found.")
            .replace(Regex("^\\(em falta\\) (.+)$"), "(missing) $1")
            .replace(Regex("^aleat:(.+)$"), "shuffle:$1")
            .replace(Regex("^rep:(.+)$"), "repeat:$1")
            .replace("a procurar tags:", "searching tags:")
            .replace("pesquisa de tags falhou:", "tag search failed:")
            .replace("tags selecionadas:", "selected tags:")
            .replace("confirma e guarda.", "confirm and save.")
            .replace("a procurar capas:", "searching artwork:")
            .replace("a procurar letra:", "searching lyrics:")
            .replace("pesquisa manual de capa:", "manual artwork search:")
            .replace("pesquisa manual de letra:", "manual lyrics search:")
            .replace("sem resultados para esta pesquisa.", "no results for this search.")
            .replace("sem resultados. Experimenta outro título ou artista.", "no results. Try another title or artist.")
            .replace("a descarregar capa...", "downloading artwork...")
            .replace("capa pronta", "artwork ready")
            .replace("não foi possível ler a imagem selecionada.", "could not read the selected image.")
            .replace("permissão recusada, nada foi guardado.", "permission denied; nothing was saved.")
            .replace("erro ao guardar:", "save error:")
            .replace("sem capa", "no cover")
            .replace("as faixas desta pasta (e subpastas) deixam de aparecer na biblioteca. podes desbloquear nas definições.", "tracks in this folder (and subfolders) will no longer appear in the library. You can unblock them in Settings.")
            .replace("as faixas desta pasta deixam de aparecer na biblioteca.", "tracks in this folder will no longer appear in the library.")
    } else {
        out = out
            .replace(Regex("^(\\d+) tracks · (\\d+) folders$"), "$1 faixas · $2 pastas")
            .replace(Regex("^(\\d+) tracks$"), "$1 faixas")
            .replace(Regex("^(\\d+) albums$"), "$1 álbuns")
            .replace(Regex("^(\\d+) track\\(s\\) · (\\d+) album\\(s\\)$"), "$1 faixas · $2 álbuns")
            .replace(Regex("^(\\d+) track\\(s\\)$"), "$1 faixas")
            .replace(Regex("^add (\\d+) track\\(s\\)$"), "adicionar $1 faixa(s)")
            .replace(Regex("^\\(missing\\) (.+)$"), "(em falta) $1")
            .replace(Regex("^shuffle:(.+)$"), "aleat:$1")
            .replace(Regex("^repeat:(.+)$"), "rep:$1")
            .replace("searching tags:", "a procurar tags:")
            .replace("tag search failed:", "pesquisa de tags falhou:")
            .replace("selected tags:", "tags selecionadas:")
            .replace("confirm and save.", "confirma e guarda.")
            .replace("searching artwork:", "a procurar capas:")
            .replace("searching lyrics:", "a procurar letra:")
            .replace("manual artwork search:", "pesquisa manual de capa:")
            .replace("manual lyrics search:", "pesquisa manual de letra:")
            .replace("no results for this search.", "sem resultados para esta pesquisa.")
            .replace("no results. Try another title or artist.", "sem resultados. Experimenta outro título ou artista.")
            .replace("downloading artwork...", "a descarregar capa...")
            .replace("artwork ready", "capa pronta")
            .replace("could not read the selected image.", "não foi possível ler a imagem selecionada.")
            .replace("permission denied; nothing was saved.", "permissão recusada, nada foi guardado.")
            .replace("save error:", "erro ao guardar:")
            .replace("tracks in this folder (and subfolders) will no longer appear in the library. You can unblock them in Settings.", "as faixas desta pasta (e subpastas) deixam de aparecer na biblioteca. podes desbloquear nas definições.")
            .replace("tracks in this folder will no longer appear in the library.", "as faixas desta pasta deixam de aparecer na biblioteca.")
    }

    return when {
        bracketed -> "[$out]"
        marked -> marker + out
        else -> out
    }
}
