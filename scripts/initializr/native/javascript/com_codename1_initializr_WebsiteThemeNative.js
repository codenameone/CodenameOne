(function(exports) {
    var o = {};

    function detectDarkMode() {
        try {
            var parentDoc = (window.parent && window.parent.document) ? window.parent.document : null;
            if (parentDoc && parentDoc.body) {
                return parentDoc.body.classList.contains("dark");
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
