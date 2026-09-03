import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const workerDir = fileURLToPath(new URL("..", import.meta.url));
const localUrl = "http://127.0.0.1:8797";
// How long the deployed route has to keep answering correctly before the
// suite trusts it. Cloudflare propagation is per-request, not a switch.
const ROUTE_SETTLE_MS = 10_000;

async function snapshotIsLive(url) {
  const response = await fetch(`${url}/api/exp004/snapshot`);
  if (!response.ok || !(response.headers.get("content-type") || "")
    .includes("application/json")) {
    return false;
  }
  const payload = await response.json();
  return payload.experiment_id === "EXP-004";
}

/*
 * A cross-site Origin is refused by hasSameOriginBrowserContext before the
 * worker reads the body or reaches the counter, so this probe costs one
 * rejected request and records nothing however often the poll loop runs it.
 *
 * Readiness is that exact 403 and its body, not merely "not a 404". A Worker
 * whose bindings are still initializing, or an edge that is having a bad
 * minute, answers 5xx; treating anything non-404 as ready would let waitFor
 * return on one of those and hand the transient error straight to the first
 * assertion -- the failure this whole probe exists to stop. Only
 * forbidden_browser_context proves the request reached our handler and ran it.
 *
 * Returns null once ready, otherwise a description of what answered instead.
 */
async function collectNotReady(url) {
  const response = await post(url, {
    event: "Exp004OwnershipExposure",
    event_id: crypto.randomUUID(),
    session_key: crypto.randomUUID(),
  }, "https://example.com");
  if (response.status !== 403) {
    return `POST /api/exp004/collect answered ${response.status}, not the 403 the `
      + "live handler returns for a cross-site Origin";
  }
  const payload = await response.json().catch(() => null);
  if (!payload || payload.error !== "forbidden_browser_context") {
    return "POST /api/exp004/collect answered 403 without the worker's "
      + "forbidden_browser_context body, so something ahead of the worker refused it";
  }
  return null;
}

/*
 * One healthy round, on both verbs. Returns null when that round passed,
 * otherwise a description of what was not ready.
 */
async function probeOnce(url) {
  if (!await snapshotIsLive(url)) {
    return "GET /api/exp004/snapshot is not serving the EXP-004 snapshot yet";
  }
  const notReady = await collectNotReady(url);
  return notReady === null ? null : `GET /api/exp004/snapshot is live but ${notReady}`;
}

/*
 * Cloudflare answers a request for a freshly deployed workers.dev route with
 * its own 404, or a plain HTML error page, until that route has propagated -
 * and propagation is neither monotonic nor global. One healthy round proves
 * nothing about the next request: a run that had just seen the snapshot serve
 * JSON and the collect endpoint return its own 403 went on to get
 * `<!DOCTYPE html>` from that same snapshot URL 0.75s later.
 *
 * So readiness is not "a probe passed", it is "the route has been answering
 * correctly for a while". Both verbs must keep passing for ROUTE_SETTLE_MS
 * without a single miss; any failure restarts that clock. The suite's POSTs
 * record telemetry, so they cannot be retried without distorting the counts it
 * asserts - waiting for the route to settle is the only lever that does not
 * change what is being tested.
 */
