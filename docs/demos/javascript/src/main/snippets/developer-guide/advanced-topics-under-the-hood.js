// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::advanced-topics-under-the-hood-javascript-001[]
(function(exports){

var o = {};

    o.helloWorld__java_lang_String = function(param1, callback) {
        callback.error(new Error("Not implemented yet"));
    };

    o.isSupported_ = function(callback) {
        callback.complete(false);
    };

exports.com_mycompany_myapp_MyNative= o;

})(cn1_get_native_interfaces());
// end::advanced-topics-under-the-hood-javascript-001[]

// tag::advanced-topics-under-the-hood-javascript-002[]
(function(exports){

var o = {};

    o.helloWorld__java_lang_String = function(param1, callback) {
        callback.complete("Hello World!!!");
    }

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_my_code_MyNative = o;

})(cn1_get_native_interfaces());
// end::advanced-topics-under-the-hood-javascript-002[]

// tag::advanced-topics-under-the-hood-javascript-003[]
o.print__java_lang_String = function(param1, callback) {
    console.log(param1);
    callback.complete();
}
// end::advanced-topics-under-the-hood-javascript-003[]

// tag::advanced-topics-under-the-hood-javascript-004[]
o.add__int_int = function(param1, param2, callback) {
    callback.complete(param1 + param2);
}
// end::advanced-topics-under-the-hood-javascript-004[]

// tag::advanced-topics-under-the-hood-javascript-005[]
o.add__int_1ARRAY = function(param1, callback) {
    var c = 0, len = param1.length;
    for (var i =0; i<len; i++) {
        c += param1[i];
    }
    callback.complete(c);
}
// end::advanced-topics-under-the-hood-javascript-005[]

// tag::advanced-topics-under-the-hood-javascript-006[]
o.createHelloComponent_ = function(callback) {
    var c = jQuery('<div>Hello World</div>')
            .css({'background-color' : 'yellow', 'border' : '1px solid blue'});
    callback.complete(c.get(0));
};
// end::advanced-topics-under-the-hood-javascript-006[]

// tag::advanced-topics-under-the-hood-javascript-007[]
o["test__byte_boolean_char_short_int_long_float_double_java_lang_String_byte_1ARRAY_boolean_1ARRAY_char_1ARRAY_short_1ARRAY_int_1ARRAY_long_1ARRAY_float_1ARRAY_double_1ARRAY_com_codename1_ui_PeerComponent"] = function(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10, param11, param12, param13, param14, param15, param16, param17, param18, callback) {
    callback.error(new Error("Not implemented yet"));
};
// end::advanced-topics-under-the-hood-javascript-007[]

// tag::advanced-topics-under-the-hood-javascript-008[]
var fireMapChangeEvent = this.$GLOBAL$.com_codename1_googlemaps_MapContainer.fireMapChangeEvent__int_int_double_double;
google.maps.event.addListener(this.map, 'bounds_changed', function() {
    fireMapChangeEvent(self.mapId, self.map.getZoom(), self.map.getCenter().lat(), self.map.getCenter().lng());
});
// end::advanced-topics-under-the-hood-javascript-008[]

// tag::advanced-topics-under-the-hood-javascript-009[]
// end::advanced-topics-under-the-hood-javascript-009[]

// tag::advanced-topics-under-the-hood-javascript-010[]
var asyncFireMapChangeEvent = this.$GLOBAL$.com_codename1_googlemaps_MapContainer.fireMapChangeEvent__int_int_double_double$async;
// end::advanced-topics-under-the-hood-javascript-010[]
