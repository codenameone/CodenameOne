(function(exports){

var o = {};

    function readWebsiteThemePreference() {
        try {
            var parentWindow = (window.parent && window.parent !== window) ? window.parent : null;
            var parentDoc = parentWindow && parentWindow.document ? parentWindow.document : null;
            var parentBody = parentDoc && parentDoc.body ? parentDoc.body : null;
            var classes = parentBody && parentBody.classList ? parentBody.classList : null;
            if (classes) {
                if (classes.contains("dark") || classes.contains("cn1-initializr-dark")) {
                    return true;
                }
                if (classes.contains("light") || classes.contains("cn1-initializr-light")) {
                    return false;
                }
            }

            if (parentWindow && parentWindow.localStorage) {
                var pref = parentWindow.localStorage.getItem("pref-theme");
                if (pref === "dark") {
                    return true;
                }
                if (pref === "light") {
                    return false;
                }
            }

            var mediaWindow = parentWindow || window;
            if (mediaWindow.matchMedia) {
                return mediaWindow.matchMedia("(prefers-color-scheme: dark)").matches;
            }
        } catch (ignored) {
            // Ignore parent access failures and fallback below.
        }

        if (window.matchMedia) {
            return window.matchMedia("(prefers-color-scheme: dark)").matches;
        }

        return false;
    }

    o.isDarkMode_ = function(callback) {
        callback.complete(!!readWebsiteThemePreference());
    };

    o.notifyUiReady_ = function(callback) {
        var sendReady = function() {
            try {
                if (window.parent && window.parent !== window && window.parent.postMessage) {
                    window.parent.postMessage({ type: "cn1-playground-ui-ready" }, "*");
                }
            } catch (ignored) {
                // Ignore cross-origin or sandbox restrictions.
            }
            callback.complete();
        };

        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(function() {
                window.requestAnimationFrame(sendReady);
            });
        } else {
            window.setTimeout(sendReady, 48);
        }
    };

    o.locationHref_ = function(callback) {
        // This native impl runs on the iframe's main thread, where ``window`` is
        // the Playground app frame. Deep-link params (?sample= / ?code= / ?css=)
        // may live either on this frame's URL (when opened directly) or on the
        // parent page that embeds it (the codenameone.com /playground wrapper).
        // Prefer whichever carries a query string so sharing works in both cases.
        try {
            var loc = window.location;
            var origin = loc.origin || (loc.protocol + "//" + loc.host);
            var search = loc.search || "";
            var hash = loc.hash || "";
            try {
                var hasQuery = search && search.length > 1;
                if (!hasQuery && window.parent && window.parent !== window && window.parent.location) {
                    var psearch = window.parent.location.search || "";
                    if (psearch && psearch.length > 1) {
                        search = psearch;
                        if (!hash) {
                            hash = window.parent.location.hash || "";
                        }
                    }
                }
            } catch (ignoredParent) {
                // Cross-origin parent (shouldn't happen same-site) -- fall back.
            }
            callback.complete(origin + (loc.pathname || "/") + search + hash);
        } catch (e) {
            callback.complete("");
        }
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codenameone_playground_WebsiteThemeNative = o;

})(cn1_get_native_interfaces());
