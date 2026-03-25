package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_gzip {
    private GeneratedAccess_com_codename1_io_gzip() {
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
        if ("Adler32".equals(simpleName)) {
            return com.codename1.io.gzip.Adler32.class;
        }
        if ("CRC32".equals(simpleName)) {
            return com.codename1.io.gzip.CRC32.class;
        }
        if ("Deflate".equals(simpleName)) {
            return com.codename1.io.gzip.Deflate.class;
        }
        if ("Deflater".equals(simpleName)) {
            return com.codename1.io.gzip.Deflater.class;
        }
        if ("DeflaterOutputStream".equals(simpleName)) {
            return com.codename1.io.gzip.DeflaterOutputStream.class;
        }
        if ("FilterInputStream".equals(simpleName)) {
            return com.codename1.io.gzip.FilterInputStream.class;
        }
        if ("FilterOutputStream".equals(simpleName)) {
            return com.codename1.io.gzip.FilterOutputStream.class;
        }
        if ("GZConnectionRequest".equals(simpleName)) {
            return com.codename1.io.gzip.GZConnectionRequest.class;
        }
        if ("GZIPException".equals(simpleName)) {
            return com.codename1.io.gzip.GZIPException.class;
        }
        if ("GZIPHeader".equals(simpleName)) {
            return com.codename1.io.gzip.GZIPHeader.class;
        }
        if ("GZIPInputStream".equals(simpleName)) {
            return com.codename1.io.gzip.GZIPInputStream.class;
        }
        if ("GZIPOutputStream".equals(simpleName)) {
            return com.codename1.io.gzip.GZIPOutputStream.class;
        }
        if ("Inflater".equals(simpleName)) {
            return com.codename1.io.gzip.Inflater.class;
        }
        if ("InflaterInputStream".equals(simpleName)) {
            return com.codename1.io.gzip.InflaterInputStream.class;
        }
        if ("JZlib".equals(simpleName)) {
            return com.codename1.io.gzip.JZlib.class;
        }
        if ("ZStream".equals(simpleName)) {
            return com.codename1.io.gzip.ZStream.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.gzip.Deflater.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.gzip.Deflater();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.io.gzip.Deflater(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.io.gzip.Deflater(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.io.gzip.Deflater(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.io.gzip.Deflater(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.io.gzip.Deflater(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if (type == com.codename1.io.gzip.GZIPException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.gzip.GZIPException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.io.gzip.GZIPException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.io.gzip.Inflater.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.gzip.Inflater();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return new com.codename1.io.gzip.Inflater(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.io.gzip.Inflater(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.io.gzip.Inflater(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.io.gzip.ZStream.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.gzip.ZStream();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.gzip.CRC32.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.gzip.JZlib.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getCRC32Table".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.gzip.CRC32.getCRC32Table();
            }
        }
        throw unsupportedStatic(com.codename1.io.gzip.CRC32.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("adler32Combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.io.gzip.JZlib.adler32Combine(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        if ("adler32_combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.io.gzip.JZlib.adler32_combine(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        if ("crc32Combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.io.gzip.JZlib.crc32Combine(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        if ("crc32_combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false);
                return com.codename1.io.gzip.JZlib.crc32_combine(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue());
            }
        }
        if ("version".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.io.gzip.JZlib.version();
            }
        }
        throw unsupportedStatic(com.codename1.io.gzip.JZlib.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.gzip.GZIPInputStream) {
            try {
                return invoke0((com.codename1.io.gzip.GZIPInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.GZIPOutputStream) {
            try {
                return invoke1((com.codename1.io.gzip.GZIPOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.Deflater) {
            try {
                return invoke2((com.codename1.io.gzip.Deflater) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.DeflaterOutputStream) {
            try {
                return invoke3((com.codename1.io.gzip.DeflaterOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.Inflater) {
            try {
                return invoke4((com.codename1.io.gzip.Inflater) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.InflaterInputStream) {
            try {
                return invoke5((com.codename1.io.gzip.InflaterInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.Adler32) {
            try {
                return invoke6((com.codename1.io.gzip.Adler32) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.CRC32) {
            try {
                return invoke7((com.codename1.io.gzip.CRC32) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.FilterInputStream) {
            try {
                return invoke8((com.codename1.io.gzip.FilterInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.FilterOutputStream) {
            try {
                return invoke9((com.codename1.io.gzip.FilterOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.GZConnectionRequest) {
            try {
                return invoke10((com.codename1.io.gzip.GZConnectionRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.GZIPException) {
            try {
                return invoke11((com.codename1.io.gzip.GZIPException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.GZIPHeader) {
            try {
                return invoke12((com.codename1.io.gzip.GZIPHeader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.gzip.ZStream) {
            try {
                return invoke13((com.codename1.io.gzip.ZStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.gzip.GZIPInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getAvailIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getCRC".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCRC();
            }
        }
        if ("getComment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComment();
            }
        }
        if ("getInflater".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInflater();
            }
        }
        if ("getModifiedtime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModifiedtime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getOS".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOS();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.read();
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("readHeader".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.readHeader(); return null;
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.gzip.GZIPOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getCRC".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCRC();
            }
        }
        if ("getDeflater".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeflater();
            }
        }
        if ("getSyncFlush".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSyncFlush();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("setComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setComment((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setModifiedTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setModifiedTime(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOS".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setOS(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSyncFlush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSyncFlush(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.write(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.write((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.write((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.gzip.Deflater typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.gzip.Deflater.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.gzip.Deflater.class}, false);
                return typedTarget.copy((com.codename1.io.gzip.Deflater) adaptedArgs[0]);
            }
        }
        if ("deflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deflate(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("deflateEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.deflateEnd();
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("deflateParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateParams(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("deflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.deflateSetDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("end".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.end();
            }
        }
        if ("finished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.finished();
            }
        }
        if ("free".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.free(); return null;
            }
        }
        if ("getAdler".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdler();
            }
        }
        if ("getAvailIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getAvailOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailOut();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getNextIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextIn();
            }
        }
        if ("getNextInIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextInIndex();
            }
        }
        if ("getNextOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextOut();
            }
        }
        if ("getNextOutIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextOutIndex();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.inflate(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("inflateEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateEnd();
            }
        }
        if ("inflateFinished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateFinished();
            }
        }
        if ("inflateInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateInit();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.inflateInit(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.inflateInit(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.inflateInit(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("inflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.inflateSetDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("inflateSync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateSync();
            }
        }
        if ("inflateSyncPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateSyncPoint();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("params".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.params(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("setAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAvailIn(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAvailOut(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.setDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("setNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setNextIn((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNextInIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setNextOut((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNextOutIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setOutput((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setOutput((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.gzip.DeflaterOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("getDeflater".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeflater();
            }
        }
        if ("getSyncFlush".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSyncFlush();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("setSyncFlush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSyncFlush(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.write(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.write((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.write((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.gzip.Inflater typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("deflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deflate(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("deflateEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.deflateEnd();
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("deflateParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateParams(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("deflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.deflateSetDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("end".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.end();
            }
        }
        if ("finished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.finished();
            }
        }
        if ("free".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.free(); return null;
            }
        }
        if ("getAdler".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdler();
            }
        }
        if ("getAvailIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getAvailOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailOut();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getNextIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextIn();
            }
        }
        if ("getNextInIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextInIndex();
            }
        }
        if ("getNextOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextOut();
            }
        }
        if ("getNextOutIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextOutIndex();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.inflate(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("inflateEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateEnd();
            }
        }
        if ("inflateFinished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateFinished();
            }
        }
        if ("inflateInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateInit();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.inflateInit(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.inflateInit(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.inflateInit(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("inflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.inflateSetDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("inflateSync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateSync();
            }
        }
        if ("inflateSyncPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateSyncPoint();
            }
        }
        if ("init".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.init();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.init(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.init(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("setAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAvailIn(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAvailOut(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.setDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("setNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setNextIn((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNextInIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setNextOut((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNextOutIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setOutput((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setOutput((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        if ("sync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.sync();
            }
        }
        if ("syncPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.syncPoint();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.gzip.InflaterInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getAvailIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getInflater".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInflater();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.read();
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("readHeader".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.readHeader(); return null;
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.gzip.Adler32 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.copy();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.reset(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.update((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.io.gzip.CRC32 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.copy();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.reset(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.update((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.io.gzip.FilterInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.read();
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.read((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.io.gzip.FilterOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flush(); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.write(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.write((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.write((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.io.gzip.GZConnectionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgument((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false);
                typedTarget.addArgumentNoEncoding((java.lang.String) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.addArguments((java.lang.String) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addRequestHeader((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToFileSystem((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false);
                typedTarget.downloadImageToStorage((java.lang.String) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1], (com.codename1.util.FailureCallback) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.ioStreamUpdate((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.onRedirect((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeArgument((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false);
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) adaptedArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCheckSSLCertificates(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setChunkedStreamingMode(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setContentType((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCookiesEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDestinationStorage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDuplicateSupported(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFailSilently(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFollowRedirects(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setHttpMethod((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setInsecure(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPost(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPriority(((Number) adaptedArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReadResponseForErrors(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setReadTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false);
                typedTarget.setRequestBody((com.codename1.io.Data) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRequestBody((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false);
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) adaptedArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSilentRetryCount(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTimeout(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserAgent((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setWriteRequest(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.io.gzip.GZIPException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.addSuppressed((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.initCause((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false);
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.io.gzip.GZIPHeader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCRC".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCRC();
            }
        }
        if ("getComment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComment();
            }
        }
        if ("getModifiedTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModifiedTime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getOS".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOS();
            }
        }
        if ("setCRC".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setCRC(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setComment((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setModifiedTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setModifiedTime(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOS".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setOS(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.io.gzip.ZStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("deflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deflate(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("deflateEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.deflateEnd();
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateInit(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("deflateParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.deflateParams(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("deflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.deflateSetDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("end".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.end();
            }
        }
        if ("finished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.finished();
            }
        }
        if ("free".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.free(); return null;
            }
        }
        if ("getAdler".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAdler();
            }
        }
        if ("getAvailIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getAvailOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailOut();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getNextIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextIn();
            }
        }
        if ("getNextInIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextInIndex();
            }
        }
        if ("getNextOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextOut();
            }
        }
        if ("getNextOutIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextOutIndex();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalOut();
            }
        }
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.inflate(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("inflateEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateEnd();
            }
        }
        if ("inflateFinished".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateFinished();
            }
        }
        if ("inflateInit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateInit();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.inflateInit(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.inflateInit(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.inflateInit(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("inflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return typedTarget.inflateSetDictionary((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("inflateSync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateSync();
            }
        }
        if ("inflateSyncPoint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.inflateSyncPoint();
            }
        }
        if ("setAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAvailIn(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAvailOut(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setInput((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("setNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setNextIn((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNextInIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setNextOut((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNextOutIndex(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.setOutput((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setOutput((byte[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.gzip.GZIPHeader.class) {
            if ("OS_AMIGA".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_AMIGA;
            if ("OS_ATARI".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_ATARI;
            if ("OS_CPM".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_CPM;
            if ("OS_MACOS".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_MACOS;
            if ("OS_MSDOS".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_MSDOS;
            if ("OS_OS2".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_OS2;
            if ("OS_QDOS".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_QDOS;
            if ("OS_RISCOS".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_RISCOS;
            if ("OS_TOPS20".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_TOPS20;
            if ("OS_UNIX".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_UNIX;
            if ("OS_UNKNOWN".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_UNKNOWN;
            if ("OS_VMCMS".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_VMCMS;
            if ("OS_VMS".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_VMS;
            if ("OS_WIN32".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_WIN32;
            if ("OS_ZSYSTEM".equals(name)) return com.codename1.io.gzip.GZIPHeader.OS_ZSYSTEM;
        }
        if (type == com.codename1.io.gzip.JZlib.class) {
            if ("DEF_WBITS".equals(name)) return com.codename1.io.gzip.JZlib.DEF_WBITS;
            if ("MAX_WBITS".equals(name)) return com.codename1.io.gzip.JZlib.MAX_WBITS;
            if ("Z_ASCII".equals(name)) return com.codename1.io.gzip.JZlib.Z_ASCII;
            if ("Z_BEST_COMPRESSION".equals(name)) return com.codename1.io.gzip.JZlib.Z_BEST_COMPRESSION;
            if ("Z_BEST_SPEED".equals(name)) return com.codename1.io.gzip.JZlib.Z_BEST_SPEED;
            if ("Z_BINARY".equals(name)) return com.codename1.io.gzip.JZlib.Z_BINARY;
            if ("Z_BUF_ERROR".equals(name)) return com.codename1.io.gzip.JZlib.Z_BUF_ERROR;
            if ("Z_DATA_ERROR".equals(name)) return com.codename1.io.gzip.JZlib.Z_DATA_ERROR;
            if ("Z_DEFAULT_COMPRESSION".equals(name)) return com.codename1.io.gzip.JZlib.Z_DEFAULT_COMPRESSION;
            if ("Z_DEFAULT_STRATEGY".equals(name)) return com.codename1.io.gzip.JZlib.Z_DEFAULT_STRATEGY;
            if ("Z_ERRNO".equals(name)) return com.codename1.io.gzip.JZlib.Z_ERRNO;
            if ("Z_FILTERED".equals(name)) return com.codename1.io.gzip.JZlib.Z_FILTERED;
            if ("Z_FINISH".equals(name)) return com.codename1.io.gzip.JZlib.Z_FINISH;
            if ("Z_FULL_FLUSH".equals(name)) return com.codename1.io.gzip.JZlib.Z_FULL_FLUSH;
            if ("Z_HUFFMAN_ONLY".equals(name)) return com.codename1.io.gzip.JZlib.Z_HUFFMAN_ONLY;
            if ("Z_MEM_ERROR".equals(name)) return com.codename1.io.gzip.JZlib.Z_MEM_ERROR;
            if ("Z_NEED_DICT".equals(name)) return com.codename1.io.gzip.JZlib.Z_NEED_DICT;
            if ("Z_NO_COMPRESSION".equals(name)) return com.codename1.io.gzip.JZlib.Z_NO_COMPRESSION;
            if ("Z_NO_FLUSH".equals(name)) return com.codename1.io.gzip.JZlib.Z_NO_FLUSH;
            if ("Z_OK".equals(name)) return com.codename1.io.gzip.JZlib.Z_OK;
            if ("Z_PARTIAL_FLUSH".equals(name)) return com.codename1.io.gzip.JZlib.Z_PARTIAL_FLUSH;
            if ("Z_STREAM_END".equals(name)) return com.codename1.io.gzip.JZlib.Z_STREAM_END;
            if ("Z_STREAM_ERROR".equals(name)) return com.codename1.io.gzip.JZlib.Z_STREAM_ERROR;
            if ("Z_SYNC_FLUSH".equals(name)) return com.codename1.io.gzip.JZlib.Z_SYNC_FLUSH;
            if ("Z_UNKNOWN".equals(name)) return com.codename1.io.gzip.JZlib.Z_UNKNOWN;
            if ("Z_VERSION_ERROR".equals(name)) return com.codename1.io.gzip.JZlib.Z_VERSION_ERROR;
        }
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.io.gzip.Deflater) {
            com.codename1.io.gzip.Deflater typedTarget = (com.codename1.io.gzip.Deflater) target;
            if ("availIn".equals(name)) return typedTarget.availIn;
            if ("availOut".equals(name)) return typedTarget.availOut;
            if ("msg".equals(name)) return typedTarget.msg;
            if ("nextIn".equals(name)) return typedTarget.nextIn;
            if ("nextInIndex".equals(name)) return typedTarget.nextInIndex;
            if ("nextOut".equals(name)) return typedTarget.nextOut;
            if ("nextOutIndex".equals(name)) return typedTarget.nextOutIndex;
            if ("totalIn".equals(name)) return typedTarget.totalIn;
            if ("totalOut".equals(name)) return typedTarget.totalOut;
        }
        if (target instanceof com.codename1.io.gzip.Inflater) {
            com.codename1.io.gzip.Inflater typedTarget = (com.codename1.io.gzip.Inflater) target;
            if ("availIn".equals(name)) return typedTarget.availIn;
            if ("availOut".equals(name)) return typedTarget.availOut;
            if ("msg".equals(name)) return typedTarget.msg;
            if ("nextIn".equals(name)) return typedTarget.nextIn;
            if ("nextInIndex".equals(name)) return typedTarget.nextInIndex;
            if ("nextOut".equals(name)) return typedTarget.nextOut;
            if ("nextOutIndex".equals(name)) return typedTarget.nextOutIndex;
            if ("totalIn".equals(name)) return typedTarget.totalIn;
            if ("totalOut".equals(name)) return typedTarget.totalOut;
        }
        if (target instanceof com.codename1.io.gzip.ZStream) {
            com.codename1.io.gzip.ZStream typedTarget = (com.codename1.io.gzip.ZStream) target;
            if ("availIn".equals(name)) return typedTarget.availIn;
            if ("availOut".equals(name)) return typedTarget.availOut;
            if ("msg".equals(name)) return typedTarget.msg;
            if ("nextIn".equals(name)) return typedTarget.nextIn;
            if ("nextInIndex".equals(name)) return typedTarget.nextInIndex;
            if ("nextOut".equals(name)) return typedTarget.nextOut;
            if ("nextOutIndex".equals(name)) return typedTarget.nextOutIndex;
            if ("totalIn".equals(name)) return typedTarget.totalIn;
            if ("totalOut".equals(name)) return typedTarget.totalOut;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.io.gzip.Deflater) {
            com.codename1.io.gzip.Deflater typedTarget = (com.codename1.io.gzip.Deflater) target;
            if ("availIn".equals(name)) {
                typedTarget.availIn = ((Number) value).intValue();
                return;
            }
            if ("availOut".equals(name)) {
                typedTarget.availOut = ((Number) value).intValue();
                return;
            }
            if ("msg".equals(name)) {
                typedTarget.msg = (java.lang.String) value;
                return;
            }
            if ("nextIn".equals(name)) {
                typedTarget.nextIn = (byte[]) value;
                return;
            }
            if ("nextInIndex".equals(name)) {
                typedTarget.nextInIndex = ((Number) value).intValue();
                return;
            }
            if ("nextOut".equals(name)) {
                typedTarget.nextOut = (byte[]) value;
                return;
            }
            if ("nextOutIndex".equals(name)) {
                typedTarget.nextOutIndex = ((Number) value).intValue();
                return;
            }
            if ("totalIn".equals(name)) {
                typedTarget.totalIn = ((Number) value).longValue();
                return;
            }
            if ("totalOut".equals(name)) {
                typedTarget.totalOut = ((Number) value).longValue();
                return;
            }
        }
        if (target instanceof com.codename1.io.gzip.Inflater) {
            com.codename1.io.gzip.Inflater typedTarget = (com.codename1.io.gzip.Inflater) target;
            if ("availIn".equals(name)) {
                typedTarget.availIn = ((Number) value).intValue();
                return;
            }
            if ("availOut".equals(name)) {
                typedTarget.availOut = ((Number) value).intValue();
                return;
            }
            if ("msg".equals(name)) {
                typedTarget.msg = (java.lang.String) value;
                return;
            }
            if ("nextIn".equals(name)) {
                typedTarget.nextIn = (byte[]) value;
                return;
            }
            if ("nextInIndex".equals(name)) {
                typedTarget.nextInIndex = ((Number) value).intValue();
                return;
            }
            if ("nextOut".equals(name)) {
                typedTarget.nextOut = (byte[]) value;
                return;
            }
            if ("nextOutIndex".equals(name)) {
                typedTarget.nextOutIndex = ((Number) value).intValue();
                return;
            }
            if ("totalIn".equals(name)) {
                typedTarget.totalIn = ((Number) value).longValue();
                return;
            }
            if ("totalOut".equals(name)) {
                typedTarget.totalOut = ((Number) value).longValue();
                return;
            }
        }
        if (target instanceof com.codename1.io.gzip.ZStream) {
            com.codename1.io.gzip.ZStream typedTarget = (com.codename1.io.gzip.ZStream) target;
            if ("availIn".equals(name)) {
                typedTarget.availIn = ((Number) value).intValue();
                return;
            }
            if ("availOut".equals(name)) {
                typedTarget.availOut = ((Number) value).intValue();
                return;
            }
            if ("msg".equals(name)) {
                typedTarget.msg = (java.lang.String) value;
                return;
            }
            if ("nextIn".equals(name)) {
                typedTarget.nextIn = (byte[]) value;
                return;
            }
            if ("nextInIndex".equals(name)) {
                typedTarget.nextInIndex = ((Number) value).intValue();
                return;
            }
            if ("nextOut".equals(name)) {
                typedTarget.nextOut = (byte[]) value;
                return;
            }
            if ("nextOutIndex".equals(name)) {
                typedTarget.nextOutIndex = ((Number) value).intValue();
                return;
            }
            if ("totalIn".equals(name)) {
                typedTarget.totalIn = ((Number) value).longValue();
                return;
            }
            if ("totalOut".equals(name)) {
                typedTarget.totalOut = ((Number) value).longValue();
                return;
            }
        }
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
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
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
            return value instanceof Number;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            return isSamInterface(type);
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
