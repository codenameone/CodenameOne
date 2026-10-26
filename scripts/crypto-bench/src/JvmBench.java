import com.codename1.security.Ed25519;
import com.codename1.security.Hash;
import com.codename1.security.Schnorr;
import com.codename1.security.Pbkdf2;
import com.codename1.security.Secp256k1;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import java.math.BigInteger;

/// Ops/sec of the core secp256k1 and hash kernels next to BouncyCastle on the same JVM.
/// Prints one line per operation: name, core ops/s, reference ops/s, ratio.
public final class JvmBench {
    interface Op { void run(int i); }

    static double opsPerSec(Op op, long millis) {
        long end = System.nanoTime() + millis * 1000000L;
        int n = 0;
        long start = System.nanoTime();
        while (System.nanoTime() < end) { op.run(n++); }
        return n / ((System.nanoTime() - start) / 1e9);
    }

    static double median(Op op) {
        opsPerSec(op, 1500);
        double[] r = new double[5];
        for (int i = 0; i < r.length; i++) r[i] = opsPerSec(op, 1000);
        java.util.Arrays.sort(r);
        return r[2];
    }

    static void row(String name, Op core, Op ref) {
        double c = median(core), b = median(ref);
        System.out.printf("%-22s core=%12.0f ref=%12.0f ratio=%6.2f%n", name, c, b, c / b);
    }

    public static void main(String[] a) {
        final byte[] priv = Hash.sha256("bench".getBytes());
        final byte[] hash = Hash.sha256("message".getBytes());
        final byte[] pub = Secp256k1.publicKey(priv, true);
        final Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash, priv);
        X9ECParameters p = CustomNamedCurves.getByName("secp256k1");
        final ECDomainParameters dom = new ECDomainParameters(p.getCurve(), p.getG(), p.getN(), p.getH());
        final ECPrivateKeyParameters bPriv = new ECPrivateKeyParameters(new BigInteger(1, priv), dom);
        final ECPublicKeyParameters bPub = new ECPublicKeyParameters(dom.getG().multiply(bPriv.getD()).normalize(), dom);
        final BigInteger r = new BigInteger(1, sig.r()), s = new BigInteger(1, sig.s());
        final byte[] kb = new byte[1024];
        final byte[] out = new byte[32];

        row("secp256k1.pubkey", i -> Secp256k1.publicKey(priv, true), i -> dom.getG().multiply(bPriv.getD()).normalize().getEncoded(true));
        row("secp256k1.sign", i -> Secp256k1.sign(hash, priv), i -> {
            ECDSASigner sg = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
            sg.init(true, bPriv); sg.generateSignature(hash); });
        row("secp256k1.verify", i -> Secp256k1.verify(hash, sig, pub), i -> {
            ECDSASigner sg = new ECDSASigner(); sg.init(false, bPub); sg.verifySignature(hash, r, s); });
        final byte[] pubU = Secp256k1.publicKey(priv, false);
        row("secp256k1.verify.u", i -> Secp256k1.verify(hash, sig, pubU), i -> {
            ECDSASigner sg = new ECDSASigner(); sg.init(false, bPub); sg.verifySignature(hash, r, s); });
        row("secp256k1.verify.dec", i -> Secp256k1.verify(hash, sig, pub), i -> {
            ECDSASigner sg = new ECDSASigner(); sg.init(false, new ECPublicKeyParameters(dom.getCurve().decodePoint(pub), dom)); sg.verifySignature(hash, r, s); });
        row("keccak256.1k", i -> Hash.keccak256(kb), i -> {
            KeccakDigest d = new KeccakDigest(256); d.update(kb, 0, kb.length); d.doFinal(out, 0); });
        row("sha256.1k", i -> Hash.sha256(kb), i -> {
            SHA256Digest d = new SHA256Digest(); d.update(kb, 0, kb.length); d.doFinal(out, 0); });
        byte[] mn = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about".getBytes();
        byte[] salt = "mnemonic".getBytes();
        row("pbkdf2.sha512.bip39", i -> Pbkdf2.hmacSha512(mn, salt, 2048, 64), i -> {
            PKCS5S2ParametersGenerator g = new PKCS5S2ParametersGenerator(new SHA512Digest());
            g.init(mn, salt, 2048); g.generateDerivedParameters(512); });
        row("ripemd160.1k", i -> Hash.ripemd160(kb), i -> {
            RIPEMD160Digest d = new RIPEMD160Digest(); d.update(kb, 0, kb.length); d.doFinal(out, 0); });

