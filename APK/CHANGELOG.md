# Changelog — Admin M&L (app Android)

Todas as mudanças do app Android. O versionamento segue SemVer e é controlado
pelas tags ``apk/vX.Y.Z`` deste repositório.

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
