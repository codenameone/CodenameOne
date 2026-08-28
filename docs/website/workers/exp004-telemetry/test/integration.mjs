import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const workerDir = fileURLToPath(new URL("..", import.meta.url));
const localUrl = "http://127.0.0.1:8797";

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
 * Cloudflare answers a request for a freshly deployed workers.dev route with
 * its own 404 until that route has propagated, and the two verbs do not
 * become live together: GET /api/exp004/snapshot can already be serving while
 * the first POST is still 404. Waiting on the GET alone therefore returns
 * about a second after `wrangler deploy` prints the URL and leaves the run's
 * first POST to fail as a 404 where the worker itself would have answered 403
 * or 400. Every assertion after the snapshot block is a POST, so whichever one
 * happened to land first was the one that failed, which is why this looked
 * random across branches rather than like one broken endpoint. Probe both
 * verbs, and report which half was still not ready when the deadline passed.
 */
async function waitFor(url, process) {
  const deadline = Date.now() + 90_000;
  let lastFailure = "no probe completed";
  while (Date.now() < deadline) {
    if (process && process.exitCode !== null) {
      throw new Error(`wrangler exited before becoming ready (${process.exitCode})`);
    }
    try {
      if (!await snapshotIsLive(url)) {
        lastFailure = "GET /api/exp004/snapshot is not serving the EXP-004 snapshot yet";
      } else {
        const notReady = await collectNotReady(url);
        if (notReady === null) {
          return;
        }
        lastFailure = `GET /api/exp004/snapshot is live but ${notReady}`;
      }
    } catch (error) {
      // The local runtime or remote hostname is still becoming ready.
      lastFailure = `probe failed: ${error.message}`;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(
    `timed out waiting for the EXP-004 telemetry worker: ${lastFailure}`);
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

async function post(baseUrl, body, origin = "https://www.codenameone.com") {
  return fetch(`${baseUrl}/api/exp004/collect`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Origin: origin,
      "Sec-Fetch-Site": "same-origin",
    },
    body: JSON.stringify({ occurred_at: Date.now(), ...body }),
  });
}

async function enroll(baseUrl, sessionKey, arm) {
  const response = await fetch(`${baseUrl}/api/exp004/session`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Origin: "https://www.codenameone.com",
      "Sec-Fetch-Site": "same-origin",
    },
    body: JSON.stringify({ session_key: sessionKey, arm }),
  });
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
  const initial = await fetch(`${baseUrl}/api/exp004/snapshot`).then((r) => r.json());
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

  const snapshot = await fetch(`${baseUrl}/api/exp004/snapshot`).then((r) => r.json());
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
