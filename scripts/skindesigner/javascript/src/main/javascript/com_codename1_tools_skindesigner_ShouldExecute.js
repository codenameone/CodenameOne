(function(exports){

var o = {};

    function notifyUiReady() {
        var sendReady = function() {
            try {
                if (window.parent && window.parent !== window && window.parent.postMessage) {
                    window.parent.postMessage({ type: "cn1-skindesigner-ui-ready" }, "*");
                }
            } catch (ignored) {
                // Ignore cross-origin/sandbox restrictions in embedded website mode.
            }
        };

        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(function() {
                window.requestAnimationFrame(sendReady);
            });
        } else {
            window.setTimeout(sendReady, 48);
        }
    }

    o.shouldExecute_ = function(callback) {
        callback.complete(true);
    };

    o.isSupported_ = function(callback) {
        notifyUiReady();
        callback.complete(true);
    };

exports.com_codename1_tools_skindesigner_ShouldExecute= o;

})(cn1_get_native_interfaces());