        final byte[] seed = Hash.sha256("ed25519".getBytes());
        final byte[] edPub = Ed25519.publicKey(seed);
        final byte[] msg = hash;
        final byte[] edSig = Ed25519.sign(seed, msg);
        final Ed25519PrivateKeyParameters bEdPriv = new Ed25519PrivateKeyParameters(seed, 0);
        final Ed25519PublicKeyParameters bEdPub = bEdPriv.generatePublicKey();
        if (!java.util.Arrays.equals(edPub, bEdPub.getEncoded())) {
            throw new IllegalStateException("Ed25519 public key differs from BouncyCastle");
        }
        row("ed25519.pubkey", i -> Ed25519.publicKey(seed), i -> new Ed25519PrivateKeyParameters(seed, 0).generatePublicKey());
        row("ed25519.sign", i -> Ed25519.sign(seed, edPub, msg), i -> {
            Ed25519Signer sg = new Ed25519Signer(); sg.init(true, bEdPriv); sg.update(msg, 0, msg.length); sg.generateSignature(); });
        row("ed25519.verify", i -> Ed25519.verify(msg, edSig, edPub), i -> {
            Ed25519Signer sg = new Ed25519Signer(); sg.init(false, new Ed25519PublicKeyParameters(edPub, 0));
            sg.update(msg, 0, msg.length); sg.verifySignature(edSig); });

        jdkEd25519(seed, edPub, msg, edSig);

        // BouncyCastle has no BIP-340; the reference is BC's ECDSA on the same curve,
        // which costs about the same multiplications
        final byte[] xonly = Schnorr.publicKey(priv);
        final byte[] schSig = Schnorr.sign(msg, priv, null);
        row("schnorr.sign", i -> Schnorr.sign(msg, priv, null), i -> {
            ECDSASigner sg = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
            sg.init(true, bPriv); sg.generateSignature(hash); });
        row("schnorr.verify", i -> Schnorr.verify(msg, schSig, xonly), i -> {
            ECDSASigner sg = new ECDSASigner(); sg.init(false, bPub); sg.verifySignature(hash, r, s); });
    }

    /// The JDK's own EdDSA (Java 15 and later), the other reference a JVM
    /// developer reaches for; skipped on an older runtime.
    static void jdkEd25519(byte[] seed, byte[] edPub, byte[] msg, byte[] edSig) {
        final java.security.PrivateKey jPriv;
        final java.security.PublicKey jPub;
        try {
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("Ed25519");
            // PKCS#8 and X.509 wrappings of the raw keys (RFC 8410)
            byte[] p8 = concat(Hash.fromHex("302e020100300506032b657004220420"), seed);
            byte[] x509 = concat(Hash.fromHex("302a300506032b6570032100"), edPub);
            jPriv = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(p8));
            jPub = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(x509));
        } catch (java.security.GeneralSecurityException noEdDsa) {
            System.out.println("# the JDK has no Ed25519; its rows are skipped");
            return;
        }
        row("ed25519.sign.jdk", i -> Ed25519.sign(seed, edPub, msg), i -> {
            try {
                java.security.Signature sg = java.security.Signature.getInstance("Ed25519");
                sg.initSign(jPriv); sg.update(msg); sg.sign();
            } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); } });
        row("ed25519.verify.jdk", i -> Ed25519.verify(msg, edSig, edPub), i -> {
            try {
                java.security.Signature sg = java.security.Signature.getInstance("Ed25519");
                sg.initVerify(jPub); sg.update(msg); sg.verify(edSig);
            } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); } });
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
