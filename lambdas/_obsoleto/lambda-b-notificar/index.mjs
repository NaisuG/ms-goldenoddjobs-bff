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
