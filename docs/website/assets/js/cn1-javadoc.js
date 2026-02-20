(() => {
  const root = document.querySelector(".cn1-javadoc");
  if (!root) return;

  if (window.location.pathname === "/javadoc") {
    window.history.replaceState(null, "", "/javadoc/");
  }

  let currentDocPath = "/javadoc/_index-raw.html";
  let searchAssetsLoaded = false;

  const ensureSearchAssets = (fetchPath) => {
    window.pathtoroot = calcPathToRoot(fetchPath);
    if (!searchAssetsLoaded && typeof window.loadScripts === "function") {
      window.loadScripts(document, "script");
      searchAssetsLoaded = true;
    }
  };

  const resolveHref = (rawHref) => {
    if (!rawHref) return null;
    if (rawHref.startsWith("http://") || rawHref.startsWith("https://") || rawHref.startsWith("//")) {
      return rawHref;
    }
    if (rawHref.startsWith("/")) {
      return `${window.location.origin}${rawHref}`;
    }
    const base = `${window.location.origin}${currentDocPath}`;
    try {
      return new URL(rawHref, base).toString();
    } catch (_e) {
      return null;
    }
  };

  const toRoute = (href) => {
    let url;
    try {
      url = new URL(href, window.location.href);
    } catch (_e) {
      return null;
    }
    if (url.origin !== window.location.origin) return null;
    if (!url.pathname.startsWith("/javadoc")) return null;
    if (url.pathname === "/javadoc" || url.pathname === "/javadoc/") {
      return { fetchPath: "/javadoc/_index-raw.html", browserPath: "/javadoc/" };
    }
    if (!url.pathname.endsWith(".html")) return null;
    return (url.pathname === "/javadoc/index.html" || url.pathname === "/javadoc/_index-raw.html")
      ? { fetchPath: "/javadoc/_index-raw.html", browserPath: "/javadoc/" }
      : { fetchPath: url.pathname, browserPath: url.pathname };
  };

  const calcPathToRoot = (fetchPath) => {
    const rel = fetchPath.replace(/^\/javadoc\//, "");
    const depth = Math.max(0, rel.split("/").length - 1);
    return depth === 0 ? "./" : "../".repeat(depth);
  };

  const loadIntoContainer = async (route, pushState) => {
    const res = await fetch(route.fetchPath, { credentials: "same-origin" });
    if (!res.ok) return;
    const html = await res.text();
    const doc = new DOMParser().parseFromString(html, "text/html");
    if (!doc.body) return;
    root.innerHTML = doc.body.innerHTML;
    currentDocPath = route.fetchPath;
    ensureSearchAssets(route.fetchPath);
    if (pushState) {
      window.history.pushState({ cn1Javadoc: route.browserPath }, "", route.browserPath);
    }
    window.scrollTo({ top: 0, behavior: "auto" });
  };

  root.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || link.target || link.hasAttribute("download")) return;
    const rawHref = link.getAttribute("href");
    if (!rawHref || rawHref.startsWith("#")) return;
    const resolved = resolveHref(rawHref);
    if (!resolved) return;
    const route = toRoute(resolved);
    if (!route) return;
    event.preventDefault();
    loadIntoContainer(route, true).catch(() => {});
  });

  window.addEventListener("popstate", () => {
    const route = toRoute(window.location.href);
    if (!route) return;
    loadIntoContainer(route, false).catch(() => {});
  });

  ensureSearchAssets(currentDocPath);
})();
