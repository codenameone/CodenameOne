// JS-port native impl of com.codename1.initializr.DownloadNative.
//
// Native-interface JS impls in the JS port run on the MAIN thread (not the
// worker) -- the registry populated by ``cn1_get_native_interfaces()`` lives
// in window, and ``initializr_native_handlers.js`` looks up the impl from
// there and invokes the chosen method with the host args + a callback that
// resolves the worker-side Promise. So we have ``document`` here and can
// trigger a download directly; no need to route through ``jvm.invokeHostNative``
// (which is worker-only).
//
// Why this exists: the standard Display.execute(file://) flow on the JS port
// reads bytes back from LocalForage/IndexedDB via openFileAsBlob, but the
// bundled localforage's setItem callback returns null for Uint8Array inputs
// and the read-back exists() check sees nothing -- exec() decides the file
// "doesn't exist" and the user is left with no download. Going Blob+<a>.click()
// direct sidesteps the entire storage layer.
(function(exports) {
  var o = {};

  // Pulls a usable JS string out of whatever shape the host bridge hands
  // a Java String to a native impl. Java Strings on the JS port carry
  // their value as a char[] (cn1_java_lang_String_value) plus offset and
  // count fields; some bridge paths also stash __nativeString as a
  // ready-made JS string sidecar. A handful of legacy paths arrive as
  // plain JS strings already.
  function unwrapJavaString(s, fallback) {
    if (s == null) return fallback;
    if (typeof s === 'string') return s;
    if (typeof s.__nativeString === 'string') return s.__nativeString;
    var chars = s.cn1_java_lang_String_value;
    if (chars && chars.length !== undefined) {
      var off = s.cn1_java_lang_String_offset | 0;
      var cnt = s.cn1_java_lang_String_count;
      if (cnt == null) cnt = chars.length - off;
      cnt = cnt | 0;
      var out = '';
      for (var i = 0; i < cnt; i++) {
        out += String.fromCharCode(chars[off + i] & 0xffff);
      }
      return out;
    }
    try { console.log('CN1INIT:download:unwrapJavaString unknown shape keys=' + Object.keys(s).slice(0, 20).join(',')); } catch (_le) {}
    return fallback;
  }

  o.downloadBytes_ = function(fileName, bytes, callback) {
    try {
      // ``bytes`` is whatever shape the host bridge hands a Java byte[] to
      // the main-thread native impl. The bridge transfers byte[] via
      // structured clone, so on this side we get a plain JS Array of signed
      // numbers (-128..127) -- not a Uint8Array. Coerce.
      var arr;
      if (bytes && bytes.BYTES_PER_ELEMENT !== undefined && bytes.buffer) {
        arr = bytes;
      } else if (bytes && typeof bytes.length === 'number') {
        arr = new Uint8Array(bytes.length);
        for (var i = 0; i < bytes.length; i++) {
          arr[i] = bytes[i] & 0xff;
        }
      } else {
        try { console.log('CN1INIT:download:native-err bad-bytes type=' + (typeof bytes)); } catch (_le) {}
        callback.complete(false);
        return;
      }
      // The host bridge transfers a Java String as its boxed object. The
      // exact shape varies: some bridge paths set ``__nativeString``,
      // others ship the char[] + offset + count directly (the
      // ``cn1_java_lang_String_value`` / ``_offset`` / ``_count`` triple),
      // others may already be a plain JS string. Cover all three, plus
      // log the shape on first call so future regressions are debuggable
      // without adding more probes.
      var name = unwrapJavaString(fileName, 'download');
      var blob = new Blob([arr], { type: 'application/octet-stream' });
      try { console.log('CN1INIT:download:native-fire fileName=' + name + ' len=' + arr.length); } catch (_le) {}
      var doc = (typeof document !== 'undefined') ? document : (window && window.document);
      if (!doc || !doc.body) {
        try { console.log('CN1INIT:download:native-err no-document'); } catch (_le) {}
        callback.complete(false);
        return;
      }
      var url = URL.createObjectURL(blob);
      var a = doc.createElement('a');
      a.href = url;
      a.download = name;
      a.style.display = 'none';
      doc.body.appendChild(a);
      try {
        a.click();
      } finally {
        doc.body.removeChild(a);
      }
      // Revoke after a short delay so the click handler has time to read the URL.
      setTimeout(function() {
        try { URL.revokeObjectURL(url); } catch (_re) {}
      }, 5000);
      callback.complete(true);
    } catch (e) {
      try { console.log('CN1INIT:download:native-err ' + (e && e.message ? e.message : String(e))); } catch (_le) {}
      callback.complete(false);
    }
  };

  o.isSupported_ = function(callback) {
    callback.complete(true);
  };

  exports.com_codename1_initializr_DownloadNative = o;

})(cn1_get_native_interfaces());
