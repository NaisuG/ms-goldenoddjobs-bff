// Lambda 2: genera el informe en HTML leyendo Supabase y lo envía por correo (Amazon SES).
// Se dispara desde la cola SQS 2.
//                       MAIL_TO (opcional: destinatario si el evento no trae correo)

const { SUPABASE_URL, SUPABASE_SERVICE_KEY, MAIL_FROM, MAIL_TO } = process.env;

const headers = () => ({
  apikey: SUPABASE_SERVICE_KEY,
  Authorization: `Bearer ${SUPABASE_SERVICE_KEY}`,
  'Content-Type': 'application/json',
});

export function parseEvento(body) {
  const parsed = JSON.parse(body);
  return typeof parsed.Message === 'string' ? JSON.parse(parsed.Message) : parsed;
}

const esCorreo = (v) => typeof v === 'string' && v.includes('@');

async function leer(tabla, consulta) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${tabla}?${consulta}`, {
    headers: { ...headers(), Prefer: 'count=exact' },
  });
  if (res.status === 404) return { filas: [], total: 0 };
  if (!res.ok) throw new Error(`Supabase respondió ${res.status} leyendo ${tabla}: ${await res.text()}`);
  const total = Number(res.headers.get('content-range')?.split('/')[1] ?? 0) || 0;
  return { filas: await res.json(), total };
}

async function actualizarInforme(eventId, cambios) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/informe?event_id=eq.${eventId}`, {
    method: 'PATCH',
    headers: { ...headers(), Prefer: 'return=minimal' },
    body: JSON.stringify(cambios),
  });
  if (!res.ok) throw new Error(`Supabase respondió ${res.status} actualizando informe: ${await res.text()}`);
}

const esc = (v) =>
  String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const fecha = (iso) => (iso ? new Date(iso).toLocaleString('es-CL', { timeZone: 'America/Santiago' }) : '');

const tabla = (titulos, filas) =>
  `<table style="border-collapse:collapse;width:100%;font-size:14px">` +
  `<tr>${titulos.map((t) => `<th style="text-align:left;border-bottom:2px solid #1d3557;padding:6px">${esc(t)}</th>`).join('')}</tr>` +
  (filas.length
    ? filas.map((f) => `<tr>${f.map((c) => `<td style="border-bottom:1px solid #ddd;padding:6px">${esc(c)}</td>`).join('')}</tr>`).join('')
    : `<tr><td colspan="${titulos.length}" style="padding:6px;color:#777">Sin datos</td></tr>`) +
  `</table>`;

export function generarHtml(evento, datos) {
  const { solicitudes, postulaciones, registros } = datos;
  return `<!doctype html><html lang="es"><body style="font-family:Arial,sans-serif;color:#222;max-width:720px;margin:auto">
<h1 style="color:#1d3557">Informe Golden-Oddjobs</h1>
<p>Solicitado por <b>${esc(evento.solicitanteNombre ?? evento.email)}</b> el ${esc(fecha(evento.solicitadoEn))}.<br>
Generado el ${esc(fecha(new Date().toISOString()))}.</p>
<h2>Resumen</h2>
<ul><li>Solicitudes en la plataforma: <b>${solicitudes.total}</b></li>
<li>Postulaciones recibidas: <b>${postulaciones.total}</b></li>
<li>Informes solicitados hasta ahora: <b>${registros.total}</b></li></ul>
<h2>Últimas solicitudes</h2>
${tabla(['#', 'Título', 'Tipo', 'Estado', 'Creada'], solicitudes.filas.map((s) => [s.id, s.titulo, s.tipo_app, s.estado, fecha(s.creada_en)]))}
<h2>Últimas postulaciones</h2>
${tabla(['#', 'Solicitud', 'Precio propuesto', 'Fecha'], postulaciones.filas.map((p) => [p.id, p.solicitud_id, p.precio_propuesto, fecha(p.creada_en)]))}
<h2>Últimos informes pedidos</h2>
${tabla(['Usuario', 'Fecha'], registros.filas.map((r) => [r.usuario_nombre, fecha(r.fecha_solicitud)]))}
</body></html>`;
}

async function enviarConSes({ from, to, subject, html }) {
  const { SESClient, SendEmailCommand } = await import('@aws-sdk/client-ses');
  const ses = new SESClient({ region: process.env.AWS_REGION ?? 'us-east-1' });
  await ses.send(new SendEmailCommand({
    Source: from,
    Destination: { ToAddresses: [to] },
    Message: {
      Subject: { Data: subject, Charset: 'UTF-8' },
      Body: { Html: { Data: html, Charset: 'UTF-8' } },
    },
  }));
}

export async function procesar(evento, { enviar = enviarConSes } = {}) {
  const actual = await leer('informe', `select=estado&event_id=eq.${evento.eventId}`);
  if (actual.filas[0]?.estado === 'GENERADO') {
    console.log(`Informe ${evento.eventId} ya estaba generado, se omite`);
    return;
  }

  const destino = esCorreo(evento.email) ? evento.email : MAIL_TO;
  if (!esCorreo(destino)) {
    await actualizarInforme(evento.eventId, { estado: 'ERROR', resumen: 'No hay correo de destino para enviar el informe' });
    console.error(`Informe ${evento.eventId}: sin destinatario`);
    return; // reintentar no ayudaría
  }

  const datos = {
    solicitudes: await leer('solicitud', 'select=id,titulo,tipo_app,estado,creada_en&order=creada_en.desc&limit=25'),
    postulaciones: await leer('postulacion', 'select=id,solicitud_id,precio_propuesto,creada_en&order=creada_en.desc&limit=25'),
    registros: await leer('registro_informe', 'select=usuario_nombre,fecha_solicitud&order=fecha_solicitud.desc&limit=10'),
  };
  const html = generarHtml(evento, datos);

  await enviar({ from: MAIL_FROM, to: destino, subject: 'Informe Golden-Oddjobs', html });

  const resumen = `Solicitudes: ${datos.solicitudes.total}. Postulaciones: ${datos.postulaciones.total}. Enviado a ${destino}.`;
  await actualizarInforme(evento.eventId, { estado: 'GENERADO', resumen, generado_en: new Date().toISOString() });
  console.log(`Informe ${evento.informeId} enviado a ${destino}`);
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
