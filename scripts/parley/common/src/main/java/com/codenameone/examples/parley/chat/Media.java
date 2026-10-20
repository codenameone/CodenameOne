/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.chat;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.security.Cipher;
import com.codename1.security.CryptoException;
import com.codename1.security.Hash;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.util.OnComplete;
import com.codenameone.examples.parley.api.MediaUploadDto;
import com.codenameone.examples.parley.crypto.Codec;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// Attachments: photos, videos, voice messages and files.
///
/// Each one is encrypted on the device with a key made for it alone, and only
/// the ciphertext goes to the server. The key, the ciphertext's SHA-256 and a
/// small thumbnail travel inside the message, so they are as private as the
/// message is; the server holds a blob it cannot read and cannot tell a photo
/// from a voice message by. A downloaded blob whose hash is not the one the
/// message promised is refused before it is decrypted.
public final class Media {
    /// The largest attachment the server takes, before encryption adds its
    /// 16-byte tag.
    public static final int MAX_BYTES = 16 * 1024 * 1024 - 16;

    private static final int THUMB = 160;

    private Media() {
    }

    /// Encrypts and uploads a file. `done` gets the attachment description
    /// for [Messenger#sendMedia].
    public static void upload(String path, final String mime, String name, long durationMillis,
            final Net.Ok<Map<String, Object>> done, Net.Failed failed) {
        byte[] plain;
        try {
            plain = read(path);
        } catch (IOException unreadable) {
            failed.failed(0, "That file could not be read.");
            return;
        }
        if (plain.length > MAX_BYTES) {
            failed.failed(413, "Attachments are limited to 16 MB.");
            return;
        }
        byte[] key = SecureRandom.bytes(32);
        byte[] nonce = SecureRandom.bytes(12);
        final byte[] sealed = Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", key), nonce,
                null, plain);
        final Map<String, Object> media = new HashMap<String, Object>();
        media.put("key", Codec.b64(key));
        media.put("iv", Codec.b64(nonce));
        media.put("digest", Codec.b64(Hash.sha256(sealed)));
        media.put("mime", mime);
        media.put("size", Long.valueOf(plain.length));
        media.put("name", name == null ? "" : name);
        if (durationMillis > 0) {
            media.put("duration", Long.valueOf(durationMillis));
        }
        if (mime.startsWith("image/")) {
            String thumb = thumbnail(plain);
            if (thumb != null) {
                media.put("thumb", thumb);
            }
        }
        // The server is told nothing about what it stores.
        Api.media().upload(new MediaUploadDto("application/octet-stream", Codec.b64(sealed)),
                Net.to(ref -> {
                    media.put("id", ref.id());
                    cache(ref.id(), sealed);
                    done.got(media);
                }, failed));
    }

    /// The decrypted bytes of an attachment, from the cache or the server.
    public static void bytes(final Map<String, Object> media, final OnComplete<byte[]> done,
            final Net.Failed failed) {
        final String id = Codec.str(media.get("id"));
        byte[] cached = cached(id);
        if (cached != null) {
            byte[] plain = open(media, cached);
            if (plain != null) {
                done.completed(plain);
                return;
            }
        }
        Api.media().download(id, Net.to(data -> {
            byte[] sealed = Codec.unb64(data.data());
            byte[] plain = sealed == null ? null : open(media, sealed);
            if (plain == null) {
                failed.failed(422, "That attachment was changed on the way, so it was not opened.");
                return;
            }
            cache(id, sealed);
            done.completed(plain);
        }, failed));
    }

    /// Decrypts an attachment into a temporary file, for the players that
    /// need a path. [#cleanUp()] deletes them.
    public static void file(final Map<String, Object> media, final OnComplete<String> done,
            Net.Failed failed) {
        bytes(media, plain -> {
            String dir = tempDir();
            String path = dir + Codec.str(media.get("id")) + extension(Codec.str(media.get("mime")));
            OutputStream out = null;
            try {
                out = FileSystemStorage.getInstance().openOutputStream(path);
                out.write(plain);
            } catch (IOException failedWrite) {
                Util.cleanup(out);
                return;
            }
            Util.cleanup(out);
            done.completed(path);
        }, failed);
    }

    /// Deletes the decrypted temporary files.
    public static void cleanUp() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = tempDir();
        try {
            String[] names = fs.listFiles(dir);
            for (int iter = 0; names != null && iter < names.length; iter++) {
                fs.delete(dir + names[iter]);
            }
        } catch (IOException none) {
            // Nothing to delete.
        }
    }

    public static Image thumbImage(Map<String, Object> media) {
        byte[] bytes = Codec.unb64(media.get("thumb"));
        return bytes == null ? null : EncodedImage.create(bytes);
    }

    private static String tempDir() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath() + "parley-open/";
        if (!fs.exists(dir)) {
            fs.mkdir(dir);
        }
        return dir;
    }

    private static String extension(String mime) {
        if (mime.indexOf("mp4") >= 0) {
            return ".mp4";
        }
        if (mime.indexOf("aac") >= 0 || mime.indexOf("m4a") >= 0) {
            return ".m4a";
        }
        if (mime.indexOf("wav") >= 0) {
            return ".wav";
        }
        if (mime.indexOf("ogg") >= 0) {
            return ".ogg";
        }
        if (mime.indexOf("jpeg") >= 0) {
            return ".jpg";
        }
        if (mime.indexOf("png") >= 0) {
            return ".png";
        }
        if (mime.indexOf("pdf") >= 0) {
            return ".pdf";
        }
        return ".bin";
    }

    private static byte[] open(Map<String, Object> media, byte[] sealed) {
        byte[] digest = Codec.unb64(media.get("digest"));
        byte[] key = Codec.unb64(media.get("key"));
        byte[] nonce = Codec.unb64(media.get("iv"));
        if (digest == null || key == null || nonce == null
                || !com.codename1.security.Hmac.constantTimeEquals(digest, Hash.sha256(sealed))) {
            return null;
        }
        try {
            return Cipher.aesDecrypt(Cipher.AES_GCM, new SecretKey("AES", key), nonce, null, sealed);
        } catch (CryptoException damaged) {
            return null;
        }
    }

    private static String thumbnail(byte[] plain) {
        try {
            Image full = Image.createImage(plain, 0, plain.length);
            int w = full.getWidth();
            int h = full.getHeight();
            if (w <= 0 || h <= 0) {
                return null;
            }
            float scale = Math.min(1f, THUMB / (float) Math.max(w, h));
            Image small = full.scaled(Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.codename1.ui.util.ImageIO io = com.codename1.ui.util.ImageIO.getImageIO();
            if (io == null) {
                return null;
            }
            io.save(small, out, com.codename1.ui.util.ImageIO.FORMAT_JPEG, 0.6f);
            return Codec.b64(out.toByteArray());
        } catch (IOException | RuntimeException notAnImage) {
            return null;
        }
    }

    private static byte[] read(String path) throws IOException {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Util.copy(in, out);
            return out.toByteArray();
        } finally {
            Util.cleanup(in);
        }
    }

    private static void cache(String id, byte[] sealed) {
        OutputStream out = null;
        try {
            out = Storage.getInstance().createOutputStream("pm-" + id);
            out.write(sealed);
        } catch (IOException full) {
            // Not cached; it is fetched again next time.
        } finally {
            Util.cleanup(out);
        }
    }

    private static byte[] cached(String id) {
        if (!Storage.getInstance().exists("pm-" + id)) {
            return null;
        }
        InputStream in = null;
        try {
            in = Storage.getInstance().createInputStream("pm-" + id);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Util.copy(in, out);
            return out.toByteArray();
        } catch (IOException gone) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }
}
