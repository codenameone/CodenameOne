package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_security {
    private GeneratedAccess_com_codename1_security() {
    }

    public static Class<?> findClass(String name) {
        int lastDot = name == null ? -1 : name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(lastDot + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("AuthenticationOptions".equals(simpleName)) {
            return com.codename1.security.AuthenticationOptions.class;
        }
        if ("Base32".equals(simpleName)) {
            return com.codename1.security.Base32.class;
        }
        if ("BiometricError".equals(simpleName)) {
            return com.codename1.security.BiometricError.class;
        }
        if ("BiometricException".equals(simpleName)) {
            return com.codename1.security.BiometricException.class;
        }
        if ("BiometricType".equals(simpleName)) {
            return com.codename1.security.BiometricType.class;
        }
        if ("Biometrics".equals(simpleName)) {
            return com.codename1.security.Biometrics.class;
        }
        if ("Cipher".equals(simpleName)) {
            return com.codename1.security.Cipher.class;
        }
        if ("CryptoException".equals(simpleName)) {
            return com.codename1.security.CryptoException.class;
        }
        if ("Hash".equals(simpleName)) {
            return com.codename1.security.Hash.class;
        }
        if ("Hmac".equals(simpleName)) {
            return com.codename1.security.Hmac.class;
        }
        if ("Jwt".equals(simpleName)) {
            return com.codename1.security.Jwt.class;
        }
        if ("Key".equals(simpleName)) {
            return com.codename1.security.Key.class;
        }
        if ("KeyGenerator".equals(simpleName)) {
            return com.codename1.security.KeyGenerator.class;
        }
        if ("KeyPair".equals(simpleName)) {
            return com.codename1.security.KeyPair.class;
        }
        if ("Otp".equals(simpleName)) {
            return com.codename1.security.Otp.class;
        }
        if ("PrivateKey".equals(simpleName)) {
            return com.codename1.security.PrivateKey.class;
        }
        if ("PublicKey".equals(simpleName)) {
            return com.codename1.security.PublicKey.class;
        }
        if ("SecretKey".equals(simpleName)) {
            return com.codename1.security.SecretKey.class;
        }
        if ("SecureRandom".equals(simpleName)) {
            return com.codename1.security.SecureRandom.class;
        }
        if ("SecureStorage".equals(simpleName)) {
            return com.codename1.security.SecureStorage.class;
        }
        if ("Signature".equals(simpleName)) {
            return com.codename1.security.Signature.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.security.BiometricException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.BiometricError.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.BiometricError.class}, false);
                return new com.codename1.security.BiometricException((com.codename1.security.BiometricError) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.BiometricError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.BiometricError.class, java.lang.String.class}, false);
                return new com.codename1.security.BiometricException((com.codename1.security.BiometricError) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.BiometricError.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.BiometricError.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.security.BiometricException((com.codename1.security.BiometricError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.security.CryptoException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.security.CryptoException((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.security.CryptoException((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.security.KeyPair.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.PublicKey.class, com.codename1.security.PrivateKey.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.PublicKey.class, com.codename1.security.PrivateKey.class}, false);
                return new com.codename1.security.KeyPair((com.codename1.security.PublicKey) adaptedArgs[0], (com.codename1.security.PrivateKey) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.security.SecretKey.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return new com.codename1.security.SecretKey((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.security.Base32.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.security.Biometrics.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.security.Cipher.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.security.Hash.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.security.Hmac.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.security.Jwt.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.security.KeyGenerator.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.security.Otp.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.security.PrivateKey.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.security.PublicKey.class) return invokeStatic9(name, safeArgs);
        if (type == com.codename1.security.SecureRandom.class) return invokeStatic10(name, safeArgs);
        if (type == com.codename1.security.SecureStorage.class) return invokeStatic11(name, safeArgs);
        if (type == com.codename1.security.Signature.class) return invokeStatic12(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.Base32.decode((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("encode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Base32.encode((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Base32.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.Biometrics.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.security.Biometrics.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("aesDecrypt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.SecretKey.class, byte[].class, byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.SecretKey.class, byte[].class, byte[].class, byte[].class}, false);
                return com.codename1.security.Cipher.aesDecrypt((java.lang.String) adaptedArgs[0], (com.codename1.security.SecretKey) adaptedArgs[1], (byte[]) adaptedArgs[2], (byte[]) adaptedArgs[3], (byte[]) adaptedArgs[4]);
            }
        }
        if ("aesEncrypt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.SecretKey.class, byte[].class, byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.SecretKey.class, byte[].class, byte[].class, byte[].class}, false);
                return com.codename1.security.Cipher.aesEncrypt((java.lang.String) adaptedArgs[0], (com.codename1.security.SecretKey) adaptedArgs[1], (byte[]) adaptedArgs[2], (byte[]) adaptedArgs[3], (byte[]) adaptedArgs[4]);
            }
        }
        if ("rsaDecrypt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PrivateKey.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PrivateKey.class, byte[].class}, false);
                return com.codename1.security.Cipher.rsaDecrypt((java.lang.String) adaptedArgs[0], (com.codename1.security.PrivateKey) adaptedArgs[1], (byte[]) adaptedArgs[2]);
            }
        }
        if ("rsaEncrypt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PublicKey.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PublicKey.class, byte[].class}, false);
                return com.codename1.security.Cipher.rsaEncrypt((java.lang.String) adaptedArgs[0], (com.codename1.security.PublicKey) adaptedArgs[1], (byte[]) adaptedArgs[2]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Cipher.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.Hash.create((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("fromHex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.Hash.fromHex((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("md5".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.md5((byte[]) adaptedArgs[0]);
            }
        }
        if ("sha1".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.sha1((byte[]) adaptedArgs[0]);
            }
        }
        if ("sha224".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.sha224((byte[]) adaptedArgs[0]);
            }
        }
        if ("sha256".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.sha256((byte[]) adaptedArgs[0]);
            }
        }
        if ("sha384".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.sha384((byte[]) adaptedArgs[0]);
            }
        }
        if ("sha512".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.sha512((byte[]) adaptedArgs[0]);
            }
        }
        if ("toHex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Hash.toHex((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Hash.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("constantTimeEquals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.constantTimeEquals((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return com.codename1.security.Hmac.create((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("md5".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.md5((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("sha1".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.sha1((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("sha224".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.sha224((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("sha256".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.sha256((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("sha384".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.sha384((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("sha512".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.security.Hmac.sha512((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Hmac.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.security.Jwt.parse((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("sign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class, java.lang.String.class}, false);
                return com.codename1.security.Jwt.sign((java.util.Map) adaptedArgs[0], (byte[]) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, com.codename1.security.PrivateKey.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, com.codename1.security.PrivateKey.class, java.lang.String.class}, false);
                return com.codename1.security.Jwt.sign((java.util.Map) adaptedArgs[0], (com.codename1.security.PrivateKey) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("signHs256".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class}, false);
                return com.codename1.security.Jwt.signHs256((java.util.Map) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("signHs384".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class}, false);
                return com.codename1.security.Jwt.signHs384((java.util.Map) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("signHs512".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, byte[].class}, false);
                return com.codename1.security.Jwt.signHs512((java.util.Map) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("signNone".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.security.Jwt.signNone((java.util.Map) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Jwt.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("aes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.security.KeyGenerator.aes(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hmac".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.security.KeyGenerator.hmac(toIntValue(adaptedArgs[0]));
            }
        }
        if ("rsa".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.security.KeyGenerator.rsa(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(com.codename1.security.KeyGenerator.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("hotp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Long.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Long.class, java.lang.Integer.class}, false);
                return com.codename1.security.Otp.hotp((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Long.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Long.class, java.lang.Integer.class, java.lang.String.class}, false);
                return com.codename1.security.Otp.hotp((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("otpauthUri".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class}, false);
                return com.codename1.security.Otp.otpauthUri((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (byte[]) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return com.codename1.security.Otp.otpauthUri((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (byte[]) adaptedArgs[2], toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), (java.lang.String) adaptedArgs[5]);
            }
        }
        if ("totp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.Otp.totp((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.security.Otp.totp((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return com.codename1.security.Otp.totp((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (java.lang.String) adaptedArgs[4]);
            }
        }
        if ("verifyTotp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class, java.lang.Integer.class}, false);
                return com.codename1.security.Otp.verifyTotp((byte[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return com.codename1.security.Otp.verifyTotp((byte[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue(), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), (java.lang.String) adaptedArgs[6]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Otp.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("fromPkcs8".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return com.codename1.security.PrivateKey.fromPkcs8((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("rsa".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.PrivateKey.rsa((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.security.PrivateKey.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("fromX509".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                return com.codename1.security.PublicKey.fromX509((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        if ("rsa".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.security.PublicKey.rsa((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.security.PublicKey.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("bytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.security.SecureRandom.bytes(toIntValue(adaptedArgs[0]));
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                com.codename1.security.SecureRandom.fill((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("intBelow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.security.SecureRandom.intBelow(toIntValue(adaptedArgs[0]));
            }
        }
        if ("longBelow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.security.SecureRandom.longBelow(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.security.SecureRandom.class, name, safeArgs);
    }

    private static Object invokeStatic11(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.security.SecureStorage.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.security.SecureStorage.class, name, safeArgs);
    }

    private static Object invokeStatic12(String name, Object[] safeArgs) throws Exception {
        if ("sign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PrivateKey.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PrivateKey.class, byte[].class}, false);
                return com.codename1.security.Signature.sign((java.lang.String) adaptedArgs[0], (com.codename1.security.PrivateKey) adaptedArgs[1], (byte[]) adaptedArgs[2]);
            }
        }
        if ("verify".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PublicKey.class, byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.security.PublicKey.class, byte[].class, byte[].class}, false);
                return com.codename1.security.Signature.verify((java.lang.String) adaptedArgs[0], (com.codename1.security.PublicKey) adaptedArgs[1], (byte[]) adaptedArgs[2], (byte[]) adaptedArgs[3]);
            }
        }
        throw unsupportedStatic(com.codename1.security.Signature.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.security.PrivateKey) {
            try {
                return invoke0((com.codename1.security.PrivateKey) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.PublicKey) {
            try {
                return invoke1((com.codename1.security.PublicKey) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.SecretKey) {
            try {
                return invoke2((com.codename1.security.SecretKey) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.AuthenticationOptions) {
            try {
                return invoke3((com.codename1.security.AuthenticationOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.BiometricException) {
            try {
                return invoke4((com.codename1.security.BiometricException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.Biometrics) {
            try {
                return invoke5((com.codename1.security.Biometrics) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.Hash) {
            try {
                return invoke6((com.codename1.security.Hash) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.Hmac) {
            try {
                return invoke7((com.codename1.security.Hmac) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.Jwt) {
            try {
                return invoke8((com.codename1.security.Jwt) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.Key) {
            try {
                return invoke9((com.codename1.security.Key) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.KeyPair) {
            try {
                return invoke10((com.codename1.security.KeyPair) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.security.SecureStorage) {
            try {
                return invoke11((com.codename1.security.SecureStorage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.security.PrivateKey typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlgorithm();
            }
        }
        if ("getEncoded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncoded();
            }
        }
        if ("getFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormat();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.security.PublicKey typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlgorithm();
            }
        }
        if ("getEncoded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncoded();
            }
        }
        if ("getFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormat();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.security.SecretKey typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlgorithm();
            }
        }
        if ("getBitLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBitLength();
            }
        }
        if ("getEncoded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncoded();
            }
        }
        if ("getFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormat();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.security.AuthenticationOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getNegativeButtonText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNegativeButtonText();
            }
        }
        if ("getReason".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReason();
            }
        }
        if ("getSubtitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubtitle();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isBiometricOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBiometricOnly();
            }
        }
        if ("isSensitiveTransaction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSensitiveTransaction();
            }
        }
        if ("isShowDialogOnAndroid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowDialogOnAndroid();
            }
        }
        if ("isStickyAuth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStickyAuth();
            }
        }
        if ("setBiometricOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setBiometricOnly(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDescription((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setNegativeButtonText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setNegativeButtonText((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setReason".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setReason((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSensitiveTransaction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setSensitiveTransaction(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setShowDialogOnAndroid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setShowDialogOnAndroid(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setStickyAuth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setStickyAuth(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setSubtitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSubtitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.security.BiometricException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.security.Biometrics typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authenticate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.AuthenticationOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.AuthenticationOptions.class}, false);
                return typedTarget.authenticate((com.codename1.security.AuthenticationOptions) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.authenticate((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("canAuthenticate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canAuthenticate();
            }
        }
        if ("getAvailableBiometrics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailableBiometrics();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("stopAuthentication".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stopAuthentication();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.security.Hash typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("digest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.digest();
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.digest((byte[]) adaptedArgs[0]);
            }
        }
        if ("digestLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.digestLength();
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.update((byte) toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.update((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.update((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.security.Hmac typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("doFinal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doFinal();
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.doFinal((byte[]) adaptedArgs[0]);
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("tagLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.tagLength();
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.update((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.update((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.security.Jwt typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlgorithm();
            }
        }
        if ("getClaim".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClaim((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClaims".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClaims();
            }
        }
        if ("getHeader".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeader();
            }
        }
        if ("getSignature".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSignature();
            }
        }
        if ("setVerifyAllowNoneAlgorithm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVerifyAllowNoneAlgorithm(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("verify".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.security.PublicKey.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.security.PublicKey.class}, false);
                return typedTarget.verify((com.codename1.security.PublicKey) adaptedArgs[0]);
            }
        }
        if ("verifyHs256".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.verifyHs256((byte[]) adaptedArgs[0]);
            }
        }
        if ("verifyHs384".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.verifyHs384((byte[]) adaptedArgs[0]);
            }
        }
        if ("verifyHs512".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.verifyHs512((byte[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.security.Key typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlgorithm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlgorithm();
            }
        }
        if ("getEncoded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncoded();
            }
        }
        if ("getFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormat();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.security.KeyPair typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPrivateKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrivateKey();
            }
        }
        if ("getPublicKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPublicKey();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.security.SecureStorage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.get((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.get((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.remove((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.remove((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.set((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.set((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("setKeychainAccessGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setKeychainAccessGroup((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.security.BiometricError.class) return getStaticField0(name);
        if (type == com.codename1.security.BiometricType.class) return getStaticField1(name);
        if (type == com.codename1.security.Cipher.class) return getStaticField2(name);
        if (type == com.codename1.security.Hash.class) return getStaticField3(name);
        if (type == com.codename1.security.Jwt.class) return getStaticField4(name);
        if (type == com.codename1.security.PublicKey.class) return getStaticField5(name);
        if (type == com.codename1.security.Signature.class) return getStaticField6(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("AUTHENTICATION_FAILED".equals(name)) return com.codename1.security.BiometricError.AUTHENTICATION_FAILED;
        if ("KEY_REVOKED".equals(name)) return com.codename1.security.BiometricError.KEY_REVOKED;
        if ("LOCKED_OUT".equals(name)) return com.codename1.security.BiometricError.LOCKED_OUT;
        if ("NOT_AVAILABLE".equals(name)) return com.codename1.security.BiometricError.NOT_AVAILABLE;
        if ("NOT_ENROLLED".equals(name)) return com.codename1.security.BiometricError.NOT_ENROLLED;
        if ("PASSCODE_NOT_SET".equals(name)) return com.codename1.security.BiometricError.PASSCODE_NOT_SET;
        if ("PERMANENTLY_LOCKED_OUT".equals(name)) return com.codename1.security.BiometricError.PERMANENTLY_LOCKED_OUT;
        if ("SYSTEM_CANCELED".equals(name)) return com.codename1.security.BiometricError.SYSTEM_CANCELED;
        if ("UNKNOWN".equals(name)) return com.codename1.security.BiometricError.UNKNOWN;
        if ("USER_CANCELED".equals(name)) return com.codename1.security.BiometricError.USER_CANCELED;
        throw unsupportedStaticField(com.codename1.security.BiometricError.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("FACE".equals(name)) return com.codename1.security.BiometricType.FACE;
        if ("FINGERPRINT".equals(name)) return com.codename1.security.BiometricType.FINGERPRINT;
        if ("IRIS".equals(name)) return com.codename1.security.BiometricType.IRIS;
        if ("STRONG".equals(name)) return com.codename1.security.BiometricType.STRONG;
        if ("WEAK".equals(name)) return com.codename1.security.BiometricType.WEAK;
        throw unsupportedStaticField(com.codename1.security.BiometricType.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("AES_CBC".equals(name)) return com.codename1.security.Cipher.AES_CBC;
        if ("AES_CBC_PKCS5".equals(name)) return com.codename1.security.Cipher.AES_CBC_PKCS5;
        if ("AES_ECB_PKCS5".equals(name)) return com.codename1.security.Cipher.AES_ECB_PKCS5;
        if ("AES_GCM".equals(name)) return com.codename1.security.Cipher.AES_GCM;
        if ("RSA_OAEP_SHA256".equals(name)) return com.codename1.security.Cipher.RSA_OAEP_SHA256;
        if ("RSA_PKCS1".equals(name)) return com.codename1.security.Cipher.RSA_PKCS1;
        throw unsupportedStaticField(com.codename1.security.Cipher.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("MD5".equals(name)) return com.codename1.security.Hash.MD5;
        if ("SHA1".equals(name)) return com.codename1.security.Hash.SHA1;
        if ("SHA224".equals(name)) return com.codename1.security.Hash.SHA224;
        if ("SHA256".equals(name)) return com.codename1.security.Hash.SHA256;
        if ("SHA384".equals(name)) return com.codename1.security.Hash.SHA384;
        if ("SHA512".equals(name)) return com.codename1.security.Hash.SHA512;
        throw unsupportedStaticField(com.codename1.security.Hash.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("ES256".equals(name)) return com.codename1.security.Jwt.ES256;
        if ("ES384".equals(name)) return com.codename1.security.Jwt.ES384;
        if ("ES512".equals(name)) return com.codename1.security.Jwt.ES512;
        if ("HS256".equals(name)) return com.codename1.security.Jwt.HS256;
        if ("HS384".equals(name)) return com.codename1.security.Jwt.HS384;
        if ("HS512".equals(name)) return com.codename1.security.Jwt.HS512;
        if ("NONE".equals(name)) return com.codename1.security.Jwt.NONE;
        if ("RS256".equals(name)) return com.codename1.security.Jwt.RS256;
        if ("RS384".equals(name)) return com.codename1.security.Jwt.RS384;
        if ("RS512".equals(name)) return com.codename1.security.Jwt.RS512;
        throw unsupportedStaticField(com.codename1.security.Jwt.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("EC".equals(name)) return com.codename1.security.PublicKey.EC;
        if ("RSA".equals(name)) return com.codename1.security.PublicKey.RSA;
        throw unsupportedStaticField(com.codename1.security.PublicKey.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("SHA256_WITH_ECDSA".equals(name)) return com.codename1.security.Signature.SHA256_WITH_ECDSA;
        if ("SHA256_WITH_RSA".equals(name)) return com.codename1.security.Signature.SHA256_WITH_RSA;
        if ("SHA384_WITH_ECDSA".equals(name)) return com.codename1.security.Signature.SHA384_WITH_ECDSA;
        if ("SHA384_WITH_RSA".equals(name)) return com.codename1.security.Signature.SHA384_WITH_RSA;
        if ("SHA512_WITH_ECDSA".equals(name)) return com.codename1.security.Signature.SHA512_WITH_ECDSA;
        if ("SHA512_WITH_RSA".equals(name)) return com.codename1.security.Signature.SHA512_WITH_RSA;
        throw unsupportedStaticField(com.codename1.security.Signature.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
