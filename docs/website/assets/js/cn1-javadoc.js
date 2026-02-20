(() => {
  const root = document.querySelector(".cn1-javadoc");
  if (!root) return;
  document.body.classList.add("cn1-javadoc-page");

  if (window.location.pathname === "/javadoc") {
    window.history.replaceState(null, "", "/javadoc/");
  }

  let currentDocPath = "/javadoc/_index-raw.html";

  const reviveSearchUi = () => {
    const inputs = root.querySelectorAll('input[type="search"], input#search-input, #search-input');
    inputs.forEach((input) => {
      input.removeAttribute("disabled");
      input.readOnly = false;
      input.tabIndex = 0;
      input.style.pointerEvents = "auto";
      input.style.position = "relative";
      input.style.zIndex = "30";
    });
    clampSearchPopup();
  };

  const clampSearchPopup = () => {
    document.querySelectorAll(".ui-autocomplete").forEach((menu) => {
      menu.style.maxWidth = "min(92vw, 48rem)";
      menu.style.background = "var(--entry)";
      menu.style.color = "var(--content)";
      menu.style.border = "1px solid var(--border)";
      menu.style.boxShadow = "0 10px 24px rgba(0, 0, 0, 0.28)";
      menu.style.opacity = "1";
      menu.style.transform = "translateX(0)";
      const rect = menu.getBoundingClientRect();
      let dx = 0;
      if (rect.right > window.innerWidth - 8) dx = (window.innerWidth - 8) - rect.right;
      if (rect.left < 8) dx = 8 - rect.left;
      if (dx !== 0) menu.style.transform = `translateX(${dx}px)`;
    });
  };

  const ensureSearchAssets = (fetchPath) => {
    window.pathtoroot = calcPathToRoot(fetchPath);
    reviveSearchUi();
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
    reviveSearchUi();
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
  window.addEventListener("resize", clampSearchPopup);
  root.addEventListener("input", clampSearchPopup, true);
  root.addEventListener("focusin", clampSearchPopup, true);

  ensureSearchAssets(currentDocPath);
})();
