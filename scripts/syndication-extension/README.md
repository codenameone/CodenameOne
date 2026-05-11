# Codename One Syndicator (Firefox extension)

Drives the Medium and DZone post editors from inside the user's logged-in
Firefox session, so syndication requests carry a real browser fingerprint
and `cf_clearance` cookie. This is the only way to syndicate to Medium /
DZone reliably — both sit behind aggressive Cloudflare bot detection that
rejects headless Playwright runs.

## How it fits together

```
       ┌───────────────────────────┐         ┌──────────────────────────┐
       │ Daily CI cron             │         │ User's Firefox           │
       │   blog-syndication.yml    │         │   (this extension)       │
       │                           │         │                          │
       │   1. picks eligible posts │         │   1. polls queue every   │
       │   2. publishes via APIs   │         │      30 min              │
       │      (foojay, dev.to,     │         │   2. opens editor tab    │
       │       hashnode)           │         │      per pending task    │
       │   3. appends Medium/DZone │ ──────▶ │   3. content script      │
       │      tasks to             │  poll   │      fills editor +      │
       │      syndication-queue    │  via    │      saves draft         │
       │      .json (committed)    │  raw.gh │   4. shows JSON patch    │
       └───────────────────────────┘         │      to paste back into  │
                                             │      syndication-state   │
                                             └──────────────────────────┘
```

* CI does not run the browser. It only knows which posts are eligible and
  appends a task entry per browser-only platform to
  `scripts/website/syndication-queue.json`. That commit is what makes the
  task visible to the extension.
* The extension polls the raw GitHub URL of that file. When the user's
  Firefox is online, queued tasks get processed. There is no daily
  schedule pressure — a 3-day Firefox-offline gap is fine.
* The extension writes results into its local `chrome.storage` and the
  popup UI prints a JSON patch the user can paste into
  `scripts/website/syndication-state.json` to record the syndication
  permanently. (Round-tripping the result via a GitHub PR from inside the
  extension would require a committed token; we keep that boundary simple.)

## Install (Firefox)

1. `about:debugging#/runtime/this-firefox` → **Load Temporary Add-on…**
2. Pick `scripts/syndication-extension/manifest.json`.
3. The icon shows up in the toolbar. Click it → **Poll syndication queue
   now** to test against whatever is currently in
   `syndication-queue.json`.

For permanent install (across browser restarts) the extension needs to
be signed by Mozilla — out of scope for the first version.

## Adapters

Each target site has a content script that runs on its editor URL:

* `adapters/medium.js` — Medium new-story editor. Types title, presses
  Enter, pastes body HTML via `execCommand('insertHTML')`, opens Story
  Settings panel, fills canonical URL.
* `adapters/dzone.js` — DZone Froala editor. Sets title and subtitle via
  React-style native value setters, calls
  `FroalaEditor.INSTANCES[0].html.set` for the body, clicks **Save draft**.

To add a new platform (Bluesky, Mastodon, Threads, …):

1. Add an entry under `EDITOR_URLS` in `background.js`.
2. Drop a new `adapters/<site>.js` content script that reads the task
   from `chrome.storage.local['task_for_<site>']` and reports back via
   `cn1Syndicator.report(taskId, { success, url })`.
3. Add a `content_scripts` entry in `manifest.json` for that editor URL.
4. Have CI append `{ "site": "<site>", … }` task entries.

## Producing the queue (CI side)

`scripts/website/queue_browser_syndication.py` walks the same eligible-
posts logic the API syndicator uses, then appends a task per browser
platform to `scripts/website/syndication-queue.json` (skipping anything
already in `syndication-state.json` or already in the queue).

Daily workflow runs:

```bash
python3 scripts/website/queue_browser_syndication.py --platforms medium,dzone
```

Then commits the queue file back to master so the next extension poll
picks it up.

## Caveats

* The extension is unsigned, so a temporary install must be re-loaded
  after every Firefox restart unless you self-sign or run from a
  Developer Edition with `xpinstall.signatures.required` disabled.
* Adapter selectors break when target sites redesign. Each adapter is a
  small, scoped file — fix the broken selectors and reload the extension.
* The queue is durable because it lives in the repo. A 3-day Firefox-
  offline gap just means the tasks process when the user is back.
