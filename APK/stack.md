# Stack — Portal Admin (Bodas M&L) → APK nativo Kotlin + Push via Gotify

> Mapeamento completo do projeto e do plano de construção do APK.
> Gerado a partir da análise do repositório `C:\Bodas\V3\admin-portal\New-Admin-Portal` e do
> PocketBase local em `C:\Bodas\V3\admin-portal\pocketbase`.
> Última atualização: 2026-10-21 (versão do portal analisada: **2.0.12**)

---

## 1. Inventário do repositório

```
C:\Bodas\V3\admin-portal\New-Admin-Portal\   (git repo, branch master)
├── public/                    # app web (PWA vanilla JS, sem build step)
│   ├── index.html             # login + troca de senha
│   ├── dashboard.html         # shell do painel (sidebar desktop / bottom-nav mobile)
│   ├── manifest.json          # PWA manifest (nome "Admin M&L", theme-color #0F1A2E)
│   ├── sw.js                  # service worker (cache do shell; SEM push handler)
│   ├── sw-register.js         # registro do SW (via /js/sw-register.js)
│   ├── offline.html           # página offline
│   ├── css/                   # admin.css, fonts.css, base/, components/
│   ├── js/
│   │   ├── config.js          # CONFIG.PB_URL='/api/pb', APP_VERSION='2.0.12'
│   │   ├── app.js             # shell, tabs, swipe, modal, export modal
│   │   ├── login.js           # fluxo de login + troca obrigatória de senha
│   │   ├── api/pocketbase.js  # PB.fetchRSVP/fetchMessages/update/create
│   │   ├── features/guests.js # lista de convidados
│   │   ├── features/messages.js# mural
│   │   ├── features/export.js # XLSX (SheetJS) + PDF (jsPDF + autotable)
│   │   └── utils/             # auth.js, theme.js, dom.js, toast.js, sanitize.js, parse.js
│   ├── lib/                   # pocketbase.umd.js, xlsx, jspdf, jspdf-autotable, fonts
│   └── images/                # Sankofa.png (marca), ícones PWA
├── admin-portal_nginx.conf    # nginx de produção do portal
├── pb_schema.json             # dump do schema do PocketBase
├── PRODUCT.md / AGENTS.md / CLAUDE.md
└── APK/                       # ★ este diretório — projeto do app Android (a criar)
```

Contexto fora do repo (mesma máquina):

```
C:\Bodas\V3\admin-portal\
├── pb_hooks/notify-rsvp.pb.js        # hook: novo RSVP → POST no Gotify  (⚠ ver §5)
├── pb_hooks/auto-sheet-row-id.pb.js  # hook: gera sheet_row_id (max+1) no create
├── pocketbase/pocketbase.exe         # PocketBase v0.40.1 (Windows) + pb_data/
├── pocketbase/pb_data/types.d.ts     # referência oficial de hooks/JSVM
└── config/admin-portal_nginx.conf
C:\Bodas\V3\Site\                      # site público de RSVP (bodasmel.com.br)
C:\Bodas\V3\memoria_temporaria.md     # memória de infra/deploy
C:\Bodas\V3\admin-portal\public\      # versão anterior do portal (Vue 3) — arquivada
```

---

## 2. Stack atual (web) — o que existe hoje

| Camada | Tecnologia | Detalhe |
|---|---|---|
| Front | HTML/CSS/JS vanilla (sem framework, sem build, sem npm) | `public/` — estático, servido por nginx:alpine |
| PWA | manifest.json + sw.js + offline.html | instalável ("Admin M&L"), mas **sem push handler** |
| API | PocketBase **v0.40.1** + JS SDK UMD | proxy nginx `/api/pb/api/` → `websites_pocketbase:80/api/` |
| Export | SheetJS (`XLSX.writeFile`) + jsPDF + autotable (`doc.save`) | ambos geram blob + download no browser |
| Ícones/brand | `public/images/Sankofa.png`, theme `#0F1A2E`, fundo `#F5E6D3` | "Admin M&L", pt-BR, tom formal-operacional |

**Produção (verificado por HTTP):**

| Item | Valor |
|---|---|
| Portal | `https://portal.bodasmel.com.br` → 200, serve v2.0.12 (igual ao repo) |
| API | `https://portal.bodasmel.com.br/api/pb/api/health` → 200 `{"message":"API is healthy."}` |
| Site público | `https://bodasmel.com.br` → 200 |
| Deploy | Contabo VPS + Traefik (TLS) + EasyPanel; redes `easypanel-websites`; containers nginx:alpine, `websites_pocketbase`, `n8n_umami`, `gotify` |
| Nginx do portal | `limit_req 60r/m burst 30`; `limit_except GET POST PATCH`; CSP estrita; `no-store`; sem `proxy_http_version 1.1` (impacta SSE) |

---

## 3. Backend — schema PocketBase

| Collection | Tipo | Regras | Campos |
|---|---|---|---|
| `rsvp` | base | list/view/update: `@request.auth.id != ""`; create: `""` (público) | id, sheet_row_id(num), nome, adulto, criancas, responsavel, nome_crianca, idade, restricoes, contato, status(select), created, updated |
| `messages` | base | list/view: `@request.auth.id != "" \|\| status = "Aprovado"`; create: `@request.body.status = ""`; update: auth | id, sheet_row_id, nome, mensagem, status, created, updated |
| `users` | auth | id = @request.auth.id (self) | id, email, password, name, avatar, **must_change_password**, created, updated |

Estados: `rsvp` → **Pendente / Confirmado** (legado `Validado` é normalizado para `Confirmado` no client);
`messages` → **Pendente / Aprovado / Rejeitado**.
Base local `pb_data/data.db`: 72 registros em `rsvp`, 12 em `messages`, 2 usuários.

Fluxo de escrita do RSVP (site público): `Site/src/js/api/pocketbase.js` → `pb.collection('rsvp').create({... status:'Pendente'})`
(existe também um dual-write legado via Google Apps Script `PocketBase.gs` / `Projeto_RVSP.gs` — **validar se ainda duplica**).

---

## 4. Gotify — o que já está integrado

- Hook `C:\Bodas\V3\admin-portal\pb_hooks\notify-rsvp.pb.js`:
  - evento `onRecordAfterCreateRequest` (filtro `rsvp`)
  - monta mensagem multilinha: Nome, Adultos, Crianças, Responsável, Criança (idade), Contato, Restrições
  - `POST {GOTIFY_URL}/message` com header `X-Gotify-Key: GOTIFY_TOKEN`, `priority: 5`
  - env vars EasyPanel: `GOTIFY_URL=http://gotify:80` (rede interna), `GOTIFY_TOKEN` (App Token)
