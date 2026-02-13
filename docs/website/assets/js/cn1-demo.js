(() => {
  const carousels = document.querySelectorAll("[data-cn1-demo-carousel]");
  carousels.forEach((root) => {
    const track = root.querySelector("[data-cn1-demo-track]");
    const slides = Array.from(root.querySelectorAll("[data-cn1-demo-slide]"));
    const dots = Array.from(root.querySelectorAll("[data-cn1-demo-dot]"));
    const prev = root.querySelector("[data-cn1-demo-prev]");
    const next = root.querySelector("[data-cn1-demo-next]");
    if (!track || slides.length <= 1) return;

    let index = 0;

    const render = () => {
      track.style.transform = `translateX(-${index * 100}%)`;
      dots.forEach((dot, i) => {
        dot.setAttribute("aria-current", i === index ? "true" : "false");
      });
    };

    const go = (value) => {
      const max = slides.length - 1;
      index = Math.max(0, Math.min(max, value));
      render();
    };

    const step = (delta) => {
      const max = slides.length - 1;
      let nextIndex = index + delta;
      if (nextIndex < 0) nextIndex = max;
      if (nextIndex > max) nextIndex = 0;
      go(nextIndex);
    };

    prev?.addEventListener("click", () => step(-1));
    next?.addEventListener("click", () => step(1));
    dots.forEach((dot, i) => {
      dot.addEventListener("click", () => go(i));
    });

    render();
  });
})();
