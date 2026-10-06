// End-to-end check of the JavaScript Playground in a real (headless) browser: user code is
// compiled by the in-tree Java compiler running in the page, translated by the ParparVM
// translator running in the page, and loaded into the running VM.
//
//   node tools/verify-playground-browser.mjs <playground-url>
//
// It runs every bundled sample, then one session through the editor: a compile error is
// reported with its line, typed code runs, a re-run redefines classes of the same name,
// an exception thrown by a listener after the run is reported, and a listener that shows
// a second form replaces the preview. Screenshots go to PLAYGROUND_BROWSER_ARTIFACT_DIR
// when it is set. Exits non-zero when any check fails.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error('Import from "playwright" failed:', String(playwrightError));
    console.error('Import from "@playwright/test" failed:', String(playwrightTestError));
    process.exit(2);
  }
}

const url = process.argv[2];
if (!url) {
  console.error('Usage: node tools/verify-playground-browser.mjs <playground-url>');
  process.exit(2);
}
const artifactDir = process.env.PLAYGROUND_BROWSER_ARTIFACT_DIR || '';
const here = path.dirname(fileURLToPath(import.meta.url));
if (artifactDir) fs.mkdirSync(artifactDir, {recursive: true});

// The sample list is read from the sources so a new sample is covered without editing this
// file; the slug rule mirrors PlaygroundExamples.slugify.
function sampleSlugs() {
  const source = fs.readFileSync(path.join(here, '..', 'common', 'src', 'main', 'java', 'com', 'codenameone',
      'playground', 'PlaygroundExamples.java'), 'utf8');
  const slugs = [];
  for (const m of source.matchAll(/new Sample\("([^"]+)"/g)) {
    slugs.push(m[1].toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, ''));
  }
  return slugs;
}

function encodeCode(code) {
  return Buffer.from(code).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

const results = [];
function check(name, ok, detail) {
  results.push({ name, ok: !!ok });
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (detail ? ' -- ' + detail : ''));
}

async function shot(page, name) {
  if (artifactDir) {
    await page.screenshot({ path: path.join(artifactDir, name + '.png') });
  }
}

