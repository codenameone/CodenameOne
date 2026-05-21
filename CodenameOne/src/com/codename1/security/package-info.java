/// Cryptographic primitives and conveniences: hashing, message authentication,
/// symmetric/asymmetric encryption, digital signatures, JWTs, OTPs and
/// random number generation.
///
/// #### What lives in this package
///
/// - [Hash] / [Hmac] -- pure-Java MD5, SHA-1, SHA-224, SHA-256, SHA-384,
///   SHA-512 and HMAC variants. Available on every platform with identical
///   output.
/// - [SecureRandom] -- wraps the platform CSPRNG.
/// - [Cipher] -- AES (CBC, GCM, ECB) and RSA (OAEP, PKCS#1) encryption. Backed
///   by the platform's native crypto.
/// - [Signature] -- RSA and ECDSA digital signatures.
/// - [KeyGenerator] / [KeyPair] / [SecretKey] / [PublicKey] / [PrivateKey] --
///   key material containers and generators.
/// - [Jwt] -- JSON Web Token signing and verification (HS, RS and ES families).
/// - [Otp] -- RFC 4226/6238 HOTP and TOTP one-time passwords, compatible with
///   standard authenticator apps.
/// - [Base32] -- 32-character encoding commonly used for OTP shared secrets.
///   URL-safe Base64 (used by JWTs) lives on
///   [com.codename1.util.Base64#encodeUrlSafe(byte[])] /
///   [com.codename1.util.Base64#decodeUrlSafe(String)] so it can share the
///   existing SIMD-optimized encoder.
///
/// For a segmented OTP input widget see
/// [com.codename1.components.OtpField].
///
/// #### Design notes
///
/// Hash and HMAC ship a built-in implementation written in portable Java so
/// they work everywhere without depending on the platform's crypto stack --
/// they are also what JWT (HS family), HOTP and TOTP build on.
///
/// AES, RSA, digital signatures and the secure RNG go through the platform's
/// native crypto provider via
/// [com.codename1.impl.CodenameOneImplementation]. The default implementation
/// uses the JRE's `java.security` / `javax.crypto` via reflection, so JavaSE
/// (simulator) and Android work out of the box. Other ports may override the
/// bridge methods with direct native calls.
package com.codename1.security;
