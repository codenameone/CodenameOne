/*
 * Native crypto benchmark: the kernels ParparVM links into an application
 * (vm/ByteCodeTranslator/src/cn1_crypto_kernels.c, called through its Java
 * entry points exactly as translated code calls them) next to the reference
 * libraries on the same machine, in the same process.
 *
 *   secp256k1 / Schnorr  vs  upstream libsecp256k1 (its own default build)
 *   Ed25519              vs  OpenSSL EVP, and libsodium when HAVE_SODIUM
 *   Keccak-256           vs  OpenSSL EVP "KECCAK-256" (OpenSSL 3.2+)
 *
 * Prints one line per operation: name core=<ops/s> ref=<ops/s> ratio=<core/ref>,
 * the same format as JvmBench, so one script gates both.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <secp256k1.h>
#include <secp256k1_schnorrsig.h>
#include <secp256k1_extrakeys.h>
#include <openssl/evp.h>
#include <openssl/hmac.h>
#ifdef HAVE_SODIUM
#include <sodium.h>
#endif

#define K(name) com_codename1_security_NativeCryptoKernels_##name
JAVA_BOOLEAN K(available___R_boolean)(CODENAME_ONE_THREAD_STATE);
JAVA_BOOLEAN K(secpPublicKey___byte_1ARRAY_byte_1ARRAY_R_boolean)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT);
JAVA_BOOLEAN K(secpSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT);
JAVA_INT K(secpVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT);
JAVA_BOOLEAN K(secpRecover___byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY_R_boolean)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_INT, JAVA_OBJECT);
JAVA_VOID K(ed25519PublicKey___byte_1ARRAY_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT);
JAVA_VOID K(ed25519Sign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT);
JAVA_INT K(ed25519Verify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT);
JAVA_VOID K(keccak256___byte_1ARRAY_int_int_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_INT, JAVA_INT, JAVA_OBJECT);
JAVA_VOID K(sha256___byte_1ARRAY_int_int_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_INT, JAVA_INT, JAVA_OBJECT);
JAVA_VOID K(sha512___byte_1ARRAY_int_int_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_INT, JAVA_INT, JAVA_OBJECT);
JAVA_VOID K(ripemd160___byte_1ARRAY_int_int_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_INT, JAVA_INT, JAVA_OBJECT);
JAVA_VOID K(hmac___int_byte_1ARRAY_byte_1ARRAY_int_int_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_INT, JAVA_OBJECT, JAVA_OBJECT, JAVA_INT, JAVA_INT, JAVA_OBJECT);
JAVA_VOID K(pbkdf2___int_byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY)(CODENAME_ONE_THREAD_STATE, JAVA_INT, JAVA_OBJECT, JAVA_OBJECT, JAVA_INT, JAVA_OBJECT);
JAVA_BOOLEAN K(schnorrSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT);
JAVA_INT K(schnorrVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT, JAVA_OBJECT);

typedef struct { void *data; int length; } Arr;
static Arr mk(void *d, int n) { Arr a; a.data = d; a.length = n; return a; }

static double now(void) {
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec + t.tv_nsec * 1e-9;
}

static volatile int sink;

typedef void (*op_fn)(void);

static double ops_per_sec(op_fn f, double seconds) {
    double t0 = now(), t;
    long n = 0;
    do {
        for (int i = 0; i < 50; i++) {
            f();
        }
        n += 50;
        t = now() - t0;
    } while (t < seconds);
    return n / t;
}

static int cmp(const void *a, const void *b) {
    double x = *(const double *) a, y = *(const double *) b;
    return x < y ? -1 : x > y;
}

/*
 * Warm both, then time them in alternation and take each one's median of
 * five, so frequency scaling and thermal drift fall on both sides alike.
 */
static void row(const char *name, op_fn core, op_fn ref) {
    double c[5], b[5];
    ops_per_sec(core, 0.4);
    ops_per_sec(ref, 0.4);
    for (int i = 0; i < 5; i++) {
        c[i] = ops_per_sec(core, 0.5);
        b[i] = ops_per_sec(ref, 0.5);
    }
    qsort(c, 5, sizeof(double), cmp);
    qsort(b, 5, sizeof(double), cmp);
    printf("%-22s core=%12.0f ref=%12.0f ratio=%6.2f\n", name, c[2], b[2], c[2] / b[2]);
    fflush(stdout);
}

static unsigned char priv[32], hash[32], pub33[33], sig65[65], msg32[32], seed[32], epub[32], esig[64],
        kb[1024], kout[32], xonly[32], ssig[64], aux[32], rec[65];
