(() => {
  if (!document.querySelector(".cn1-hero")) return;
  document.body.classList.add("cn1-homepage");

  const accordions = document.querySelectorAll(".cn1-accordion");
  accordions.forEach((accordion) => {
    const items = Array.from(accordion.querySelectorAll(".cn1-acc-item"));
    const setState = (item, open) => {
      const trigger = item.querySelector(".cn1-acc-trigger");
      const panel = item.querySelector(".cn1-acc-panel");
      if (!trigger || !panel) return;
      item.classList.toggle("is-open", open);
      trigger.setAttribute("aria-expanded", open ? "true" : "false");
      panel.style.maxHeight = open ? panel.scrollHeight + "px" : "0px";
      const icon = trigger.querySelector(".cn1-acc-caret i");
      if (icon) {
        icon.classList.toggle("fa-chevron-down", open);
        icon.classList.toggle("fa-chevron-right", !open);
      }
    };

    items.forEach((item) => setState(item, item.classList.contains("is-open")));
    accordion.addEventListener("click", (event) => {
      const trigger = event.target.closest(".cn1-acc-trigger");
      if (!trigger) return;
      const item = trigger.closest(".cn1-acc-item");
      if (!item) return;
      const willOpen = !item.classList.contains("is-open");
      items.forEach((it) => setState(it, false));
      setState(item, willOpen);
    });
  });

  const carousels = document.querySelectorAll(".cn1-carousel[data-carousel]");
  carousels.forEach((carousel) => {
    const track = carousel.querySelector(".cn1-carousel__track");
    const slides = Array.from(carousel.querySelectorAll(".cn1-carousel__slide"));
    const dotsWrap = carousel.querySelector(".cn1-carousel__dots");
    const prev = carousel.querySelector(".cn1-carousel__prev");
    const next = carousel.querySelector(".cn1-carousel__next");
    if (!track || slides.length === 0) return;

    let current = slides.findIndex((s) => s.classList.contains("is-active"));
    if (current < 0) current = 0;

    const setSlide = (index) => {
      current = (index + slides.length) % slides.length;
      slides.forEach((slide, i) => slide.classList.toggle("is-active", i === current));
      track.style.transform = `translateX(${-100 * current}%)`;
      if (dotsWrap) {
        dotsWrap.querySelectorAll("button").forEach((dot, i) => {
          dot.classList.toggle("is-active", i === current);
          dot.setAttribute("aria-current", i === current ? "true" : "false");
        });
      }
    };

    if (dotsWrap) {
      dotsWrap.innerHTML = slides
        .map((_, i) => `<button type="button" aria-label="Go to slide ${i + 1}"></button>`)
        .join("");
      dotsWrap.querySelectorAll("button").forEach((dot, i) => {
        dot.addEventListener("click", () => setSlide(i));
      });
    }

    if (prev) prev.addEventListener("click", () => setSlide(current - 1));
    if (next) next.addEventListener("click", () => setSlide(current + 1));
    setSlide(current);
  });
})();
