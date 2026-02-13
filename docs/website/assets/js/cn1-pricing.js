(() => {
  const root = document.querySelector("[data-billing-toggle]");
  const signupLinks = Array.from(document.querySelectorAll("[data-signup-link][data-level]"));
  if (!root && signupLinks.length === 0) return;

  const buttons = root ? Array.from(root.querySelectorAll("[data-billing]")) : [];
  const hint = root ? root.querySelector("[data-billing-hint]") : null;
  const cards = Array.from(document.querySelectorAll(".cn1-price-card[data-plan]"));
  const buildSignupUrl = (level, mode) =>
    `https://cloud.codenameone.com/secure/signup?level=${encodeURIComponent(level)}&mode=${encodeURIComponent(mode)}`;

  const update = (mode) => {
    if (root) {
      root.dataset.billingMode = mode;
    }

    buttons.forEach((btn) => {
      const active = btn.dataset.billing === mode;
      btn.classList.toggle("is-active", active);
      btn.setAttribute("aria-selected", active ? "true" : "false");
    });

    cards.forEach((card) => {
      const priceNode = card.querySelector("[data-price]");
      const billNode = card.querySelector("[data-bill]");
      const monthly = card.dataset.monthly;
      const annualMonthly = card.dataset.annualMonthly;
      const annualTotal = card.dataset.annualTotal;

      if (!priceNode || !billNode) return;

      if (mode === "annual") {
        priceNode.textContent = annualMonthly;
        billNode.textContent = `Billed annually at $${annualTotal}/year`;
      } else {
        priceNode.textContent = monthly;
        billNode.textContent = "Billed monthly";
      }
    });

    signupLinks.forEach((link) => {
      const level = (link.dataset.level || "").toLowerCase();
      if (!level) return;
      link.href = buildSignupUrl(level, mode);
    });

    if (hint) {
      hint.textContent = mode === "annual"
        ? "Annual billing selected. Prices shown are monthly equivalents with yearly billing."
        : "Monthly billing selected. Switch to annual to lower the monthly equivalent.";
    }
  };

  buttons.forEach((btn) => {
    btn.addEventListener("click", () => update(btn.dataset.billing));
  });

  update("monthly");
})();
