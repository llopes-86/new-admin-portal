// =============================================================================
// notify-rsvp.pb.js — Notifica o Gotify quando um novo RSVP e criado
// =============================================================================
// Onde: <pocketbase>/pb_hooks/notify-rsvp.pb.js (pasta persistente)
// Env vars (EasyPanel -> PocketBase -> Environment):
//   GOTIFY_URL  = http://gotify:80   (ou dominio publico do Gotify)
//   GOTIFY_TOKEN = App Token criado em Gotify -> Apps
// Apos criar: reiniciar o servico PocketBase (hooks carregam na inicializacao).
//
// -----------------------------------------------------------------------------
// REGRAS DO POCKETBASE v0.40 (validadas em execucao local)
// -----------------------------------------------------------------------------
//   * O nome antigo "onRecordAfterCreateRequest" NAO EXISTE na v0.40.
//     O hook correto e "onRecordAfterCreateSuccess". Com o nome antigo o
//     arquivo falha ao carregar e NENHUMA notificacao e enviada.
//   * Este arquivo se registra sozinho no hook correto da versao em uso
//     (o `typeof` evita erro de carregamento em versoes antigas), e o
//     handler SEMPRE chama e.next() antes de fazer o trabalho.
//   * A tag de colecao ("rsvp") funciona neste hook (tags so quebram em
//     hooks de request).
// =============================================================================

if (typeof onRecordAfterCreateSuccess === "function") {
  // PocketBase >= 0.23 (inclusive v0.40)
  onRecordAfterCreateSuccess(
    function (e) {
      e.next();
      try {
        var url = $os.getenv("GOTIFY_URL");
        var token = $os.getenv("GOTIFY_TOKEN");
        if (!url || !token) {
          console.error("Gotify env vars not set (GOTIFY_URL / GOTIFY_TOKEN)");
          return;
        }

        var r = e.record;
        if (!r) return;

        var msg = [];
        msg.push("Nome: " + (r.getString("nome") || "-"));
        msg.push("Adultos: " + (r.getString("adulto") || "0"));
        msg.push("Crianças: " + (r.getString("criancas") || "0"));

        if (r.getString("responsavel")) {
          msg.push("Responsável: " + r.getString("responsavel"));
        }
        if (r.getString("nome_crianca")) {
          msg.push("Criança: " + r.getString("nome_crianca") +
            (r.getString("idade") ? " (" + r.getString("idade") + " anos)" : ""));
        }
        if (r.getString("contato")) {
          msg.push("Contato: " + r.getString("contato"));
        }
        if (r.getString("restricoes") && r.getString("restricoes") !== "Nenhuma") {
          msg.push("Restrições: " + r.getString("restricoes"));
        }

        $http.send({
          url: url + "/message",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Gotify-Key": token
          },
          body: JSON.stringify({
            title: "Novo RSVP",
            message: msg.join("\n"),
            priority: 5
          }),
          timeout: 10
        });
      } catch (err) {
        console.error("Gotify notify failed: " + err);
      }
    },
    "rsvp"
  );
} else if (typeof onRecordAfterCreateRequest === "function") {
  // PocketBase <= 0.22 (hook antigo)
  onRecordAfterCreateRequest(
    function (e) {
      try { e.next(); } catch (_) {}
      try {
        var url = $os.getenv("GOTIFY_URL");
        var token = $os.getenv("GOTIFY_TOKEN");
        if (!url || !token) {
          console.error("Gotify env vars not set (GOTIFY_URL / GOTIFY_TOKEN)");
          return;
        }

        var r = e.record;
        if (!r) return;

        var msg = [];
        msg.push("Nome: " + (r.getString("nome") || "-"));
        msg.push("Adultos: " + (r.getString("adulto") || "0"));
        msg.push("Crianças: " + (r.getString("criancas") || "0"));

        if (r.getString("responsavel")) {
          msg.push("Responsável: " + r.getString("responsavel"));
        }
        if (r.getString("nome_crianca")) {
          msg.push("Criança: " + r.getString("nome_crianca") +
            (r.getString("idade") ? " (" + r.getString("idade") + " anos)" : ""));
        }
        if (r.getString("contato")) {
          msg.push("Contato: " + r.getString("contato"));
        }
        if (r.getString("restricoes") && r.getString("restricoes") !== "Nenhuma") {
          msg.push("Restrições: " + r.getString("restricoes"));
        }

        $http.send({
          url: url + "/message",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Gotify-Key": token
          },
          body: JSON.stringify({
            title: "Novo RSVP",
            message: msg.join("\n"),
            priority: 5
          }),
          timeout: 10
        });
      } catch (err) {
        console.error("Gotify notify failed: " + err);
      }
    },
    "rsvp"
  );
} else {
  console.error("notify-rsvp.pb.js: nenhum hook de create compativel encontrado");
}
