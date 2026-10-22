// =============================================================================
// auto-sheet-row-id.pb.js — Gera sheet_row_id automatico (max+1) no create
// =============================================================================
// Onde: <pocketbase>/pb_hooks/auto-sheet-row-id.pb.js (pasta persistente)
// Motivo: o front-end nao le mais a colecao para calcular o proximo id
//         (listRule foi restrita). O id agora e gerado no servidor.
// Apos criar: reiniciar o servico PocketBase (hooks carregam na inicializacao).
//
// -----------------------------------------------------------------------------
// REGRAS DO POCKETBASE v0.40 (validadas em execucao local)
// -----------------------------------------------------------------------------
//   * Hook de create na versao nova: onRecordCreateRequest
//     (a versao antiga usava onRecordBeforeCreateRequest)
//   * Hooks de request EXIGEM e.next() — sem isso a API responde HTTP 200
//     com corpo VAZIO e o registro NAO e gravado.
//   * Hooks de request NAO aceitam tag de colecao no v0.40: com tag a
//     requisicao nao continua (erro "Failed to create record" / 400).
//     Por isso o filtro de colecao e feito dentro do handler.
//   * Nao registre DOIS hooks de request para o mesmo evento.
// =============================================================================

if (typeof onRecordCreateRequest === "function") {
  // PocketBase >= 0.23 (inclusive v0.40)
  onRecordCreateRequest(function (e) {
    try {
      if (!e.record) return;
      if (e.collection.name !== "rsvp" && e.collection.name !== "messages") {
        return;
      }

      var records = e.app.findRecordsByFilter(
        e.collection.name,
        "",
        "-sheet_row_id",
        1,
        0
      );

      var last = 0;
      if (records.length > 0) {
        last = parseInt(records[0].get("sheet_row_id"), 10) || 0;
      }

      e.record.set("sheet_row_id", last + 1);
    } catch (err) {
      console.error("auto-sheet-row-id failed: " + err);
    } finally {
      // sempre libera a requisicao (uma unica vez)
      e.next();
    }
  });
} else if (typeof onRecordBeforeCreateRequest === "function") {
  // PocketBase <= 0.22
  onRecordBeforeCreateRequest(function (e) {
    try {
      if (!e.record) return;
      if (e.collection.name !== "rsvp" && e.collection.name !== "messages") {
        return;
      }

      var records = e.app.findRecordsByFilter(
        e.collection.name,
        "",
        "-sheet_row_id",
        1,
        0
      );

      var last = 0;
      if (records.length > 0) {
        last = parseInt(records[0].get("sheet_row_id"), 10) || 0;
      }

      e.record.set("sheet_row_id", last + 1);
    } catch (err) {
      console.error("auto-sheet-row-id failed: " + err);
    } finally {
      e.next();
    }
  });
} else {
  console.error("auto-sheet-row-id.pb.js: nenhum hook de create compativel encontrado");
}
