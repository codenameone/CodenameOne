// Bazaar storefront progressive enhancement. Every page works without it.
(function () {
  var body = document.body;
  // The store's colour arrives as a data attribute: compiled views do not
  // write dynamic style attributes. Only a #rgb/#rrggbb value is applied.
  var accent = body && body.getAttribute('data-accent');
  if (accent && /^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(accent)) {
    document.documentElement.style.setProperty('--accent', accent);
  }
  // schema.org JSON-LD travels HTML-escaped in a data attribute, because a
  // compiled view refuses dynamic script content. Google renders JavaScript
  // and reads the script this creates; the page's microdata covers crawlers
  // that do not.
  var holder = document.getElementById('structured-data');
  var ld = holder && holder.getAttribute('data-ld');
  if (ld) {
    try {
      JSON.parse(ld);
      var s = document.createElement('script');
      s.type = 'application/ld+json';
      s.text = ld;
      document.head.appendChild(s);
    } catch (e) { /* malformed data is simply not published */ }
  }
  // App slot scripts the owner placed. The server printed only those this
  // visitor consented to (and the page's policy allows only their hosts); a
  // compiled view never prints a dynamic script element, so they load here.
  var slots = document.querySelectorAll('.slot-script[data-src]');
  for (var i = 0; i < slots.length; i++) {
    var el = document.createElement('script');
    el.src = slots[i].getAttribute('data-src');
    el.async = true;
    el.referrerPolicy = 'strict-origin';
    (slots[i].getAttribute('data-head') === '1' ? document.head : body).appendChild(el);
  }
  // The consent banner posts and reloads without a navigation round trip;
  // with scripts off it is an ordinary form post that redirects back.
  var consent = document.querySelector('form[data-consent]');
  if (consent && window.fetch && window.FormData) {
    consent.addEventListener('submit', function (e) {
      var data = new FormData(consent);
      if (e.submitter && e.submitter.name) {
        data.append(e.submitter.name, e.submitter.value);
      }
      e.preventDefault();
      var banner = consent.closest('.consent');
      if (banner) { banner.hidden = true; }
      fetch(consent.action, { method: 'POST', body: new URLSearchParams(data),
          credentials: 'same-origin', redirect: 'manual' })
        .then(function () {
          if (data.get('choice') !== 'none') { window.location.reload(); }
        });
    });
  }
})();
// The header's language and market menus. Each is a <details> holding a
// form, so with scripts off it opens on click and its buttons post. Here it
// becomes a menu button: aria-expanded on the toggle, menuitemradio items,
// arrow keys and Home/End between them, Escape and an outside click close it
// and give focus back to the toggle.
(function () {
  var picks = document.querySelectorAll('details[data-pick]');
  function items(d) { return d.querySelectorAll('.pick-item'); }
  function close(d, focus) {
    if (!d.open) { return; }
    d.open = false;
    d.querySelector('summary').setAttribute('aria-expanded', 'false');
    if (focus) { d.querySelector('summary').focus(); }
  }
  for (var i = 0; i < picks.length; i++) {
    (function (d) {
      var summary = d.querySelector('summary');
      var menu = d.querySelector('.pick-menu');
      var id = 'pick-' + i;
      menu.id = id;
      menu.setAttribute('role', 'menu');
      summary.setAttribute('role', 'button');
      summary.setAttribute('aria-haspopup', 'menu');
      summary.setAttribute('aria-controls', id);
      summary.setAttribute('aria-expanded', 'false');
      var list = items(d);
      for (var j = 0; j < list.length; j++) {
        list[j].setAttribute('role', 'menuitemradio');
        list[j].tabIndex = -1;
      }
      d.addEventListener('toggle', function () {
        summary.setAttribute('aria-expanded', d.open ? 'true' : 'false');
        if (!d.open) { return; }
        for (var k = 0; k < picks.length; k++) {
          if (picks[k] !== d) { close(picks[k], false); }
        }
        var all = items(d);
        var current = d.querySelector('.pick-item[aria-checked=true]') || all[0];
        if (current) { current.focus(); }
      });
      d.addEventListener('keydown', function (e) {
        var all = Array.prototype.slice.call(items(d));
        var at = all.indexOf(document.activeElement);
        if (e.key === 'Escape') {
          e.preventDefault();
          close(d, true);
        } else if (d.open && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
          e.preventDefault();
          var step = e.key === 'ArrowDown' ? 1 : -1;
          all[(at + step + all.length) % all.length].focus();
        } else if (d.open && (e.key === 'Home' || e.key === 'End')) {
          e.preventDefault();
          all[e.key === 'Home' ? 0 : all.length - 1].focus();
        } else if (!d.open && document.activeElement === summary && e.key === 'ArrowDown') {
          e.preventDefault();
          d.open = true;
        } else if (e.key === 'Tab' && d.open) {
          close(d, false);
        }
      });
    })(picks[i]);
  }
  document.addEventListener('click', function (e) {
    for (var k = 0; k < picks.length; k++) {
      if (!picks[k].contains(e.target)) { close(picks[k], false); }
    }
  });
})();