- API Gotify relevante (confirmada na doc oficial):
  - `POST /client` → cria **client token** (recebe e envia mensagens; exige basic auth do usuário ou token)
  - `GET /stream` → **WebSocket** (não SSE) que entrega mensagens novas; auth por `X-Gotify-Key: <clientToken>`; suporta `sinceId` para backfill
  - payload da mensagem: `{id, appid, message, title, priority, date, extras}`
  - `POST /message` (App Token) → envia mensagem; App Token **só** pode usar `/message`

---

## 5. ⚠ Bug encontrado (bloqueia o push atual)

Em PocketBase **v0.40.1** o hook `onRecordAfterCreateRequest` **não existe** — foi renomeado para
`onRecordAfterCreateSuccess` (verificado em `pocketbase/pb_data/types.d.ts`: existem
`onRecordAfterCreateSuccess/Error`, `onRecordAfterUpdateSuccess/Error`, e apenas
`onRecordCreateRequest`/`onRecordUpdateRequest` para pré-ação).

Consequência provável: `notify-rsvp.pb.js` falha ao carregar no PocketBase v0.40 → **as notificações
Gotify de hoje não funcionam**. Confirmar no servidor antes deAssuming que "nada chega" — mas o
hook precisa ser corrigido para `onRecordAfterCreateSuccess` de qualquer forma.

---

## 6. Arquitetura alvo do app

```
Site RSVP (bodasmel.com.br) ──POST──> PocketBase (rsvp)
                                            │
              hook onRecordAfterCreateSuccess ──POST /message──> Gotify
                                                                    │
                                                     WebSocket /stream (client token)
                                                                    │
                                        Foreground Service (Kotlin) mantém a conexão
                                                                    │
                                          Notificação local (canal HIGH "RSVP")
```

Decisões fechadas com o usuário:

| Tema | Decisão |
|---|---|
| Escopo do app | **Reescrita 100% nativa em Kotlin** (Compose) — não WebView |
| Transporte do push | **Reaproveitar o Gotify** já existente (sem FCM/Firebase) |
| Gatilho da notificação | **Somente novo RSVP criado no site** (create) |
| Distribuição | **Sideload direto** (APK assinado), sem Play Store |
| API do app | Direto no PocketBase via `https://portal.bodasmel.com.br/api/pb` (nativo não é browser → sem CORS; métodos GET/POST/PATCH já liberados) |
| Servidor do Gotify | Precisa ser alcançável por HTTPS a partir do celular (subdomínio Traefik **ou** proxy `/gotify/` no portal) |

Stack do app:

| Item | Escolha | Versão/verificação |
|---|---|---|
| Linguagem | Kotlin | **2.3.20** (JDK 21 em `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`) |
| Build | **AGP 9.0.1 + Gradle 9.1.0** (wrapper) | `dl.google.com` e Maven Central acessíveis ✅ |
| SDK | **compileSdk/targetSdk 36**, minSdk 26 | platform-tools + build-tools 35.0.0 + platforms 35/36 instalados ✅ |
| UI | Jetpack Compose (Material 3, BOM 2026.03.01) | `material-icons-extended` adicionado |
| Navegação | Navigation3 1.0.1 (template) → substituído por abas internas + bottom nav/rail | — |
| Rede | OkHttp 4.12.0 (WebSocket embutido) + kotlinx-serialization-json 1.9.0 | confirmados no Maven Central |
| Segredo local | EncryptedSharedPreferences (`androidx.security:security-crypto` 1.1.0) + DataStore 1.3.0 | — |
| Background | Foreground Service `specialUse` (evita o limite de 6 h/dia do `dataSync` no Android 15+) + `START_STICKY` + `onTaskRemoved` | — |
| Notificação | `NotificationManagerCompat`, canal HIGH, permissão `POST_NOTIFICATIONS` (Android 13+) | — |
| XLSX | writer próprio (zip + XML, ~200 linhas) — evita Apache POI (pesado no Android) | alternativa: CSV |
| PDF | `android.graphics.pdf.PdfDocument` (A4 paisagem) + MediaStore (Downloads) | — |
| Ícone | adaptativo a partir de `public/images/Sankofa.png` | pendente (F8) |

---

## 7. Paridade funcional (extraída do código real do portal)

| Módulo web | O que o app precisa fazer |
|---|---|
| `login.js` / `utils/auth.js` | Login e-mail+senha; **troca obrigatória de senha** (`must_change_password`); logout; token persistido e expiração; sessão sobrevive ao fechamento do app |
| `features/guests.js` | Lista (tabela no desktop / cards no mobile); busca por nome, responsável e **telefone (só dígitos, ≥2)**; filtro status (Todos/Pendentes/Confirmados); filtro categoria (Adultos/Crianças/Necessidades especiais/Veganos/Intolerância à lactose); **contadores por opção** nos filtros; paginação 25/pág; **swipe horizontal** para paginar; badge de status clicável (toggle Pendente↔Confirmado) com animação; "Aprovar Todos" com modal de confirmação e feedback (`X confirmados, Y falhas`); estatística "N registros (X pendentes)"; badge de pendentes na navegação |
| `features/messages.js` | Mural em grid de cards; filtro (Todas/Pendentes/Aprovadas/Rejeitadas) com contadores; botões Aprovar / Rejeitar / Pendente; "Aprovar Todas" em lote; paginação 12/pág; toasts de sucesso/erro |
| `features/export.js` | **XLSX** com abas `Convidados` e `Mural` (só cria aba se houver dados), larguras de coluna, nomes `convidados-bodas-ml-AAAAMMDD-<hash8>.xlsx` / `mensagens-mural-ml-...` / `dados-bodas-ml-...`; **PDF** A4 **paisagem**, título + linha de filtros + "Exportado em", cabeçalho verde `rgb(5,150,105)` (convidados) e índigo `rgb(99,102,241)` (mural), linhas alternadas `rgb(245,247,250)`, coluna de mensagem com quebra de linha, footer "Exportado em" + "Página N" |
| `utils/theme.js` | Tema claro/escuro persistido; `theme-color` do app acompanha |
| `utils/parse.js` | `restricoes` pode ser JSON **ou** CSV; "necessidades especiais" = contém "necessidade" ou "especial" (case-insensitive) |
| Layout | Sidebar ≥1025 px / Bottom-nav ≤1024 px; tema Sankofa; pt-BR |

