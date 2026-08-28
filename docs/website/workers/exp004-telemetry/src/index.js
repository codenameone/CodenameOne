import { DurableObject } from "cloudflare:workers";

const EXPERIMENT_ID = "EXP-004";
const ORIGINAL_EXPERIMENT_START = "2026-08-27T04:35:14.000Z";
const COUNTER_NAME = "homepage-positioning";
const MAX_BODY_BYTES = 1024;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const EVENT_PATTERN = /^Exp004(Ownership|Reach)(Exposure|Download)$/;
const ALLOWED_ORIGINS = new Set([
  "https://www.codenameone.com",
  "https://codenameone.com",
]);

const responseHeaders = {
  "Cache-Control": "no-store",
  "Content-Type": "application/json; charset=utf-8",
  "X-Content-Type-Options": "nosniff",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: responseHeaders });
}

async function readBoundedJson(request) {
  const declaredLength = Number(request.headers.get("content-length"));
  if (Number.isFinite(declaredLength) && declaredLength > MAX_BODY_BYTES) {
    throw new Error("payload_too_large");
  }
  if (!request.body) {
    throw new Error("missing_body");
  }

  const reader = request.body.getReader();
  const chunks = [];
  let length = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    length += value.byteLength;
    if (length > MAX_BODY_BYTES) {
      await reader.cancel();
      throw new Error("payload_too_large");
    }
    chunks.push(value);
  }

  const bytes = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  try {
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch (error) {
    throw new Error("invalid_json", { cause: error });
  }
}

function parseEvent(payload) {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    return null;
  }
  const match = typeof payload.event === "string"
    ? payload.event.match(EVENT_PATTERN) : null;
  if (!match || !UUID_PATTERN.test(payload.event_id || "") ||
      !UUID_PATTERN.test(payload.session_key || "")) {
    return null;
  }
  return {
    event: payload.event,
    eventId: payload.event_id.toLowerCase(),
    sessionKey: payload.session_key.toLowerCase(),
    arm: match[1].toLowerCase(),
    kind: match[2].toLowerCase(),
  };
}

