(() => {
  const CONVERSION_ARRIVAL_KEY = "cn1-conversion-arrival-v1";
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

    const closeNav = () => {
      header.classList.remove("cn1-nav-open");
      if (navToggle) navToggle.setAttribute("aria-expanded", "false");
    };

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
        event.preventDefault();
        const willOpen = !item.classList.contains("open");
        closeAll();
        if (willOpen) {
          item.classList.add("open");
          trigger.setAttribute("aria-expanded", "true");
        }
      });
    });

    document.addEventListener("click", (event) => {
      if (!header.contains(event.target)) closeAll();
    });

    if (navToggle) {
      navToggle.addEventListener("click", () => {
        const open = !header.classList.contains("cn1-nav-open");
        header.classList.toggle("cn1-nav-open", open);
        navToggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (!open) closeAll();
      });
    }

    const resetDesktop = () => {
      if (!mobileMq.matches) {
        closeNav();
        closeAll();
      }
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

    document.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") return;
      closeNav();
      closeAll();
      if (navToggle && mobileMq.matches) navToggle.focus();
    });

    header.querySelectorAll(".cn1-nav-links a").forEach((link) => {
      link.addEventListener("click", () => {
        if (mobileMq.matches) closeNav();
      });
    });
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

  const motionProof = document.querySelector("[data-cn1-motion-proof]");
  if (motionProof) {
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const syncMotionProof = () => {
      if (reducedMotion.matches) {
        motionProof.pause();
        motionProof.removeAttribute("src");
        motionProof.load();
        return;
      }
      if (!motionProof.getAttribute("src")) {
        motionProof.setAttribute("src", motionProof.getAttribute("data-src"));
        motionProof.load();
      }
      const playback = motionProof.play();
      if (playback && playback.catch) playback.catch(() => {});
    };
    syncMotionProof();
    if (reducedMotion.addEventListener) {
      reducedMotion.addEventListener("change", syncMotionProof);
    } else if (reducedMotion.addListener) {
      reducedMotion.addListener(syncMotionProof);
    }
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
        trigger.setAttribute("aria-selected", active ? "true" : "false");
        trigger.setAttribute("tabindex", active ? "0" : "-1");
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
      trigger.addEventListener("keydown", (event) => {
        if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
        event.preventDefault();
        const current = triggers.indexOf(trigger);
        let next;
        if (event.key === "Home") {
          next = triggers[0];
        } else if (event.key === "End") {
          next = triggers[triggers.length - 1];
        } else {
          const direction = event.key === "ArrowRight" ? 1 : -1;
          next = triggers[(current + direction + triggers.length) % triggers.length];
        }
        setActive(next.getAttribute("data-cn1-tab-trigger"));
        next.focus();
      });
    });

    const initial = tabs.querySelector("[data-cn1-tab-trigger].is-active");
    setActive(
      (initial && initial.getAttribute("data-cn1-tab-trigger")) ||
        triggers[0].getAttribute("data-cn1-tab-trigger")
    );
  });

  const fireConversion = (link) => {
    if (window.cn1CrispEvents && window.cn1CrispEvents.conversionClick) {
      window.cn1CrispEvents.conversionClick({
        action: link.getAttribute("data-cn1-conversion"),
        page: window.location.pathname
      });
    }
  };

  document.querySelectorAll("[data-cn1-conversion]").forEach((link) => {
    link.addEventListener("click", (event) => {
      const destination = new URL(link.href, window.location.href);
      const current = `${window.location.pathname}${window.location.search}`;
      const target = `${destination.pathname}${destination.search}`;
      const opensElsewhere = link.target === "_blank" || event.metaKey || event.ctrlKey ||
        event.shiftKey || event.altKey || event.button !== 0;
      if (destination.origin !== window.location.origin || opensElsewhere || target === current) {
        fireConversion(link);
        return;
      }

      try {
        sessionStorage.setItem(CONVERSION_ARRIVAL_KEY, JSON.stringify({
          action: link.getAttribute("data-cn1-conversion"),
          source: current,
          destination: target,
          createdAt: Date.now()
        }));
      } catch (_) {
        fireConversion(link);
      }
    });
  });

})();
