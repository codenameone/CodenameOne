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
