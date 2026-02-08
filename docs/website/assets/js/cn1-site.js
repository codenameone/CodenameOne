(() => {
  const mobileMq = window.matchMedia("(max-width: 980px)");
  const header = document.querySelector(".cn1-header");
  if (!header) return;

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
})();