Colunas do export (paridade exata):
- Convidados: `#`, `Nome / Responsável`, `Tipo`, `Idade`, `Nome Da Criança`, `Restrições`, `Contato`, `Data`, `Status` (larguras 6/22/10/8/22/22/18/12/10)
- Mural: `#`, `Nome`, `Mensagem`, `Data`, `Status` (larguras 6/20/50/14/10)
- Regras de linha: `nome` = `responsavel || nome || '-'`; `tipo` = `Adulto` (adulto=='1') / `Criança` (criancas=='1') / `-`; `restricoes` = lista parseada join ', ' ou valor cru ou `-`

---

## 8. Fases de execução

| Fase | Entrega | Est. |
|---|---|---|
| **F0** Toolchain | Instalar Android SDK cmdline-tools (`platform-tools`, `platforms;android-35`, `build-tools;35.0.0`) em `C:\Users\llopes\AppData\Local\Android\Sdk` + `sdkmanager --licenses`; projeto Gradle (Kotlin 2.x, AGP 8.9, Gradle 8.11, JDK 21); keystore release via `keytool`; script `build-apk.ps1`; APK "hello" assinado | 1–2 h |
| **F1** Base do app | Compose Material3, tema claro/escuro (`#0F1A2E` / `#F5E6D3` / verde `#059669`), navegação, ícone adaptativo Sankofa, tela de ajustes (URLs do PocketBase e do Gotify) | ~2 h |
| **F2** Auth | Login, troca de senha obrigatória, token em storage criptografado, expiração, logout, tratamento de 401 | ~2 h |
| **F3** Convidados | Busca, filtros, contadores, paginação, cards/tabela, toggle de status, aprovar todos, swipe, stats | 3–4 h |
| **F4** Mural | Cards, filtros com contadores, moderação, aprovar todas | 1–1,5 h |
| **F5** Exportar | XLSX (writer próprio) e PDF (`PdfDocument` A4 paisagem) salvos em Downloads via MediaStore | 2–3 h |
| **F6** Push Gotify | `GotifyService` (FGS `specialUse`) + WebSocket `/stream` + criação do client token no 1º uso + canal HIGH + permissão de notificação + dedupe por `message.id` + deep link (abre Convidados e destaca o novo RSVP) | 2–3 h |
| **F7** Servidor | Corrigir `notify-rsvp.pb.js` → `onRecordAfterCreateSuccess`; opcional `extras` JSON (rsvp_id/nome) no Gotify; reiniciar PocketBase no EasyPanel; validar 1 RSVP = 1 notificação (checar dual-write legado do Apps Script) | ~1 h |
| **F8** Polimento + entrega | Empty states, toasts, animações, testes no aparelho, `app-release.apk` assinado + sideload (`adb install`) | ~2 h |

**Total estimado: 2–4 dias.** O portal web não é alterado → risco de regressão zero no que está no ar.

---

## 9. Infraestrutura — ações do usuário

1. **Gotify alcançável por HTTPS** a partir do celular (hoje é `http://gotify:80`, rede interna):
   - (a) subdomínio público via Traefik (ex.: `gotify.bodasmel.com.br`) — recomendado; ou
   - (b) proxy `location /gotify/` no nginx do portal (exige `proxy_http_version 1.1` + `Upgrade/Connection` para WebSocket).
2. **Credencial do Gotify** (usuário/senha) para o app criar o `client token` no primeiro uso — ou token pronto colado na tela de ajustes.
3. **Reiniciar o serviço PocketBase** no EasyPanel após subir o hook corrigido.
4. **Nginx (opcional)**: adicionar `proxy_http_version 1.1; proxy_buffering off;` em `/api/pb/api/` só é necessário se quisermos realtime/SSE do PocketBase (o app funciona sem isso, usando refresh no foco + sinal do Gotify).
5. **Rate limit** do portal (`60r/m`): o app não deve polinhar.

---

