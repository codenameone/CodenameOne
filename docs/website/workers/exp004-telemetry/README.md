# EXP-004 telemetry counter

This Worker mirrors the four consent-gated EXP-004 Crisp events into a
queryable first-party counter:

- `POST /api/exp004/session` issues a random, short-lived capability token for
  a consented browser session. Issuance requires same-origin browser fetch
  metadata and is limited at Cloudflare's edge to six sessions per source per
  minute.
- `POST /api/exp004/collect` accepts only the four exact event names from the
  production Codename One origin and requires the token bound to that random
  session ID and arm.
- `GET /api/exp004/snapshot` returns cumulative and UTC-daily arm counts.
- A SQLite-backed Durable Object deduplicates random per-session event IDs and
  guarantees that an attributed download has a matching exposure.

The counter stores no Crisp session ID, account identifier, email, IP address,
user agent, referrer, or geography. It stores only random UUIDs, capability
tokens, arm, event kind, and server timestamp. Cloudflare's rate-limit binding
uses the connecting address transiently as its counter key; the Worker does
not persist or return it. The browser calls the counter only after the existing
Crisp consent gate accepted and queued the corresponding Crisp event. Failed
posts retry with the same persisted event ID so retries cannot inflate counts.

The original experiment started at `2026-08-27T04:35:14Z`. The snapshot keeps
that timestamp but also exposes `coverage_start` and
`coverage_complete_from_original_start: false`, because Crisp-only events from
before this counter's deployment cannot be recovered through the available
scoped API. Do not combine the two windows or treat pre-counter events as zero.

Run the integration against a real local Workers runtime with:

```sh
node test/integration.mjs
```

The website workflow also deploys an isolated PR Worker, runs the same HTTP
integration at Cloudflare's edge, deletes that preview, and deploys the
production Worker before the site. The GitHub Cloudflare token therefore needs
the standard Edit Cloudflare Workers permissions, including Workers Scripts
and the `codenameone.com` Workers route.
