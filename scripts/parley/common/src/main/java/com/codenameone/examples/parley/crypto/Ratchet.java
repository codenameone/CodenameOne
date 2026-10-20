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
package com.codenameone.examples.parley.crypto;

import com.codename1.security.Cipher;
import com.codename1.security.CryptoException;
import com.codename1.security.Hkdf;
import com.codename1.security.Hmac;
import com.codename1.security.KeyAgreement;
import com.codename1.security.KeyGenerator;
import com.codename1.security.KeyPair;
import com.codename1.security.PrivateKey;
import com.codename1.security.PublicKey;
import com.codename1.security.SecretKey;
import com.codename1.security.Signature;
import com.codenameone.examples.parley.api.PreKeyBundleDto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/// One end-to-end session with one device of one person: X3DH to start it,
/// then the Double Ratchet for every message after.
///
/// This follows Signal's two published specifications over the primitives
/// Codename One has on every port -- P-256 for the Diffie-Hellman steps where
/// Signal uses X25519, HKDF-SHA-256 for the key chains and AES-256-GCM for the
/// messages:
///
/// - [X3DH](https://signal.org/docs/specifications/x3dh/): the sender
///   combines its identity key and a fresh ephemeral key with the receiver's
///   identity key, signed prekey and (when one is left) a one-time prekey,
///   so the receiver can be offline and the first message is still forward
///   secret.
/// - [Double Ratchet](https://signal.org/docs/specifications/doubleratchet/):
///   every message gets its own key from a hash chain, and every change of
///   speaker adds a Diffie-Hellman step, so a key stolen today opens neither
///   yesterday's messages nor, after the next reply, tomorrow's.
///
/// The state is a plain object that [#toMap()] and [#fromMap(Map)] turn into
/// a document for the [Vault]. Decryption works on a copy and the caller keeps
/// it only when the message opened, so a forged or damaged message can never
/// advance a session.
public final class Ratchet {
    private static final byte[] X3DH_INFO = Codec.utf8("Parley X3DH");
    private static final byte[] ROOT_INFO = Codec.utf8("Parley ratchet");
    private static final byte[] MESSAGE_INFO = Codec.utf8("Parley message");
    private static final byte[] CHAIN_MESSAGE = {1};
    private static final byte[] CHAIN_NEXT = {2};

    /// The most message keys one header may make the receiver skip ahead,
    /// and the most it keeps for messages that are still on their way.
    static final int MAX_SKIP = 1000;
    static final int MAX_KEPT = 2000;

    private byte[] rootKey;
    private byte[] sendChain;
    private byte[] receiveChain;
    private byte[] myRatchetPrivate;
    private byte[] myRatchetPublic;
    private byte[] theirRatchet;
    private long sent;
    private long received;
    private long previous;
    private byte[] associated;
    private byte[] theirIdentity;
    /// The X3DH fields an initiator repeats on every message until the other
    /// side answers, so whichever message arrives first can open the session.
    private Map<String, Object> pending;
    private final Map<String, String> skipped = new LinkedHashMap<String, String>();

    private Ratchet() {
    }

    /// The other device's identity key, for safety numbers.
    public byte[] theirIdentity() {
        return theirIdentity;
    }

