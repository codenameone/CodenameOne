import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const source = fs.readFileSync(new URL('../assets/js/cn1-acquisition.js', import.meta.url), 'utf8');
const KEY = 'cn1-acquisition-v1';
const values = new Map();
const store = { getItem: k => values.get(k) || null, setItem: (k,v) => values.set(k,v), removeItem: k => values.delete(k) };
const tags = '?utm_source=bluesky&utm_medium=social&utm_campaign=social-2026-10&utm_content=2026-10-20-v2-b1';
const link = href => ({ href, getAttribute() { return this.href; }, setAttribute(_,v) { this.href=v; }, closest() { return this; } });
function load(search=tags, consent='accepted', storage=store) {
  const links = [link('https://cloud.codenameone.com/console/'),
    link('https://cloud.codenameone.com/secure/signup?level=pro&mode=annual#pay'),
    link('https://cloud.codenameone.com/register'), link('https://evil.example/console/'),
    link('/pricing/'), link('https://cloud.codenameone.com.evil.example/console/'),
    link('https://cloud.codenameone.com/api/v2/build?utm_source=untouched')];
  const events = {}, winEvents = {};
  const document = { cookie: `cn1_crisp_consent_v2=${consent}`, querySelectorAll: () => links,
    addEventListener: (k,v) => events[k]=v };
  const window = { location: { search, href:'https://www.codenameone.com/blog/demo/'+search },
    addEventListener: (k,v) => winEvents[k]=v };
  vm.runInNewContext(source, {window,document,sessionStorage:storage,localStorage:store,URL,URLSearchParams,Date,Map,Set});
  return {links,events,winEvents,document};
}
function content(l) { return new URL(l.href).searchParams.get('utm_content'); }
values.clear();
let s=load();
assert.equal(content(s.links[0]),'2026-10-20-v2-b1');
assert.equal(new URL(s.links[1].href).searchParams.get('level'),'pro');
assert.equal(new URL(s.links[1].href).hash,'#pay');
assert.equal(content(s.links[2]),'2026-10-20-v2-b1');
assert.equal(s.links[3].href,'https://evil.example/console/');
assert.equal(s.links[4].href,'/pricing/');
assert.equal(s.links[5].href,'https://cloud.codenameone.com.evil.example/console/');
assert.equal(s.links[6].href,'https://cloud.codenameone.com/api/v2/build?utm_source=untouched');
assert.equal(content(load('').links[0]),'2026-10-20-v2-b1','survives internal navigation');
assert.equal(content(load(tags.replace('bluesky','x').replace('-b1','-x1')).links[0]),'2026-10-20-v2-b1','first touch survives later campaign');
s.links[1].href='https://cloud.codenameone.com/secure/signup?level=pro&mode=monthly';
s.events.auxclick({target:s.links[1]});
assert.equal(new URL(s.links[1].href).searchParams.get('mode'),'monthly');
assert.equal(content(s.links[1]),'2026-10-20-v2-b1');
s.document.cookie='cn1_crisp_consent_v2=declined';
s.winEvents['cn1-consent-changed']();
assert.equal(content(s.links[0]),null,'withdrawal restores untagged links');
assert.equal(values.has(KEY),false);
for (const consent of ['declined','']) {
  s=load(tags,consent);
  assert.equal(content(s.links[0]),null);
  assert.equal(values.has(KEY),false);
}
s=load(tags,'');
s.document.cookie='cn1_crisp_consent_v2=accepted';
s.winEvents['cn1-consent-changed']();
assert.equal(content(s.links[0]),'2026-10-20-v2-b1','consent acceptance on landing captures tags');
const old=JSON.parse(values.get(KEY));old.at=Date.now()-25*3600000;values.set(KEY,JSON.stringify(old));
assert.equal(content(load('').links[0]),null,'expired touch not forwarded');
values.clear();
assert.equal(content(load('?utm_source=x&utm_medium=social&utm_campaign=bad%20campaign').links[0]),null);
assert.equal(content(load('?utm_source=codenameone&utm_medium=website&utm_campaign=homepage').links[0]),null,'internal topic click not acquisition');
const blocked={getItem(){throw Error();},setItem(){throw Error();},removeItem(){throw Error();}};
assert.equal(content(load(tags,'accepted',blocked).links[0]),'2026-10-20-v2-b1','current-page fallback');
values.clear();
const mixed=load();mixed.links[0].href='https://cloud.codenameone.com/console/?utm_source=other&utm_term=other';
mixed.events.click({target:mixed.links[0]});
assert.equal(new URL(mixed.links[0].href).searchParams.get('utm_term'),null,'never mix attribution tuples');
console.log('Acquisition navigation, consent, expiry, dynamic links and origin-boundary checks passed');
