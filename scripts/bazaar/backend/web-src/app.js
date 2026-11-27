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

/* The live inbox: the page's #inbox-live joins the store's chat socket
   (/ws/chat, with a single-use ticket from data-ticket). On a conversation
   the thread updates as the shopper writes, shows when they type and read,
   and a reply typed in the form goes straight over the socket (a canned reply
   still posts the form, which reaches the shopper live as well). Anywhere in
   the inbox a call from a shopper rings; it is answered on its conversation
   with rtc.js. htmx swaps the page under it, so the socket is kept while the
   store stays the same and dropped when the page has no #inbox-live. */
(function () {
  "use strict";
  var d = document;
  var ws = null, box = null, store = "", chat = "", ice = [], retry = 1000;
  var ring = null, call = null, lastTyping = 0, typingTimer = 0, staffOnline = 0;
  function $(s, r) { return (r || box || d).querySelector(s); }
  function send(f) { if (ws && ws.readyState === 1) { ws.send(JSON.stringify(f)); return true; } return false; }
  function state(t) { var s = $(".live-state"); if (s) { s.textContent = t; } }
  function clock(at) {
    try { return new Date(at).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }); }
    catch (e) { return new Date(at).toLocaleString(); }
  }
  function append(m) {
    var log = $(".live-log");
    if (!log) { return; }
    var li = d.createElement("li");
    li.className = "live-msg" + (m.staff ? " staff" : "");
    li.setAttribute("data-at", String(m.at));
    var p = d.createElement("p");
    p.textContent = m.body;
    var small = d.createElement("small");
    var who = d.createElement("span");
    who.textContent = m.author + (m.staff ? " (staff)" : "");
    var t = d.createElement("time");
    t.textContent = clock(m.at);
    small.appendChild(who);
    small.appendChild(d.createTextNode(" "));
    small.appendChild(t);
    li.appendChild(p);
    li.appendChild(small);
    log.appendChild(li);
    li.scrollIntoView({ block: "nearest" });
  }
  function readUpTo(at) {
    var log = $(".live-log");
    if (!log) { return; }
    var old = log.querySelector(".live-read");
    if (old) { old.parentNode.removeChild(old); }
    var mine = log.querySelectorAll(".live-msg.staff");
    for (var i = mine.length - 1; i >= 0; i--) {
      if (Number(mine[i].getAttribute("data-at")) <= at) {
        var r = d.createElement("span");
        r.className = "live-read";
        r.textContent = "Read";
        mine[i].querySelector("small").appendChild(r);
        return;
      }
    }
  }
  function seen() {
    var log = $(".live-log");
    if (!log || d.hidden) { return; }
    var them = log.querySelectorAll(".live-msg:not(.staff)");
    if (them.length) { send({ op: "seen", chat: chat, at: Number(them[them.length - 1].getAttribute("data-at")) }); }
  }
  function news(text, href) {
    var ul = $(".live-news");
    if (!ul) { return; }
    ul.hidden = false;
    var li = d.createElement("li");
    var a = d.createElement("a");
    a.href = href;
    a.textContent = text;
    li.appendChild(a);
    ul.insertBefore(li, ul.firstChild);
  }
  function chatUrl(id) { return box.getAttribute("data-ticket").replace(/\/chat-ticket$/, "/inbox/" + id); }

  // ------------------------------------------------------------ calls
  function showRing() {
    var r = $(".live-ring");
    if (!r) { return; }
    if (!ring || call) { r.hidden = true; return; }
    r.hidden = false;
    var here = ring.chat === chat;
    $(".live-ring-t").textContent = (ring.video ? "Incoming video call" : "Incoming call")
        + (ring.name ? " from " + ring.name : "");
    var open = $(".live-open");
    open.hidden = here;
    open.href = chatUrl(ring.chat);
    var accept = $(".live-accept");
    accept.hidden = !here;
    accept.disabled = !ring.offer || !window.BazaarCall || !window.BazaarCall.supported();
    $(".live-decline").hidden = !here;
  }
  function endCall(text) {
    if (call) { call.close(); }
    call = null;
    var c = $(".live-call");
    if (c) { c.hidden = true; c.classList.remove("video"); }
    if (text) { state(text); }
    showRing();
  }
  function answer() {
    if (!ring || !ring.offer || call) { return; }
    var r = ring;
    ring = null;
    var c = $(".live-call");
    c.hidden = false;
    c.classList.toggle("video", !!r.video);
    $(".live-call-t").textContent = "Connecting...";
    call = new window.BazaarCall({
      ice: ice,
      local: $(".live-local"),
      remote: $(".live-remote"),
      signal: function (kind, extra) {
        var f = { op: "call", chat: r.chat, kind: kind };
        for (var k in extra) { if (Object.prototype.hasOwnProperty.call(extra, k)) { f[k] = extra[k]; } }
        send(f);
      },
      state: function (s) {
        var t = $(".live-call-t");
        if (s === "connected" && t) { t.textContent = "On a call"; }
        if (s === "failed") { send({ op: "call", chat: r.chat, kind: "END" }); endCall("The call could not connect."); }
      }
    });
    call.chat = r.chat;
    call.answer(r.offer, r.video).then(function () {
      for (var i = 0; i < r.cands.length; i++) { call.candidate(r.cands[i]); }
    }).catch(function () {
      send({ op: "call", chat: r.chat, kind: "END" });
      endCall("Allow the microphone to answer calls.");
    });
    showRing();
  }
  function callFrame(f) {
    if (f.kind === "RING") {
      if (!call) { ring = { chat: f.chat, video: !!f.video, name: f.name || "", offer: null, cands: [] }; }
    } else if (f.kind === "OFFER") {
      if (ring && ring.chat === f.chat) { ring.offer = f.sdp; }
    } else if (f.kind === "ICE") {
      if (call && call.chat === f.chat) { call.candidate(f.candidate); }
      else if (ring && ring.chat === f.chat) { ring.cands.push(f.candidate); }
    } else if (f.kind === "TAKEN") {
      if (ring && ring.chat === f.chat) { ring = null; }
    } else if (f.kind === "END") {
      if (ring && ring.chat === f.chat) { ring = null; }
      if (call && call.chat === f.chat) { endCall("The call ended."); }
    }
    showRing();
  }

  // ------------------------------------------------------------ socket
  function frame(f) {
    if (f.type === "hello") {
      ice = f.ice || [];
      staffOnline = f.online || 1;
      state("Live");
    } else if (f.type === "thread" && f.chat === chat) {
      var log = $(".live-log");
      if (log) {
        while (log.firstChild) { log.removeChild(log.firstChild); }
        (f.messages || []).forEach(append);
        readUpTo(f.visitorRead || 0);
        seen();
      }
      state(f.online ? "Live - the shopper has the chat open" : "Live - the shopper is away; replies are also emailed");
    } else if (f.type === "message") {
      if (f.chat === chat) {
        append(f);
        var ty = $(".live-typing");
        if (ty) { ty.hidden = true; }
        if (!f.staff) { seen(); }
      } else if (!f.staff) {
        news("New message from " + (f.name || "a shopper") + ": " + f.body, chatUrl(f.chat));
      }
    } else if (f.type === "typing" && f.chat === chat && !f.staff) {
      var t = $(".live-typing");
      if (t) {
        t.hidden = false;
        clearTimeout(typingTimer);
        typingTimer = setTimeout(function () { t.hidden = true; }, 5000);
      }
    } else if (f.type === "seen" && f.chat === chat && !f.staff) {
      readUpTo(f.at);
    } else if (f.type === "call") {
      callFrame(f);
    } else if (f.type === "error") {
      state(f.reason || "Something went wrong");
    }
  }
  function teardown() {
    if (call) { send({ op: "call", chat: call.chat, kind: "END" }); endCall(""); }
    ring = null;
    if (ws) { ws.onclose = null; ws.close(); }
    ws = null;
  }
  function connect() {
    var me = box;
    state("Connecting...");
    fetch(me.getAttribute("data-ticket"), { credentials: "same-origin", cache: "no-store" })
      .then(function (r) { if (!r.ok) { throw new Error("ticket " + r.status); } return r.json(); })
      .then(function (t) {
        if (me !== box && store !== me.getAttribute("data-store")) { return; }
        var url = (location.protocol === "https:" ? "wss://" : "ws://") + location.host
            + "/ws/chat?staff=" + encodeURIComponent(t.ticket) + "&store=" + encodeURIComponent(store);
        ws = new WebSocket(url);
        ws.onopen = function () { retry = 1000; if (chat) { send({ op: "watch", chat: chat }); } };
        ws.onmessage = function (e) { frame(JSON.parse(e.data)); };
        ws.onclose = function () {
          ws = null;
          if (call) { endCall("The connection dropped."); }
          state("Reconnecting...");
          setTimeout(function () { if (box && !ws) { connect(); } }, retry);
          retry = Math.min(retry * 2, 30000);
        };
      })
      .catch(function () {
        state("Live updates are unavailable.");
        setTimeout(function () { if (box && !ws) { connect(); } }, retry);
        retry = Math.min(retry * 2, 30000);
      });
  }
  function boot() {
    var s = d.getElementById("inbox-live");
    if (!s) { box = null; teardown(); return; }
    if (s === box) { return; }
    var sameStore = ws && store === s.getAttribute("data-store");
    box = s;
    store = s.getAttribute("data-store");
    var next = s.getAttribute("data-chat");
    if (call && call.chat !== next) { send({ op: "call", chat: call.chat, kind: "END" }); endCall(""); }
    chat = next;
    $(".live-accept").addEventListener("click", answer);
    $(".live-decline").addEventListener("click", function () {
      if (ring) { send({ op: "call", chat: ring.chat, kind: "DECLINE" }); ring = null; showRing(); }
    });
    $(".live-end").addEventListener("click", function () {
      if (call) { send({ op: "call", chat: call.chat, kind: "END" }); endCall("The call ended."); }
    });
    var mute = $(".live-mute");
    mute.addEventListener("click", function () { if (call) { mute.setAttribute("aria-pressed", call.mute() ? "true" : "false"); } });
    var form = d.querySelector("form[action$='/reply']");
    var area = form && form.querySelector("textarea[name=body]");
    if (area) {
      area.addEventListener("input", function () {
        var now = Date.now();
        if (now - lastTyping > 3000) { lastTyping = now; send({ op: "typing", chat: chat }); }
      });
    }
    if (sameStore) {
      state("Live");
      if (chat) { send({ op: "watch", chat: chat }); }
      showRing();
    } else {
      teardown();
      connect();
    }
  }
  /* A reply typed into the form goes over the socket, ahead of htmx's boost. */
  d.addEventListener("submit", function (e) {
    var form = e.target;
    if (!box || !chat || !form.matches || !form.matches("form[action$='/reply']")) { return; }
    var area = form.querySelector("textarea[name=body]");
    var text = area ? area.value.trim() : "";
    if (!text || !send({ op: "send", chat: chat, body: text })) { return; }
    e.preventDefault();
    e.stopPropagation();
    area.value = "";
    var canned = form.querySelector("select[name=canned]");
    if (canned) { canned.selectedIndex = 0; }
  }, true);
  d.addEventListener("visibilitychange", function () { if (!d.hidden && box) { seen(); } });
  setInterval(function () { send({ op: "ping" }); }, 25000);
  d.addEventListener("DOMContentLoaded", boot);
  d.addEventListener("htmx:afterSettle", boot);
})();