    /// Starts a session as the sender, from the receiver's prekey bundle.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: when the bundle's signed prekey is not signed by
    ///   its identity key -- a server handing out keys that are not that
    ///   device's
    public static Ratchet initiate(PreKeyBundleDto bundle) {
        byte[] identity = Codec.unb64(bundle.identityKey());
        byte[] signedKey = Codec.unb64(bundle.signedPreKey().publicKey());
        byte[] signature = Codec.unb64(bundle.signedPreKey().signature());
        if (identity == null || signedKey == null || signature == null
                || !Signature.verify(Signature.SHA256_WITH_ECDSA, pub(identity), signedKey,
                        signature)) {
            throw new CryptoException("The signed prekey of " + bundle.username()
                    + " is not signed by its identity key");
        }
        byte[] oneTime = bundle.oneTimePreKey() == null ? null
                : Codec.unb64(bundle.oneTimePreKey().publicKey());

        KeyPair ephemeral = KeyGenerator.ec(256);
        PrivateKey me = Identity.privateKey();
        byte[] dh1 = KeyAgreement.ecdh(me, pub(signedKey));
        byte[] dh2 = KeyAgreement.ecdh(ephemeral.getPrivateKey(), pub(identity));
        byte[] dh3 = KeyAgreement.ecdh(ephemeral.getPrivateKey(), pub(signedKey));
        byte[] secret = x3dh(dh1, dh2, dh3,
                oneTime == null ? null : KeyAgreement.ecdh(ephemeral.getPrivateKey(), pub(oneTime)));

        Ratchet session = new Ratchet();
        session.theirIdentity = identity;
        session.associated = Codec.concat(Identity.publicKey(), identity);
        session.theirRatchet = signedKey;
        session.newRatchetKey();
        byte[][] root = rootStep(secret, KeyAgreement.ecdh(session.myPrivate(), pub(signedKey)));
        session.rootKey = root[0];
        session.sendChain = root[1];

        Map<String, Object> x3dh = new HashMap<String, Object>();
        x3dh.put("ik", Codec.b64(Identity.publicKey()));
        x3dh.put("ek", Codec.b64(ephemeral.getPublicKey().getEncoded()));
        x3dh.put("spk", Long.valueOf(bundle.signedPreKey().id()));
        if (oneTime != null) {
            x3dh.put("opk", Long.valueOf(bundle.oneTimePreKey().id()));
        }
        session.pending = x3dh;
        return session;
    }

    /// Whether a header starts a session: the first messages from a device
    /// that just fetched this one's prekey bundle.
    public static boolean starts(Map<String, Object> header) {
        return header != null && header.get("ek") != null;
    }

    /// The one-time prekey a session-starting header names, or -1.
    public static long oneTimePreKey(Map<String, Object> header) {
        return header == null || header.get("opk") == null ? -1 : Codec.num(header.get("opk"));
    }

    /// The ephemeral key that started the session a header belongs to; a
    /// header that names a different one than the session holds means the
    /// other device started over (reinstalled, restored).
    public static String ephemeral(Map<String, Object> header) {
        return header == null ? "" : Codec.str(header.get("ek"));
    }

    /// Starts a session as the receiver, from the first message's header.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: when the header names prekeys this device no
    ///   longer has (a one-time prekey is used once)
    public static Ratchet respond(Map<String, Object> header) {
        byte[] identity = Codec.unb64(header.get("ik"));
        byte[] ephemeral = Codec.unb64(header.get("ek"));
        PrivateKey signed = Identity.signedPreKey(Codec.num(header.get("spk")));
        if (identity == null || ephemeral == null || signed == null) {
            throw new CryptoException("That message names a signed prekey this device does not have");
        }
        PrivateKey oneTime = null;
        if (header.get("opk") != null) {
            // Read, not removed: Sessions spends it once the message opened.
            oneTime = Identity.preKey(Codec.num(header.get("opk")));
            if (oneTime == null) {
                throw new CryptoException("That message uses a one-time prekey that was already used");
            }
        }
        byte[] dh1 = KeyAgreement.ecdh(signed, pub(identity));
        byte[] dh2 = KeyAgreement.ecdh(Identity.privateKey(), pub(ephemeral));
        byte[] dh3 = KeyAgreement.ecdh(signed, pub(ephemeral));
        byte[] secret = x3dh(dh1, dh2, dh3,
                oneTime == null ? null : KeyAgreement.ecdh(oneTime, pub(ephemeral)));

        Ratchet session = new Ratchet();
        session.theirIdentity = identity;
        session.associated = Codec.concat(identity, Identity.publicKey());
        session.rootKey = secret;
        session.myRatchetPrivate = signed.getEncoded();
        session.myRatchetPublic = Identity.signedPreKeyPublic();
        session.ephemeralSeen = Codec.b64(ephemeral);
        return session;
    }

