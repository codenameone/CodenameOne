(function(exports){

var o = {};

    // Runs on the MAIN thread (native-interface impls do), so document/<a> are
    // available and the click happens where the browser's download machinery
    // lives. The data: URL carries the bytes, so there is no worker->main Blob
    // marshalling to fail.
    //
    // NB: the worker dispatches a parameterised native method under its
    // descriptor-mangled key ("download__java_lang_String_java_lang_String"),
    // and the callback is passed LAST (after the declared params) -- see
    // cn1InvokeNativeInterface in browser_bridge.js.
    o["download__java_lang_String_java_lang_String"] = function(fileName, dataUrl, callback) {
        try {
            console.log("CN1INIT:js-downloader:download fileName=" + fileName +
                " dataUrlLen=" + (dataUrl ? dataUrl.length : 0));
            var doc = window.document;
            var a = doc.createElement("a");
            a.href = dataUrl;
            a.download = fileName || "download";
            a.style.display = "none";
            doc.body.appendChild(a);
            a.click();
            doc.body.removeChild(a);
            callback.complete(true);
        } catch (e) {
            try { console.warn("CN1INIT:js-downloader:download-failed " + (e && e.message ? e.message : e)); } catch (ignored) {}
            callback.complete(false);
        }
    };

    o.isSupported_ = function(callback) {
        try { console.log("CN1INIT:js-downloader:isSupported called"); } catch (ignored) {}
        callback.complete(true);
    };

exports.com_codename1_initializr_JsDownloader = o;

})(cn1_get_native_interfaces());
