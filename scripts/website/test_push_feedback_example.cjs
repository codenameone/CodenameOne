#!/usr/bin/env node
// Exercise the published cleanup code against SQLite, including retry rollback.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {DatabaseSync} = require('node:sqlite');
const root = path.resolve(__dirname, '../..');
const article = fs.readFileSync(path.join(root,
  'docs/website/content/blog/push-feedback-retire-dead-tokens.md'), 'utf8');
const blocks = [...article.matchAll(/```javascript\n([\s\S]*?)```/g)].map(m => m[1]);
assert.equal(blocks.length, 3, 'registration, identity, and transaction examples');
const guide = fs.readFileSync(path.join(root,
  'docs/demos/javascript/src/main/snippets/developer-guide/push-notifications.js'), 'utf8');
assert.ok(guide.includes(blocks[1].trim()), 'guide and article use the same event identity helper');
const java = article.match(/```java\n([\s\S]*?)```/)[1]
  .replace(/^\s*\/\/.*$/gm, '').replace(/\s+/g, ' ').trim();
const receiver = fs.readFileSync(path.join(root,
  'docs/demos/backend/src/main/java/com/codenameone/developerguide/backend/PushFeedback.java'), 'utf8');
assert.ok(receiver.replace(/\s+/g, ' ').includes(java), 'Java excerpt matches the guide receiver');
const context = vm.createContext({require(name) {
  assert.equal(name, 'node:sqlite');
  return {DatabaseSync: class extends DatabaseSync {
    constructor() { super(':memory:'); }
  }};
}});
vm.runInContext(blocks.join('\n') + '\nthis.api = {db, mobileTarget, cleanupTarget, applyInvalidTarget};', context);
const {db, mobileTarget, cleanupTarget, applyInvalidTarget} = context.api;
const insert = db.prepare('INSERT INTO devices VALUES (?, ?, ?, ?)');
const count = table => db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get().n;
const register = (organization, key) => {
  const {provider, target} = mobileTarget(key);
  insert.run(organization, provider, target, key);
};
const invalid = (target, extra = {}) => ({provider: 'fcm', token: target,
  reason: 'INVALID_TARGET', at: 1790178322418, ...extra});
try {
  assert.equal(mobileTarget('cn1-gcm-abc').provider, 'fcm');
  assert.equal(mobileTarget('cn1-ios-abc').provider, 'apns');
  assert.equal(mobileTarget('cn1-fcm-abc-def').target, 'abc-def');
  register('org', 'cn1-gcm-abc');
  register('org', 'cn1-ios-abc');
  register('other-org', 'cn1-fcm-abc');
  const event = invalid('abc', {deliveryId: 'delivery-1'});
  applyInvalidTarget('org', event);
  assert.equal(count('devices'), 2, 'delete the legacy key, preserve other provider and organization');
  assert.equal(count('applied_feedback'), 1);
  applyInvalidTarget('org', event);
  assert.equal(count('applied_feedback'), 1, 'v3 retry is deduplicated');

  for (const target of ['classic-a', 'classic-b']) {
    register('org', 'cn1-fcm-' + target);
    applyInvalidTarget('org', invalid(target));
    applyInvalidTarget('org', invalid(target));
  }
  assert.equal(count('devices'), 2);
  assert.equal(count('applied_feedback'), 3, 'classic devices do not share a null marker');
  assert.notEqual(cleanupTarget(invalid('classic-a')).eventKey,
    cleanupTarget(invalid('classic-a', {at: 1790178322419})).eventKey);

  insert.run('org', 'web', 'https://push.example/sub?q=1', 'cn1-web-original-with-key-material');
  applyInvalidTarget('org', {provider: 'web', endpoint: 'https://push.example/sub?q=1',
    reason: 'INVALID_TARGET', at: 1790178322418});
  assert.equal(count('devices'), 2, 'web endpoint matches its normalized column');
  register('org', 'cn1-fcm-replacement');
  applyInvalidTarget('org', invalid('old-key'));
  assert.equal(count('devices'), 3, 'an old failure cannot delete a replacement');
  applyInvalidTarget('org', invalid('replacement', {reason: 'TRANSIENT_FAILURE'}));
  assert.equal(count('devices'), 3);
  assert.throws(() => applyInvalidTarget('org', invalid('replacement', {at: undefined})));

  register('org', 'cn1-fcm-fail-delete');
  const before = count('applied_feedback');
  db.exec(`CREATE TRIGGER fail_cleanup BEFORE DELETE ON devices
    WHEN OLD.target = 'fail-delete' BEGIN SELECT RAISE(ABORT, 'test failure'); END;`);
  assert.throws(() => applyInvalidTarget('org', invalid('fail-delete')), /test failure/);
  assert.equal(count('applied_feedback'), before, 'failed delete rolls back its marker');
  assert.equal(count('devices'), 4);
  db.exec('DROP TRIGGER fail_cleanup');
  applyInvalidTarget('org', invalid('fail-delete'));
  assert.equal(count('devices'), 3, 'retry completes after database repair');
  assert.equal(count('applied_feedback'), before + 1);
  console.log('Push article examples: normalization, scope, v3/classic retries, web targets, replacement keys, and rollback passed.');
} finally {
  db.close();
}