async function waitFor(url, process) {
  const deadline = Date.now() + 90_000;
  let healthySince = null;
  let lastFailure = "no probe completed";
  while (Date.now() < deadline) {
    if (process && process.exitCode !== null) {
      throw new Error(`wrangler exited before becoming ready (${process.exitCode})`);
    }
    let failure;
    try {
      failure = await probeOnce(url);
    } catch (error) {
      // The local runtime or remote hostname is still becoming ready.
      failure = `probe failed: ${error.message}`;
    }
    if (failure === null) {
      if (healthySince === null) {
        healthySince = Date.now();
      }
      const healthyFor = Date.now() - healthySince;
      if (healthyFor >= ROUTE_SETTLE_MS) {
        return;
      }
      lastFailure = `the route has only been healthy for ${healthyFor}ms of the `
        + `${ROUTE_SETTLE_MS}ms it has to hold`;
    } else {
      healthySince = null;
      lastFailure = failure;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(
    `timed out waiting for the EXP-004 telemetry worker: ${lastFailure}`);
}

/*
 * A bare `.json()` on the snapshot turns an edge error page into
 * "SyntaxError: Unexpected token '<'" from deep inside undici, which names
 * neither the URL nor the status. Check what came back, say so when it is not
 * ours, and retry: this GET is idempotent, so unlike the POSTs it can be
 * repeated without changing a single count the suite asserts on.
 */
async function readSnapshot(baseUrl) {
  const deadline = Date.now() + 30_000;
  for (;;) {
    let failure;
    try {
      const response = await fetch(`${baseUrl}/api/exp004/snapshot`);
      const contentType = response.headers.get("content-type") || "";
      if (response.ok && contentType.includes("application/json")) {
        return await response.json();
      }
      const body = (await response.text()).slice(0, 200).replace(/\s+/g, " ");
      failure = `${response.status} `
        + `${contentType || "with no content-type"}: ${body}`;
    } catch (error) {
      failure = error.message;
    }
    if (Date.now() >= deadline) {
      throw new Error(
        `GET /api/exp004/snapshot never returned the worker's JSON: ${failure}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
}

/*
 * assert.equal on response.status alone reports "404 !== 403" and drops the
 * body, which is the difference between "the edge has not routed us yet" and
 * "the worker rejected the payload". Keep the body in the failure message; the
 * response is left unread when the status matches so callers can still parse
 * it.
 */
async function expectStatus(response, expected, what) {
  if (response.status === expected) {
    return response;
  }
  let body;
  try {
    body = (await response.text()).slice(0, 400);
  } catch (error) {
    body = `<body unreadable: ${error.message}>`;
  }
  assert.fail(
    `${what}: expected ${expected}, got ${response.status} with body ${body}`);
}

/*
 * True when a response is the edge saying it has no route for us yet, rather
 * than our worker answering.
 *
 * The distinction is the one collectNotReady already draws and for the same
 * reason: a 404 carrying Cloudflare's HTML error page is produced BEFORE the
 * worker runs, so nothing was read, counted or deduplicated. A 5xx is not this
 * -- bindings that are still initializing answer after the request reached the
 * handler -- so it is deliberately not included here.
 */
async function isUnroutedEdgeResponse(response) {
  if (response.status !== 404) {
    return false;
  }
  const contentType = response.headers.get("content-type") || "";
  return !contentType.includes("application/json");
}

/*
 * Posts, retrying only while the edge has not routed us yet.
 *
 * waitFor gates the suite on one healthy round, and that is not enough on its
 * own: propagation is per-request and neither monotonic nor global, so a later
 * POST can still land on an edge node that has not caught up. That is what
 * failed here -- the "unknown event name" assertion got Cloudflare's 404 page
 * instead of the worker's 400, on a run whose readiness probe had already
 * passed.
 *
 * Safe to repeat, and ONLY in this exact case. readSnapshot retries because a
 * GET changes nothing; the objection to retrying a POST is that it could count
 * twice. An unrouted 404 cannot: the request never reached the worker. Any
 * response the worker itself produced -- including every rejection the suite
 * asserts on -- is returned untouched on the first attempt.
 */
async function post(baseUrl, body, origin = "https://www.codenameone.com") {
  const payload = JSON.stringify({ occurred_at: Date.now(), ...body });
  const deadline = Date.now() + 30_000;
  for (;;) {
    const response = await fetch(`${baseUrl}/api/exp004/collect`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Origin: origin,
        "Sec-Fetch-Site": "same-origin",
      },
      body: payload,
    });
    if (!await isUnroutedEdgeResponse(response) || Date.now() >= deadline) {
      return response;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
}

async function enroll(baseUrl, sessionKey, arm) {
  // Same unrouted-edge retry as post(): enrolment is a POST on a second route,
  // and a route propagates on its own schedule, so gating on /collect being
  // live says nothing about /session.
  const body = JSON.stringify({ session_key: sessionKey, arm });
  const deadline = Date.now() + 30_000;
  let response;
  for (;;) {
    response = await fetch(`${baseUrl}/api/exp004/session`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Origin: "https://www.codenameone.com",
        "Sec-Fetch-Site": "same-origin",
      },
      body,
    });
    if (!await isUnroutedEdgeResponse(response) || Date.now() >= deadline) {
      break;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  await expectStatus(response, 201, `enroll ${sessionKey} into ${arm}`);
  const payload = await response.json();
  assert.match(payload.submission_token, /^[0-9a-f-]{36}$/);
  return payload.submission_token;
}

async function stopProcess(child) {
  if ((child.exitCode !== null || child.signalCode !== null) &&
      (process.platform === "win32" || !child.pid)) return;

  const signalTree = (signal) => {
    if (process.platform !== "win32" && child.pid) {
      try {
        process.kill(-child.pid, signal);
        return;
      } catch (error) {
        // Fall back to signaling the wrapper when no process group exists.
      }
    }
    child.kill(signal);
  };

  await new Promise((resolve) => {
    let finished = false;
    const finish = () => {
      if (finished) return;
      finished = true;
      clearTimeout(forceTimer);
      clearTimeout(giveUpTimer);
      child.off("exit", finish);
      resolve();
    };
    const forceTimer = setTimeout(() => {
      if (child.exitCode === null && child.signalCode === null) {
        signalTree("SIGKILL");
      }
    }, 5_000);
    const giveUpTimer = setTimeout(finish, 10_000);

    child.once("exit", finish);
    signalTree("SIGTERM");
    if (child.exitCode !== null || child.signalCode !== null) finish();
  });
}

async function verify(baseUrl) {
  const testStartedAt = Date.now();
  const initial = await readSnapshot(baseUrl);
  assert.equal(initial.experiment_id, "EXP-004");
  assert.equal(initial.original_experiment_start, "2026-08-27T04:35:14.000Z");
  assert.equal(initial.coverage_complete_from_original_start, false);
  const coverageStartedAt = Date.parse(initial.coverage_start);
  assert.ok(Number.isFinite(coverageStartedAt), "coverage_start must be an ISO timestamp");
  assert.ok(coverageStartedAt <= Date.now() + 1_000,
    "coverage_start cannot be in the future");
  assert.ok(coverageStartedAt >= Date.parse(initial.original_experiment_start),
    "coverage cannot begin before the original experiment deployment");
  assert.ok(coverageStartedAt <= testStartedAt + 1_000,
    "coverage must have started by this test run");

  const forbidden = await post(baseUrl, {
    event: "Exp004OwnershipExposure",
    event_id: crypto.randomUUID(),
    session_key: crypto.randomUUID(),
  }, "https://example.com");
  await expectStatus(forbidden, 403, "cross-site exposure POST");

  const invalid = await post(baseUrl, {
    event: "Exp004UnknownExposure",
    event_id: crypto.randomUUID(),
    session_key: crypto.randomUUID(),
  });
  await expectStatus(invalid, 400, "unknown event name POST");

  const ownershipSession = crypto.randomUUID();
  const ownershipToken = await enroll(baseUrl, ownershipSession, "ownership");
  const ownershipOccurredAt = Math.max(
    Date.parse(initial.original_experiment_start) + 1_000,
    Date.now() - 24 * 60 * 60 * 1000,
  );
  const ownershipExposure = {
    event: "Exp004OwnershipExposure",
    event_id: crypto.randomUUID(),
    occurred_at: ownershipOccurredAt,
    session_key: ownershipSession,
    submission_token: ownershipToken,
  };
  const firstExposure = await post(baseUrl, ownershipExposure);
  await expectStatus(firstExposure, 202, "first ownership exposure");
  assert.equal((await firstExposure.json()).accepted, true);
  const duplicateExposure = await post(baseUrl, ownershipExposure);
  await expectStatus(duplicateExposure, 200, "duplicate ownership exposure");
  assert.equal((await duplicateExposure.json()).accepted, false);

  const reachSession = crypto.randomUUID();
  const reachToken = await enroll(baseUrl, reachSession, "reach");
  const unauthorized = await post(baseUrl, {
    event: "Exp004ReachExposure",
    event_id: crypto.randomUUID(),
    session_key: reachSession,
    submission_token: crypto.randomUUID(),
  });
  await expectStatus(unauthorized, 401, "reach exposure with a foreign token");

  const futureDated = await post(baseUrl, {
    event: "Exp004ReachExposure",
    event_id: crypto.randomUUID(),
    occurred_at: Date.now() + 10 * 60 * 1000,
    session_key: reachSession,
    submission_token: reachToken,
  });
  await expectStatus(futureDated, 400, "future dated reach exposure");

  const reachDownload = await post(baseUrl, {
    event: "Exp004ReachDownload",
    event_id: crypto.randomUUID(),
    session_key: reachSession,
    submission_token: reachToken,
  });
  await expectStatus(reachDownload, 202, "reach download");
  assert.deepEqual(await reachDownload.json(), {
    accepted: true,
    recovered_exposure: true,
  });

  const reachExposureOccurredAt = Math.max(
    Date.parse(initial.original_experiment_start) + 2_000,
    Date.now() - 24 * 60 * 60 * 1000,
  );
  const correctedReachExposure = await post(baseUrl, {
    event: "Exp004ReachExposure",
    event_id: crypto.randomUUID(),
    occurred_at: reachExposureOccurredAt,
    session_key: reachSession,
    submission_token: reachToken,
  });
  await expectStatus(correctedReachExposure, 202, "corrected reach exposure");
  assert.equal((await correctedReachExposure.json()).accepted, true);

  const ownershipDownload = await post(baseUrl, {
    event: "Exp004OwnershipDownload",
    event_id: crypto.randomUUID(),
    session_key: ownershipSession,
    submission_token: ownershipToken,
  });
  await expectStatus(ownershipDownload, 202, "ownership download");

  const snapshot = await readSnapshot(baseUrl);
  assert.deepEqual(snapshot.counts, {
    ownership: { exposures: 1, downloads: 1 },
    reach: { exposures: 1, downloads: 1 },
  });
  const ownershipOccurrenceDate = new Date(ownershipOccurredAt)
    .toISOString().slice(0, 10);
  const ownershipOccurrenceBucket = snapshot.daily.find(
    (row) => row.date === ownershipOccurrenceDate
  );
  assert.equal(ownershipOccurrenceBucket?.ownership_exposures, 1,
    "daily exposure counts must use the client occurrence date");
  const reachOccurrenceDate = new Date(reachExposureOccurredAt)
    .toISOString().slice(0, 10);
  const reachOccurrenceBucket = snapshot.daily.find(
    (row) => row.date === reachOccurrenceDate
  );
  assert.equal(reachOccurrenceBucket?.reach_exposures, 1,
    "a real exposure must replace a download-first recovery timestamp");
}

const remoteUrl = process.argv[2];
if (remoteUrl) {
  const normalizedRemoteUrl = remoteUrl.replace(/\/$/, "");
  await waitFor(normalizedRemoteUrl);
  await verify(normalizedRemoteUrl);
  console.log("EXP-004 live telemetry integration passed");
} else {
  const persistPath = await mkdtemp(join(tmpdir(), "cn1-exp004-worker-"));
  const wrangler = spawn(
    "npx",
    [
      "--yes", "wrangler@4", "dev", "--env", "preview", "--local",
      "--persist-to", persistPath, "--port", "8797", "--ip", "127.0.0.1",
    ],
    {
      cwd: workerDir,
      detached: process.platform !== "win32",
      stdio: ["ignore", "pipe", "pipe"],
    },
  );
  let output = "";
  wrangler.stdout.on("data", (chunk) => { output += chunk; });
  wrangler.stderr.on("data", (chunk) => { output += chunk; });
  try {
    await waitFor(localUrl, wrangler);
    await verify(localUrl);
    console.log("EXP-004 local Durable Object integration passed");
  } catch (error) {
    process.stderr.write(output);
    throw error;
  } finally {
    await stopProcess(wrangler);
    await rm(persistPath, { recursive: true, force: true });
  }
}
