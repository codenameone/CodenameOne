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
      // The host bridge transfers a Java String as its boxed object (with
      // ``__nativeString`` set to the JS-side value). Direct ``String(x)``
      // on that object would give ``"[object Object]"``. Prefer the
      // native-string sidecar, fall back to toString().
      var name;
      if (fileName == null || fileName === '') {
        name = 'download';
      } else if (typeof fileName === 'string') {
        name = fileName;
      } else if (fileName.__nativeString != null) {
        name = String(fileName.__nativeString);
      } else if (typeof fileName.toString === 'function') {
        name = fileName.toString();
      } else {
        name = 'download';
      }
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
