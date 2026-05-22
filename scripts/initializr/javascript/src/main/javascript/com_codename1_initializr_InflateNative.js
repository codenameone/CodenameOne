// JS-port native impl of com.codename1.initializr.InflateNative.
//
// Runs on the main thread (where the host bridge dispatches NativeInterface
// calls). Uses the browser's built-in DecompressionStream "deflate-raw"
// format to inflate raw deflate bytes from a zip Local File Header entry.
//
// Why bypass zipme's Inflater: on the JS port the Inflater state leaks
// between successive ZipInputStream entries -- the second getNextEntry +
// read throws "size mismatch: <small actualCSize>;<uSize> <-> <declaredCSize>;<uSize>"
// because the Inflater hits an END marker far earlier than the declared
// compressed size, leaving the stream desynced.  Routing decompression
// through DecompressionStream sidesteps the buggy class entirely.
(function(exports) {
  var o = {};

  function bytesToUint8Array(bytes) {
    if (bytes == null) return null;
    if (bytes.BYTES_PER_ELEMENT !== undefined && bytes.buffer) {
      return new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    }
    if (typeof bytes.length === 'number') {
      var arr = new Uint8Array(bytes.length);
      for (var i = 0; i < bytes.length; i++) {
        arr[i] = bytes[i] & 0xff;
      }
      return arr;
    }
    return null;
  }

  function uint8ArrayToReturnArray(u8) {
    // The host-bridge return path serialises a returned typed array as the
    // matching Java byte[] cleanly, so we hand the Uint8Array back directly.
    return u8;
  }

  o.inflateRaw_ = function(compressed, callback) {
    try {
      var src = bytesToUint8Array(compressed);
      if (src == null) {
        try { console.log('CN1INIT:inflate:err bad-input'); } catch (_le) {}
        callback.complete(null);
        return;
      }
      if (typeof DecompressionStream === 'undefined') {
        try { console.log('CN1INIT:inflate:err DecompressionStream-missing'); } catch (_le) {}
        callback.complete(null);
        return;
      }
      var blob = new Blob([src]);
      var stream = blob.stream().pipeThrough(new DecompressionStream('deflate-raw'));
      // Collect all output chunks then concatenate into a single Uint8Array.
      var reader = stream.getReader();
      var chunks = [];
      var totalLen = 0;
      function pump() {
        reader.read().then(function(result) {
          if (result.done) {
            var combined = new Uint8Array(totalLen);
            var off = 0;
            for (var i = 0; i < chunks.length; i++) {
              combined.set(chunks[i], off);
              off += chunks[i].length;
            }
            callback.complete(uint8ArrayToReturnArray(combined));
            return;
          }
          chunks.push(result.value);
          totalLen += result.value.length;
          pump();
        }).catch(function(err) {
          try { console.log('CN1INIT:inflate:err ' + (err && err.message ? err.message : String(err))); } catch (_le) {}
          callback.complete(null);
        });
      }
      pump();
    } catch (e) {
      try { console.log('CN1INIT:inflate:err ' + (e && e.message ? e.message : String(e))); } catch (_le) {}
      callback.complete(null);
    }
  };

  o.isSupported_ = function(callback) {
    callback.complete(typeof DecompressionStream !== 'undefined');
  };

  exports.com_codename1_initializr_InflateNative = o;

})(cn1_get_native_interfaces());
