// Lambda A — "Generar informe"
// Disparada por la cola SQS A (suscrita al topic SNS). Por cada evento:
//   1) cuenta solicitudes y postulaciones en Supabase (API REST)
//   2) marca el informe como GENERADO con su resumen
//
// Node.js 20: usa fetch global, sin dependencias (en el Learner Lab no se puede hacer npm install).
// Variables de entorno: SUPABASE_URL (https://<ref>.supabase.co), SUPABASE_SERVICE_KEY

const { SUPABASE_URL, SUPABASE_SERVICE_KEY } = process.env;

const headers = () => ({
  apikey: SUPABASE_SERVICE_KEY,
  Authorization: `Bearer ${SUPABASE_SERVICE_KEY}`,
  'Content-Type': 'application/json',
});

/** SNS entrega {"Message": "<json>"} dentro del body de SQS, salvo con "raw message delivery". Soporta ambos. */
export function parseEvento(body) {
  const parsed = JSON.parse(body);
  return typeof parsed.Message === 'string' ? JSON.parse(parsed.Message) : parsed;
}

async function contar(tabla) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${tabla}?select=id`, {
    method: 'HEAD',
    headers: { ...headers(), Prefer: 'count=exact' },
  });
  if (!res.ok) return 0; // la tabla aún no existe
  const range = res.headers.get('content-range'); // "0-4/5" o "*/0"
  return Number(range?.split('/')[1] ?? 0) || 0;
}

export async function procesar(evento) {
  const solicitudes = await contar('solicitud');
  const postulaciones = await contar('postulacion');
  const resumen = `Solicitudes en la plataforma: ${solicitudes}. Postulaciones recibidas: ${postulaciones}.`;

  const res = await fetch(`${SUPABASE_URL}/rest/v1/informe?event_id=eq.${evento.eventId}&estado=neq.GENERADO`, {
    method: 'PATCH', // idempotente: si ya está GENERADO el filtro no toca nada
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
  // Con "Report batch item failures" activado en el trigger, solo se reintentan los mensajes fallidos
  return { batchItemFailures: fallidos };
};
