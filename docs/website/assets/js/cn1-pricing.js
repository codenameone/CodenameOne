(() => {
  const root = document.querySelector("[data-billing-toggle]");
  const checkoutButtons = Array.from(document.querySelectorAll("[data-paddle-checkout]"));
  if (!root && checkoutButtons.length === 0) return;

  const buttons = root ? Array.from(root.querySelectorAll("[data-billing]")) : [];
  const hint = root ? root.querySelector("[data-billing-hint]") : null;
  const cards = Array.from(document.querySelectorAll(".cn1-price-card[data-plan]"));
  let currentMode = "monthly";

  const setupPaddle = () => {
    if (!window.Paddle || typeof window.Paddle.Setup !== "function") return false;
    if (!window.__cn1PaddleInitialized) {
      window.Paddle.Setup({ vendor: 122248 });
      window.__cn1PaddleInitialized = true;
    }
    return true;
  };

  const resolveEmail = () => {
    const cached = localStorage.getItem("cn1_checkout_email") || "";
    const input = window.prompt("Enter your email for checkout:", cached);
    if (!input) return null;
    const email = input.trim();
    if (!email || !email.includes("@")) {
      window.alert("Please enter a valid email address.");
      return null;
    }
    localStorage.setItem("cn1_checkout_email", email);
    return email;
  };

  const wireCheckout = () => {
    checkoutButtons.forEach((button) => {
      button.addEventListener("click", () => {
        if (!setupPaddle()) {
          window.alert("Checkout is temporarily unavailable. Please try again in a moment.");
          return;
        }
        const email = resolveEmail();
        if (!email) return;

        const product = currentMode === "annual"
          ? button.dataset.productAnnual
          : button.dataset.productMonthly;
        const success = button.dataset.successUrl || "https://www.codenameone.com/pricing/";
        if (!product) return;

        window.Paddle.Checkout.open({
          product,
          marketingConsent: "1",
          success,
          email
        });
      });
    });
  };

  const update = (mode) => {
    currentMode = mode;
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
  wireCheckout();
})();
