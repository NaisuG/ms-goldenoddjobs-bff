// Lambda B — "Notificar"
// Disparada por la cola SQS B (suscrita al mismo topic SNS). Por cada evento hace un
// POST a /api/notice de la PROPIA app (vía API Gateway) con el header x-api-secret.
//
// Node.js 20: fetch global, sin dependencias.
// Variables de entorno: NOTICE_URL (https://<api-id>.execute-api.us-east-1.amazonaws.com/api/notice), API_SECRET

const { NOTICE_URL, API_SECRET } = process.env;

export function parseEvento(body) {
  const parsed = JSON.parse(body);
  return typeof parsed.Message === 'string' ? JSON.parse(parsed.Message) : parsed;
}

export async function procesar(evento) {
  const res = await fetch(NOTICE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'x-api-secret': API_SECRET },
    body: JSON.stringify({
      eventId: evento.eventId,
      destinatarioSub: evento.solicitanteSub,
      tipo: evento.tipo,
      titulo: 'Informe procesado',
      mensaje: `Tu informe ${evento.tipoInforme} (#${evento.informeId}) fue procesado.`,
    }),
  });
  // 201 = creada, 200 = duplicado ignorado por el BFF (ambos son éxito)
  if (!res.ok) {
    throw new Error(`/api/notice respondió ${res.status}: ${await res.text()}`);
  }
  console.log(`Notificación enviada para event ${evento.eventId} (HTTP ${res.status})`);
}

export const handler = async (event) => {
  const fallidos = [];
  for (const record of event.Records ?? []) {
    try {
      await procesar(parseEvento(record.body));
    } catch (err) {
      console.error('Error procesando', record.messageId, err);
      fallidos.push({ itemIdentifier: record.messageId });
    }
  }
  return { batchItemFailures: fallidos };
};
