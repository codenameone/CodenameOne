// Popup UI: list pending + completed syndication tasks, surface a JSON
// patch the user can drop into scripts/website/syndication-state.json
// to record the syndication results in the repo.

async function refresh() {
  const data = await chrome.storage.local.get(["pending_tasks", "completed_tasks", "last_poll"]);
  const pending = data.pending_tasks || [];
  const completed = data.completed_tasks || [];
  const lastPoll = data.last_poll || "never";

  document.getElementById("status").textContent = `Last poll: ${lastPoll}`;
  document.getElementById("pending").innerHTML =
    pending.length === 0
      ? '<div class="meta">none</div>'
      : pending.map((t) => `<div class="row"><strong>${t.site}</strong>: ${t.slug}</div>`).join("");
  document.getElementById("completed").innerHTML =
    completed.length === 0
      ? '<div class="meta">none</div>'
      : completed
          .map(
            (c) =>
              `<div class="row"><strong>${c.site}</strong>: ${c.slug}<br>` +
              `<span class="${c.success ? "ok" : "fail"}">${c.success ? "OK" : "FAIL"}</span> ` +
              `<a href="${c.url}" target="_blank">${c.url || "(no url)"}</a>` +
              `${c.error ? `<br><span class="fail">${c.error}</span>` : ""}` +
              `<br><span class="meta">${c.completed_at}</span></div>`
          )
          .join("");

  // Build a JSON patch grouped by slug
  const patch = { posts: {} };
  for (const c of completed) {
    if (!c.success) continue;
    if (!patch.posts[c.slug]) patch.posts[c.slug] = {};
    patch.posts[c.slug][c.site] = {
      url: c.url,
      syndicated_at: c.completed_at,
    };
  }
  document.getElementById("state-patch").textContent =
    completed.filter((c) => c.success).length === 0 ? "(none yet)" : JSON.stringify(patch, null, 2);
}

document.getElementById("poll").addEventListener("click", async () => {
  await chrome.runtime.sendMessage({ type: "poll-now" }).catch(() => {});
  // The background's onClicked handler also runs poll on action click; but
  // the popup is its own action. Send a message AND fall back to invoking
  // the alarm by triggering the browser action explicitly via chrome.alarms.
  setTimeout(refresh, 500);
});

document.getElementById("clear").addEventListener("click", async () => {
  await chrome.storage.local.set({ completed_tasks: [] });
  refresh();
});

// Make the popup poll immediately when opened
chrome.runtime.sendMessage({ type: "poll-now" }).catch(() => {});
refresh();
