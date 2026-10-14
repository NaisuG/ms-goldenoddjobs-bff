// Prueba local sin AWS: node lambdas/_test/run.mjs
import assert from 'node:assert/strict';

process.env.SUPABASE_URL = 'https://fake.supabase.co';
process.env.SUPABASE_SERVICE_KEY = 'k';
process.env.MAIL_FROM = 'admin@correo.cl';

const llamadas = [];
let informeEstado = 'PENDIENTE';
globalThis.fetch = async (url, opts = {}) => {
  const u = String(url);
  llamadas.push({ url: u, opts });
  const ok = (cuerpo, total) => ({ ok: true, status: 200, headers: { get: () => `0-0/${total}` }, json: async () => cuerpo, text: async () => '' });
  if (opts.method === 'POST' || opts.method === 'PATCH') return { ok: true, status: 201, text: async () => '' };
  if (u.includes('/informe?')) return ok([{ estado: informeEstado }], 1);
  if (u.includes('/solicitud?')) return ok([{ id: 1, titulo: 'App <b>web</b>', tipo_app: 'WEB', estado: 'ABIERTA', creada_en: '2026-10-01T10:00:00Z' }], 5);
  if (u.includes('/postulacion?')) return ok([{ id: 9, solicitud_id: 1, precio_propuesto: 150000, creada_en: '2026-10-02T10:00:00Z' }], 3);
  if (u.includes('/registro_informe?')) return ok([{ usuario_nombre: 'Ana Admin', fecha_solicitud: '2026-10-14T13:00:00Z' }], 2);
  return { ok: false, status: 500, text: async () => 'inesperado' };
};

const evento = {
  eventId: '11111111-1111-1111-1111-111111111111', tipo: 'INFORME_SOLICITADO', informeId: 7,
  solicitanteSub: 'sub-1', solicitanteNombre: 'Ana Admin', email: 'ana@correo.cl',
  tipoInforme: 'RESUMEN_GENERAL', solicitadoEn: '2026-10-14T13:00:00Z',
};
const conSns = { Records: [{ messageId: 'm1', body: JSON.stringify({ Message: JSON.stringify(evento) }) }] };
const crudo = { Records: [{ messageId: 'm2', body: JSON.stringify(evento) }] };

const L1 = await import('../lambda-1-registrar-solicitud/index.mjs');
const L2 = await import('../lambda-2-generar-informe/index.mjs');

// Lambda 1: guarda usuario y fecha (SNS envuelto y crudo)
for (const ev of [conSns, crudo]) {
  llamadas.length = 0;
  const r = await L1.handler(ev);
  assert.deepEqual(r.batchItemFailures, []);
  const post = llamadas.find((c) => c.opts.method === 'POST');
  assert.ok(post.url.includes('/registro_informe'));
  assert.match(post.opts.headers.Prefer, /ignore-duplicates/);
  const fila = JSON.parse(post.opts.body);
  assert.equal(fila.usuario_nombre, 'Ana Admin');
  assert.equal(fila.fecha_solicitud, evento.solicitadoEn);
  assert.equal(fila.event_id, evento.eventId);
}

// Lambda 1: Supabase falla -> el mensaje se reporta como fallido
const fetchOk = globalThis.fetch;
globalThis.fetch = async () => ({ ok: false, status: 500, text: async () => 'boom' });
assert.deepEqual((await L1.handler(conSns)).batchItemFailures, [{ itemIdentifier: 'm1' }]);
globalThis.fetch = fetchOk;

// Lambda 2: arma el HTML, lo "envía" y marca GENERADO
const enviados = [];
llamadas.length = 0;
await L2.procesar(evento, { enviar: async (m) => enviados.push(m) });
assert.equal(enviados.length, 1);
assert.equal(enviados[0].to, 'ana@correo.cl');
assert.equal(enviados[0].from, 'admin@correo.cl');
assert.match(enviados[0].html, /Solicitudes en la plataforma: <b>5<\/b>/);
assert.match(enviados[0].html, /Ana Admin/);
assert.ok(!enviados[0].html.includes('<b>web</b>'), 'el contenido de la BD debe ir escapado');
assert.match(enviados[0].html, /&lt;b&gt;web&lt;\/b&gt;/);
const patch = llamadas.find((c) => c.opts.method === 'PATCH');
assert.equal(JSON.parse(patch.opts.body).estado, 'GENERADO');

// Lambda 2: idempotente (ya GENERADO -> no envía otro correo)
informeEstado = 'GENERADO';
enviados.length = 0;
await L2.procesar(evento, { enviar: async (m) => enviados.push(m) });
assert.equal(enviados.length, 0);
informeEstado = 'PENDIENTE';

// Lambda 2: sin correo de destino -> ERROR y no reintenta
llamadas.length = 0;
await L2.procesar({ ...evento, email: null }, { enviar: async (m) => enviados.push(m) });
assert.equal(enviados.length, 0);
assert.equal(JSON.parse(llamadas.find((c) => c.opts.method === 'PATCH').opts.body).estado, 'ERROR');

// Lambda 2: si SES falla, el mensaje se reintenta (no se marca GENERADO)
llamadas.length = 0;
await assert.rejects(L2.procesar(evento, { enviar: async () => { throw new Error('SES no verificado'); } }));
assert.ok(!llamadas.some((c) => c.opts.method === 'PATCH'));

console.log('OK: lambda 1 (registro) y lambda 2 (HTML + correo): éxito, duplicado, sin destinatario y fallo de SES');
