(() => {
  const CONSENT_KEY = "cn1-crisp-consent-v1";
  const CONSENT_COOKIE = "cn1_crisp_consent";
  const WEBSITE_ID = "e0201fca-1e59-4f30-9d00-8c37aa18293e";
  const CONSENT_TTL_DAYS = 365;
  const CONVERSION_ARRIVAL_KEY = "cn1-conversion-arrival-v1";

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
  // Pages and product surfaces explicitly call the matching function below. This
  // shared script handles consent, timing, and deduplication; it never guesses user
  // intent from the current URL.
  const firedThisView = {};
  const pendingEvents = {};

  const fireCrispEvent = (name, data) => {
    // Consent can change after a page schedules a dwell event. Check it at the
    // moment the event fires, before touching either deduplication guard.
    const dedupeName = data && data.action ? `${name}-${data.action}` : name;
    if (getConsent() !== "accepted" || !window.$crisp || firedThisView[dedupeName]) {
      return false;
    }
    try {
      const sessionKey = `cn1-crisp-ev-${dedupeName}`;
      if (sessionStorage.getItem(sessionKey)) {
        return false; // already fired earlier this session
      }
    } catch (e) {
      // sessionStorage unavailable (private mode) — use the per-view guard only
    }
    try {
      const event = data ? [name, data, "blue"] : [name];
      window.$crisp.push(["set", "session:event", [[event]]]);
      firedThisView[dedupeName] = true;
      try {
        sessionStorage.setItem(`cn1-crisp-ev-${dedupeName}`, "1");
      } catch (e) {
        // sessionStorage unavailable — the per-view guard still applies
      }
      return true;
    } catch (e) {
      return false;
    }
  };

  const requestCrispEvent = (name, data) => {
    const consent = getConsent();
    if (consent === null) {
      // The page requested the event before the visitor answered the consent
      // banner. Remember the request, but don't mark it as fired.
      pendingEvents[name] = data || null;
      return false;
    }
    return consent === "accepted" && fireCrispEvent(name, data);
  };

  const flushPendingEvents = () => {
    Object.keys(pendingEvents).forEach((name) => {
      const data = pendingEvents[name];
      delete pendingEvents[name];
      fireCrispEvent(name, data);
    });
  };

  const clearPendingEvents = () => {
    Object.keys(pendingEvents).forEach((name) => delete pendingEvents[name]);
  };

  const scheduleCrispEvent = (name, delay, data) => {
    window.setTimeout(() => requestCrispEvent(name, data), delay);
  };

  // Explicit hooks for the pages and product surfaces that own each event.
  const crispEvents = window.cn1CrispEvents || {};
  crispEvents.consoleDwell60 = (data) => scheduleCrispEvent(
    "ConsoleDwell60", 60000, data || { page: "console" }
  );
  crispEvents.signingScreenView = (data) => requestCrispEvent(
    "SigningScreenView", data || { page: "signing" }
  );
  // Keep the original console-facing name as an alias.
  crispEvents.signingScreenOpen = (data) => requestCrispEvent(
    "SigningScreenView", data || { page: "console" }
  );
  crispEvents.buildError = (data) => requestCrispEvent("BuildError", data);
  crispEvents.conversionClick = (data) => requestCrispEvent(
    "ConversionClick", data
  );
  crispEvents.gettingStartedDwell = (data) => scheduleCrispEvent(
    "GettingStartedDwell", 20000, data
  );
  crispEvents.pricingEvaluator = (data) => {
    const firePricing = () => requestCrispEvent("PricingEvaluator", data);
    window.setTimeout(firePricing, 30000);
    document.addEventListener("mouseout", (event) => {
      if (!event.relatedTarget && event.clientY <= 0) {
        firePricing();
      }
    });
  };
  window.cn1CrispEvents = crispEvents;

  const consumeConversionArrival = () => {
    let raw;
    try {
      raw = sessionStorage.getItem(CONVERSION_ARRIVAL_KEY);
      if (!raw) return;
      sessionStorage.removeItem(CONVERSION_ARRIVAL_KEY);
    } catch (e) {
      return;
    }

    try {
      const arrival = JSON.parse(raw);
      const current = `${window.location.pathname}${window.location.search}`;
      const referrer = document.referrer ? new URL(document.referrer) : null;
      const referrerPath = referrer ? `${referrer.pathname}${referrer.search}` : "";
      const isFresh = Number.isFinite(arrival.createdAt) && Date.now() - arrival.createdAt < 10 * 60 * 1000;
      if (arrival.action && arrival.destination === current && isFresh && referrer &&
          referrer.origin === window.location.origin && referrerPath === arrival.source) {
        crispEvents.conversionClick({
          action: arrival.action,
          page: arrival.source,
          destination: current
        });
      }
    } catch (e) {
      // Ignore malformed or stale navigation state.
    }
  };
  const loadCrisp = () => {
    if (window.CRISP_WEBSITE_ID || document.getElementById("cn1-crisp-loader")) {
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
  };

  const hideCrisp = () => {
    if (!window.$crisp) {
      return;
    }
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
    flushPendingEvents();
  };

  const declineConsent = () => {
    setConsent("declined");
    clearPendingEvents();
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
  consumeConversionArrival();

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
