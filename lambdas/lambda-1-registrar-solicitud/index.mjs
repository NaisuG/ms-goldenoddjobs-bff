// Lambda 1: registra quién pidió el informe y cuándo.
// Se dispara desde la cola SQS 1. Guarda una fila en la tabla registro_informe de Supabase (API REST).

const { SUPABASE_URL, SUPABASE_SERVICE_KEY } = process.env;

const headers = () => ({
  apikey: SUPABASE_SERVICE_KEY,
  Authorization: `Bearer ${SUPABASE_SERVICE_KEY}`,
  'Content-Type': 'application/json',
});

export function parseEvento(body) {
  const parsed = JSON.parse(body);
  return typeof parsed.Message === 'string' ? JSON.parse(parsed.Message) : parsed;
}

export async function procesar(evento) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/registro_informe?on_conflict=event_id`, {
    method: 'POST',
    headers: { ...headers(), Prefer: 'resolution=ignore-duplicates,return=minimal' },
    body: JSON.stringify({
      event_id: evento.eventId,
      usuario_nombre: evento.solicitanteNombre ?? evento.email ?? evento.solicitanteSub,
      usuario_email: evento.email ?? null,
      fecha_solicitud: evento.solicitadoEn,
    }),
  });
  if (!res.ok) {
    throw new Error(`Supabase respondió ${res.status}: ${await res.text()}`);
  }
  console.log(`Registrado informe ${evento.informeId} pedido por ${evento.solicitanteNombre} (${evento.solicitadoEn})`);
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
