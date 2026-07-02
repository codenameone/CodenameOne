(function(exports){

var o = {};

    // Runs on the JavaScript main-thread front end (not the Web Worker), where
    // window.location and matchMedia are available. The website sets the iframe
    // src to "?theme=dark" / "?theme=light"; resolve that, otherwise fall back
    // to the OS prefers-color-scheme. Returns "dark" / "light" / "".
    o.currentTheme_ = function(callback) {
        var theme = "";
        try {
            var loc = (typeof window !== "undefined" && window.location) ? window.location
                    : ((typeof self !== "undefined" && self.location) ? self.location : null);
            var search = (loc && loc.search) ? String(loc.search) : "";
            var m = /[?&]theme=([^&#]*)/.exec(search);
            if (m && m[1]) {
                theme = decodeURIComponent(m[1].replace(/\+/g, " ")).toLowerCase();
            }
            if (theme !== "dark" && theme !== "light"
                    && typeof window !== "undefined" && window.matchMedia) {
                theme = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
            }
        } catch (e) {
            theme = "";
        }
        callback.complete(theme);
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codename1_tools_skindesigner_WebsiteTheme= o;

})(cn1_get_native_interfaces());
