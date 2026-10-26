import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.SHA256Digest;
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
    }
}
