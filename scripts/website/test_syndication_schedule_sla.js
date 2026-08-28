const assert = require('node:assert/strict');
const { expectedRunWindow } = require('./syndication_schedule_sla.js');

const beforeDeadline = expectedRunWindow(
  '2026-08-28T15:16:59Z',
  13,
  17,
  2
);
assert.equal(beforeDeadline.expectedStart.toISOString(), '2026-08-27T13:17:00.000Z');
assert.equal(beforeDeadline.deadline.toISOString(), '2026-08-27T15:17:00.000Z');

const atDeadline = expectedRunWindow(
  '2026-08-28T15:17:00Z',
  13,
  17,
  2
);
assert.equal(atDeadline.expectedStart.toISOString(), '2026-08-28T13:17:00.000Z');
assert.equal(atDeadline.deadline.toISOString(), '2026-08-28T15:17:00.000Z');

const lateEvening = expectedRunWindow(
  '2026-08-28T18:45:00Z',
  13,
  17,
  2
);
assert.equal(lateEvening.expectedStart.toISOString(), '2026-08-28T13:17:00.000Z');

assert.throws(
  () => expectedRunWindow('not-a-date', 13, 17, 2),
  /finite numbers/
);

console.log('Syndication schedule SLA tests passed.');