## 10. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Android 15+ limita FGS `dataSync` a 6 h/dia | `foregroundServiceType=specialUse` (sem limite; sideload não passa por revisão do Play) |
| App morto pelo sistema | FGS + `START_STICKY` + `onTaskRemoved` + pedir isenção de otimização de bateria no 1º start |
| Notificação duplicada (dual-write legado do Apps Script) | Testar com 1 RSVP; tornar o hook idempotente por `record.id` |
| Keystore perdida → updates impossíveis | Guardar `.jks` + senhas fora do repo (ex.: `C:\Bodas\backup\`) |
| Divergência entre app e portal | Regras de negócio no PocketBase; app só cliente; regras de parse/filtro isoladas e testáveis |
| Gotify fora do ar | O app continua funcionando (lista via PocketBase); só o push para |
| Sem Google Play no aparelho | Irrelevante: Gotify não depende de Play Services |

---

## 11. Perguntas em aberto

1. **URL do Gotify**: existe subdomínio público hoje? Se não, criar `gotify.bodasmel.com.br` no Traefik ou usar proxy `/gotify/`?
2. **Credencial**: o app cria o client token sozinho (user+senha do Gotify, guardados só no aparelho) ou você cola um token pronto?
3. **Local do código**: `New-Admin-Portal/android/` (mesmo repo git) ou repositório separado `C:\Bodas\V3\admin-portal\AdminML-Android`?
4. **Identidade**: package `com.bodasmel.adminml` e nome "Admin M&L" — ok? Qual o Android do aparelho (para targetSdk e testes)?

---

## 12. Referências verificadas

- Hooks PocketBase v0.40.1: `C:\Bodas\V3\admin-portal\pocketbase\pb_data\types.d.ts` (`onRecordAfterCreateSuccess`, `onRecordAfterUpdateSuccess`, `onRecordCreateRequest`…)
- PocketBase sem FCM nativo (binário v0.40.1 não contém endpoints FCM; só "fcm" de fontes de ícone)
- Gotify: `/stream` é **WebSocket**; `clientToken` recebe; `appToken` só envia (`POST /message`); `POST /client` cria client token
- Maven Central: `com.squareup.okhttp3:okhttp:4.12.0`, `okhttp-sse:4.12.0`, `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`; SDK Kotlin oficial do PocketBase = `io.github.agrevster:pocketbase-kotlin-android:2.7.1` (optou-se por cliente HTTP próprio: 5 endpoints, sem risco de versão)
- Ambiente local: JDK 21 ✅, Node ✅, Python ✅, Android SDK ❌ (a instalar), `adb` ❌, Gradle ❌, internet p/ dl.google.com + Maven Central ✅

---

## 13. Status de execução (2026-10-21)

### Concluído

| Fase | Situação | Entregue |
|---|---|---|
| **F0** Toolchain | ✅ | Android SDK instalado em `C:\Users\llopes\AppData\Local\Android\Sdk` (cmdline-tools 23.0, platform-tools 37.0.1, platforms;android-35/36, build-tools;35.0.0). Projeto Gradle criado em `APK/` (AGP 9.0.1, Gradle 9.1.0, Kotlin 2.3.20, Compose BOM 2026.03.01, Navigation3 1.0.1). Keystore de release: `C:\Bodas\backup\adminml-keystore\adminml-release.jks` (alias `adminml`, senha em `keystore-credentials.txt`). `keystore.properties` (gitignored) aponta para ele. `assembleDebug` e `assembleRelease` **verdes**; APK assinado em `APK/dist/AdminML-1.0.0.apk` (14,1 MB), certificado `CN=Admin M&L` SHA-256 `2d1dce3f162a8bf3073a6d26a018d6c0783db830f20f408970ca63de23863a65` |
| **F1** Base do app | ✅ | Tema claro/escuro com a paleta exata do portal (`theme/Color.kt`), `AdminMLTheme` sem dynamic color, `SettingsStore` (DataStore: URL PocketBase, URL Gotify, tema), `SecureStore` (EncryptedSharedPreferences: token PB, client token Gotify, último id de mensagem), HomeScreen com bottom-nav (<600dp) / navigation-rail (>=600dp) e abas Convidados/Mural/Exportar/Sair, tela de Ajustes com URL do PocketBase, URL do Gotify, tema, teste de conexão e **vínculo do Gotify** (usuário/senha -> `POST /client` -> client token) |
| **F2** Auth | ✅ | `SessionManager` (login, troca obrigatória de senha, logout, re-autenticação com a nova senha, mensagens de erro idênticas ao portal), `LoginScreen`/`LoginViewModel` com modo login/troca de senha, sessão restaurada do storage criptografado |
| **F6** Push (parte do app) | ✅ (falta teste real) | `GotifyClient` (cria client token, histórico `/message`, WebSocket `/stream` com `X-Gotify-Key` e `sinceId`), `GotifyService` (Foreground Service `specialUse`, notificação persistente, reconexão com backoff, dedupe por id, filtra título "RSVP"), `Notifications` (canais `rsvp_confirmacoes` HIGH e `monitoramento`, notificação com big text e deep link para a aba de convidados), `ic_stat_rsvp.xml` |
| **F7** Servidor | ⏳ pendente | Hook `notify-rsvp.pb.js` ainda precisa ser corrigido (`onRecordAfterCreateRequest` -> `onRecordAfterCreateSuccess`) e o PocketBase reiniciado no EasyPanel |

### Estrutura criada em `APK/app/src/main/java/com/bodasmel/adminml/`

```
AdminMlApp.kt            Application + AppContainer (service locator)
MainActivity.kt          porta de entrada, tema, gate de auth, permissão de notificação
core/Settings.kt         DataStore (URLs + tema)
core/SecureStore.kt      storage criptografado
core/SessionManager.kt   login / troca de senha / logout
data/Models.kt           RsvpRecord, MessageRecord, UserRecord, ApiException
data/PocketBaseClient.kt cliente HTTP próprio (auth, list paginado, PATCH de status)
domain/ParseUtils.kt     paridade de parse.js (restricoes JSON ou CSV)
push/GotifyClient.kt     client token + WebSocket /stream
push/GotifyService.kt    Foreground Service (specialUse) + reconexão
push/Notifications.kt    canais e construção das notificações
push/GotifyMessage.kt    DTOs do Gotify
theme/                   paleta + tema Material3 do portal
ui/home/HomeScreen.kt    scaffold, bottom nav / rail, abas
ui/login/                login + troca de senha
ui/guests/               (F3 — placeholder atual)
ui/messages/             (F4 — placeholder atual)
ui/export/               (F5 — placeholder atual)
ui/settings/             Ajustes + vínculo do Gotify
```

### Próximos passos

1. **F3 Convidados** — `GuestsViewModel` (lista, busca por nome/responsável/telefone, filtros de status e categoria com contadores, paginação 25/pág, toggle de status, aprovar todos) + `GuestsScreen` (cards no mobile, tabela no desktop, badge de status, destaque de necessidades especiais, swipe para paginar) + reação ao evento de novo RSVP do Gotify.
2. **F4 Mural** — moderação (Aprovar/Rejeitar/Pendente), filtro com contadores, "Aprovar Todas", paginação 12/pág.
3. **F5 Exportar** — `XlsxWriter` próprio (abas Convidados/Mural) e `PdfExporter` (`PdfDocument` A4 paisagem) salvos em Downloads via MediaStore.
4. **F6 (fecho)** — wire do evento Gotify -> refresh da lista + deep link abrindo a aba de convidados.
5. **F7 Servidor** — corrigir o hook e reiniciar o PocketBase.
6. **F8** — ícone Sankofa, polimento, testes no aparelho e sideload.

---

## 14. Status final (2026-10-22) — APK pronto

### Entregável

| Item | Valor |
|---|---|
| APK | `APK/dist/AdminML-1.0.apk` (13,8 MB) — release assinado |
| Package | `com.bodasmel.adminml` |
| Label | **Admin M&L** |
| SDK | minSdk 26 / targetSdk 36 / compileSdk 36 |
| Certificado | `CN=Admin M&L, OU=Bodas de Esmeralda, O=Bodas M&L, C=BR` — SHA-256 `2d1dce3f162a8bf3073a6d26a018d6c0783db830f20f408970ca63de23863a65` |
| Keystore | `C:\Bodas\backup\adminml-keystore\adminml-release.jks` (alias `adminml`, senha em `keystore-credentials.txt`) |
| Build | `APK\build-apk.ps1` (release) / `gradlew.bat assembleDebug` |
| Guia de instalação | `APK/README.md` |

### Fases concluídas

| Fase | Situação |
|---|---|
| F0 Toolchain + keystore + APK assinado | ✅ |
| F1 Base do app (tema da marca, DataStore, SecureStore, Home com bottom-nav/rail, Ajustes) | ✅ |
| F2 Auth (login, troca obrigatória de senha, logout, sessão criptografada) | ✅ |
| F3 Convidados (busca, filtros com contadores, paginação, swipe, cards/tabela, toggle, Aprovar Todos) | ✅ |
| F4 Mural (cards, filtro com contadores, Aprovar/Rejeitar/Pendente, Aprovar Todas) | ✅ |
| F5 Exportar (XLSX próprio com abas Convidados/Mural + PDF A4 paisagem, salvos em Downloads) | ✅ |
| F6 Push Gotify (client token, WebSocket `/stream`, FGS `specialUse`, notificação, dedupe, deep link, refresh da lista) | ✅ (falta teste real no aparelho) |
| F7 Hook do PocketBase (corrigido e **validado contra o PocketBase v0.40.1 local**) | ✅ |
| F8 Ícone Sankofa (adaptive icon), documentação, build release | ✅ |

### Descobertas técnicas (validadas em execução — importante!)

1. **PocketBase v0.40.1 não tem `onRecordAfterCreateRequest`** → o hook renomeado é
   `onRecordAfterCreateSuccess`. Com o nome antigo o arquivo **falha ao carregar**
   e **nenhuma notificação sai** (era o estado de produção).
2. **`onRecordBeforeCreateRequest` também não existe** → usar `onRecordCreateRequest`.
3. **Hooks de request exigem `e.next()`** → sem isso a API responde `200` com corpo
   **vazio** e o registro **não é gravado** (sintoma observado durante os testes).
4. `onModelAfterCreateSuccess` dispara, mas `e.model` é um **objeto simples** (não um
   `Record`) — não dá para chamar `e.model.getString(...)`.
5. `PdfDocument.getPages()` devolve `List<PageInfo>` (não `Page`) → o rodapé de cada
   página precisa ser desenhado no momento em que a página é criada.
6. O WebSocket do Gotify (`/stream`) usa `X-Gotify-Key` com o **client token**;
   `sinceId` evita repetir notificações após reconexão.

### Teste end-to-end executado (hook → Gotify)

```
CREATE: 200 | id: 1d3fpz3q7yrt5yp | sheet_row_id: 74      (hook sheet_row_id OK)
notificacoes recebidas: 1
  path: /message | X-Gotify-Key: token-de-teste | Content-Type: application/json
  title: Novo RSVP | priority: 5
  message: Nome / Adultos / Crianças / Responsável / Criança (idade) / Contato / Restrições
```

### O que ainda depende do usuário

1. **Expor o Gotify por HTTPS** (subdomínio via Traefik ou proxy `/gotify/` no nginx do portal).
2. **Publicar os dois hooks** no PocketBase de produção e **reiniciar o serviço**.
3. **Instalar o APK** no celular (sideload) e **vincular o Gotify** em Ajustes.
4. Teste real: enviar um RSVP no site e confirmar a notificação com o app fechado.

---

## 15. Esclarecimento sobre os hooks (2026-10-22)

**Nenhum hook novo foi criado.** O repositório `C:\Bodas\V3\admin-portal\pb_hooks\`
contém **apenas 2 arquivos, que já existiam**:

| Arquivo | Função | O que eu alterei |
|---|---|---|
| `notify-rsvp.pb.js` | Envia o novo RSVP ao **Gotify** (`POST /message`, `X-Gotify-Key: GOTIFY_TOKEN`) | Hook `onRecordAfterCreateRequest` → `onRecordAfterCreateSuccess` + registro por `typeof` (compatível com versões antigas) + `e.next()` |
| `auto-sheet-row-id.pb.js` | Gera `sheet_row_id` (max+1) no create | Hook `onRecordBeforeCreateRequest` → `onRecordCreateRequest` (sem tag), filtro por `e.collection.name` dentro do handler, `e.next()` obrigatório |

O **hook de WhatsApp não está neste repositório** — ele vive apenas no container de
produção (EasyPanel). **Não deve ser apagado** ao publicar: basta copiar/substituir
os 2 arquivos acima e reiniciar o PocketBase.

### Regras do PocketBase v0.40 (validadas em execução real, local)

1. Hooks de **sucesso** (`onRecordAfterCreateSuccess`): registrar no topo do arquivo e o
   handler **deve chamar `e.next()`** — sem isso a API responde `200` com **corpo vazio**.
2. Hooks de **request** (`onRecordCreateRequest`): **não aceitam tag** de coleção
   (com tag → 400 / `Failed to create record`); filtrar por `e.collection.name` dentro do
   handler e chamar `e.next()` (obrigatório, senão o registro não é gravado).
3. Nunca registrar **dois** hooks de request para o mesmo evento (o segundo `e.next()` falha).

### Gotify de produção

| Item | Valor |
|---|---|
| URL | `https://websites-gotify.qlnxj1.easypanel.host` (v2.9.1) |
| `/stream` | WebSocket — handshake responde **401** sem token (endpoint OK) |
| `/client` | Criar client token (basic auth) — 401 sem credencial |
| `/message` | Enviar mensagem (App Token) — 401 sem token |
| No app | URL **já preenchida** (`AppSettings.DEFAULT_GOTIFY_URL`) |

O hook do servidor continua usando `GOTIFY_URL=http://gotify:80` (rede interna do
Docker) — isso não muda nada: o hook roda dentro do Docker. O celular fala com o
Gotify pela URL pública acima.

---

## 16. CRASH na abertura (2026-10-22) — causa raiz, correção e teste de regressão

### Sintoma
Instalado no Redmi Note 12S (Android 15): o app **abria e fechava** imediatamente.

### Causa raiz
Ordem de inicialização do `AppContainer`:

```kotlin
val guestsStore  = GuestsStore(this)   // <- construída aqui
val messagesStore = MessagesStore(this)
val rsvpEvents   = MutableSharedFlow(...)   // <- mas o fluxo só existia AQUI
val rsvpEventFlow = rsvpEvents
```

O `init` das stores já disparava uma corrotina que lia `container.rsvpEventFlow`.
Com a propriedade ainda **nula** (corrida de inicialização), vinha um
`NullPointerException` dentro de uma corrotina **sem handler** → o Android
encerra o processo → "abre e fecha".

### Correções aplicadas
| # | Correção |
|---|---|
| 1 | `rsvpEvents`/`rsvpEventFlow` declarados **antes** das stores |
| 2 | Observadores de fundo saíram do `init` das stores para `container.startBackgroundObservers()` (chamado só com o container completo) |
| 3 | Guarda determinística no `init` das stores: `val eventos = container.rsvpEventFlow; check(eventos != null)` — leitura da propriedade força NPE imediato se a ordem voltar a ser errada |
| 4 | `appScope` com `CoroutineExceptionHandler`: erro em tarefa de fundo não mata o app |
| 5 | `AdminMlApp.onCreate` com `runCatching` + `initError`, exibido em tela de erro (em vez de crash) |
| 6 | `SecureStore` cai para `SharedPreferences` simples se o Keystore falhar |
| 7 | `SessionManager.restoreSession()` com `runCatching` (falha = "sem sessão") |
| 8 | Listagens só carregam quando há sessão ativa (HomeScreen), não no start do Application |
| 9 | `Thread.setDefaultUncaughtExceptionHandler` registrando em `Log.e("AdminML", ...)` |

### Teste de regressão (Robolectric 4.15.1, sdk 34)

`app/src/test/java/com/bodasmel/adminml/`:
- `StartupTest` (3 testes): Application cria o container sem erro; fluxo existe antes das stores; `startBackgroundObservers()` não lança.
- `ScreenCompositionTest` (3 testes): `MainActivity` abre; tela de login compõe; tela do painel compõe.

**Prova de que o teste pega o bug:** reintroduzindo a ordem antiga das propriedades,
**5 dos 6 testes falham** com `rsvpEventFlow precisa existir antes das stores`.
Com a correção: **6/6 verdes**.

Comandos:
```powershell
.\gradlew.bat testDebugUnitTest      # testes
.\diagnose-crash.ps1                 # instala via adb e coleta logcat (tag AdminML)
.\diagnose-crash.ps1 -Wireless "IP:PORTA"   # depuração sem fio
```

---

## 17. Erro "unable to resolve host" ao vincular o Gotify (2026-10-22)

### Diagnóstico (feito deste PC)

| Checagem | Resultado |
|---|---|
| DNS público do host | ✅ `A 66.94.105.104` (1.1.1.1, 8.8.8.8 e local) — **sem** registro AAAA |
| `bodasmel.com.br` / `portal.bodasmel.com.br` | ✅ mesmo IP (66.94.105.104) |
| TLS / `GET /version` | ✅ `200 {"version":"2.9.1"}` |
| `WebSocket /stream` | ✅ handshake chega ao servidor (401 por token inválido = formato correto) |

Conclusão: **o endereço e o servidor estão corretos**; o erro vinha do **endereço
digitado no aparelho**. Causa mais provável: o campo já vem preenchido e a URL
foi colada por cima, gerando `https://hosthttps://host` (host inexistente →
"unable to resolve host").

### Correções no app

| # | Correção |
|---|---|
| 1 | `push/GotifyUrl.kt`: `sanitize()` remove espaços, **esquema duplicado** (fica com o último `://`), query/fragment e barra final, preservando o caminho (proxy `/gotify/`); `webSocketUrl()` deriva `wss`/`ws`; `describeFailure()` traduz DNS/TLS/conexão/timeout em pt-BR |
| 2 | Ajustes: botão **Testar Gotify** (GET /version) e **Testar PocketBase** separados |
| 3 | A tela mostra "Será usado: <endereço normalizado>" e salva o valor normalizado |
| 4 | Erro 401/403 ao criar o client token → "Usuário ou senha do Gotify incorretos." |

### Testes

| Suite | Testes | Resultado |
|---|---|---|
| `StartupTest` | 3 | ✅ (regressão do crash de abertura) |
| `ScreenCompositionTest` | 3 | ✅ (Activity + composição das telas) |
| `GotifyUrlTest` | 6 | ✅ (normalização de URL, esquema duplicado, proxy, wss) |
| `GotifyProductionSmokeTest` | 2 | ✅ **contra o Gotify de produção**: `diagnose` → Ok v2.9.1; WebSocket `/stream` → 401 (chegou ao servidor) |
| **Total** | **14** | ✅ |

### Recomendação de robustez (opcional, servidor)

Para depender do próprio domínio (evita qualquer bloqueio de `*.easypanel.host` em
redes móveis), exponha o Gotify atrás do portal:

```nginx
location /gotify/ {
    proxy_pass http://gotify:80/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_buffering off;
    proxy_read_timeout 3600s;
}
```
E no app use `https://portal.bodasmel.com.br/gotify` (o app preserva o caminho e
gera o WebSocket corretamente — coberto por `GotifyUrlTest`).

---

## 18. Versionamento SemVer amarrado ao git (2026-10-22)

### Sim, o projeto usa git

O `New-Admin-Portal` é um repositório git (branch `master`, identity `Admin M&L <admin@bodasmel.com.br>`).
Antes desta etapa o app Android **não estava versionado**; agora está, junto com os hooks.

```
2caf08c  feat(android): app nativo Admin M&L com push via Gotify (v1.0.0)
ab9dd97  fix(tools): BOM UTF-8 nos scripts PowerShell
cfe6367  feat(android): versionamento SemVer derivado do git (tag apk/vX.Y.Z) + docs
55b254e  chore: baseline completo 2.0.12 (criador do projeto)
```

### Fonte da verdade: a tag `apk/vX.Y.Z`

O Gradle lê o git no build (`providers.exec`, única API permitida no Gradle 9) e deriva:

| Estado do git | versionName | versionCode |
|---|---|---|
| Tag em HEAD + árvore limpa | `1.0.0` | nº de commits |
| N commits à frente | `1.0.0-devN` | nº de commits |
| Árvore com alterações locais | `1.0.0-devN.dirty` | nº de commits |
| Sem git | `0.0.0-dev` | 1 |

`versionCode` = número de commits → monotônico e único por build (instalar versão
nova nunca falha por "versão já instalada"). Também expostos em `BuildConfig`:
`VERSION_NAME`, `VERSION_CODE`, `GIT_COMMIT`, `GIT_TAG` — exibidos em *Ajustes*.

### Scripts

| Script | Função |
|---|---|
| `tools/bump-app-version.ps1 -Version 1.1.0` | valida SemVer, avisa sobre árvore suja, cria a tag anotada `apk/vX.Y.Z`, insere entrada no `APK/CHANGELOG.md`, `-Push` envia branch+tag |
| `APK/build-apk.ps1` | build release assinado + copia para `dist\AdminML-<versão>.apk` (lê a versão do BuildConfig gerado) |
| `APK/diagnose-crash.ps1` | instala via adb (USB ou `-Wireless IP:PORTA`) e coleta logcat (tag `AdminML`) + buffer de crash |

### Detalhes que custaram tempo (importante)

1. **Gradle 9 proíbe `ProcessBuilder` em build scripts** → usar `providers.exec`.
2. **Configuration cache desligado** (`org.gradle.configuration-cache=false`): a versão
   depende do git, e o cache serviria uma versão antiga.
3. **PowerShell 5.1 + UTF-8 sem BOM**: os "—" (em-dash) viram aspas curvas cp1252 e
   o parser quebrava ("chave de fechamento ausente"). Todos os `.ps1` agora têm BOM.
4. **`$ErrorActionPreference='Stop'` + git**: `git describe` sem tags escreve "fatal"
   em stderr → erro fatal no PS 5.1 → o script usa `Continue` na parte de git.
5. `.gitattributes` criado para o `gradlew` ficar com LF (funciona em Linux/macOS).

### O que está (e o que não está) no repositório

Versionado: `APK/` (fontes, gradle, testes, README, CHANGELOG, stack.md, scripts),
`pb_hooks/` (os 2 hooks do PocketBase corrigidos), `tools/bump-app-version.ps1`,
`.gitattributes`.

**Fora** do repositório (no `.gitignore`): `APK/local.properties` (caminho do SDK),
`APK/keystore.properties` (senhas), `APK/keystore`, `APK/dist/*.apk` (binários),
`APK/app/build`, `APK/.gradle`.

> A chave de release fica em `C:\Bodas\backup\adminml-keystore\adminml-release.jks`
> (fora do repo, junto das senhas em `keystore-credentials.txt`).

### Publicar uma versão (fluxo)

```powershell
git add -A; git commit -m "feat(android): ..."
powershell -ExecutionPolicy Bypass -File tools\bump-app-version.ps1 -Version 1.1.0
cd APK; .\build-apk.ps1      # => dist\AdminML-1.1.0.apk
```

---

## 19. UX da v1.1.0 (2026-10-22) — alinhamento com o portal web

Feedback do usuário no aparelho (Redmi Note 12S / Android 15) e o que foi feito:

| Reclamação | Correção | Arquivo |
|---|---|---|
| Notificação persistente do Gotify atrapalha | **Sem mudança** (decisão do usuário — evitar Firebase) | — |
| Mural não paginava por swipe | `Modifier.swipeToPaginate` extraído para componente compartilhado e aplicado no Mural | `ui/components/SwipeToPaginate.kt`, `ui/messages/MessagesScreen.kt` |
| Botão "Pendente" horrível | Botões do Mural no padrão do `.btn-group` da web: sem ícones, cores `#10B981`/`#EF4444`/`#F59E0B`, texto branco, 36 dp, largura 1:1:1, quebra em telas estreitas (`FlowRow`) | `ui/messages/MessagesScreen.kt` |
| Filtros mal dispostos | Empilhados e com largura total abaixo de 1025 dp (como o `flex-direction: column` do portal), fonte 16 sp e altura de toque maior | `ui/guests/GuestsScreen.kt`, `ui/components/FilterDropdown.kt` |
| Tela inicial não salva usuário e senha | "Lembrar-me" (default ligado): e-mail e senha no `EncryptedSharedPreferences` (Keystore), formulário pré-preenchido, botão "Limpar credenciais salvas" nos Ajustes; logout só limpa a sessão | `core/SecureStore.kt`, `core/SessionManager.kt`, `ui/login/*`, `ui/settings/SettingsScreen.kt` |
| Login precisa parecer com o web | Tela reconstruída: fundo `africa-day-bg.jpg`, card areia com borda dourada, logo Sankofa em círculo de 150 dp, "Portal Admin" / "Bodas de Esmeralda — M&L", labels acima dos campos, overlay "Acessando painel", versão no rodapé e *shake* no erro | `ui/login/LoginScreen.kt`, `res/drawable-nodpi/{africa_day_bg.jpg,sankofa.png}` |

Bônus de paridade: **indicador de página flutuante "3/5"** (`ui/components/PageIndicator.kt`),
igual ao `showPageIndicator` do portal.

### Testes (26 no total, todos verdes)

| Suite | n | Cobre |
|---|---|---|
| `StartupTest` | 3 | regressão do crash de abertura |
| `ScreenCompositionTest` | 3 | Activity + composição das telas |
| `LoginViewModelTest` | 6 | pré-preenchimento, switch "Lembrar-me", validações de login/troca de senha |
| `SwipeToPaginateTest` | 6 | cálculo da página de destino no swipe |
| `GotifyUrlTest` | 6 | normalização de URL |
| `GotifyProductionSmokeTest` | 2 | contra o Gotify de produção |

### Entrega

`APK/dist/AdminML-1.1.0.apk` — 14 MB, `versionName=1.1.0`, `versionCode=8`, tag `apk/v1.1.0`,
mesma chave de assinatura (instala por cima, sem desinstalar).

---

## 20. UX v1.1.1 — paridade visual com o portal web (2026-10-22)

Feedback do usuário no aparelho: *o card de notificação está muito grande, assim como o campo
de pesquisa* e *o estilo do login deve ser igual da versão web (cores, fontes e etc.)*.

### Fontes empacotadas (antes não havia nenhuma fonte no app)

| Fonte | Papel | Origem | Arquivos |
|---|---|---|---|
| **Inter** | `--font-sans` (todo o texto) | `public/lib/fonts/Inter-latin.woff2` + `Inter-latin-ext.woff2` (variáveis) | `res/font/inter_{regular,medium,semibold,bold}.ttf` + `inter_ext_*` (fallback) |
| **Playfair Display** | `--font-display` (só o título do login) | woff2 do Google Fonts (o mesmo do `fonts.css`) | `res/font/playfair_display_{semibold,bold}.ttf` |

Conversão feita com `fonttools` (`pip install fonttools brotli`): woff2 → TTF e
`fonttools.varLib.instancer` para fixar os pesos 400/500/600/700 (o Compose não precisa lidar
com fontes variáveis). São os **mesmos glifos** que o portal serve.

Aplicação: `theme/Fonts.kt` (`InterFamily`, `PlayfairFamily`) + `theme/Type.kt` (a tipografia
Material inteira passa a usar Inter, como o `body { font-family: var(--font-sans) }` da web).

### Login reescrito (paridade 1:1 com `public/css/admin.css`)

| Elemento | Web | App |
|---|---|---|
| Card | `rgba(245,230,211,0.92)`, raio 20 px (14 px no celular), `border 1px rgba(212,160,23,0.15)` | `PortalColors.LoginCardBg` + `LoginGoldBorder` |
| Logo | círculo 160 px (100 px no celular) com `background:#fff` e `object-fit:cover` | `Box` circular branco + `ContentScale.Crop` |
| Título | `--font-display` 26 px (20 px no celular), `#1e293b` | `PlayfairFamily` |
| Campos | `rgba(255,255,255,0.85)`, borda `rgba(94,58,42,0.12)`, raio 10 px, foco terracota + anel | `PortalInput` (`BasicTextField` próprio) |
| Botão | ouro `#D4A017`, raio 10 px, 48 px, 15 px/600 | idem |
| Erro | `rgba(239,68,68,0.1)` + borda `rgba(239,68,68,0.2)`, raio 10 px | `LoginErrorBg` / `LoginErrorBorder` |
| Versão | **dentro** do card, `margin-top:24px` | dentro do card |
| Breakpoints | <=480 px / 481–1024 px / >1024 px | `LocalConfiguration.screenWidthDp` |

### Campos compactos (toolbar)

- `PortalSearchField`: 44 px, ícone de lupa de 16 px, borda de 1 px, botão de limpar
  (o `OutlinedTextField` com label flutuante ocupava ~64 px).
- `FilterDropdown`: 44 px, borda de 1 px e seta à direita, no lugar do `ExposedDropdownMenuBox`
  com label flutuante (paridade com os `<select>`).
- Ambos respeitam o tema claro/escuro (`MaterialTheme.colorScheme`).

### Cards de notificação

- Serviço persistente: título "Monitorando" + texto "Conectado" (antes "Conectado ao servidor
  Gotify"), `setSilent`, `setOnlyAlertOnce`, `setLocalOnly` → card de uma linha.
- Novo RSVP: `BigTextStyle` passa a receber só a primeira linha da mensagem (antes a mensagem
  multilinha inteira, que abria um card alto).

### Ferramenta nova: captura de tela em teste

`app/src/test/java/com/bodasmel/adminml/UiScreenshotTest.kt` renderiza as telas em PNG com
Robolectric em modo de gráficos nativos (`@GraphicsMode(NATIVE)`) e salva em
`APK/app/build/screenshots/`. Serve para conferir a paridade com o portal sem aparelho
(usado para comparar o login com `https://portal.bodasmel.com.br` a 393 px).

> O `captureToImage()` do `createComposeRule` **não funciona** no Robolectric (timeout em
> `forceRedraw`); a solução é desenhar a `decorView` num `Bitmap` com `Canvas`.

### Testes (27 no total, todos verdes)

| Suite | n |
|---|---|
| `StartupTest` | 3 |
| `ScreenCompositionTest` | 3 |
| `LoginViewModelTest` | 6 |
| `SwipeToPaginateTest` | 6 |
| `GotifyUrlTest` | 6 |
| `GotifyProductionSmokeTest` | 2 |
| `UiScreenshotTest` | 2 (login e convidados em PNG) |

### Build

`APK/dist/AdminML-1.1.0-dev0.dirty.apk` — 14,4 MB, mesma chave de assinatura (instala por cima
da v1.1.0). Ainda **sem commit/tag** — a release oficial exige
`tools/bump-app-version.ps1 -Version 1.2.0` + commit.
---

## 21. Rodada 2 de UX — card igual à web, data corrigida e página rolável (2026-10-22)

Novo feedback do usuário no aparelho, com print do **portal web** como referência.

### 1. Data do convidado/mural aparecia crua — causa raiz

O PocketBase **v0.40** devolve os campos de data no formato SQLite:

```
2026-10-22 03:52:29.868Z      <- espaço no lugar do "T" (ISO seria "2026-10-22T03:52:29.868Z")
```

`Format.dateTime` usava `Instant.parse`, que **só** aceita o "T" → exceção → o `getOrDefault`
devolvia o texto cru e o card mostrava `Data: 2026-10-22 03:52:29.868Z`.

Correção (`core/Format.kt`): normaliza o separador e tenta, nesta ordem,
`OffsetDateTime(ISO_DATE_TIME)` → `Instant.parse` → `LocalDateTime` (sem fuso). Teste novo
`FormatTest` (5 casos) fixa o comportamento.

> Como conferir o formato no banco:
> `sqlite3 pb_data/data.db "select created from rsvp limit 1"` (ou `python -c "import sqlite3..."`).

### 2. Card do convidado igual ao `.guest-card` do portal

Estrutura do portal (`public/js/features/guests.js` + `public/css/components/cards.css`):

```
.guest-card        padding 16px, column, gap 8px
.guest-card-header nome (rótulo em negrito + nome normal)  |  #id
.guest-card-body   linhas (rótulo em negrito + valor), gap 4px
.guest-card-footer border-top, padding-top 10px → "Data: ..."  |  badge clicável
```

O app põe o badge no **canto superior** do card; agora ele fica no **rodapé, ao lado da data**
(mesma posição do portal). As linhas usam um `AnnotatedString` só (rótulo em negrito + valor) para
a quebra de linha continuar na margem esquerda, como o `flex-wrap` do CSS.

### 3. Busca e filtros passam a rolar com a lista

Antes: `Column` (busca + filtros) + `Box` com `LazyColumn` (lista) → a lista rolava **por dentro**
e a toolbar ficava parada no topo. Agora a tela inteira é um `Column` com `verticalScroll`
(card "Aprovar Todos", busca, filtros, total e a lista), e o `swipeToPaginate` ficou só sobre a
lista — sem brigar com o gesto vertical. Mesmo tratamento no Mural.

### 4. Texto digitado no login estava cinza (regressão da rodada anterior)

Na rodada 1, uma troca em lote trocou a cor do texto do `PortalInput` para
`MaterialTheme.colorScheme.onSurface` — que no tema **escuro** é claro. Resultado: texto
cinza-claro sobre o card areia, ilegível. O card do login é sempre claro, então a cor do texto
agora é **fixa** (`PortalColors.LightText` = `#1E293B`). O `UiScreenshotTest` renderiza os
campos preenchidos para blindar contra essa regressão.

### Testes (32 no total, todos verdes)

| Suite | n | Cobre |
|---|---|---|
| `StartupTest` | 3 | regressão do crash de abertura |
| `ScreenCompositionTest` | 3 | Activity + composição das telas |
| `LoginViewModelTest` | 6 | pré-preenchimento, switch "Lembrar-me", validações |
| `SwipeToPaginateTest` | 6 | cálculo da página de destino no swipe |
| `GotifyUrlTest` | 6 | normalização de URL |
| `GotifyProductionSmokeTest` | 2 | contra o Gotify de produção |
| `FormatTest` | 5 | **novo** — formatos de data do PocketBase/ISO |
| `UiScreenshotTest` | 4 | **novo** — PNGs de login, convidados, card e campos |