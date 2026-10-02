/*
 * Anonymous Initializr download beacon.
 *
 * The Initializr iframe posts "cn1-initializr-project-downloaded" (with the
 * package name and template the visitor chose) after it hands the project ZIP
 * to the browser. layouts/_default/initializr.html forwards that message here.
 *
 * BuildCloud records the package name of every cloud build, so a SHA-256 of
 * the lower-cased package name lets it join "downloaded a project" to "built
 * that project" without the website learning who the visitor is. Only the hash
 * and the template id leave the browser: no email, no cookie, no referrer.
 *
 * Fire-and-forget: the download has already happened when this runs, every
 * failure is swallowed, and nothing waits on the request.
 */
(function () {
  "use strict";

  var ENDPOINT = "https://cloud.codenameone.com/api/v2/funnel/initializr-download";

  // Only the production site reports. Local `hugo server`, PR previews and
  // forks would otherwise post their test downloads into the production funnel
  // (the request is no-cors, so the server's origin allow-list cannot stop it
  // from arriving).
  var REPORTING_HOSTS = { "www.codenameone.com": true, "codenameone.com": true };

  function toHex(buffer) {
    var bytes = new Uint8Array(buffer);
    var out = "";
    for (var i = 0; i < bytes.length; i++) {
      out += (bytes[i] < 16 ? "0" : "") + bytes[i].toString(16);
    }
    return out;
  }

  function hashPackageName(packageName) {
    var subtle = window.crypto && window.crypto.subtle;
    if (!subtle || typeof TextEncoder === "undefined") {
      return Promise.resolve(null);
    }
    var data = new TextEncoder().encode(String(packageName).toLowerCase());
    return subtle.digest("SHA-256", data).then(toHex);
  }

  /*
   * Returns a promise that always resolves (true when a request was issued),
   * so callers never see a rejection.
   */
  function send(packageName, template) {
    try {
      var host = window.location && window.location.hostname;
      if (!REPORTING_HOSTS[host] || !packageName || typeof window.fetch !== "function") {
        return Promise.resolve(false);
      }
      return hashPackageName(packageName).then(function (pkg) {
        if (!pkg) {
          return false;
        }
        var body = "pkg=" + encodeURIComponent(pkg)
          + "&template=" + encodeURIComponent(template ? String(template) : "");
        // fetch rather than navigator.sendBeacon: sendBeacon always sends
        // credentials, and cloud.codenameone.com is same-site with the website,
        // so it would attach the visitor's BuildCloud cookies -- an identifier
        // this beacon must not carry. credentials:"omit" and no-referrer keep
        // the request anonymous; keepalive lets it outlive a page navigation.
        // A form-encoded body is a CORS "simple" request, so no preflight.
        return window.fetch(ENDPOINT, {
          method: "POST",
          mode: "no-cors",
          credentials: "omit",
          keepalive: true,
          referrerPolicy: "no-referrer",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: body
        }).then(function () {
          return true;
        }, function () {
          return true;
        });
      }).catch(function () {
        return false;
      });
    } catch (e) {
      return Promise.resolve(false);
    }
  }

  window.cn1InitializrBeacon = { send: send };
})();
