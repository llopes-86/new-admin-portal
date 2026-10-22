# Admin M&L — App Android (APK)

Aplicativo nativo (Kotlin/Compose) do Portal Admin das Bodas de Esmeralda M&L.
Substitui/Complementa o portal web com **notificações em segundo plano** para cada
novo RSVP enviado pelo site.

---

## 1. O que o app faz

| Área | Implementação |
|---|---|
| Login | E-mail + senha no PocketBase; troca obrigatória de senha (`must_change_password`); sessão criptografada no aparelho |
| Convidados | Lista com busca (nome/responsável/telefone), filtros de status e categoria **com contadores**, paginação (25/pág), **swipe horizontal** para paginar, badge de status clicável (Pendente ⇄ Confirmado), "Aprovar Todos", contagem de pendentes na navegação, destaque de *Necessidades Especiais* |
| Mural | Cards de mensagem, filtro (Todas/Pendentes/Aprovadas/Rejeitadas) com contadores, Aprovar/Rejeitar/Pendente, "Aprovar Todas", paginação (12/pág) |
| Exportar | **XLSX** (abas `Convidados` e `Mural`) e **PDF** (A4 paisagem, cabeçalho colorido, linhas alternadas, rodapé com página) — salvam em **Downloads** e abrem no app de visualização do celular |
| Notificações | Foreground Service mantendo WebSocket no Gotify: cada RSVP novo gera uma **notificação push** mesmo com o app fechado. Tocar na notificação abre o app na aba de convidados |
| Tema | Claro/escuro com a paleta exata do portal web (`#0F1A2E`, `#F5E6D3`, verde `#059669`) |

---

## 2. Como instalar

1. Copie `dist/AdminML-<versão>.apk` para o celular (cabo, WhatsApp, Drive…).
2. No celular: **Configurações → Apps → Acesso especial → Instalar apps desconhecidos** → habilite o app que vai abrir o arquivo (Files/Chrome) para o aplicativo "Admin M&L".
3. Toque no `.apk` e instale. (O APK é assinado com a chave de release — atualizações futuras exigem a mesma chave.)
4. Instale também pela USB, se preferir:
   ```powershell
   adb install -r dist\AdminML-1.0.0.apk
   ```

---

## 3. Configuração no app (2 telas de Ajustes)

Abra o app → ícone de **Ajustes** (engrenagem):

| Campo | Valor |
|---|---|
| **URL do PocketBase** | `https://portal.bodasmel.com.br/api/pb` (já é o padrão) |
| **URL do Gotify** | Já vem preenchida: `https://websites-gotify.qlnxj1.easypanel.host` (a app normaliza o endereço, então colar a URL por cima do campo preenchido não quebra) |
| **Usuário / Senha do Gotify** | Credenciais do seu usuário Gotify (usadas **uma única vez** para criar o *client token* deste aparelho) |

Antes de vincular, toque em **Testar Gotify**: o app responde se o endereço foi resolvido
(DNS), se o certificado HTTPS está ok e se o servidor respondeu. Erros são explicados
("Não foi possível resolver...", "Usuário ou senha incorretos", etc.).

Depois de preencher, toque em **Vincular este aparelho**. O app:
1. cria um *client token* no Gotify (`POST /client`);
2. guarda o token de forma criptografada (Android Keystore);
3. inicia o Foreground Service (aparece a notificação persistente "Monitorando confirmações");
4. passa a emitir notificação a cada novo RSVP.

> Se você preferir não informar a senha no app, crie o client token direto no Gotify
> (Usuário → Clients → New Client) e use o app oficial do Gotify como canal reserva.

---

## 4. Expor o Gotify para o celular (obrigatório)

**Já resolvido:** o seu Gotify de produção está em
`https://websites-gotify.qlnxj1.easypanel.host` (Gotify v2.9.1) e o `/stream`
aceita WebSocket — o app usa exatamente esse endereço, que já vem preenchido
nos Ajustes. Se um dia mudar de host, é só editar o campo.

Se o PocketBase enviar as notificações para `http://gotify:80` (rede interna
do Docker), isso continua funcionando: o hook roda **dentro** do Docker e
alcança o Gotify por nome de serviço. O celular fala com o Gotify pela URL
pública acima.

**Opção A — subdomínio (recomendado)**
```
gotify.bodasmel.com.br  →  http://gotify:80
```
O Traefik já termina TLS; nada a mudar no app além de apontar a URL para `https://gotify.bodasmel.com.br`.

**Opção B — proxy no portal**
Adicione ao `admin-portal_nginx.conf`:
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
Nesse caso a URL do Gotify no app é `https://portal.bodasmel.com.br/gotify`
(o app normaliza `http` → `https` e `ws` → `wss` automaticamente).

---

## 5. Hook do PocketBase (lado servidor)

`pb_hooks/notify-rsvp.pb.js` envia a mensagem ao Gotify a cada novo RSVP e
`pb_hooks/auto-sheet-row-id.pb.js` gera o `sheet_row_id`.

Ambos já foram **corrigidos e validados contra o PocketBase v0.40.1** (ver §7).
Para publicar:

```bash
cp pb_hooks/notify-rsvp.pb.js      <pocketbase>/pb_hooks/
cp pb_hooks/auto-sheet-row-id.pb.js <pocketbase>/pb_hooks/
# reiniciar o serviço PocketBase no EasyPanel (hooks carregam no boot)
```

Variáveis de ambiente (EasyPanel → PocketBase → Environment):
```
GOTIFY_URL=http://gotify:80
GOTIFY_TOKEN=<App Token criado em Gotify → Apps>
```

> O `GOTIFY_TOKEN` é o **App Token** (usado para *enviar*). O aparelho usa o
> **Client Token** (criado pelo próprio app) para *receber*.

---

## 6. Compilar

```powershell
# build completo (debug + release assinado)
.\build-apk.ps1

# ou direto
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleRelease
```

Saída: `app\build\outputs\apk\release\app-release.apk` (copiado para `dist\`).

**Keystore de release** (guardar fora do repositório — sem ele não há atualizações):
```
C:\Bodas\backup\adminml-keystore\adminml-release.jks
alias: adminml
```
O arquivo `keystore.properties` (gitignored) aponta para ele.

---

## 7. Notas técnicas discovered (importantes para manutenção)

1. **PocketBase v0.40.1 não tem `onRecordAfterCreateRequest`** — o hook renomeado é
   `onRecordAfterCreateSuccess`. Com o nome antigo o arquivo **falha ao carregar**
   e nenhuma notificação é enviada (foi o que acontecia em produção).
2. **`onRecordBeforeCreateRequest` também não existe** — use `onRecordCreateRequest`.
3. **Hooks de request exigem `e.next()`** — sem isso a API responde `200` com corpo
   **vazio** e o registro **não é gravado** (foi exatamente o sintoma observado).
4. O WebSocket do Gotify (`/stream`) usa header `X-Gotify-Key` com o **client token**;
   `sinceId` evita repetir notificações após reconexão.
5. O Foreground Service usa `foregroundServiceType="specialUse"` — o tipo `dataSync`
   tem limite de 6 h/dia no Android 15+.
