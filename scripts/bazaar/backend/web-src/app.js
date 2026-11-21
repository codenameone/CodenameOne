/* Bazaar web app script: progressive enhancement only. Every page works
   without it; with it, the sidebar opens on a phone, toasts fade, keyboard
   shortcuts work, tables get row navigation and a column chooser, and app slot
   scripts the owner allowed (and the visitor consented to) load. No eval, no
   inline handlers: the page CSP forbids both. */
(function () {
  "use strict";
  var d = document;
  function $(s, r) { return (r || d).querySelector(s); }
  function $$(s, r) { return Array.prototype.slice.call((r || d).querySelectorAll(s)); }

  function toasts(root) {
    $$(".toast:not(.bad)", root).forEach(function (t) {
      setTimeout(function () { t.remove(); }, 5000);
    });
  }

  function menu() {
    var b = $("[data-menu]"), side = $("#side");
    if (!b || !side || b.dataset.on) { return; }
    b.dataset.on = "1";
    b.addEventListener("click", function () {
      var open = side.classList.toggle("open");
      b.setAttribute("aria-expanded", open ? "true" : "false");
    });
  }

  /* Column chooser: hides columns by their data-col, remembered per table. */
  function columns(root) {
    $$("[data-cols]", root).forEach(function (box) {
      var id = box.getAttribute("data-cols"), table = $("table[data-table='" + id + "']");
      if (!table) { return; }
      var key = "cols:" + id, hidden = {};
      try { hidden = JSON.parse(localStorage.getItem(key) || "{}"); } catch (e) { hidden = {}; }
      function apply() {
        $$("[data-col]", table).forEach(function (c) {
          c.hidden = !!hidden[c.getAttribute("data-col")];
        });
      }
      $$("input[type=checkbox]", box).forEach(function (cb) {
        cb.checked = !hidden[cb.value];
        cb.addEventListener("change", function () {
          if (cb.checked) { delete hidden[cb.value]; } else { hidden[cb.value] = 1; }
          try { localStorage.setItem(key, JSON.stringify(hidden)); } catch (e) { /* private mode */ }
          apply();
        });
      });
      apply();
    });
    /* Select-all for bulk actions. */
    $$("input[data-all]", root).forEach(function (all) {
      all.addEventListener("change", function () {
        $$("input[name=ids]", all.closest("form") || d).forEach(function (c) { c.checked = all.checked; });
      });
    });
  }

  /* Owner-allowed slot scripts, only for consented categories (the server
     already left out the rest; this is the loader a compiled view cannot be). */
  function slots(root) {
    $$(".slot-script[data-src]", root).forEach(function (s) {
      if (s.dataset.loaded) { return; }
      s.dataset.loaded = "1";
      var el = d.createElement("script");
      el.src = s.getAttribute("data-src");
      el.async = true;
      el.referrerPolicy = "strict-origin";
      d.body.appendChild(el);
    });
  }

  /* Keyboard: "/" focuses search, "g" + letter jumps, j/k move rows, ? help. */
  var pendingG = 0, row = -1;
  function rows() { return $$("table.data tbody tr[data-href]"); }
  function mark(list) {
    list.forEach(function (r, i) { r.classList.toggle("sel", i === row); });
    if (list[row]) { list[row].scrollIntoView({ block: "nearest" }); }
  }
  d.addEventListener("keydown", function (e) {
    var t = e.target, tag = t && t.tagName;
    if (e.ctrlKey || e.metaKey || e.altKey || tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || (t && t.isContentEditable)) {
      return;
    }
    var k = e.key;
    if (pendingG && Date.now() - pendingG < 1200) {
      pendingG = 0;
      var a = $(".nav a[data-key='" + k + "']");
      if (a) { e.preventDefault(); a.click(); }
      return;
    }
    if (k === "g") { pendingG = Date.now(); return; }
    if (k === "/") {
      var s = $("main input[type=search]");
      if (s) { e.preventDefault(); s.focus(); }
    } else if (k === "?") {
      var h = $("#help");
      if (h && h.showModal) { h.showModal(); }
    } else if (k === "j" || k === "k") {
      var list = rows();
      if (!list.length) { return; }
      row = Math.max(0, Math.min(list.length - 1, row + (k === "j" ? 1 : -1)));
      mark(list);
    } else if (k === "Enter" && row >= 0) {
      var r = rows()[row];
      if (r) { window.location = r.getAttribute("data-href"); }
    }
  });

  function wire(root) {
    toasts(root); columns(root); slots(root); menu(); row = -1;
    var help = $("[data-shortcuts]");
    if (help && !help.dataset.on) {
      help.dataset.on = "1";
      help.addEventListener("click", function () { var h = $("#help"); if (h && h.showModal) { h.showModal(); } });
    }
    /* Rows open on click as well as Enter. */
    $$("table.data tbody tr[data-href]", root).forEach(function (r) {
      r.addEventListener("click", function (e) {
        if (e.target.closest("a,button,input,label,select")) { return; }
        window.location = r.getAttribute("data-href");
      });
    });
    /* The consent banner's "choose" link shows the form in place. */
    $$("[data-consent-more]", root).forEach(function (b) {
      b.addEventListener("click", function (e) { e.preventDefault(); var f = $("#consent-prefs"); if (f) { f.hidden = false; } });
    });
  }

  d.addEventListener("DOMContentLoaded", function () { wire(d); });
  /* htmx swaps: wire the new content and move focus to it for screen readers. */
  d.addEventListener("htmx:afterSwap", function (e) {
    wire(e.target);
    var m = $("#main");
    if (m && e.target === d.body) { m.focus({ preventScroll: true }); }
  });
})();
