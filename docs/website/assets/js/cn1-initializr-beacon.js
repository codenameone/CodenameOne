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
  // "Email me these steps" from the Initializr's post-download panel. Unlike
  // the download beacon this one carries what the visitor typed -- an email
  // address they asked us to write to -- and nothing else that identifies them.
  var STEPS_ENDPOINT = "https://cloud.codenameone.com/api/v2/funnel/initializr-steps";

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
   * Posts `fields` plus pkg (the hashed package name) to `endpoint`. Returns a
   * promise that always resolves, so callers never see a rejection: for the
   * fire-and-forget download beacon, true when a request was issued; with
   * `confirm`, true only when the server ANSWERED with a success status.
   */
  function post(endpoint, packageName, fields, confirm) {
    try {
      var host = window.location && window.location.hostname;
      if (!REPORTING_HOSTS[host] || !packageName || typeof window.fetch !== "function") {
        return Promise.resolve(false);
      }
      return hashPackageName(packageName).then(function (pkg) {
        if (!pkg) {
          return false;
        }
        var body = "pkg=" + encodeURIComponent(pkg);
        for (var i = 0; i < fields.length; i++) {
          body += "&" + fields[i][0] + "=" + encodeURIComponent(fields[i][1] ? String(fields[i][1]) : "");
        }
        // fetch rather than navigator.sendBeacon: sendBeacon always sends
        // credentials, and cloud.codenameone.com is same-site with the website,
        // so it would attach the visitor's BuildCloud cookies -- an identifier
        // this beacon must not carry. credentials:"omit" and no-referrer keep
        // the request anonymous; keepalive lets it outlive a page navigation.
        // A form-encoded body is a CORS "simple" request, so no preflight.
        // `confirm` uses mode "cors" so the status is readable (BuildCloud
        // answers the website origin with Access-Control-Allow-Origin):
        // "no-cors" gives an opaque response that looks the same for a 202 as
        // for a 400, 429 or 503, and the steps panel would claim an email that
        // was refused.
        return window.fetch(endpoint, {
          method: "POST",
          mode: confirm ? "cors" : "no-cors",
          credentials: "omit",
          keepalive: true,
          referrerPolicy: "no-referrer",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: body
        }).then(function (response) {
          return confirm ? !!(response && response.ok) : true;
        }, function () {
          // The request never left (offline, blocked): say so, so the steps
          // panel does not claim an email is on its way. Still never rejects.
          return false;
        });
      }).catch(function () {
        return false;
      });
    } catch (e) {
      return Promise.resolve(false);
    }
  }

  function send(packageName, template) {
    return post(ENDPOINT, packageName, [["template", template]]);
  }

  /*
   * The visitor asked for the next steps by email. BuildCloud sends them once
   * and may follow up if the hashed package never reaches a first build. No
   * request without an address.
   */
  function sendSteps(email, packageName, template, ide, build) {
    var address = email ? String(email).trim() : "";
    if (!address) {
      return Promise.resolve(false);
    }
    return post(STEPS_ENDPOINT, packageName,
      [["email", address], ["template", template], ["ide", ide], ["build", build]], true);
  }

  window.cn1InitializrBeacon = { send: send, sendSteps: sendSteps };
})();
