import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const workerDir = fileURLToPath(new URL("..", import.meta.url));
const localUrl = "http://127.0.0.1:8797";

async function waitFor(url, process) {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    if (process?.exitCode !== null) {
      throw new Error(`wrangler exited before becoming ready (${process.exitCode})`);
    }
    try {
      const response = await fetch(`${url}/api/exp004/snapshot`);
      if (response.ok) return;
    } catch (error) {
      // The local runtime is still starting.
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error("timed out waiting for the EXP-004 telemetry worker");
}

async function post(baseUrl, body, origin = "https://www.codenameone.com") {
  return fetch(`${baseUrl}/api/exp004/collect`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Origin: origin },
    body: JSON.stringify(body),
  });
}

async function stopProcess(process) {
  if (process.exitCode !== null || process.signalCode !== null) return;

  await new Promise((resolve) => {
    let finished = false;
    const finish = () => {
      if (finished) return;
      finished = true;
      clearTimeout(forceTimer);
      clearTimeout(giveUpTimer);
      process.off("exit", finish);
      resolve();
    };
    const forceTimer = setTimeout(() => {
      if (process.exitCode === null && process.signalCode === null) {
        process.kill("SIGKILL");
      }
    }, 5_000);
    const giveUpTimer = setTimeout(finish, 10_000);

    process.once("exit", finish);
    process.kill("SIGTERM");
    if (process.exitCode !== null || process.signalCode !== null) finish();
  });
}

async function verify(baseUrl) {
  const initial = await fetch(`${baseUrl}/api/exp004/snapshot`).then((r) => r.json());
  assert.equal(initial.experiment_id, "EXP-004");
  assert.equal(initial.original_experiment_start, "2026-08-27T04:35:14.000Z");
  assert.equal(initial.coverage_complete_from_original_start, false);
  assert.match(initial.coverage_start, /^2026-/);

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
  const ownershipExposure = {
    event: "Exp004OwnershipExposure",
    event_id: crypto.randomUUID(),
    session_key: ownershipSession,
  };
  const firstExposure = await post(baseUrl, ownershipExposure);
  assert.equal(firstExposure.status, 202);
  assert.equal((await firstExposure.json()).accepted, true);
  const duplicateExposure = await post(baseUrl, ownershipExposure);
  assert.equal(duplicateExposure.status, 200);
  assert.equal((await duplicateExposure.json()).accepted, false);

  const reachDownload = await post(baseUrl, {
    event: "Exp004ReachDownload",
    event_id: crypto.randomUUID(),
    session_key: crypto.randomUUID(),
  });
  assert.equal(reachDownload.status, 202);
  assert.deepEqual(await reachDownload.json(), {
    accepted: true,
    recovered_exposure: true,
  });

  const ownershipDownload = await post(baseUrl, {
    event: "Exp004OwnershipDownload",
    event_id: crypto.randomUUID(),
    session_key: ownershipSession,
  });
  assert.equal(ownershipDownload.status, 202);

  const snapshot = await fetch(`${baseUrl}/api/exp004/snapshot`).then((r) => r.json());
  assert.deepEqual(snapshot.counts, {
    ownership: { exposures: 1, downloads: 1 },
    reach: { exposures: 1, downloads: 1 },
  });
  assert.equal(snapshot.daily.length, 1);
  assert.deepEqual(
    Object.fromEntries(Object.entries(snapshot.daily[0]).filter(([key]) => key !== "date")),
    {
      ownership_exposures: 1,
      ownership_downloads: 1,
      reach_exposures: 1,
      reach_downloads: 1,
    },
  );
}

const remoteUrl = process.argv[2];
if (remoteUrl) {
  await verify(remoteUrl.replace(/\/$/, ""));
  console.log("EXP-004 live telemetry integration passed");
} else {
  const persistPath = await mkdtemp(join(tmpdir(), "cn1-exp004-worker-"));
  const wrangler = spawn(
    "npx",
    [
      "--yes", "wrangler@4", "dev", "--env", "preview", "--local",
      "--persist-to", persistPath, "--port", "8797", "--ip", "127.0.0.1",
    ],
    { cwd: workerDir, stdio: ["ignore", "pipe", "pipe"] },
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
