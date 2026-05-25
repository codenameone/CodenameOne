# Browser-syndication runner (Playwright)

Drives Medium and DZone editors locally via headed Playwright + your
**system Firefox** (not Playwright's bundled Firefox build, which
Cloudflare reliably fingerprints and challenge-loops). Each site gets
a dedicated persistent profile under
`scripts/website/syndication-auth/<site>-profile/` so cookies survive
across runs without sharing your day-to-day Firefox profile.

## Why Playwright instead of an extension

| Wall | Extension | Headed Playwright |
|---|---|---|
| Cloudflare bot detection | Bypassed (real browser session) | Bypassed (saved storage state with `cf_clearance` cookie) |
| Medium's `isTrusted` input gate | **Blocks all synthetic events** — content is invisible to Medium's editor model, autosave never fires | Routes input through Firefox's real input pipeline — events carry `isTrusted=true`, accepted as if typed |

## One-time setup

```bash
# Inside the repo root, in a venv if you prefer
pip install playwright
playwright install firefox

# Capture session cookies + cf_clearance for each site (interactive):
python scripts/website/syndicate_login.py --site medium
python scripts/website/syndicate_login.py --site dzone
```

Each `syndicate_login.py` invocation launches your system Firefox with
a dedicated profile under `scripts/website/syndication-auth/<site>-profile/`,
opens the site's login URL, and watches the page URL. When you finish
signing in and navigate to the verify URL the script prints, it
auto-detects the navigation and exits cleanly. The profile retains
cookies + `cf_clearance` between runs. Re-run when cookies expire
(Cloudflare clearance ~30 days; site logins last longer).

The whole `syndication-auth/` directory is gitignored — never commit it.

## Weekly run

```bash
python scripts/website/syndicate_browser.py
```

Reads `scripts/website/syndication-queue.json` (still produced by the
existing `queue_browser_syndication.py`), opens a headed Firefox per
task using the appropriate `syndication-auth/<site>.json` storage
state, drives the editor, and writes the resulting draft URL into
`scripts/website/syndication-state.json`.

Useful flags:

```bash
# Preview only:
python scripts/website/syndicate_browser.py --dry-run

# One specific task:
python scripts/website/syndicate_browser.py --task-id medium:liquid-glass-material-3-modern-native-themes

# Only one platform:
python scripts/website/syndicate_browser.py --site dzone
```

The browser windows are visible — if anything goes sideways (Cloudflare
challenge re-pops, Medium UI changed, DZone hits a validation banner)
you can intervene manually before the script's timeout.

## What each driver does

**Medium (`/new-story`):**
1. Locate `h3[data-testid="editorTitleParagraph"]`, click, type title with
   `page.keyboard.type` (real keystrokes → Medium's typing handler fires).
2. Press Enter, type body markdown line-by-line. Medium converts `## ` →
   heading, `* ` → bullet, ``` ``` ``` → code block, `**bold**` → bold,
   `[text](url)` → link on the fly.
3. Wait for autosave to redirect to `/p/<id>/edit`. Record that URL.

**DZone (`/articles/post.html`):**
1. `page.fill` title / subtitle / meta-description / original-source URL.
2. Click article-type dropdown, click matching item.
3. `page.set_input_files` for the cover image (downloaded to a tempfile
   from `task.cover_image_url`).
4. `page.evaluate` to call `FroalaEditor.INSTANCES[0].html.set(html)` —
   runs in the page world (no xray issues), Froala accepts cleanly.
5. Click Save Draft. Record resulting URL.

## Scheduling (macOS launchd)

Once you've verified the flow works end-to-end, install the launchd
LaunchAgent so the runner fires daily without manual invocation. The
schedule fires after the daily blog-syndication.yml GitHub Action
commits any new queue entries, so the wrapper picks them up on the
next run.

```bash
# Install
cp scripts/website/com.codenameone.syndication.plist ~/Library/LaunchAgents/
launchctl load ~/Library/LaunchAgents/com.codenameone.syndication.plist

# Trigger now (one-shot test)
launchctl start com.codenameone.syndication

# Watch what happens
tail -f scripts/website/logs/syndication-$(date +%Y-%m-%d).log

# Uninstall
launchctl unload ~/Library/LaunchAgents/com.codenameone.syndication.plist
rm ~/Library/LaunchAgents/com.codenameone.syndication.plist
```

The wrapper script (`scripts/website/run-syndication.sh`) does:

1. `git pull --ff-only origin master` to grab any queue entries CI
   appended since the last run.
2. Runs `syndicate_browser.py`. The runner self-skips if no tasks are
   pending (no Chrome window opens), so the schedule is safe to fire
   daily.
3. If `syndication-state.json` changed, commits just that one file
   and pushes so the next run (and CI) sees it.

Per-run logs land in `scripts/website/logs/syndication-YYYY-MM-DD.log`.
launchd's own stdio is captured separately in
`scripts/website/logs/launchd.out` and `launchd.err`. The whole `logs/`
directory is gitignored.

The default schedule is daily at 14:00 local time — edit the
`StartCalendarInterval` block in the plist to change it. If you want
the runner to also fire once at login, set `RunAtLoad` to `true`.

Heads-up: the runner opens visible Chrome windows because Cloudflare
blocks headless. If you're away from the machine when it fires, the
windows still open but anomalies (Cloudflare re-challenge, Medium UI
change, etc.) won't get human intervention. Check the per-day log
file when you return.
