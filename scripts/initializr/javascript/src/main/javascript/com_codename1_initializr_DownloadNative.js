// JS-port native impl of com.codename1.initializr.DownloadNative.
//
// Runs inside the worker. We can't touch ``document.createElement('a')``
// here (Workers don't have ``document``), so the Blob construction
// happens in the worker but the actual download trigger is routed
// through the existing main-thread host handler
// ``__cn1_register_save_blob__`` in browser_bridge.js. That handler
// auto-fires the click on receive, so we only need to register --
// the download starts before this generator resumes.
//
// Why this exists: the standard Display.execute(file://) flow on the
// JS port reads the bytes back from LocalForage/IndexedDB via
// openFileAsBlob, but the bundled localforage's setItem callback
// (and the immediate getItem after it) returns null for Uint8Array
// inputs -- diagnosed against the PR #4795 preview Initializr Generate
// flow. Going Blob-direct sidesteps that entire layer.
(function(exports) {
  var o = {};

  o.downloadBytes_ = function*(fileName, bytes, callback) {
    try {
      // The worker-side ``bytes`` param is whatever the translator hands
      // a Java ``byte[]`` parameter on the JS port. Empirically that's a
      // plain JS Array of signed-byte numbers (-128..127); some bridge
      // paths hand back a Uint8Array view directly. Cover both: detect
      // typed-array via ``BYTES_PER_ELEMENT`` (defined on every typed
      // array constructor instance) and copy into a fresh Uint8Array
      // otherwise so the Blob constructor sees the canonical shape.
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
      var name = (fileName == null || fileName === '') ? 'download' : String(fileName);
      var blob = new Blob([arr], { type: 'application/octet-stream' });
      try { console.log('CN1INIT:download:native register fileName=' + name + ' len=' + arr.length); } catch (_le) {}
      // The register handler in browser_bridge.js auto-fires the
      // download on receipt -- by the time this yield resolves the
      // browser has already presented (or queued) the file.
      yield jvm.invokeHostNative('__cn1_register_save_blob__', [{ fileName: name, blob: blob }]);
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
