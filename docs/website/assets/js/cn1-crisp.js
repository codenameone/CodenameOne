(() => {
  const CONSENT_KEY = "cn1-crisp-consent-v1";
  const CONSENT_COOKIE = "cn1_crisp_consent";
  const WEBSITE_ID = "e0201fca-1e59-4f30-9d00-8c37aa18293e";
  const CONSENT_TTL_DAYS = 365;

  const readCookie = (name) => {
    const prefix = `${name}=`;
    const found = document.cookie
      .split(";")
      .map((part) => part.trim())
      .find((part) => part.startsWith(prefix));
    return found ? decodeURIComponent(found.substring(prefix.length)) : null;
  };

  const writeCookie = (name, value, days) => {
    const expires = new Date(Date.now() + days * 24 * 60 * 60 * 1000).toUTCString();
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`;
  };

  const getConsent = () => {
    const cookieValue = readCookie(CONSENT_COOKIE);
    if (cookieValue === "accepted" || cookieValue === "declined") {
      return cookieValue;
    }
    try {
      const storageValue = localStorage.getItem(CONSENT_KEY);
      if (storageValue === "accepted" || storageValue === "declined") {
        return storageValue;
      }
    } catch (e) {
      // no-op
    }
    return null;
  };

  const setConsent = (value) => {
    writeCookie(CONSENT_COOKIE, value, CONSENT_TTL_DAYS);
    try {
      localStorage.setItem(CONSENT_KEY, value);
    } catch (e) {
      // no-op
    }
  };

  // --- Crisp trigger events -------------------------------------------------
  // Fire named Crisp `session:event`s so the matching automated campaigns launch
  // for the visitor. Each fires at most once per page view and once per session.
  // These only run once Crisp is active (consent accepted), because `$crisp` is
  // only defined then — a declined visitor never fires an event.
  const firedThisView = {};

  const fireCrispEvent = (name, data) => {
    if (firedThisView[name]) {
      return;
    }
    firedThisView[name] = true;
    try {
      const sessionKey = `cn1-crisp-ev-${name}`;
      if (sessionStorage.getItem(sessionKey)) {
        return; // already fired earlier this session
      }
      sessionStorage.setItem(sessionKey, "1");
    } catch (e) {
      // sessionStorage unavailable (private mode) — fall back to per-view guard only
    }
    if (!window.$crisp) {
      return;
    }
    try {
      const event = data ? [name, data, "blue"] : [name];
      window.$crisp.push(["set", "session:event", [[event]]]);
    } catch (e) {
      // no-op
    }
  };

  let triggersRegistered = false;

  const registerCrispTriggerEvents = () => {
    if (triggersRegistered) {
      return;
    }
    triggersRegistered = true;
    const path = (window.location.pathname || "").toLowerCase();
    const meta = { page: path };

    // 1. ConsoleDwell60 -> "T1 — First-build nudge (console)".
    //    The build console lives on cloud.codenameone.com/console (a separate app);
    //    this matcher fires there if this script is ever bundled into it. On the
    //    marketing site the path never contains /console, so it stays dormant here.
    if (path.includes("/console")) {
      setTimeout(() => fireCrispEvent("ConsoleDwell60", meta), 60000);
    }

    // 2. SigningScreenView -> "T2 — iOS signing help".
    //    The real signing/certificate screen is in the console app; the site's proxy
    //    is a view of the /signing/ doc page. `onSigningScreenOpen` is also exposed
    //    globally so the console app can call it directly when that screen opens.
    if (/\/signing/.test(path)) {
      fireCrispEvent("SigningScreenView", meta);
    }

    // 3. BuildError -> "T3 — Build-error rescue".
    //    Fired on build failure by the console app (not this repo). Exposed here as a
    //    helper so whatever surfaces a build error can call it; not auto-fired on the site.

    // 4. GettingStartedDwell -> "T4 — Getting-started nudge".
    //    Spec paths /getting-started and /docs; on this site "docs" maps to
    //    /developing-in-codename-one and /developer-guide.
    if (/\/(getting-started|docs|developing-in-codename-one|developer-guide)/.test(path)) {
      setTimeout(() => fireCrispEvent("GettingStartedDwell", meta), 20000);
    }

    // 5. PricingEvaluator -> "T5 — Evaluator (pricing/vs pages)".
    //    30s on pricing/compare pages OR upward exit-intent, whichever comes first.
    if (/\/(pricing|compare|comparison-chart)/.test(path) || /vs/i.test(path)) {
      const firePricing = () => fireCrispEvent("PricingEvaluator", meta);
      setTimeout(firePricing, 30000);
      document.addEventListener("mouseout", (e) => {
        if (!e.relatedTarget && e.clientY <= 0) {
          firePricing();
        }
      });
    }
  };

  // Public hooks for surfaces that know exactly when the moment happened (e.g. the
  // external console): call these to fire the event on demand.
  window.cn1CrispEvents = window.cn1CrispEvents || {
    signingScreenOpen: () => fireCrispEvent("SigningScreenView", { page: "console" }),
    buildError: (data) => fireCrispEvent("BuildError", data),
  };

  const loadCrisp = () => {
    if (window.CRISP_WEBSITE_ID || document.getElementById("cn1-crisp-loader")) {
      registerCrispTriggerEvents();
      return;
    }
    window.$crisp = window.$crisp || [];
    window.CRISP_WEBSITE_ID = WEBSITE_ID;
    const d = document;
    const s = d.createElement("script");
    s.id = "cn1-crisp-loader";
    s.src = "https://client.crisp.chat/l.js";
    s.async = true;
    d.head.appendChild(s);
    registerCrispTriggerEvents();
  };

  const hideCrisp = () => {
    window.$crisp = window.$crisp || [];
    try {
      window.$crisp.push(["do", "chat:hide"]);
    } catch (e) {
      // no-op
    }
    const crispNode = document.querySelector(".crisp-client");
    if (crispNode) {
      crispNode.style.display = "none";
    }
  };

  const banner = document.querySelector("[data-cn1-cookie-banner]");
  const acceptBtn = document.querySelector("[data-cn1-cookie-accept]");
  const declineBtn = document.querySelector("[data-cn1-cookie-decline]");

  const closeBanner = () => {
    if (banner) {
      banner.setAttribute("hidden", "hidden");
      banner.style.display = "none";
    }
  };

  const openBanner = () => {
    if (banner) {
      banner.removeAttribute("hidden");
      banner.style.display = "flex";
    }
  };

  const acceptConsent = () => {
    setConsent("accepted");
    closeBanner();
    loadCrisp();
  };

  const declineConsent = () => {
    setConsent("declined");
    closeBanner();
    hideCrisp();
  };

  const consent = getConsent();
  if (consent === "accepted") {
    loadCrisp();
    closeBanner();
  } else if (consent === "declined") {
    hideCrisp();
    closeBanner();
  } else {
    openBanner();
  }

  if (acceptBtn) {
    acceptBtn.addEventListener("click", acceptConsent);
  }

  if (declineBtn) {
    declineBtn.addEventListener("click", declineConsent);
  }

  document.querySelectorAll("[data-cn1-enable-chat]").forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      acceptConsent();
    });
  });

  document.querySelectorAll("[data-cn1-manage-chat]").forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      openBanner();
    });
  });
})();
