(() => {
  const root = document.querySelector(".cn1-javadoc");
  if (!root) return;

  const toRoute = (href) => {
    let url;
    try {
      url = new URL(href, window.location.href);
    } catch (_e) {
      return null;
    }
    if (url.origin !== window.location.origin) return null;
    if (!url.pathname.startsWith("/javadoc")) return null;
    if (!url.pathname.endsWith(".html")) return null;
    return url.pathname === "/javadoc/index.html"
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
    window.pathtoroot = calcPathToRoot(route.fetchPath);
    if (typeof window.loadScripts === "function") {
      window.loadScripts(document, "script");
    }
    if (pushState) {
      window.history.pushState({ cn1Javadoc: route.browserPath }, "", route.browserPath);
    }
    window.scrollTo({ top: 0, behavior: "auto" });
  };

  root.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || link.target || link.hasAttribute("download")) return;
    if (link.getAttribute("href").startsWith("#")) return;
    const route = toRoute(link.href);
    if (!route) return;
    event.preventDefault();
    loadIntoContainer(route, true).catch(() => {});
  });

  window.addEventListener("popstate", () => {
    const route = toRoute(window.location.href);
    if (!route) return;
    loadIntoContainer(route, false).catch(() => {});
  });
})();
