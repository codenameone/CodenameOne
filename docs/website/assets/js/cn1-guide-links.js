/* Fragments never reach the server. Resolve old book bookmarks in the browser. */
(() => {
  const menu = document.querySelector('.cn1-guide-menu');
  const desktop = window.matchMedia('(min-width: 721px)');
  const updateMenu = () => {
    if (!menu) return;
    menu.open = desktop.matches;
    if (desktop.matches) {
      const sidebar = document.querySelector('.cn1-guide-sidebar');
      const current = menu.querySelector('[aria-current="page"]');
      if (sidebar && current) {
        const below = current.getBoundingClientRect().bottom - sidebar.getBoundingClientRect().bottom;
        if (below > 0) sidebar.scrollTop += below + 24;
      }
    }
  };
  updateMenu();
  desktop.addEventListener('change', updateMenu);
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
