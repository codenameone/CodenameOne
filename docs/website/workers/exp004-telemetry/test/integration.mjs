import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const workerDir = fileURLToPath(new URL("..", import.meta.url));
const localUrl = "http://127.0.0.1:8797";

// A brand-new workers.dev hostname is not live in every Cloudflare colo the
// instant `wrangler deploy` returns; the route reaches them independently. So a
// run can get a correct answer from one request and a 404 from the very next --
// Cloudflare's "no Worker on this hostname", not the Worker's own not_found
// body. That is exactly how this test failed 0.6s after a preview deploy: the
// same POST path answered 403 and then 404.
//
// Retrying absorbs it without hiding a routing bug, because the retry is
// narrow in three ways. Only 404 is retried, and no assertion in this file
// expects one. Only the remote run arms the window -- a local `wrangler dev`
// has no colos and keeps zero tolerance. And the window is an absolute deadline
// from the start of the run rather than a per-request budget, so a path the
// Worker genuinely does not serve stays 404 until it expires and still fails.
const COLD_ROUTE_TOLERANCE_MS = 60_000;
let coldRouteDeadline = 0;

async function request(url, init) {
  for (;;) {
    const response = await fetch(url, init);
    if (response.status !== 404 || Date.now() >= coldRouteDeadline) {
      return response;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
}

async function waitFor(url, process) {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    if (process && process.exitCode !== null) {
      throw new Error(`wrangler exited before becoming ready (${process.exitCode})`);
    }
    try {
      const response = await request(`${url}/api/exp004/snapshot`);
      if (response.ok && (response.headers.get("content-type") || "")
        .includes("application/json")) {
        const payload = await response.json();
        if (payload.experiment_id === "EXP-004") return;
      }
    } catch (error) {
      // The local runtime or remote hostname is still becoming ready.
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error("timed out waiting for the EXP-004 telemetry worker");
}

async function post(baseUrl, body, origin = "https://www.codenameone.com") {
  return request(`${baseUrl}/api/exp004/collect`, {
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
  const response = await request(`${baseUrl}/api/exp004/session`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Origin: "https://www.codenameone.com",
      "Sec-Fetch-Site": "same-origin",
    },
    body: JSON.stringify({ session_key: sessionKey, arm }),
  });
  assert.equal(response.status, 201);
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
  const initial = await request(`${baseUrl}/api/exp004/snapshot`).then((r) => r.json());
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
  assert.equal(forbidden.status, 403);

  const invalid = await post(baseUrl, {
    event: "Exp004UnknownExposure",
    event_id: crypto.randomUUID(),
    session_key: crypto.randomUUID(),
  });
  assert.equal(invalid.status, 400);

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
  assert.equal(firstExposure.status, 202);
  assert.equal((await firstExposure.json()).accepted, true);
  const duplicateExposure = await post(baseUrl, ownershipExposure);
  assert.equal(duplicateExposure.status, 200);
  assert.equal((await duplicateExposure.json()).accepted, false);

  const reachSession = crypto.randomUUID();
  const reachToken = await enroll(baseUrl, reachSession, "reach");
  const unauthorized = await post(baseUrl, {
    event: "Exp004ReachExposure",
    event_id: crypto.randomUUID(),
    session_key: reachSession,
    submission_token: crypto.randomUUID(),
  });
  assert.equal(unauthorized.status, 401);

  const futureDated = await post(baseUrl, {
    event: "Exp004ReachExposure",
    event_id: crypto.randomUUID(),
    occurred_at: Date.now() + 10 * 60 * 1000,
    session_key: reachSession,
    submission_token: reachToken,
  });
  assert.equal(futureDated.status, 400);

  const reachDownload = await post(baseUrl, {
    event: "Exp004ReachDownload",
    event_id: crypto.randomUUID(),
    session_key: reachSession,
    submission_token: reachToken,
  });
  assert.equal(reachDownload.status, 202);
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
  assert.equal(correctedReachExposure.status, 202);
  assert.equal((await correctedReachExposure.json()).accepted, true);

  const ownershipDownload = await post(baseUrl, {
    event: "Exp004OwnershipDownload",
    event_id: crypto.randomUUID(),
    session_key: ownershipSession,
    submission_token: ownershipToken,
  });
  assert.equal(ownershipDownload.status, 202);

  const snapshot = await request(`${baseUrl}/api/exp004/snapshot`).then((r) => r.json());
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
  coldRouteDeadline = Date.now() + COLD_ROUTE_TOLERANCE_MS;
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
