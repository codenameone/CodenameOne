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
