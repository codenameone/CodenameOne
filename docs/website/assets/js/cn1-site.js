(() => {
  const path = window.location.pathname || "";
  const isBlogPath = path === "/blog/" || path.startsWith("/blog/");
  if (isBlogPath) {
    document.body.classList.add("cn1-blog-post");
  }

  const mobileMq = window.matchMedia("(max-width: 980px)");
  const header = document.querySelector(".cn1-header");
  if (header) {
    const items = Array.from(header.querySelectorAll("#menu > li.has-children"));
    const navToggle = header.querySelector("#cn1-nav-toggle");

    const closeAll = () => {
      items.forEach((item) => {
        item.classList.remove("open");
        const trigger = item.querySelector(".cn1-menu-trigger");
        if (trigger) trigger.setAttribute("aria-expanded", "false");
      });
    };

    items.forEach((item) => {
      const trigger = item.querySelector(".cn1-menu-trigger");
      if (!trigger) return;
      trigger.addEventListener("click", (event) => {
        if (!mobileMq.matches) return;
        event.preventDefault();
        const willOpen = !item.classList.contains("open");
        closeAll();
        if (willOpen) {
          item.classList.add("open");
          trigger.setAttribute("aria-expanded", "true");
        }
      });
    });

    if (navToggle) {
      navToggle.addEventListener("change", () => {
        if (!navToggle.checked) closeAll();
      });
    }

    const resetDesktop = () => {
      if (!mobileMq.matches) closeAll();
    };

    if (mobileMq.addEventListener) {
      mobileMq.addEventListener("change", resetDesktop);
    } else if (mobileMq.addListener) {
      mobileMq.addListener(resetDesktop);
    }

    const onScroll = () => {
      header.classList.toggle("cn1-header--compact", window.scrollY > 36);
    };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  const updateThemeUi = () => {
    const dark = document.body.classList.contains("dark");
    const label = dark ? "Switch to Light Mode" : "Switch to Dark Mode";
    document.querySelectorAll("[data-cn1-theme-toggle]").forEach((el) => {
      el.setAttribute("aria-label", label);
      el.setAttribute("title", label);
    });
  };

  const toggleTheme = () => {
    const dark = document.body.classList.contains("dark");
    if (dark) {
      document.body.classList.remove("dark");
      localStorage.setItem("pref-theme", "light");
    } else {
      document.body.classList.add("dark");
      localStorage.setItem("pref-theme", "dark");
    }
    updateThemeUi();
  };

  const pref = localStorage.getItem("pref-theme");
  if (pref === "dark") {
    document.body.classList.add("dark");
  } else if (pref === "light") {
    document.body.classList.remove("dark");
  }

  document.querySelectorAll("[data-cn1-theme-toggle]").forEach((button) => {
    button.addEventListener("click", toggleTheme);
  });

  const observer = new MutationObserver(updateThemeUi);
  observer.observe(document.body, { attributes: true, attributeFilter: ["class"] });
  updateThemeUi();

  const scheme = window.matchMedia("(prefers-color-scheme: dark)");
  if (scheme.addEventListener) {
    scheme.addEventListener("change", updateThemeUi);
  } else if (scheme.addListener) {
    scheme.addListener(updateThemeUi);
  }

  document.querySelectorAll("[data-cn1-tabs]").forEach((tabs) => {
    const triggers = Array.from(tabs.querySelectorAll("[data-cn1-tab-trigger]"));
    if (!triggers.length) return;
    const scope = tabs.closest("article, section, main, body") || document;
    const panels = triggers
      .map((trigger) =>
        scope.querySelector(
          `[data-cn1-tab-panel="${trigger.getAttribute("data-cn1-tab-trigger")}"]`
        )
      )
      .filter(Boolean);

    const setActive = (name) => {
      triggers.forEach((trigger) => {
        const active = trigger.getAttribute("data-cn1-tab-trigger") === name;
        trigger.classList.toggle("is-active", active);
      });
      panels.forEach((panel) => {
        const active = panel.getAttribute("data-cn1-tab-panel") === name;
        panel.classList.toggle("is-active", active);
        panel.hidden = !active;
      });
    };

    triggers.forEach((trigger) => {
      trigger.addEventListener("click", () => {
        setActive(trigger.getAttribute("data-cn1-tab-trigger"));
      });
    });

    const initial = tabs.querySelector("[data-cn1-tab-trigger].is-active");
    setActive(
      (initial && initial.getAttribute("data-cn1-tab-trigger")) ||
        triggers[0].getAttribute("data-cn1-tab-trigger")
    );
  });

})();