static Arr aPriv, aHash, aPub, aSig, aSig64, aSeed, aEpub, aEsig, aMsg, aKb, aKout, aXonly, aSsig, aAux, aRec;

static secp256k1_context *ref_ctx;
static secp256k1_pubkey ref_pub;
static secp256k1_ecdsa_signature ref_sig;
static secp256k1_keypair ref_kp;
static secp256k1_xonly_pubkey ref_xonly;
static EVP_PKEY *ossl_ed;
static EVP_MD *ossl_keccak;

static void c_pub(void) { sink += K(secpPublicKey___byte_1ARRAY_byte_1ARRAY_R_boolean)(0, &aPriv, &aPub); }
static void r_pub(void) {
    secp256k1_pubkey p;
    size_t len = 33;
    sink += secp256k1_ec_pubkey_create(ref_ctx, &p, priv);
    secp256k1_ec_pubkey_serialize(ref_ctx, pub33, &len, &p, SECP256K1_EC_COMPRESSED);
}
static void c_sign(void) { sink += K(secpSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean)(0, &aHash, &aPriv, &aSig); }
static void r_sign(void) {
    secp256k1_ecdsa_signature s;
    sink += secp256k1_ecdsa_sign(ref_ctx, &s, hash, priv, NULL, NULL);
    secp256k1_ecdsa_signature_serialize_compact(ref_ctx, sig65, &s);
}
static void c_verify(void) { sink += K(secpVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(0, &aHash, &aSig64, &aPub); }
static void r_verify(void) {
    secp256k1_pubkey p;
    secp256k1_ecdsa_signature s;
    secp256k1_ec_pubkey_parse(ref_ctx, &p, pub33, 33);
    secp256k1_ecdsa_signature_parse_compact(ref_ctx, &s, sig65);
    sink += secp256k1_ecdsa_verify(ref_ctx, &s, hash, &p);
}
static void c_schnorr_sign(void) {
    sink += K(schnorrSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean)(0, &aMsg, &aPriv, &aAux, &aSsig);
}
static void r_schnorr_sign(void) {
    secp256k1_keypair kp;
    sink += secp256k1_keypair_create(ref_ctx, &kp, priv);
    sink += secp256k1_schnorrsig_sign32(ref_ctx, ssig, msg32, &kp, aux);
}
static void c_schnorr_verify(void) { sink += K(schnorrVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(0, &aMsg, &aSsig, &aXonly); }
static void r_schnorr_verify(void) {
    secp256k1_xonly_pubkey x;
    secp256k1_xonly_pubkey_parse(ref_ctx, &x, xonly);
    sink += secp256k1_schnorrsig_verify(ref_ctx, ssig, msg32, 32, &x);
}
static void c_ed_sign(void) { K(ed25519Sign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY)(0, &aSeed, &aEpub, &aMsg, &aEsig); }
static void c_ed_verify(void) { sink += K(ed25519Verify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(0, &aMsg, &aEsig, &aEpub); }
static void o_ed_sign(void) {
    unsigned char out[64];
    size_t len = 64;
    EVP_MD_CTX *ctx = EVP_MD_CTX_new();
    EVP_DigestSignInit(ctx, NULL, NULL, NULL, ossl_ed);
    sink += EVP_DigestSign(ctx, out, &len, msg32, 32);
    EVP_MD_CTX_free(ctx);
}
static void o_ed_verify(void) {
    EVP_PKEY *pk = EVP_PKEY_new_raw_public_key(EVP_PKEY_ED25519, NULL, epub, 32);
    EVP_MD_CTX *ctx = EVP_MD_CTX_new();
    EVP_DigestVerifyInit(ctx, NULL, NULL, NULL, pk);
    sink += EVP_DigestVerify(ctx, esig, 64, msg32, 32);
    EVP_MD_CTX_free(ctx);
    EVP_PKEY_free(pk);
}
#ifdef HAVE_SODIUM
static unsigned char sodium_sk[64];
static void s_ed_sign(void) {
    unsigned char out[64];
    sink += crypto_sign_detached(out, NULL, msg32, 32, sodium_sk);
}
static void s_ed_verify(void) { sink += crypto_sign_verify_detached(esig, msg32, 32, epub); }
#endif
static void c_keccak(void) { K(keccak256___byte_1ARRAY_int_int_byte_1ARRAY)(0, &aKb, 0, 1024, &aKout); }
static void o_keccak(void) {
    unsigned int len = 32;
    sink += EVP_Digest(kb, sizeof(kb), kout, &len, ossl_keccak, NULL);
}

/* SHA-256, SHA-512 and RIPEMD-160 of 1 KiB; HMAC-SHA-512 of 37 bytes under a 32-byte chain code
 * (one BIP-32 step); PBKDF2-HMAC-SHA-512 with 2048 iterations (one BIP-39 seed) */
static unsigned char hout[64], bip39pw[48], bip39salt[16] = "mnemonicTREZOR";
static Arr aHout, aHout32, aHout20, aHout64, aPw, aSalt, aChain, aData37;
static void c_sha256(void) { K(sha256___byte_1ARRAY_int_int_byte_1ARRAY)(0, &aKb, 0, 1024, &aHout32); }
static void o_sha256(void) { unsigned int l = 32; sink += EVP_Digest(kb, 1024, hout, &l, EVP_sha256(), NULL); }
static void c_sha512(void) { K(sha512___byte_1ARRAY_int_int_byte_1ARRAY)(0, &aKb, 0, 1024, &aHout64); }
static void o_sha512(void) { unsigned int l = 64; sink += EVP_Digest(kb, 1024, hout, &l, EVP_sha512(), NULL); }
static const EVP_MD *ossl_rmd;
static void c_rmd(void) { K(ripemd160___byte_1ARRAY_int_int_byte_1ARRAY)(0, &aKb, 0, 1024, &aHout20); }
static void o_rmd(void) { unsigned int l = 20; sink += EVP_Digest(kb, 1024, hout, &l, ossl_rmd, NULL); }
static void c_hmac(void) { K(hmac___int_byte_1ARRAY_byte_1ARRAY_int_int_byte_1ARRAY)(0, 512, &aChain, &aData37, 0, 37, &aHout64); }
static void o_hmac(void) {
    unsigned int l = 64;
    sink += HMAC(EVP_sha512(), priv, 32, kb, 37, hout, &l) != NULL;
}
static void c_pbkdf2(void) { K(pbkdf2___int_byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY)(0, 512, &aPw, &aSalt, 2048, &aHout64); }
static void o_pbkdf2(void) {
    sink += PKCS5_PBKDF2_HMAC((const char*) bip39pw, 48, bip39salt, 14, 2048, EVP_sha512(), 64, hout);
}

static int same(const char *what, op_fn mine, op_fn theirs, int n) {
    unsigned char a[64];
    mine();
    memcpy(a, hout, 64);
    memset(hout, 0, 64);
    theirs();
    if (memcmp(a, hout, (size_t) n) != 0) {
        fprintf(stderr, "%s differs from OpenSSL\n", what);
        return 0;
    }
    return 1;
}

int main(void) {
    for (int i = 0; i < (int) sizeof(kb); i++) {
        kb[i] = (unsigned char) (i * 29 + 1);
    }
    for (int i = 0; i < 32; i++) {
        priv[i] = (unsigned char) (i * 7 + 1);
        hash[i] = (unsigned char) (i * 13 + 5);
        msg32[i] = (unsigned char) (i * 3 + 9);
        seed[i] = (unsigned char) (i * 11 + 2);
        aux[i] = (unsigned char) (i * 5 + 3);
    }
    aPriv = mk(priv, 32); aHash = mk(hash, 32); aPub = mk(pub33, 33); aSig = mk(sig65, 65);
    aSig64 = mk(sig65, 64); aSeed = mk(seed, 32); aEpub = mk(epub, 32); aEsig = mk(esig, 64);
    aMsg = mk(msg32, 32); aKb = mk(kb, 1024); aKout = mk(kout, 32); aXonly = mk(xonly, 32);
    aSsig = mk(ssig, 64); aAux = mk(aux, 32); aRec = mk(rec, 65);
    if (!K(available___R_boolean)(0)) {
        fprintf(stderr, "the kernels compiled to stubs: the amalgamation was not found\n");
        return 2;
    }
    ref_ctx = secp256k1_context_create(SECP256K1_CONTEXT_NONE);

    c_pub();
    c_sign();
    K(secpVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int)(0, &aHash, &aSig64, &aPub);
    {
        /* the two implementations must agree before their speed means anything */
        unsigned char rpub[33], rsig[64];
        size_t len = 33;
        secp256k1_ecdsa_signature s;
        secp256k1_ec_pubkey_create(ref_ctx, &ref_pub, priv);
        secp256k1_ec_pubkey_serialize(ref_ctx, rpub, &len, &ref_pub, SECP256K1_EC_COMPRESSED);
        secp256k1_ecdsa_sign(ref_ctx, &s, hash, priv, NULL, NULL);
        secp256k1_ecdsa_signature_serialize_compact(ref_ctx, rsig, &s);
        if (memcmp(rpub, pub33, 33) != 0 || memcmp(rsig, sig65, 64) != 0) {
            fprintf(stderr, "secp256k1 output differs from the reference\n");
            return 3;
        }
    }
    secp256k1_keypair_create(ref_ctx, &ref_kp, priv);
    secp256k1_keypair_xonly_pub(ref_ctx, &ref_xonly, NULL, &ref_kp);
    secp256k1_xonly_pubkey_serialize(ref_ctx, xonly, &ref_xonly);
    c_schnorr_sign();
    K(ed25519PublicKey___byte_1ARRAY_byte_1ARRAY)(0, &aSeed, &aEpub);
    c_ed_sign();
    ossl_ed = EVP_PKEY_new_raw_private_key(EVP_PKEY_ED25519, NULL, seed, 32);
    {
        unsigned char opub[32], osig[64];
        size_t len = 32, slen = 64;
        EVP_MD_CTX *ctx = EVP_MD_CTX_new();
        EVP_PKEY_get_raw_public_key(ossl_ed, opub, &len);
        EVP_DigestSignInit(ctx, NULL, NULL, NULL, ossl_ed);
        EVP_DigestSign(ctx, osig, &slen, msg32, 32);
        EVP_MD_CTX_free(ctx);
        if (memcmp(opub, epub, 32) != 0 || memcmp(osig, esig, 64) != 0) {
            fprintf(stderr, "Ed25519 output differs from OpenSSL\n");
            return 3;
        }
    }
    ossl_keccak = EVP_MD_fetch(NULL, "KECCAK-256", NULL);

    /* untimed: the first second of a run is spent bringing the core up to speed */
    ops_per_sec(c_pub, 1.0);
    ops_per_sec(r_pub, 1.0);
    row("secp256k1.pubkey", c_pub, r_pub);
    row("secp256k1.sign", c_sign, r_sign);
    row("secp256k1.verify", c_verify, r_verify);
    row("schnorr.sign", c_schnorr_sign, r_schnorr_sign);
    row("schnorr.verify", c_schnorr_verify, r_schnorr_verify);
    row("ed25519.sign.openssl", c_ed_sign, o_ed_sign);
    row("ed25519.verify.openssl", c_ed_verify, o_ed_verify);
#ifdef HAVE_SODIUM
    if (sodium_init() >= 0) {
        unsigned char spk[32];
        crypto_sign_seed_keypair(spk, sodium_sk, seed);
        row("ed25519.sign.sodium", c_ed_sign, s_ed_sign);
        row("ed25519.verify.sodium", c_ed_verify, s_ed_verify);
    }
#endif
    if (ossl_keccak != NULL) {
        unsigned char mine[32], theirs[32];
        unsigned int len = 32;
        for (int n = 0; n <= 300; n += 17) {
            Arr in = mk(kb, n), o = mk(mine, 32);
            K(keccak256___byte_1ARRAY_int_int_byte_1ARRAY)(0, &in, 0, n, &o);
            EVP_Digest(kb, (size_t) n, theirs, &len, ossl_keccak, NULL);
            if (memcmp(mine, theirs, 32) != 0) {
                fprintf(stderr, "Keccak-256 of %d bytes differs from OpenSSL\n", n);
                return 3;
            }
        }
        row("keccak256.1k.openssl", c_keccak, o_keccak);
    }
    for (int i = 0; i < 48; i++) {
        bip39pw[i] = (unsigned char) ('a' + i % 26);
    }
    aHout32 = mk(hout, 32); aHout64 = mk(hout, 64); aHout20 = mk(hout, 20); aHout = aHout64;
    aPw = mk(bip39pw, 48); aSalt = mk(bip39salt, 14); aChain = mk(priv, 32); aData37 = mk(kb, 37);
    ossl_rmd = EVP_MD_fetch(NULL, "RIPEMD-160", NULL);
    if (!same("SHA-256", c_sha256, o_sha256, 32) || !same("SHA-512", c_sha512, o_sha512, 64)
            || !same("HMAC-SHA-512", c_hmac, o_hmac, 64) || !same("PBKDF2", c_pbkdf2, o_pbkdf2, 64)) {
        return 3;
    }
    row("sha256.1k.openssl", c_sha256, o_sha256);
    row("sha512.1k.openssl", c_sha512, o_sha512);
    row("hmac.sha512.bip32.openssl", c_hmac, o_hmac);
    row("pbkdf2.sha512.bip39.openssl", c_pbkdf2, o_pbkdf2);
    if (ossl_rmd != NULL) {
        if (!same("RIPEMD-160", c_rmd, o_rmd, 20)) {
            return 3;
        }
        row("ripemd160.1k.openssl", c_rmd, o_rmd);
    }
    return 0;
}