    /// The ephemeral key of the X3DH that opened this session, receiver side.
    private String ephemeralSeen = "";

    public String startedBy() {
        return ephemeralSeen;
    }

    /// Encrypts one message. Answers the header (JSON, sent in the clear but
    /// authenticated) and the ciphertext.
    public String[] encrypt(byte[] plaintext) {
        byte[] messageKey = chainKey(sendChain, CHAIN_MESSAGE);
        sendChain = chainKey(sendChain, CHAIN_NEXT);
        Map<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("v", Long.valueOf(1));
        header.put("dh", Codec.b64(myRatchetPublic));
        header.put("pn", Long.valueOf(previous));
        header.put("n", Long.valueOf(sent));
        if (pending != null) {
            header.putAll(pending);
        }
        sent++;
        String headerText = Codec.json(header);
        return new String[] {headerText, Codec.b64(seal(messageKey, headerText, plaintext))};
    }

    /// Decrypts one message into this session, which the caller then keeps.
    /// Call it on a [#copy()] and keep the copy only when this returns.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: when the message does not open: forged, damaged,
    ///   replayed, or from a session this one is not
    public byte[] decrypt(Map<String, Object> header, String headerText, byte[] ciphertext) {
        byte[] dh = Codec.unb64(header.get("dh"));
        long n = Codec.num(header.get("n"));
        long pn = Codec.num(header.get("pn"));
        if (dh == null || n < 0 || pn < 0) {
            throw new CryptoException("That message has no ratchet header");
        }
        String skippedId = Codec.b64(dh) + ":" + n;
        String kept = skipped.remove(skippedId);
        if (kept != null) {
            return open(Codec.unb64(kept), headerText, ciphertext);
        }
        if (theirRatchet == null || !Codec.b64(dh).equals(Codec.b64(theirRatchet))) {
            if (receiveChain != null) {
                skip(pn);
            }
            dhStep(dh);
        }
        skip(n);
        byte[] messageKey = chainKey(receiveChain, CHAIN_MESSAGE);
        receiveChain = chainKey(receiveChain, CHAIN_NEXT);
        received++;
        // An answer means the other side has the session: stop repeating the
        // X3DH fields.
        pending = null;
        return open(messageKey, headerText, ciphertext);
    }

    /// A deep copy, made through the stored form.
    public Ratchet copy() {
        return fromMap(toMap());
    }

    private void dhStep(byte[] theirs) {
        previous = sent;
        sent = 0;
        received = 0;
        theirRatchet = theirs;
        byte[][] root = rootStep(rootKey, KeyAgreement.ecdh(myPrivate(), pub(theirs)));
        rootKey = root[0];
        receiveChain = root[1];
        newRatchetKey();
        root = rootStep(rootKey, KeyAgreement.ecdh(myPrivate(), pub(theirs)));
        rootKey = root[0];
        sendChain = root[1];
    }

