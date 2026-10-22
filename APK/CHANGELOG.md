# Changelog — Admin M&L (app Android)

Todas as mudanças do app Android. O versionamento segue SemVer e é controlado
pelas tags ``apk/vX.Y.Z`` deste repositório.

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

[1.0.0]: https://github.com/bodasmel/new-admin-portal/releases/tag/apk/v1.0.0
