(() => {
  const STORAGE_KEY = "cn1-exp-004-arm-v1";
  const EXPERIMENT_ID = "EXP-004";
  const OWNERSHIP = "ownership";
  const REACH = "reach";
  const CONSENT_KEY = "cn1-crisp-consent-v1";
  const CONSENT_COOKIE = "cn1_crisp_consent=accepted";
  const hostname = window.location && window.location.hostname;
  const localPreview = hostname === "localhost" || hostname === "127.0.0.1";
  const previewArm = localPreview
    ? new URLSearchParams(window.location.search || "").get("cn1_exp004") : null;

  const consentAccepted = () => {
    if ((document.cookie || "").split(";").some((part) => part.trim() === CONSENT_COOKIE)) {
      return true;
    }
    try {
      return localStorage.getItem(CONSENT_KEY) === "accepted";
    } catch (e) {
      return false;
    }
  };

  let arm = previewArm === OWNERSHIP || previewArm === REACH ? previewArm : null;
  try {
    if (!arm) {
      arm = localStorage.getItem(STORAGE_KEY);
    }
  } catch (e) {
    // A stable assignment is preferred, but the page still needs a variant when
    // first-party storage is unavailable.
  }

  if (arm !== OWNERSHIP && arm !== REACH) {
    let bucket;
    try {
      const value = new Uint32Array(1);
      window.crypto.getRandomValues(value);
      bucket = value[0];
    } catch (e) {
      bucket = Math.floor(Math.random() * 0x100000000);
    }
    arm = bucket % 2 === 0 ? OWNERSHIP : REACH;
    if (!localPreview && consentAccepted()) {
      try {
        localStorage.setItem(STORAGE_KEY, arm);
      } catch (e) {
        // Keep the in-memory assignment for this page view.
      }
    }
  }

  const persist = () => {
    if (localPreview || !consentAccepted()) {
      return false;
    }
    try {
      localStorage.setItem(STORAGE_KEY, arm);
      return true;
    } catch (e) {
      return false;
    }
  };

  document.documentElement.dataset.cn1Exp004Arm = arm;
  window.cn1Exp004 = Object.freeze({ id: EXPERIMENT_ID, arm, persist });
})();
