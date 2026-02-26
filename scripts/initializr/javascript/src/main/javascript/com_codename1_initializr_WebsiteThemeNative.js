(function(exports){

var o = {};

    o.isDarkMode_ = function(callback) {
        var dark = false;
        var hasExplicitPreference = false;
        try {
            var parentDoc = (window.parent && window.parent.document) ? window.parent.document : null;
            if (parentDoc && parentDoc.body && parentDoc.body.classList) {
                dark = parentDoc.body.classList.contains("dark") || parentDoc.body.classList.contains("cn1-initializr-dark");
            }
            if (!dark && window.parent && window.parent.localStorage) {
                var pref = window.parent.localStorage.getItem("pref-theme");
                if (pref === "dark") {
                    dark = true;
                    hasExplicitPreference = true;
                } else if (pref === "light") {
                    dark = false;
                    hasExplicitPreference = true;
                }
            }
        } catch (ignored) {
            // Ignore parent access failures and fallback below.
        }
        if (!hasExplicitPreference && !dark && window.matchMedia) {
            dark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        }
        callback.complete(!!dark);
    };


    o.notifyUiReady_ = function(callback) {
        try {
            if (window.parent && window.parent !== window && window.parent.postMessage) {
                window.parent.postMessage({ type: "cn1-initializr-ui-ready" }, "*");
            }
        } catch (ignored) {
            // Ignore cross-origin or sandbox restrictions.
        }
        callback.complete();
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codename1_initializr_WebsiteThemeNative= o;

})(cn1_get_native_interfaces());
