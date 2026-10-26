# Native crypto kernels

`cn1_crypto_kernels.c` binds `com.codename1.security.NativeCryptoKernels` to two vendored
libraries, both inlined into `cn1_crypto_kernels_amalgamation.h`:

- **libsecp256k1** (MIT): ECDSA sign/verify/recover, key tweaks, BIP-340 Schnorr. Built with
  the recovery, extrakeys and schnorrsig modules and a window-15 precomputed table.
- **ed25519-donna** (public domain): Ed25519 per RFC 8032, hashing through the SHA-512 in
  `cn1_crypto_kernels.c`.

Keccak-256, SHA-256, SHA-512, RIPEMD-160, HMAC and PBKDF2 are our own code in
`cn1_crypto_kernels.c`. SHA-256 and SHA-512 use the CPU's SHA instructions where it has
them (ARMv8 SHA-256 and ARMv8.2 SHA-512, x86 SHA-NI), chosen once per process from what the
CPU reports, and fall back to unrolled portable C. PBKDF2 lays the fixed-padding block out once,
so each iteration is two compressions.

## When it is compiled

The translator always emits `cn1_crypto_kernels.c`. It emits the amalgamation only when
`-Dcn1.cryptoKernels=true`, which the builders pass when the application's classes reference
the elliptic-curve or Keccak APIs. The `.c` file detects the amalgamation with
`__has_include`: without it every entry point is a stub, `available()` answers false, and
the Java engine runs instead. Both paths produce identical bytes.

## Regenerating

```bash
git clone https://github.com/bitcoin-core/secp256k1 "$TMPDIR/secp256k1"
git clone https://github.com/floodyberry/ed25519-donna "$TMPDIR/ed25519-donna"
python3 scripts/crypto-kernels/amalgamate.py "$TMPDIR/secp256k1" "$TMPDIR/ed25519-donna"
```

The script records both commits in the header of the generated file. Never edit the
amalgamation by hand.

## Batches

`NativeCryptoKernels.batch` verifies or hashes a whole `CryptoBatch` in one call and cuts it
into contiguous chunks, one per core: `dispatch_apply_f` on Apple, pthreads on Linux and
Android, Win32 threads on Windows. A batch too small to pay for a thread runs inline. Each
entry gets the single-call answer; the Ed25519 batch equation is deliberately not used, since it
can accept a signature with a small-order component that the single verify rejects.
