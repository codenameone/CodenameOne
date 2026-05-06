// Helpers shared by every adapter.

window.cn1Syndicator = window.cn1Syndicator || {};

window.cn1Syndicator.waitFor = function (predicate, { timeout = 30000, interval = 200 } = {}) {
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + timeout;
    const tick = () => {
      try {
        const value = predicate();
        if (value) {
          resolve(value);
          return;
        }
      } catch (err) {
        // ignore until timeout
      }
      if (Date.now() > deadline) {
        reject(new Error(`waitFor timed out after ${timeout}ms`));
        return;
      }
      setTimeout(tick, interval);
    };
    tick();
  });
};

window.cn1Syndicator.setReactValue = function (element, value) {
  if (!element) return false;
  const proto = Object.getPrototypeOf(element);
  const setter = Object.getOwnPropertyDescriptor(proto, "value").set;
  setter.call(element, value);
  element.dispatchEvent(new Event("input", { bubbles: true }));
  return true;
};

window.cn1Syndicator.report = function (taskId, payload) {
  chrome.runtime.sendMessage({ type: "syndication-complete", task_id: taskId, ...payload });
};

window.cn1Syndicator.getTaskFor = async function (site) {
  const key = `task_for_${site}`;
  const data = await chrome.storage.local.get(key);
  return data[key] || null;
};

window.cn1Syndicator.downloadAsFile = async function (url, fileName) {
  // Returns a File object suitable for handing to a hidden file input.
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`download ${url} -> ${resp.status}`);
  const blob = await resp.blob();
  return new File([blob], fileName, { type: blob.type || "image/jpeg" });
};

window.cn1Syndicator.attachFile = function (input, file) {
  // Programmatically populate a hidden <input type="file">.
  const dt = new DataTransfer();
  dt.items.add(file);
  input.files = dt.files;
  input.dispatchEvent(new Event("change", { bubbles: true }));
};
