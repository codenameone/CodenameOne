(function(exports) {
    var o = {};

    function detectDarkMode() {
        try {
            var parentDoc = (window.parent && window.parent.document) ? window.parent.document : null;
            if (parentDoc) {
                var body = parentDoc.body;
                var html = parentDoc.documentElement;
                if (body && body.classList) {
                    if (body.classList.contains("dark") || body.classList.contains("cn1-initializr-dark")) {
                        return true;
                    }
                }
                if (html && html.classList && html.classList.contains("dark")) {
                    return true;
                }
                try {
                    var pref = window.parent.localStorage ? window.parent.localStorage.getItem("pref-theme") : null;
                    if (pref === "dark") {
                        return true;
                    }
                    if (pref === "light") {
                        return false;
                    }
                } catch (ignored) {
                    // Ignore storage access failures and continue.
                }
            }
        } catch (err) {
            // Ignore cross-frame access issues and fallback below.
        }
        if (window.matchMedia) {
            return window.matchMedia("(prefers-color-scheme: dark)").matches;
        }
        return false;
    }

    o.isDarkMode_ = function(callback) {
        callback.complete(detectDarkMode());
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

    exports.com_codename1_initializr_WebsiteThemeNative = o;
})(cn1_get_native_interfaces());
