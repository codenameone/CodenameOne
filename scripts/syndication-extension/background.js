// Background service worker for the Codename One Syndicator extension.
//
// Polls a JSON queue file in the repo at a slow cadence (default 30 min) and,
// for each pending task, opens the relevant editor in a new tab. Each
// adapter content script picks up the task from chrome.storage when its tab
// loads, fills the editor, saves a draft, and reports back here so the task
// can be marked complete.
//
// The queue file lives at:
//   https://raw.githubusercontent.com/codenameone/CodenameOne/master/
//     scripts/website/syndication-queue.json
//
// Format:
//   {
//     "tasks": [
//       {
//         "id": "<unique id>",
//         "site": "medium" | "dzone",
//         "slug": "<post slug>",
//         "title": "<post title>",
//         "canonical": "<https://www.codenameone.com/blog/...>",
//         "body_html": "<rendered HTML body>",
//         "description": "<short description>",
//         "cover_image_url": "<https://...>"
//       }
//     ]
//   }
//
// Once an adapter completes a task, results land in chrome.storage under
// `completed_tasks`. The popup UI surfaces them so the user can manually
// commit the matching state-file update back to the repo. (Round-tripping
// the result via a GitHub PR from inside the extension would require a
// committed token; we keep the trust boundary simple by leaving that to
// the user.)

const QUEUE_URL = "https://raw.githubusercontent.com/codenameone/CodenameOne/master/scripts/website/syndication-queue.json";
const POLL_INTERVAL_MINUTES = 30;
const EDITOR_URLS = {
  medium: "https://medium.com/new-story",
  dzone: "https://dzone.com/content/article/post.html",
};

chrome.runtime.onInstalled.addListener(() => {
  chrome.alarms.create("poll", { periodInMinutes: POLL_INTERVAL_MINUTES });
  chrome.storage.local.set({ completed_tasks: [], pending_tasks: [], last_poll: null });
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === "poll") void runPoll();
});

chrome.action.onClicked.addListener(() => {
  void runPoll();
});

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg && msg.type === "poll-now") {
    runPoll().then(() => sendResponse({ ok: true })).catch((err) => sendResponse({ ok: false, error: String(err) }));
    return true; // keep the channel open for the async response
  }
});

async function runPoll() {
  await chrome.storage.local.set({ last_poll: new Date().toISOString() });
  let queue;
  try {
    const resp = await fetch(QUEUE_URL, { cache: "no-store" });
    if (!resp.ok) {
      console.warn("[syndicator] queue fetch failed", resp.status);
      return;
    }
    queue = await resp.json();
  } catch (err) {
    console.warn("[syndicator] queue fetch error", err);
    return;
  }
  const completed = (await chrome.storage.local.get("completed_tasks")).completed_tasks || [];
  const completedIds = new Set(completed.map((c) => c.id));
  const tasks = (queue.tasks || []).filter((t) => !completedIds.has(t.id));
  await chrome.storage.local.set({ pending_tasks: tasks });
  for (const task of tasks) {
    await processTask(task);
  }
}

async function processTask(task) {
  const editorUrl = EDITOR_URLS[task.site];
  if (!editorUrl) {
    console.warn("[syndicator] unknown site", task.site);
    return;
  }
  // Stash the task in storage keyed by site; the content script reads it on load.
  await chrome.storage.local.set({ [`task_for_${task.site}`]: task });
  const tab = await chrome.tabs.create({ url: editorUrl, active: false });
  // Wait for completion message from the content script (with a long timeout)
  // — the content script signals via chrome.runtime.sendMessage.
  await new Promise((resolve) => {
    const listener = (msg, sender) => {
      if (msg && msg.type === "syndication-complete" && msg.task_id === task.id) {
        chrome.runtime.onMessage.removeListener(listener);
        chrome.storage.local.get("completed_tasks").then(({ completed_tasks = [] }) => {
          completed_tasks.push({
            id: task.id,
            site: task.site,
            slug: task.slug,
            url: msg.url || tab.url,
            success: msg.success,
            error: msg.error || null,
            completed_at: new Date().toISOString(),
          });
          chrome.storage.local.set({ completed_tasks });
        });
        // Close the tab once the adapter is done (give it a beat to flush state).
        setTimeout(() => chrome.tabs.remove(tab.id).catch(() => {}), 2000);
        resolve();
      }
    };
    chrome.runtime.onMessage.addListener(listener);
    setTimeout(() => {
      chrome.runtime.onMessage.removeListener(listener);
      resolve();
    }, 5 * 60 * 1000); // 5-minute hard timeout per task
  });
}
