// Prueba local sin AWS: node lambdas/_test/run.mjs
import assert from 'node:assert/strict';

process.env.SUPABASE_URL = 'https://fake.supabase.co';
process.env.SUPABASE_SERVICE_KEY = 'k';
process.env.NOTICE_URL = 'https://api.example.com/api/notice';
process.env.API_SECRET = 'secreto';

const llamadas = [];
globalThis.fetch = async (url, opts) => {
  llamadas.push({ url: String(url), opts });
  if (opts.method === 'HEAD') return { ok: true, headers: { get: () => '0-0/5' } };
  if (String(url).includes('/api/notice')) return { ok: true, status: 201, text: async () => '' };
  return { ok: true, status: 204, text: async () => '' };
};

const evento = { eventId: '11111111-1111-1111-1111-111111111111', tipo: 'INFORME_SOLICITADO', informeId: 7,
  solicitanteSub: 'sub-1', tipoInforme: 'RESUMEN_GENERAL' };
const conSns = { Records: [{ messageId: 'm1', body: JSON.stringify({ Message: JSON.stringify(evento) }) }] };
const crudo = { Records: [{ messageId: 'm2', body: JSON.stringify(evento) }] };

const A = await import('../lambda-a-generar-informe/index.mjs');
const B = await import('../lambda-b-notificar/index.mjs');

// Lambda A (envuelto por SNS y crudo)
for (const ev of [conSns, crudo]) {
  llamadas.length = 0;
  const r = await A.handler(ev);
  assert.deepEqual(r.batchItemFailures, []);
  const patch = llamadas.find((c) => c.opts.method === 'PATCH');
  assert.ok(patch.url.includes(`event_id=eq.${evento.eventId}`));
  assert.equal(JSON.parse(patch.opts.body).estado, 'GENERADO');
  assert.match(JSON.parse(patch.opts.body).resumen, /Solicitudes en la plataforma: 5/);
}

// Lambda B
llamadas.length = 0;
const rb = await B.handler(conSns);
assert.deepEqual(rb.batchItemFailures, []);
assert.equal(llamadas[0].opts.headers['x-api-secret'], 'secreto');
const body = JSON.parse(llamadas[0].opts.body);
assert.equal(body.eventId, evento.eventId);
assert.equal(body.destinatarioSub, 'sub-1');

// Fallo: /api/notice devuelve 401 -> el mensaje se reporta como fallido (SQS lo reintenta)
globalThis.fetch = async () => ({ ok: false, status: 401, text: async () => 'no' });
const rf = await B.handler(conSns);
assert.deepEqual(rf.batchItemFailures, [{ itemIdentifier: 'm1' }]);

console.log('OK: lambdas A y B (SNS envuelto, crudo, éxito y fallo)');
