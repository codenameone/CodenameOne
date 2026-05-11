(function(exports){

var o = {};

    o.simulateStatusBarTap_ = function(callback) {
        callback.complete(false);
    };

    o.getTapCount_ = function(callback) {
        callback.complete(0);
    };

    o.isSupported_ = function(callback) {
        callback.complete(false);
    };

exports.com_codenameone_examples_hellocodenameone_StatusBarTapDiagnosticNative = o;

})(cn1_get_native_interfaces());