const browser = await chromium.launch({ headless: true });
try {
  // 1. Every bundled sample compiles, loads and runs.
  const slugs = sampleSlugs();
  check('found the bundled samples', slugs.length > 10, slugs.length + ' samples');
  for (const slug of slugs) {
    const page = await browser.newPage({ viewport: { width: 1280, height: 860 } });
    let outcome = null;
    const errors = [];
    page.on('console', m => {
      const t = m.text();
      if (/PARPAR:ERROR|Missing virtual method|\[playground\].*failed/.test(t)) errors.push(t);
      if (t.startsWith('[playground]') && outcome === null) {
        outcome = t;
      }
    });
    page.on('pageerror', e => errors.push(e.message));
    await page.goto(url + '?sample=' + slug, { waitUntil: 'domcontentloaded', timeout: 90000 });
    for (let i = 0; i < 600 && outcome === null; i++) {
      await page.waitForTimeout(150);
    }
    // Initialization can succeed before a worker animation/listener callback fails.
    await page.waitForTimeout(1000);
    check('sample ' + slug, outcome && outcome.includes('preview updated') && errors.length === 0,
        (outcome || 'no outcome') + (errors.length ? ' pageerrors=' + errors.slice(0, 2).join(' | ') : ''));
    await shot(page, 'sample-' + slug);
    await page.close();
  }

  // 2. One editing session.
  const page = await browser.newPage({ viewport: { width: 1280, height: 860 } });
  const log = [];
  page.on('console', m => {
    const t = m.text();
    if (t.startsWith('PG-') || t.startsWith('[playground]') || t.includes('Exception')) {
      log.push(t);
    }
  });
  page.on('pageerror', e => log.push('[pageerror] ' + e.message));
  async function waitFor(pred, ms) {
    for (let i = 0; i < ms / 100; i++) {
      const hit = log.find(pred);
      if (hit) {
        return hit;
      }
      await page.waitForTimeout(100);
    }
    return null;
  }
  // Replaces the editor's text through its real input path: the lightweight editor takes
  // keyboard input through a hidden textarea that gains focus when the editor is clicked.
  async function setCode(code) {
    await page.mouse.click(300, 190);
    await page.waitForFunction(() => {
      const input = document.querySelector('textarea.cn1-lightweight-text-input');
      return input && document.activeElement === input;
    }, null, { timeout: 15000 });
    await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A');
    await page.keyboard.insertText(code);
  }
  // The preview scales with the window. Use the current semantic bounds and
  // send a real canvas click rather than assuming a particular phone position.
  async function clickPreviewButton(name) {
    const control = page.getByRole('button', {name, exact: true});
    await control.waitFor({timeout: 15000});
    const box = await control.boundingBox();
    if (!box || box.height < 2) throw new Error(name + ' is not visible');
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  }

  await page.goto(url + '?code=' + encodeCode('Label l = new Label("x");\nint n = "not a number";\nl\n'),
      { waitUntil: 'domcontentloaded', timeout: 90000 });
  let r = await waitFor(l => l.startsWith('[playground]'), 90000);
  check('a compile error is reported with its line', r && r.includes('failed: 2:') && r.includes('incompatible types'), r);
  await shot(page, 'flow-compile-error');

  log.length = 0;
  await setCode('class Greeter { String hi() { return "v1"; } }\nSystem.out.println("PG-RUN " + new Greeter().hi());\n'
      + 'Label l = new Label(new Greeter().hi());\nl\n');
  r = await waitFor(l => l.startsWith('PG-RUN'), 30000);
  check('typed code compiles and runs', r === 'PG-RUN v1', r);

  log.length = 0;
  await setCode('class Greeter { String hi() { return "v2"; } }\nSystem.out.println("PG-RUN " + new Greeter().hi());\n'
      + 'Label l = new Label(new Greeter().hi());\nl\n');
  r = await waitFor(l => l.startsWith('PG-RUN'), 30000);
  check('a re-run redefines classes of the same name', r === 'PG-RUN v2', r);

  // A class whose VM name is a host class's (java/lang/String is java_lang_String) must be
  // refused before it is evaluated, and leave the running VM intact for the next run.
  log.length = 0;
  await setCode('class java_lang_String { }\nLabel l = new Label("clash");\nl\n');
  r = await waitFor(l => l.startsWith('[playground]'), 30000);
  check('a class clashing with a host class is refused', r && r.includes('failed') && r.includes('clashes'), r);
  log.length = 0;
  await setCode('System.out.println("PG-RUN after " + "x".length());\nLabel l = new Label("ok");\nl\n');
  r = await waitFor(l => l.startsWith('PG-RUN'), 30000);
  check('the VM still works after the refused class', r === 'PG-RUN after 1', r);

  log.length = 0;
  await setCode('Button b = new Button("Boom");\nb.addActionListener(e -> { String s = null; s.length(); });\n'
      + 'Container root = BoxLayout.encloseY(b);\nroot\n');
  r = await waitFor(l => l.startsWith('[playground] preview updated'), 30000);
  await clickPreviewButton('Boom');
  r = await waitFor(l => l.includes('NullPointerException'), 10000);
  await page.waitForTimeout(500);
  await shot(page, 'flow-listener-exception');
  check('an exception thrown by a listener is reported', r, r);

  log.length = 0;
  await setCode('Button b = new Button("Next");\nb.addActionListener(e -> { Form f = new Form("Second", BoxLayout.y()); '
      + 'f.add(new Label("second screen")); f.show(); System.out.println("PG-SHOWN"); });\n'
      + 'Container root = BoxLayout.encloseY(b);\nroot\n');
  r = await waitFor(l => l.startsWith('[playground] preview updated'), 30000);
  await clickPreviewButton('Next');
  r = await waitFor(l => l.startsWith('PG-SHOWN'), 10000);
  await page.waitForTimeout(1500);
  await shot(page, 'flow-second-form');
  check('a listener can show a second form', r, r);
  await page.close();
} finally {
  await browser.close();
}

const failed = results.filter(x => !x.ok).length;
console.log(failed ? failed + ' of ' + results.length + ' checks FAILED' : 'All ' + results.length + ' checks passed');
process.exitCode = failed ? 1 : 0;
