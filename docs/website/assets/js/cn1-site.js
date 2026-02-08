(() => {
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
})();
