# CerejaPlay

App Android (Kotlin + Jetpack Compose) para filmes, séries e animes. O app é um **cliente genérico guiado pela API**: provedores, fileiras da home, abas, idiomas, qualidades, tema e flags chegam de `https://cereja-api.vercel.app/api/app/config`.

Adicionar um provedor, uma fileira, um idioma ou uma nova aba **não exige novo APK**. Basta atualizar a CerejaApi.

## Recursos

- Home com destaque rotativo (hero), fileiras por categoria e "Continuar assistindo" local.
- Catálogo com paginação infinita para filmes, séries e animes.
- Busca unificada com debounce e resultados agrupados.
- Detalhe com sinopse, gêneros, temporadas e episódios.
- Player Media3 ExoPlayer para HLS/MP4 (com headers `Referer`/`Origin` e legendas) e WebView para embeds.
- Troca de fonte, áudio (dublado/legendado/original), idioma e qualidade.
- Tema, abas, textos legais e preferências vindos da API.
- Cache local da configuração para abrir rápido e continuar útil se a API oscilar.
- Ícone adaptativo, splash screen e suporte a edge-to-edge.

## API remota

Endpoints consumidos:

| Método | Rota | Uso |
|---|---|---|
| GET | `/api/app/config` | Bootstrap: tema, abas, preferências, flags e endpoints |
| GET | `/api/app/home` | Fileiras da home já resolvidas |
| GET | `/api/app/catalog/{movies\|series\|animes}?page=` | Catálogo paginado |
| GET | `/api/app/search?q=&page=` | Busca agrupada |
| GET | `/api/app/detail?type=&id=` | Detalhe unificado |
| GET | `/api/watch?type=&id=&season=&episode=&lang=&audio=&quality=` | Fontes ranqueadas + `best` |

A URL base fica em `BuildConfig.API_BASE_URL`, definida em `app/build.gradle.kts` e sobrescrevível com `-PAPI_BASE_URL=https://.../`.

## Build

O build roda no GitHub Actions (`.github/workflows/android.yml`) e publica o artefato **cerejaplay-debug** em cada push na `main`.

Para compilar localmente:

```bash
gradle assembleDebug
```

Requisitos: JDK 17, Android SDK 35 e Gradle 8.11.1.

## Estrutura

```
app/src/main/java/com/cerejaplay/app
├── data
│   ├── api        # Retrofit + kotlinx.serialization
│   ├── model      # Contratos da API
│   ├── prefs      # DataStore (config, preferências, histórico)
│   └── repo       # Repositório e fallback de configuração
├── ui
│   ├── components # Hero, cards, estados, folhas
│   ├── screens    # Home, catálogo, busca, detalhe, player
│   ├── theme      # Tema dinâmico vindo da API
│   └── viewmodel  # ViewModels por tela
├── AppContainer.kt
├── CerejaPlayApp.kt
└── MainActivity.kt
```

## Distribuição

Por enquanto o projeto distribui apenas **APK de debug** pelo artefato do GitHub Actions. O repositório é público e não contém segredos: a única URL embutida é a API pública.

## Aviso legal

O CerejaPlay agrega metadata e fontes de terceiros e não hospeda conteúdo. Use apenas para fins educacionais e respeite direitos autorais.