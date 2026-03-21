package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_gzip {
    private GeneratedAccess_com_codename1_io_gzip() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.io.gzip.Adler32".equals(name)) return com.codename1.io.gzip.Adler32.class;
        if ("com.codename1.io.gzip.CRC32".equals(name)) return com.codename1.io.gzip.CRC32.class;
        if ("com.codename1.io.gzip.Deflate".equals(name)) return com.codename1.io.gzip.Deflate.class;
        if ("com.codename1.io.gzip.Deflater".equals(name)) return com.codename1.io.gzip.Deflater.class;
        if ("com.codename1.io.gzip.DeflaterOutputStream".equals(name)) return com.codename1.io.gzip.DeflaterOutputStream.class;
        if ("com.codename1.io.gzip.FilterInputStream".equals(name)) return com.codename1.io.gzip.FilterInputStream.class;
        if ("com.codename1.io.gzip.FilterOutputStream".equals(name)) return com.codename1.io.gzip.FilterOutputStream.class;
        if ("com.codename1.io.gzip.GZConnectionRequest".equals(name)) return com.codename1.io.gzip.GZConnectionRequest.class;
        if ("com.codename1.io.gzip.GZIPException".equals(name)) return com.codename1.io.gzip.GZIPException.class;
        if ("com.codename1.io.gzip.GZIPHeader".equals(name)) return com.codename1.io.gzip.GZIPHeader.class;
        if ("com.codename1.io.gzip.GZIPInputStream".equals(name)) return com.codename1.io.gzip.GZIPInputStream.class;
        if ("com.codename1.io.gzip.GZIPOutputStream".equals(name)) return com.codename1.io.gzip.GZIPOutputStream.class;
        if ("com.codename1.io.gzip.Inflater".equals(name)) return com.codename1.io.gzip.Inflater.class;
        if ("com.codename1.io.gzip.InflaterInputStream".equals(name)) return com.codename1.io.gzip.InflaterInputStream.class;
        if ("com.codename1.io.gzip.JZlib".equals(name)) return com.codename1.io.gzip.JZlib.class;
        if ("com.codename1.io.gzip.ZStream".equals(name)) return com.codename1.io.gzip.ZStream.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.gzip.Deflater.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.gzip.Deflater();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.io.gzip.Deflater(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.io.gzip.Deflater(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.io.gzip.Deflater(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.io.gzip.Deflater(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new com.codename1.io.gzip.Deflater(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if (type == com.codename1.io.gzip.GZIPException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.gzip.GZIPException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.io.gzip.GZIPException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.io.gzip.Inflater.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.io.gzip.Inflater();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return new com.codename1.io.gzip.Inflater(((Boolean) safeArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.io.gzip.Inflater(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return new com.codename1.io.gzip.Inflater(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if (type == com.codename1.io.gzip.ZStream.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.io.gzip.CRC32.getCRC32Table();
            }
        }
        throw unsupportedStatic(com.codename1.io.gzip.CRC32.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("adler32Combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                return com.codename1.io.gzip.JZlib.adler32Combine(((Number) safeArgs[0]).longValue(), ((Number) safeArgs[1]).longValue(), ((Number) safeArgs[2]).longValue());
            }
        }
        if ("adler32_combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                return com.codename1.io.gzip.JZlib.adler32_combine(((Number) safeArgs[0]).longValue(), ((Number) safeArgs[1]).longValue(), ((Number) safeArgs[2]).longValue());
            }
        }
        if ("crc32Combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                return com.codename1.io.gzip.JZlib.crc32Combine(((Number) safeArgs[0]).longValue(), ((Number) safeArgs[1]).longValue(), ((Number) safeArgs[2]).longValue());
            }
        }
        if ("crc32_combine".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                return com.codename1.io.gzip.JZlib.crc32_combine(((Number) safeArgs[0]).longValue(), ((Number) safeArgs[1]).longValue(), ((Number) safeArgs[2]).longValue());
            }
        }
        if ("version".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
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
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("getAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getCRC".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCRC();
            }
        }
        if ("getComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComment();
            }
        }
        if ("getInflater".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInflater();
            }
        }
        if ("getModifiedtime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getModifiedtime();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getOS".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOS();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.mark(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.read();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("readHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.readHeader(); return null;
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.skip(((Number) safeArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.gzip.GZIPOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flush(); return null;
            }
        }
        if ("getCRC".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCRC();
            }
        }
        if ("getDeflater".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDeflater();
            }
        }
        if ("getSyncFlush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSyncFlush();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("setComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setComment((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setModifiedTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setModifiedTime(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOS".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setOS(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setSyncFlush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSyncFlush(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.write((byte[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.gzip.Deflater typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.gzip.Deflater.class}, false)) {
                return typedTarget.copy((com.codename1.io.gzip.Deflater) safeArgs[0]);
            }
        }
        if ("deflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deflate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("deflateEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.deflateEnd();
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("deflateParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateParams(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateSetDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("end".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.end();
            }
        }
        if ("finished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.finished();
            }
        }
        if ("free".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.free(); return null;
            }
        }
        if ("getAdler".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAdler();
            }
        }
        if ("getAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailOut();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextIn();
            }
        }
        if ("getNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextInIndex();
            }
        }
        if ("getNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextOut();
            }
        }
        if ("getNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextOutIndex();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.inflate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("inflateEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateEnd();
            }
        }
        if ("inflateFinished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateFinished();
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateInit();
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.inflateInit(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.inflateInit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.inflateInit(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("inflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.inflateSetDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("inflateSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateSync();
            }
        }
        if ("inflateSyncPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateSyncPoint();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue());
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("params".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.params(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("setAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAvailIn(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAvailOut(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.setDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("setNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setNextIn((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setNextInIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setNextOut((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setNextOutIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setOutput((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setOutput((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.io.gzip.DeflaterOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flush(); return null;
            }
        }
        if ("getDeflater".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDeflater();
            }
        }
        if ("getSyncFlush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSyncFlush();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("setSyncFlush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSyncFlush(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.write((byte[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.io.gzip.Inflater typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("deflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deflate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("deflateEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.deflateEnd();
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("deflateParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateParams(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateSetDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("end".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.end();
            }
        }
        if ("finished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.finished();
            }
        }
        if ("free".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.free(); return null;
            }
        }
        if ("getAdler".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAdler();
            }
        }
        if ("getAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailOut();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextIn();
            }
        }
        if ("getNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextInIndex();
            }
        }
        if ("getNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextOut();
            }
        }
        if ("getNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextOutIndex();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.inflate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("inflateEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateEnd();
            }
        }
        if ("inflateFinished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateFinished();
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateInit();
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.inflateInit(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.inflateInit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.inflateInit(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("inflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.inflateSetDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("inflateSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateSync();
            }
        }
        if ("inflateSyncPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateSyncPoint();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.init();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.init(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue());
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.init(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("setAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAvailIn(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAvailOut(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.setDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("setNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setNextIn((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setNextInIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setNextOut((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setNextOutIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setOutput((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setOutput((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.sync();
            }
        }
        if ("syncPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.syncPoint();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.io.gzip.InflaterInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("getAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getInflater".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInflater();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.mark(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.read();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("readHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.readHeader(); return null;
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.skip(((Number) safeArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.io.gzip.Adler32 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.copy();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValue();
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.reset(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.update((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.io.gzip.CRC32 typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.copy();
            }
        }
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValue();
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.reset(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.update((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.io.gzip.FilterInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("available".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.available();
            }
        }
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.mark(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.read();
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.skip(((Number) safeArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.io.gzip.FilterOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.close(); return null;
            }
        }
        if ("flush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flush(); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.write((byte[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.io.gzip.GZConnectionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (byte[]) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgument((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncoding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, false)) {
                typedTarget.addArgumentNoEncoding((java.lang.String) safeArgs[0], (java.lang.String[]) safeArgs[1]); return null;
            }
        }
        if ("addArgumentNoEncodingArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArgumentNoEncodingArray((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                typedTarget.addArguments((java.lang.String) safeArgs[0], varArgs); return null;
            }
        }
        if ("addExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addRequestHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.addRequestHeader((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("addResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("canGetSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canGetSSLCertificates();
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToFileSystem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToFileSystem((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0]);
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2]); return null;
            }
        }
        if ("downloadImageToStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.util.SuccessCallback.class, com.codename1.util.FailureCallback.class, java.lang.Boolean.class}, false)) {
                typedTarget.downloadImageToStorage((java.lang.String) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1], (com.codename1.util.FailureCallback) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCacheMode();
            }
        }
        if ("getContentLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentLength();
            }
        }
        if ("getContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getContentType();
            }
        }
        if ("getDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationFile();
            }
        }
        if ("getDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDestinationStorage();
            }
        }
        if ("getDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisposeOnCompletion();
            }
        }
        if ("getHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHttpMethod();
            }
        }
        if ("getPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPriority();
            }
        }
        if ("getReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReadTimeout();
            }
        }
        if ("getRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBody();
            }
        }
        if ("getRequestBodyData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRequestBodyData();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseCode();
            }
        }
        if ("getResponseContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseContentType();
            }
        }
        if ("getResponseData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseData();
            }
        }
        if ("getResponseErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResponseErrorMessage();
            }
        }
        if ("getResposeCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResposeCode();
            }
        }
        if ("getSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSSLCertificates();
            }
        }
        if ("getShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShowOnInit();
            }
        }
        if ("getSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSilentRetryCount();
            }
        }
        if ("getTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeout();
            }
        }
        if ("getUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrl();
            }
        }
        if ("getUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUserAgent();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("ioStreamUpdate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                typedTarget.ioStreamUpdate((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("isCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCheckSSLCertificates();
            }
        }
        if ("isCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCookiesEnabled();
            }
        }
        if ("isDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDuplicateSupported();
            }
        }
        if ("isFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFailSilently();
            }
        }
        if ("isFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFollowRedirects();
            }
        }
        if ("isInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isInsecure();
            }
        }
        if ("isPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPost();
            }
        }
        if ("isReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadRequest();
            }
        }
        if ("isReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReadResponseForErrors();
            }
        }
        if ("isRedirecting".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRedirecting();
            }
        }
        if ("isWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isWriteRequest();
            }
        }
        if ("kill".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.kill(); return null;
            }
        }
        if ("onRedirect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.onRedirect((java.lang.String) safeArgs[0]);
            }
        }
        if ("pause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pause();
            }
        }
        if ("purgeCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.purgeCache(); return null;
            }
        }
        if ("removeAllArguments".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllArguments(); return null;
            }
        }
        if ("removeArgument".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.removeArgument((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("removeExceptionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeExceptionListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("removeResponseListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeResponseListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.resume();
            }
        }
        if ("retry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.retry(); return null;
            }
        }
        if ("setCacheMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.ConnectionRequest.CachingMode.class}, false)) {
                typedTarget.setCacheMode((com.codename1.io.ConnectionRequest.CachingMode) safeArgs[0]); return null;
            }
        }
        if ("setCheckSSLCertificates".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCheckSSLCertificates(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setChunkedStreamingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setChunkedStreamingMode(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setContentType((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCookiesEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setCookiesEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDestinationFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationFile((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDestinationStorage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDestinationStorage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisposeOnCompletion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setDisposeOnCompletion((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setDuplicateSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDuplicateSupported(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFailSilently".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFailSilently(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFollowRedirects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFollowRedirects(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHttpMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setHttpMethod((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setInsecure".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setInsecure(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPost(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPriority(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setReadRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadResponseForErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReadResponseForErrors(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReadTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setReadTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.Data.class}, false)) {
                typedTarget.setRequestBody((com.codename1.io.Data) safeArgs[0]); return null;
            }
        }
        if ("setRequestBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRequestBody((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setShowOnInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Dialog.class}, false)) {
                typedTarget.setShowOnInit((com.codename1.ui.Dialog) safeArgs[0]); return null;
            }
        }
        if ("setSilentRetryCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSilentRetryCount(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTimeout(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUserAgent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setUserAgent((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setWriteRequest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setWriteRequest(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.io.gzip.GZIPException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.addSuppressed((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return typedTarget.initCause((java.lang.Throwable) safeArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.io.gzip.GZIPHeader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCRC".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCRC();
            }
        }
        if ("getComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getComment();
            }
        }
        if ("getModifiedTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getModifiedTime();
            }
        }
        if ("getName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getName();
            }
        }
        if ("getOS".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOS();
            }
        }
        if ("setCRC".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setCRC(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setComment((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setModifiedTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setModifiedTime(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOS".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setOS(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.io.gzip.ZStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("deflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deflate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("deflateEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.deflateEnd();
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("deflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateInit(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("deflateParams".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateParams(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.deflateSetDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("end".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.end();
            }
        }
        if ("finished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.finished();
            }
        }
        if ("free".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.free(); return null;
            }
        }
        if ("getAdler".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAdler();
            }
        }
        if ("getAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailIn();
            }
        }
        if ("getAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAvailOut();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextIn();
            }
        }
        if ("getNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextInIndex();
            }
        }
        if ("getNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextOut();
            }
        }
        if ("getNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNextOutIndex();
            }
        }
        if ("getTotalIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalIn();
            }
        }
        if ("getTotalOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTotalOut();
            }
        }
        if ("inflate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.inflate(((Number) safeArgs[0]).intValue());
            }
        }
        if ("inflateEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateEnd();
            }
        }
        if ("inflateFinished".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateFinished();
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateInit();
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.inflateInit(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.inflateInit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("inflateInit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.inflateInit(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("inflateSetDictionary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return typedTarget.inflateSetDictionary((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("inflateSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateSync();
            }
        }
        if ("inflateSyncPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.inflateSyncPoint();
            }
        }
        if ("setAvailIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAvailIn(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAvailOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAvailOut(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Boolean.class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setInput((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("setNextIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setNextIn((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setNextInIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setNextInIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setNextOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setNextOut((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setNextOutIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setNextOutIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.setOutput((byte[]) safeArgs[0]); return null;
            }
        }
        if ("setOutput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setOutput((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
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
