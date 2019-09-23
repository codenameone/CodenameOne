(function(exports){

var o = {};

    o.getDouble_ = function(callback) {
        callback.complete([1, 2, 3, -1]);
    };

    o.getBytes_ = function(callback) {
        callback.complete([1, 2, 3, -1]);
    };

    o.getInts_ = function(callback) {
        callback.complete([1, 2, 3, -1]);
    };

    o.setInts__int_1ARRAY = function(param1, callback) {
        console.log(param1);
        var array = [];
        for (var i=0; i<param1.length; i++) {
            array[i] = param1[i];
        }
        callback.complete(array);
    };

    o.setDoubles__double_1ARRAY = function(param1, callback) {
        console.log(param1);
        var array = [];
        for (var i=0; i<param1.length; i++) {
            array[i] = param1[i];
        }
        callback.complete(array);
    };

    o.setBytes__byte_1ARRAY = function(param1, callback) {
        console.log(param1);
        var array = [];
        for (var i=0; i<param1.length; i++) {
            array[i] = param1[i];
        }
        callback.complete(array);
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codename1_testnatives_MyNativeInterface= o;

})(cn1_get_native_interfaces());
