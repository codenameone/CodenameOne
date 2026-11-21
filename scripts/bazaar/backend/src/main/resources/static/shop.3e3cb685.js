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
})();