export class Exp004Counter extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    ctx.blockConcurrencyWhile(async () => {
      this.ctx.storage.sql.exec(`
        CREATE TABLE IF NOT EXISTS meta (
          key TEXT PRIMARY KEY,
          value TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS sessions (
          session_key TEXT PRIMARY KEY,
          arm TEXT NOT NULL CHECK (arm IN ('ownership', 'reach')),
          exposed_at INTEGER NOT NULL,
          downloaded_at INTEGER
        );
        CREATE TABLE IF NOT EXISTS events (
          event_id TEXT PRIMARY KEY,
          session_key TEXT NOT NULL,
          arm TEXT NOT NULL CHECK (arm IN ('ownership', 'reach')),
          kind TEXT NOT NULL CHECK (kind IN ('exposure', 'download')),
          occurred_at INTEGER NOT NULL,
          UNIQUE (session_key, kind)
        );
        CREATE INDEX IF NOT EXISTS events_occurred_at
          ON events (occurred_at, arm, kind);
      `);
      this.ensureCoverageStart();
    });
  }

  ensureCoverageStart() {
    this.ctx.storage.sql.exec(
      "INSERT OR IGNORE INTO meta (key, value) VALUES ('coverage_start', ?)",
      new Date().toISOString(),
    );
  }

  async record(input) {
    const now = Date.now();
    this.ctx.storage.sql.exec(
      "INSERT OR IGNORE INTO sessions (session_key, arm, exposed_at) VALUES (?, ?, ?)",
      input.sessionKey,
      input.arm,
      now,
    );

    const session = this.ctx.storage.sql.exec(
      "SELECT arm FROM sessions WHERE session_key = ?",
      input.sessionKey,
    ).one();
    if (session.arm !== input.arm) {
      return { accepted: false, reason: "arm_conflict" };
    }

    let recoveredExposure = false;
    if (input.kind === "download") {
      const recovered = this.ctx.storage.sql.exec(`
        INSERT OR IGNORE INTO events
          (event_id, session_key, arm, kind, occurred_at)
        VALUES (?, ?, ?, 'exposure', ?)
        RETURNING event_id
      `, `derived:${input.eventId}`, input.sessionKey, input.arm, now).toArray();
      recoveredExposure = recovered.length === 1;
    }

    const inserted = this.ctx.storage.sql.exec(`
      INSERT OR IGNORE INTO events
        (event_id, session_key, arm, kind, occurred_at)
      VALUES (?, ?, ?, ?, ?)
      RETURNING event_id
    `, input.eventId, input.sessionKey, input.arm, input.kind, now).toArray();

    if (input.kind === "download" && inserted.length === 1) {
      this.ctx.storage.sql.exec(
        "UPDATE sessions SET downloaded_at = ? WHERE session_key = ?",
        now,
        input.sessionKey,
      );
    }
    return {
      accepted: inserted.length === 1,
      recovered_exposure: recoveredExposure,
    };
  }

  async snapshot() {
    this.ensureCoverageStart();
    const totals = {
      ownership: { exposures: 0, downloads: 0 },
      reach: { exposures: 0, downloads: 0 },
    };
    for (const row of this.ctx.storage.sql.exec(`
      SELECT arm, kind, COUNT(*) AS count
      FROM events
      GROUP BY arm, kind
    `).toArray()) {
      totals[row.arm][`${row.kind}s`] = Number(row.count);
    }

    const daily = this.ctx.storage.sql.exec(`
      SELECT
        date(occurred_at / 1000, 'unixepoch') AS date,
        SUM(CASE WHEN arm = 'ownership' AND kind = 'exposure' THEN 1 ELSE 0 END)
          AS ownership_exposures,
        SUM(CASE WHEN arm = 'ownership' AND kind = 'download' THEN 1 ELSE 0 END)
          AS ownership_downloads,
        SUM(CASE WHEN arm = 'reach' AND kind = 'exposure' THEN 1 ELSE 0 END)
          AS reach_exposures,
        SUM(CASE WHEN arm = 'reach' AND kind = 'download' THEN 1 ELSE 0 END)
          AS reach_downloads
      FROM events
      GROUP BY date
      ORDER BY date
    `).toArray().map((row) => ({
      date: row.date,
      ownership_exposures: Number(row.ownership_exposures),
      ownership_downloads: Number(row.ownership_downloads),
      reach_exposures: Number(row.reach_exposures),
      reach_downloads: Number(row.reach_downloads),
    }));

    const coverage = this.ctx.storage.sql.exec(
      "SELECT value FROM meta WHERE key = 'coverage_start'",
    ).one();
    return {
      experiment_id: EXPERIMENT_ID,
      original_experiment_start: ORIGINAL_EXPERIMENT_START,
      coverage_start: coverage.value,
      coverage_complete_from_original_start: false,
      generated_at: new Date().toISOString(),
      counts: totals,
      daily,
    };
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const counter = env.EXP004_COUNTER.getByName(COUNTER_NAME);

    if (url.pathname === "/api/exp004/snapshot" && request.method === "GET") {
      return json(await counter.snapshot());
    }

    if (url.pathname === "/api/exp004/collect" && request.method === "POST") {
      const origin = request.headers.get("origin");
      if (!ALLOWED_ORIGINS.has(origin)) {
        return json({ error: "forbidden_origin" }, 403);
      }
      if (!(request.headers.get("content-type") || "").toLowerCase()
        .startsWith("application/json")) {
        return json({ error: "content_type_required" }, 415);
      }

      let payload;
      try {
        payload = await readBoundedJson(request);
      } catch (error) {
        const status = error.message === "payload_too_large" ? 413 : 400;
        return json({ error: error.message }, status);
      }
      const event = parseEvent(payload);
      if (!event) {
        return json({ error: "invalid_event" }, 400);
      }

      const result = await counter.record(event);
      if (result.reason === "arm_conflict") {
        return json(result, 409);
      }
      return json(result, result.accepted ? 202 : 200);
    }

    return json({ error: "not_found" }, 404);
  },
};
