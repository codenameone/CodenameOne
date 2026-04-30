# Skin Designer device database

`devices.json` (bundled at `common/src/main/resources/devices.json`) is the
catalog the Skin Designer wizard shows in the device picker. It is generated
directly from GSMArena by `build_devices_json.py` and refreshed by CI.

## Why scrape directly?

We previously sourced the catalog from third-party Kaggle / GitHub dumps
(e.g. `KhalSCI/GSMARENA_Scraper`, `foykes/gsm-arena-dataset`). They go stale
the moment the maintainer stops updating them — at the time we switched they
topped out at iPhone 16, so anything Apple announced after September 2024
was missing. Scraping GSMArena ourselves means whatever is on the site today
is in the bundled catalog after the next CI run.

## How the scraper behaves

`build_devices_json.py` is stdlib-only and:

1. Fetches the brand listing page (e.g. `apple-phones-48.php`).
2. Walks the pagination on each brand page.
3. Follows every phone link to its detail page.
4. Reads the `data-spec="..."` rows for resolution / PPI / OS / display size /
   year, plus the `<title>` for the canonical model name.
5. Normalises everything (notch / island / hole heuristics, safe-area insets,
   font defaults) into the JSON schema below.
6. Merges into the existing `devices.json` so partial / aborted scrapes
   never lose history.

To stay polite to GSMArena and avoid IP bans we:

- Sleep `--delay` seconds (default 1 s) between requests with random jitter.
- Cache fetched HTML to `tools/devicedb/cache/` (gzipped). Re-runs only
  re-fetch what cache-restore missed.
- Retry transient failures with exponential backoff.
- Cap each brand at `--max-pages` listing pages.

## Output schema (per device)

```
{
  "id":        "apple_iphone_15_pro",
  "brand":     "Apple",
  "name":      "iPhone 15 Pro",
  "os":        "iOS 17, upgradable to iOS 18",
  "year":      2023,
  "form":      "Phone" | "Tablet" | "Foldable",
  "tablet":    false,
  "w":         1179,           // resolution in pixels
  "h":         2556,
  "ppi":       460,
  "inches":    6.1,
  "platform":  "ios" | "and",
  "hasNotch":  false,
  "hasIsland": true,
  "hasHole":   false,
  "hasHome":   true,
  "safeTop":   59,
  "safeBottom": 34,
  "fonts":     {"system": "SF Pro", "prop": "SF Pro", "mono": "SF Mono",
                 "small": 12, "medium": 15, "large": 22}
}
```

## Refreshing locally

Default scrape (the curated brand list — Apple, Samsung, Google, OnePlus,
Xiaomi, OPPO, Vivo, Motorola, Sony, Nokia, Asus, LG, HTC, BlackBerry, Honor,
Realme, Nothing — about 1500 phones, 30–60 min):

    python3 scripts/skindesigner/tools/devicedb/build_devices_json.py

Faster sanity run (limit to 8 phones from one brand, ~30 s):

    python3 build_devices_json.py --brands apple-phones-48 \
        --max-pages 1 --limit 8 --out /tmp/test-devices.json

Full scrape (every brand on `makers.php3`; multi-hour, only run when the
curated list misses a brand someone needs):

    python3 build_devices_json.py --full

## Refreshing in CI

`.github/workflows/skin-designer-devices-update.yml` runs the scraper on the
1st of each month and opens an automated PR if the JSON drifted. The HTML
cache is persisted across runs via `actions/cache` so the scrape only
re-fetches phones whose pages changed. Run it manually via the Actions tab
when you want a fresh dump.

## Notes / caveats

- Notch / island / hole flags are heuristics keyed off the model name
  (iPhone X-14 → notch, iPhone 14 Pro / 15+ → island, Android post-2019 →
  hole-punch). GSMArena does not have structured fields for these. Users
  can override per-skin in the editor.
- Safe-area insets are rough defaults by device class — also user-editable.
- Pixel ratio (Codename One's pixels-per-millimeter) is computed as
  `ppi / 25.4` when the wizard writes `skin.properties`. PPI itself comes
  straight from GSMArena.
- IP bans: if the scraper starts seeing 429s or HTML we can't parse,
  bump `--delay` and let the cache from previous runs carry us through.
