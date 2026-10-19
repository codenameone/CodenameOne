(() => {
  // First tagged acquisition, not internal topic-interest or last-click tracking.
  const KEY = "cn1-acquisition-v1";
  const TTL = 24 * 60 * 60 * 1000;
  const FIELDS = ["utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term"];
  const VALUE = /^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,127}$/;
  const ENTRY_PATHS = new Set(["/console", "/console/", "/console/index.html",
    "/secure", "/secure/", "/secure/index.html", "/secure/signup", "/login", "/register"]);
  let touch = null;
  const decorated = new Map();

  function consent() {
    const cookie = document.cookie.split(";").map(x => x.trim())
      .find(x => x.startsWith("cn1_crisp_consent_v2="));
    if (cookie) return cookie.slice(cookie.indexOf("=") + 1) === "accepted";
    try { return localStorage.getItem("cn1-crisp-consent-v2") === "accepted"; }
    catch (_) { return false; }
  }

  function valid(candidate) {
    return candidate && Number.isFinite(candidate.at) && candidate.at <= Date.now() &&
      Date.now() - candidate.at < TTL && candidate.tags &&
      ["social", "community", "oss", "email"].includes(candidate.tags.utm_medium) &&
      VALUE.test(candidate.tags.utm_source || "") && VALUE.test(candidate.tags.utm_campaign || "") &&
      Object.entries(candidate.tags).every(([k, v]) => FIELDS.includes(k) && VALUE.test(v));
  }

  function capture() {
    if (!consent()) {
      touch = null;
      try { sessionStorage.removeItem(KEY); } catch (_) { /* storage unavailable */ }
      return null;
    }
    if (valid(touch)) return touch;
    try { touch = JSON.parse(sessionStorage.getItem(KEY)); } catch (_) { touch = null; }
    if (valid(touch)) return touch;
    touch = null;
    const params = new URLSearchParams(window.location.search);
    const tags = {};
    for (const field of FIELDS) {
      if (params.has(field)) tags[field] = params.get(field);
    }
    const candidate = { at: Date.now(), tags };
    if (valid(candidate)) touch = candidate;
    try {
      if (touch) sessionStorage.setItem(KEY, JSON.stringify(touch));
      else sessionStorage.removeItem(KEY);
    } catch (_) { /* retain this page's touch when storage is unavailable */ }
    return touch;
  }

  function decorate(link) {
    const current = link.getAttribute("href");
    const previous = decorated.get(link);
    // Other scripts (for example pricing) may have changed the destination.
    const original = previous && previous.result === current ? previous.original : current;
    if (previous && previous.result === current) link.setAttribute("href", original);
    decorated.delete(link);
    const acquisition = capture();
    if (!acquisition || !original) return;
    let url;
    try { url = new URL(original, window.location.href); } catch (_) { return; }
    if (url.origin !== "https://cloud.codenameone.com" || !ENTRY_PATHS.has(url.pathname)) return;
    // Replace the whole acquisition tuple, never mix a later CTA campaign with first touch.
    FIELDS.forEach(field => url.searchParams.delete(field));
    Object.entries(acquisition.tags).forEach(([key, value]) => url.searchParams.set(key, value));
    const result = url.href;
    link.setAttribute("href", result);
    decorated.set(link, { original, result });
  }

  function refresh() {
    capture();
    document.querySelectorAll("a[href]").forEach(decorate);
  }
  refresh();
  // Covers newly created CTAs, pricing switches, keyboard clicks and middle-clicks.
  ["click", "auxclick", "contextmenu"].forEach(type => document.addEventListener(type, event => {
    const link = event.target.closest?.("a[href]");
    if (link) decorate(link);
  }, true));
  window.addEventListener("cn1-consent-changed", refresh);
  window.addEventListener("pageshow", refresh);
})();
