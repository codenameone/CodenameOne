/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

import com.codename1.impl.html5.JavaScriptPortBootstrap;
import com.codename1.security.CryptoBatch;
import com.codename1.security.Ed25519;
import com.codename1.security.Schnorr;
import com.codename1.security.Secp256k1;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

/// Runs one batch of each verify kernel and both hashes through the asynchronous CryptoBatch
/// forms, which go to WebGPU in a browser that has it, and the same batches through the
/// synchronous forms with the GPU switched off. Prints one `CN1-CRYPTO` line per kernel to the
/// console, with both answers' agreement and both rates, and `CN1-CRYPTO-DONE` at the end;
/// `scripts/crypto-gpu/js-batch-check.mjs` reads them.
public class JavaScriptCryptoBatchApp extends Lifecycle {
    // distinct signatures; a batch repeats them, which costs the GPU exactly as much
    private static final int DISTINCT = 64;

    private Label status;

    public static void main(String[] args) {
        JavaScriptPortBootstrap.bootstrap(new JavaScriptCryptoBatchApp());
    }

    @Override
    public void runApp() {
        Form form = new Form("CryptoBatch", BoxLayout.y());
        status = new Label("running");
        form.add(status);
        form.show();
        String query = com.codename1.ui.Display.getInstance().getProperty("browser.window.location.search", "");
        int at = query.indexOf("n=");
        final int n = at < 0 ? 4096 : Integer.parseInt(query.substring(at + 2));
        new Thread(new Runnable() {
            public void run() {
                try {
                    runAll(n < 256 ? 4096 : n);
                } catch (RuntimeException e) {
                    System.out.println("CN1-CRYPTO-ERROR " + e);
                }
                System.out.println("CN1-CRYPTO-DONE");
            }
        }).start();
    }

    private static byte[] bytes(int seed, int length) {
        byte[] out = new byte[length];
        int x = seed * 0x9E3779B1 + 0x7F4A7C15;
        for (int i = 0; i < length; i++) {
            x ^= x << 13;
            x ^= x >>> 17;
            x ^= x << 5;
            out[i] = (byte) x;
        }
        return out;
    }

    private void runAll(int n) {
        byte[] hashes = new byte[32 * n];
        byte[] ecdsaSigs = new byte[64 * n];
        byte[] ecdsaKeys = new byte[33 * n];
        byte[] msgs = new byte[32 * n];
        byte[] schSigs = new byte[64 * n];
        byte[] schKeys = new byte[32 * n];
        byte[] edSigs = new byte[64 * n];
        byte[] edKeys = new byte[32 * n];
        for (int d = 0; d < DISTINCT; d++) {
            byte[] priv = bytes(d + 1, 32);
            byte[] h = bytes(d + 100, 32);
            byte[] sig = Secp256k1.sign(h, priv).toCompact();
            byte[] key = Secp256k1.publicKey(priv, true);
            byte[] ss = Schnorr.sign(h, priv, null);
            byte[] sk = Schnorr.publicKey(priv);
            byte[] ek = Ed25519.publicKey(priv);
            byte[] es = Ed25519.sign(priv, ek, h);
            if (d % 7 == 3) {
                // one in seven broken by a bit, so a GPU that answered "valid" to everything fails
                sig[5] ^= 1;
                ss[40] ^= 2;
                es[9] ^= 4;
            }
            for (int i = d; i < n; i += DISTINCT) {
                System.arraycopy(h, 0, hashes, 32 * i, 32);
                System.arraycopy(sig, 0, ecdsaSigs, 64 * i, 64);
                System.arraycopy(key, 0, ecdsaKeys, 33 * i, 33);
                System.arraycopy(h, 0, msgs, 32 * i, 32);
                System.arraycopy(ss, 0, schSigs, 64 * i, 64);
                System.arraycopy(sk, 0, schKeys, 32 * i, 32);
                System.arraycopy(es, 0, edSigs, 64 * i, 64);
                System.arraycopy(ek, 0, edKeys, 32 * i, 32);
            }
        }
        final byte[][] inputs = {hashes, ecdsaSigs, ecdsaKeys, msgs, schSigs, schKeys, edSigs, edKeys};
        for (int kernel = 0; kernel < 3; kernel++) {
            byte[] gpu = new byte[n];
            byte[] cpu = new byte[n];
            long t0 = System.currentTimeMillis();
            await(kernel, inputs, gpu, n, true);
            long t1 = System.currentTimeMillis();
            // the CPU arm on the portable engine, a tenth of the batch: it is a hundred times slower
            int m = Math.max(DISTINCT, n / 10);
            await(kernel, inputs, cpu, m, false);
            long t2 = System.currentTimeMillis();
            int agree = 0;
            int valid = 0;
            for (int i = 0; i < m; i++) {
                agree += gpu[i] == cpu[i] ? 1 : 0;
            }
            for (int i = 0; i < n; i++) {
                valid += gpu[i];
            }
            System.out.println("CN1-CRYPTO " + (kernel == 0 ? "ecdsa" : kernel == 1 ? "schnorr" : "ed25519")
                    + " n=" + n + " agree=" + agree + "/" + m + " valid=" + valid
                    + " gpuPerSec=" + rate(n, t1 - t0) + " cpuPerSec=" + rate(m, t2 - t1));
            status.setText("kernel " + kernel + " done");
        }
        int len = 64;
        int count = (2 << 20) / len;
        byte[] in = bytes(5, len * count);
        for (int k = 0; k < 2; k++) {
            byte[] gpu = new byte[32 * count];
            byte[] cpu = new byte[32 * count];
            long t0 = System.currentTimeMillis();
            hashAwait(k, in, len, count, gpu, true);
            long t1 = System.currentTimeMillis();
            hashAwait(k, in, len, count, cpu, false);
            long t2 = System.currentTimeMillis();
            boolean same = java.util.Arrays.equals(gpu, cpu);
            System.out.println("CN1-CRYPTO " + (k == 0 ? "sha256" : "keccak256") + " n=" + count + " agree="
                    + (same ? count : 0) + "/" + count + " gpuMBps=" + rate(len * count / 1000, t1 - t0) / 1000.0
                    + " cpuMBps=" + rate(len * count / 1000, t2 - t1) / 1000.0);
        }
    }