    private void skip(long until) {
        if (receiveChain == null) {
            return;
        }
        if (until - received > MAX_SKIP) {
            throw new CryptoException("That message claims too many messages were lost");
        }
        while (received < until) {
            byte[] messageKey = chainKey(receiveChain, CHAIN_MESSAGE);
            receiveChain = chainKey(receiveChain, CHAIN_NEXT);
            skipped.put(Codec.b64(theirRatchet) + ":" + received, Codec.b64(messageKey));
            received++;
        }
        Iterator<String> oldest = skipped.keySet().iterator();
        while (skipped.size() > MAX_KEPT && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    private void newRatchetKey() {
        KeyPair pair = KeyGenerator.ec(256);
        myRatchetPrivate = pair.getPrivateKey().getEncoded();
        myRatchetPublic = pair.getPublicKey().getEncoded();
    }

    private PrivateKey myPrivate() {
        return PrivateKey.fromPkcs8(PublicKey.EC, myRatchetPrivate);
    }

    private byte[] seal(byte[] messageKey, String headerText, byte[] plaintext) {
        byte[] material = Hkdf.derive("SHA-256", null, messageKey, MESSAGE_INFO, 44);
        return Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", Codec.slice(material, 0, 32)),
                Codec.slice(material, 32, 12), Codec.concat(associated, Codec.utf8(headerText)),
                plaintext);
    }

    private byte[] open(byte[] messageKey, String headerText, byte[] ciphertext) {
        byte[] material = Hkdf.derive("SHA-256", null, messageKey, MESSAGE_INFO, 44);
        return Cipher.aesDecrypt(Cipher.AES_GCM, new SecretKey("AES", Codec.slice(material, 0, 32)),
                Codec.slice(material, 32, 12), Codec.concat(associated, Codec.utf8(headerText)),
                ciphertext);
    }

    private static byte[] x3dh(byte[] dh1, byte[] dh2, byte[] dh3, byte[] dh4) {
        byte[] pad = new byte[32];
        for (int iter = 0; iter < pad.length; iter++) {
            pad[iter] = (byte) 0xff;
        }
        byte[] input = dh4 == null ? Codec.concat(pad, dh1, dh2, dh3)
                : Codec.concat(pad, dh1, dh2, dh3, dh4);
        return Hkdf.derive("SHA-256", new byte[32], input, X3DH_INFO, 32);
    }

    private static byte[][] rootStep(byte[] root, byte[] dhOut) {
        byte[] out = Hkdf.derive("SHA-256", root, dhOut, ROOT_INFO, 64);
        return new byte[][] {Codec.slice(out, 0, 32), Codec.slice(out, 32, 32)};
    }

    private static byte[] chainKey(byte[] chain, byte[] which) {
        if (chain == null) {
            throw new CryptoException("This session cannot send or receive yet");
        }
        return Hmac.sha256(chain, which);
    }

    private static PublicKey pub(byte[] spki) {
        return PublicKey.fromX509(PublicKey.EC, spki);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        put(out, "rk", rootKey);
        put(out, "cks", sendChain);
        put(out, "ckr", receiveChain);
        put(out, "dhsPriv", myRatchetPrivate);
        put(out, "dhsPub", myRatchetPublic);
        put(out, "dhr", theirRatchet);
        put(out, "ad", associated);
        put(out, "theirs", theirIdentity);
        out.put("ns", Long.valueOf(sent));
        out.put("nr", Long.valueOf(received));
        out.put("pn", Long.valueOf(previous));
        out.put("seen", ephemeralSeen);
        if (pending != null) {
            out.put("pending", new HashMap<String, Object>(pending));
        }
        out.put("skipped", new LinkedHashMap<String, Object>(skipped));
        return out;
    }

    @SuppressWarnings("unchecked")
    public static Ratchet fromMap(Map<String, Object> in) {
        Ratchet session = new Ratchet();
        session.rootKey = Codec.unb64(in.get("rk"));
        session.sendChain = Codec.unb64(in.get("cks"));
        session.receiveChain = Codec.unb64(in.get("ckr"));
        session.myRatchetPrivate = Codec.unb64(in.get("dhsPriv"));
        session.myRatchetPublic = Codec.unb64(in.get("dhsPub"));
        session.theirRatchet = Codec.unb64(in.get("dhr"));
        session.associated = Codec.unb64(in.get("ad"));
        session.theirIdentity = Codec.unb64(in.get("theirs"));
        session.sent = Codec.num(in.get("ns"));
        session.received = Codec.num(in.get("nr"));
        session.previous = Codec.num(in.get("pn"));
        session.ephemeralSeen = Codec.str(in.get("seen"));
        Object pending = in.get("pending");
        if (pending instanceof Map) {
            session.pending = new HashMap<String, Object>((Map<String, Object>) pending);
        }
        Object skipped = in.get("skipped");
        if (skipped instanceof Map) {
            for (Map.Entry<String, Object> each : ((Map<String, Object>) skipped).entrySet()) {
                session.skipped.put(each.getKey(), Codec.str(each.getValue()));
            }
        }
        return session;
    }

    private static void put(Map<String, Object> out, String name, byte[] value) {
        if (value != null) {
            out.put(name, Codec.b64(value));
        }
    }
}
