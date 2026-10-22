# Changelog — Admin M&L (app Android)

Todas as mudanças do app Android. O versionamento segue SemVer e é controlado
pelas tags ``apk/vX.Y.Z`` deste repositório.

## 1.2.0 - 2026-10-22

UX do app alinhada ao portal web: login com as fontes e cores do site, card de
convidado igual ao da web (badge de status ao lado da data), data no formato correto,
busca e filtros rolando com a lista e cards de notificação compactos.

### Adicionado
- Fontes do portal empacotadas no app: **Inter** (`--font-sans`, texto de toda a interface) e
  **Playfair Display** (`--font-display`, título "Portal Admin"), geradas a partir dos mesmos
  woff2 servidos pelo site (subconjuntos latin e latin-ext, pesos 400/500/600/700).
- Ferramenta de conferência visual: teste `UiScreenshotTest` renderiza as telas do app em PNG
  (`APK/app/build/screenshots/`) para comparação lado a lado com o portal.

### Corrigido
- Login reconstruído com a paridade da web: card areia translúcido com borda dourada, logo
  Sankofa recortada em círculo branco, título em Playfair, campos com borda terracota e anel de
  foco, botão dourado, overlay "Acessando painel" e versão **dentro** do card. Os breakpoints da
  web (<=480 px, 481–1024 px, >1024 px) são respeitados.
- **Texto digitado no login legível**: a cor do texto passou a ser sempre escura (`#1E293B`),
  independente do tema claro/escuro do app — antes seguia o tema e ficava cinza-claro sobre o
  card areia.
- **Data do convidado e do mural**: o PocketBase v0.40 devolve as datas no formato SQLite
  ("2026-10-22 03:52:29.868Z", com espaço) e o app caía no `catch` do `Instant.parse`, mostrando
  a data crua. Agora aceita os formatos com e sem "T" e com deslocamento de fuso.
- **Badge de status ao lado da data**: o card do convidado ficou igual ao `.guest-card` do portal
  (cabeçalho com nome e #id, corpo com os dados, rodapé com "Data" e o badge clicável), em vez do
  badge no canto superior.
- **Busca e filtros não ficam fixos no topo**: a tela de convidados e a do mural agora rolam
  inteiras (a busca, os filtros e o total saem de vista ao deslizar), como no portal web.
- Campo de pesquisa da lista de convidados: 44 px de altura (antes era um `OutlinedTextField`
  com label flutuante, ~64 px), com ícone de lupa e botão de limpar — igual ao `.search-input`.
- Filtros de Status e Categoria: 44 px, borda de 1 px e seta à direita, no lugar do campo
  Material com label flutuante (paridade com os `<select>` do portal).
- Cards de notificação: o card do serviço persistente ("Monitorando") e o aviso de novo RSVP
  passaram a ocupar uma única linha, sem texto longo nem expansão.


## 1.1.0 - 2026-10-22

Melhorias de UX alinhadas ao portal web.

### Adicionado
- Tela de login com a identidade visual do portal: fundo `africa-day-bg.jpg`, card areia com
  borda dourada, logo Sankofa em círculo, títulos "Portal Admin" / "Bodas de Esmeralda — M&L",
  rótulos acima dos campos, overlay "Acessando painel", versão no rodapé e *shake* no erro.
- **Lembrar-me**: e-mail e senha guardados criptografados (Android Keystore) e formulário
  pré-preenchido. Botão "Limpar credenciais salvas" nos Ajustes.
- Swipe horizontal para paginar também no Mural (antes só funcionava nos Convidados).
- Indicador de página flutuante "3/5" (igual ao portal).

### Corrigido
- Botões Aprovar / Rejeitar / **Pendente** do Mural: agora seguem o `.btn-group` do portal
  (cores `#10B981` / `#EF4444` / `#F59E0B`, texto branco, sem ícones, largura igual).
- Filtros de Status e Categoria: empilhados e com largura total no celular, como o
  `flex-direction: column` do portal; fonte 16 px e altura de toque maior.
- Tela do Mural não paginava por swipe.


## 1.0.0 - 2026-10-22

Primeira versão do app nativo (Kotlin/Compose) do Portal Admin das Bodas de Esmeralda M&L.

### Adicionado
- Login no PocketBase com troca obrigatória de senha e sessão criptografada (Android Keystore).
- Lista de convidados com busca por nome/responsável/telefone, filtros de status e categoria
  com contadores, paginação (25/pág), swipe horizontal, badge de status clicável e "Aprovar Todos".
- Mural com moderação (Aprovar/Rejeitar/Pendente), filtro com contadores e "Aprovar Todas".
- Exportar XLSX (abas `Convidados` e `Mural`) e PDF (A4 paisagem) salvos em Downloads.
- Notificações push via Gotify: Foreground Service + WebSocket `/stream`, com deep link para
  a aba de convidados e deduplicação por id de mensagem.
- Tela de Ajustes: URL do PocketBase, URL do Gotify (com normalização), vínculo do aparelho,
  diagnóstico de conexão e tema claro/escuro.
- Ícone adaptativo com a marca Sankofa.
- Versionamento SemVer derivado das tags `apk/vX.Y.Z` do git.

### Corrigido
- Crash na abertura (corrida de inicialização do `AppContainer`).
- "unable to resolve host" ao vincular o Gotify (URL colada por cima do campo preenchido).

[1.0.0]: https://github.com/llopes-86/new-admin-portal/releases/tag/apk/v1.0.0
[1.1.0]: https://github.com/llopes-86/new-admin-portal/releases/tag/apk/v1.1.0
[1.2.0]: https://github.com/llopes-86/new-admin-portal/releases/tag/apk/v1.2.0
