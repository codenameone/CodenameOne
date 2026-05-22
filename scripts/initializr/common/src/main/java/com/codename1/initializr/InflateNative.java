package com.codename1.initializr;

import com.codename1.system.NativeInterface;

/**
 * Platform hook for raw-deflate decompression. The JavaScript port needs this
 * because the bundled net.sf.zipme Inflater leaks state between entries on
 * that port (ZipInputStream's second ``getNextEntry`` + read throws
 * ``size mismatch: actualCSize;uSize <-> declaredCSize;uSize`` because the
 * Inflater hits an END-of-block marker much earlier than the declared
 * compressed size, leaving the stream desynced). On platforms where
 * ZipInputStream works the normal way, this interface stays unsupported and
 * GeneratorModel falls back to its existing zipme code path.
 *
 * The JS-port impl uses the browser's built-in DecompressionStream
 * ("deflate-raw" format) to do the actual decompression, which sidesteps
 * the broken zipme code entirely. Java code does the zip framing (local file
 * header parsing) itself; only the deflate inflate step routes through the
 * native call.
 */
public interface InflateNative extends NativeInterface {
    /**
     * Inflate raw deflate bytes (no zlib header / no gzip wrapper -- exactly
     * what's between a zip Local File Header and the next entry for
     * compression method 8). Returns null on error.
     */
    byte[] inflateRaw(byte[] compressed);
}
