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

async function contar(tabla) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${tabla}?select=id`, {
    method: 'HEAD',
    headers: { ...headers(), Prefer: 'count=exact' },
  });
  if (!res.ok) return 0;
  const range = res.headers.get('content-range');
  return Number(range?.split('/')[1] ?? 0) || 0;
}

export async function procesar(evento) {
  const solicitudes = await contar('solicitud');
  const postulaciones = await contar('postulacion');
  const resumen = `Solicitudes en la plataforma: ${solicitudes}. Postulaciones recibidas: ${postulaciones}.`;

  const res = await fetch(`${SUPABASE_URL}/rest/v1/informe?event_id=eq.${evento.eventId}&estado=neq.GENERADO`, {
    method: 'PATCH',
    headers: { ...headers(), Prefer: 'return=minimal' },
    body: JSON.stringify({ estado: 'GENERADO', resumen, generado_en: new Date().toISOString() }),
  });
  if (!res.ok) {
    throw new Error(`Supabase respondió ${res.status}: ${await res.text()}`);
  }
  console.log(`Informe ${evento.informeId} (event ${evento.eventId}) marcado GENERADO`);
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