    private static long rate(long n, long ms) {
        return ms <= 0 ? n * 1000 : n * 1000 / ms;
    }

    // one batch through the asynchronous form (onGpu) or the synchronous one on the portable
    // engine, waiting here for the answer
    private void await(int kernel, byte[][] in, byte[] results, int n, boolean onGpu) {
        com.codename1.security.CryptoEngine engine = com.codename1.security.CryptoEngine.get();
        if (!onGpu) {
            com.codename1.security.CryptoEngine.install(null);
            try {
                if (kernel == 0) {
                    CryptoBatch.verifySecp256k1(in[0], in[1], in[2], 33, results, n);
                } else if (kernel == 1) {
                    CryptoBatch.verifySchnorr(in[3], 32, in[4], in[5], results, n);
                } else {
                    CryptoBatch.verifyEd25519(in[3], 32, in[6], in[7], results, n);
                }
            } finally {
                com.codename1.security.CryptoEngine.install(engine);
            }
            return;
        }
        Done done = new Done();
        if (kernel == 0) {
            CryptoBatch.verifySecp256k1Async(in[0], in[1], in[2], 33, results, n, done);
        } else if (kernel == 1) {
            CryptoBatch.verifySchnorrAsync(in[3], 32, in[4], in[5], results, n, done);
        } else {
            CryptoBatch.verifyEd25519Async(in[3], 32, in[6], in[7], results, n, done);
        }
        done.await();
    }

    private void hashAwait(int k, byte[] in, int len, int count, byte[] digests, boolean onGpu) {
        com.codename1.security.CryptoEngine engine = com.codename1.security.CryptoEngine.get();
        if (!onGpu) {
            com.codename1.security.CryptoEngine.install(null);
            try {
                if (k == 0) {
                    CryptoBatch.sha256(in, len, count, digests);
                } else {
                    CryptoBatch.keccak256(in, len, count, digests);
                }
            } finally {
                com.codename1.security.CryptoEngine.install(engine);
            }
            return;
        }
        Done done = new Done();
        if (k == 0) {
            CryptoBatch.sha256Async(in, len, count, digests, done);
        } else {
            CryptoBatch.keccak256Async(in, len, count, digests, done);
        }
        done.await();
    }

    // the callback runs on the EDT; this thread waits for it
    private static final class Done implements CryptoBatch.Callback<byte[]> {
        private boolean finished;

        public synchronized void done(byte[] result, Throwable error) {
            if (error != null) {
                System.out.println("CN1-CRYPTO-ERROR " + error);
            }
            finished = true;
            notifyAll();
        }

        synchronized void await() {
            while (!finished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
