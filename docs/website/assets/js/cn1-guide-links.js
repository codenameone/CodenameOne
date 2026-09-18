/* Fragments never reach the server. Resolve old book bookmarks in the browser. */
(() => {
  const menu = document.querySelector('.cn1-guide-menu');
  if (menu) menu.open = window.matchMedia('(min-width: 721px)').matches;
  async function resolveBookmark() {
    if (!location.hash) return;
    let id;
    try { id = decodeURIComponent(location.hash.slice(1)); } catch (_) { return; }
    if (document.getElementById(id)) return;
    try {
      const response = await fetch('/developer-guide/anchors.json');
      if (!response.ok) return;
      const anchors = await response.json();
      const target = anchors[id];
      if (target && target.startsWith('/developer-guide/') && target !== location.pathname + location.hash) {
        const next = new URL(target, location.origin);
        next.search = location.search;
        location.replace(next.href);
      }
    } catch (_) { /* Leave the chapter directory usable when offline. */ }
  }
  resolveBookmark();
  window.addEventListener('hashchange', resolveBookmark);
})();
