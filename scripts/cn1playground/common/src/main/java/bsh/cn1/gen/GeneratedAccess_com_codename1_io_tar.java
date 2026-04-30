package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_io_tar {
    private GeneratedAccess_com_codename1_io_tar() {
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
        if ("Octal".equals(simpleName)) {
            return com.codename1.io.tar.Octal.class;
        }
        if ("TarConstants".equals(simpleName)) {
            return com.codename1.io.tar.TarConstants.class;
        }
        if ("TarEntry".equals(simpleName)) {
            return com.codename1.io.tar.TarEntry.class;
        }
        if ("TarHeader".equals(simpleName)) {
            return com.codename1.io.tar.TarHeader.class;
        }
        if ("TarInputStream".equals(simpleName)) {
            return com.codename1.io.tar.TarInputStream.class;
        }
        if ("TarOutputStream".equals(simpleName)) {
            return com.codename1.io.tar.TarOutputStream.class;
        }
        if ("TarUtils".equals(simpleName)) {
            return com.codename1.io.tar.TarUtils.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.tar.TarEntry.class) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return new com.codename1.io.tar.TarEntry((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.io.tar.TarEntry((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.io.tar.TarHeader.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.io.tar.TarHeader();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.io.tar.Octal.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.io.tar.TarHeader.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.io.tar.TarUtils.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getCheckSumOctalBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.io.tar.Octal.getCheckSumOctalBytes(((Number) adaptedArgs[0]).longValue(), (byte[]) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("getLongOctalBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.io.tar.Octal.getLongOctalBytes(((Number) adaptedArgs[0]).longValue(), (byte[]) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("getOctalBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.io.tar.Octal.getOctalBytes(((Number) adaptedArgs[0]).longValue(), (byte[]) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("parseOctal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.io.tar.Octal.parseOctal((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        throw unsupportedStatic(com.codename1.io.tar.Octal.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getNameBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StringBuffer.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StringBuffer.class, byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.io.tar.TarHeader.getNameBytes((java.lang.StringBuffer) adaptedArgs[0], (byte[]) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("parseName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.io.tar.TarHeader.parseName((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        throw unsupportedStatic(com.codename1.io.tar.TarHeader.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("calculateTarSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.io.tar.TarUtils.calculateTarSize((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.io.tar.TarUtils.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.io.tar.TarEntry) {
            try {
                return invoke0((com.codename1.io.tar.TarEntry) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.tar.TarInputStream) {
            try {
                return invoke1((com.codename1.io.tar.TarInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.io.tar.TarOutputStream) {
            try {
                return invoke2((com.codename1.io.tar.TarOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.io.tar.TarEntry typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("computeCheckSum".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.computeCheckSum((byte[]) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.tar.TarEntry.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.tar.TarEntry.class}, false);
                return typedTarget.equals((com.codename1.io.tar.TarEntry) adaptedArgs[0]);
            }
        }
        if ("extractTarHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.extractTarHeader((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("getFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFile();
            }
        }
        if ("getGroupId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGroupId();
            }
        }
        if ("getGroupName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGroupName();
            }
        }
        if ("getHeader".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeader();
            }
        }
        if ("getModTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModTime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("getUserId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserId();
            }
        }
        if ("getUserName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUserName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isDescendent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.tar.TarEntry.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.tar.TarEntry.class}, false);
                return typedTarget.isDescendent((com.codename1.io.tar.TarEntry) adaptedArgs[0]);
            }
        }
        if ("isDirectory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDirectory();
            }
        }
        if ("parseTarHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.parseTarHeader((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("setGroupId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setGroupId(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setGroupName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setGroupName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setIds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setModTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setModTime(((Number) adaptedArgs[0]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setModTime((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setSize(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setUserId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setUserId(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setUserName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUserName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("writeEntryHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.writeEntryHeader((byte[]) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.io.tar.TarInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getConnection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnection();
            }
        }
        if ("getLastActivityTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastActivityTime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNextEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextEntry();
            }
        }
        if ("getTotalBytesRead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalBytesRead();
            }
        }
        if ("getYield".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYield();
            }
        }
        if ("isDefaultSkip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultSkip();
            }
        }
        if ("isDisableBuffering".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisableBuffering();
            }
        }
        if ("isPrintInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrintInput();
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(toIntValue(adaptedArgs[0])); return null;
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
                return typedTarget.read((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("setConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setConnection((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultSkip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultSkip(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisableBuffering".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisableBuffering(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPrintInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPrintInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false);
                typedTarget.setProgressListener((com.codename1.io.IOProgressListener) adaptedArgs[0]); return null;
            }
        }
        if ("setYield".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setYield(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.io.tar.TarOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("flushBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushBuffer(); return null;
            }
        }
        if ("getConnection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConnection();
            }
        }
        if ("getLastActivityTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastActivityTime();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getTotalBytesWritten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalBytesWritten();
            }
        }
        if ("putNextEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.tar.TarEntry.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.tar.TarEntry.class}, false);
                typedTarget.putNextEntry((com.codename1.io.tar.TarEntry) adaptedArgs[0]); return null;
            }
        }
        if ("setConnection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setConnection((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setProgressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.IOProgressListener.class}, false);
                typedTarget.setProgressListener((com.codename1.io.IOProgressListener) adaptedArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.write(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.write((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.write((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.io.tar.TarConstants.class) return getStaticField0(name);
        if (type == com.codename1.io.tar.TarHeader.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DATA_BLOCK".equals(name)) return com.codename1.io.tar.TarConstants.DATA_BLOCK;
        if ("EOF_BLOCK".equals(name)) return com.codename1.io.tar.TarConstants.EOF_BLOCK;
        if ("HEADER_BLOCK".equals(name)) return com.codename1.io.tar.TarConstants.HEADER_BLOCK;
        throw unsupportedStaticField(com.codename1.io.tar.TarConstants.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CHKSUMLEN".equals(name)) return com.codename1.io.tar.TarHeader.CHKSUMLEN;
        if ("DEVLEN".equals(name)) return com.codename1.io.tar.TarHeader.DEVLEN;
        if ("GIDLEN".equals(name)) return com.codename1.io.tar.TarHeader.GIDLEN;
        if ("GNAMELEN".equals(name)) return com.codename1.io.tar.TarHeader.GNAMELEN;
        if ("GNU_TMAGIC".equals(name)) return com.codename1.io.tar.TarHeader.GNU_TMAGIC;
        if ("LF_BLK".equals(name)) return com.codename1.io.tar.TarHeader.LF_BLK;
        if ("LF_CHR".equals(name)) return com.codename1.io.tar.TarHeader.LF_CHR;
        if ("LF_CONTIG".equals(name)) return com.codename1.io.tar.TarHeader.LF_CONTIG;
        if ("LF_DIR".equals(name)) return com.codename1.io.tar.TarHeader.LF_DIR;
        if ("LF_FIFO".equals(name)) return com.codename1.io.tar.TarHeader.LF_FIFO;
        if ("LF_LINK".equals(name)) return com.codename1.io.tar.TarHeader.LF_LINK;
        if ("LF_NORMAL".equals(name)) return com.codename1.io.tar.TarHeader.LF_NORMAL;
        if ("LF_OLDNORM".equals(name)) return com.codename1.io.tar.TarHeader.LF_OLDNORM;
        if ("LF_SYMLINK".equals(name)) return com.codename1.io.tar.TarHeader.LF_SYMLINK;
        if ("MAGICLEN".equals(name)) return com.codename1.io.tar.TarHeader.MAGICLEN;
        if ("MODELEN".equals(name)) return com.codename1.io.tar.TarHeader.MODELEN;
        if ("MODTIMELEN".equals(name)) return com.codename1.io.tar.TarHeader.MODTIMELEN;
        if ("NAMELEN".equals(name)) return com.codename1.io.tar.TarHeader.NAMELEN;
        if ("SIZELEN".equals(name)) return com.codename1.io.tar.TarHeader.SIZELEN;
        if ("TMAGIC".equals(name)) return com.codename1.io.tar.TarHeader.TMAGIC;
        if ("UIDLEN".equals(name)) return com.codename1.io.tar.TarHeader.UIDLEN;
        if ("UNAMELEN".equals(name)) return com.codename1.io.tar.TarHeader.UNAMELEN;
        throw unsupportedStaticField(com.codename1.io.tar.TarHeader.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.io.tar.TarHeader) {
            com.codename1.io.tar.TarHeader typedTarget = (com.codename1.io.tar.TarHeader) target;
            if ("checkSum".equals(name)) return typedTarget.checkSum;
            if ("devMajor".equals(name)) return typedTarget.devMajor;
            if ("devMinor".equals(name)) return typedTarget.devMinor;
            if ("groupId".equals(name)) return typedTarget.groupId;
            if ("groupName".equals(name)) return typedTarget.groupName;
            if ("linkFlag".equals(name)) return typedTarget.linkFlag;
            if ("linkName".equals(name)) return typedTarget.linkName;
            if ("magic".equals(name)) return typedTarget.magic;
            if ("modTime".equals(name)) return typedTarget.modTime;
            if ("mode".equals(name)) return typedTarget.mode;
            if ("name".equals(name)) return typedTarget.name;
            if ("size".equals(name)) return typedTarget.size;
            if ("userId".equals(name)) return typedTarget.userId;
            if ("userName".equals(name)) return typedTarget.userName;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.io.tar.TarHeader) {
            com.codename1.io.tar.TarHeader typedTarget = (com.codename1.io.tar.TarHeader) target;
            if ("checkSum".equals(name)) {
                typedTarget.checkSum = toIntValue(value);
                return;
            }
            if ("devMajor".equals(name)) {
                typedTarget.devMajor = toIntValue(value);
                return;
            }
            if ("devMinor".equals(name)) {
                typedTarget.devMinor = toIntValue(value);
                return;
            }
            if ("groupId".equals(name)) {
                typedTarget.groupId = toIntValue(value);
                return;
            }
            if ("groupName".equals(name)) {
                typedTarget.groupName = (java.lang.StringBuffer) value;
                return;
            }
            if ("linkFlag".equals(name)) {
                typedTarget.linkFlag = (byte) toIntValue(value);
                return;
            }
            if ("linkName".equals(name)) {
                typedTarget.linkName = (java.lang.StringBuffer) value;
                return;
            }
            if ("magic".equals(name)) {
                typedTarget.magic = (java.lang.StringBuffer) value;
                return;
            }
            if ("modTime".equals(name)) {
                typedTarget.modTime = ((Number) value).longValue();
                return;
            }
            if ("mode".equals(name)) {
                typedTarget.mode = toIntValue(value);
                return;
            }
            if ("name".equals(name)) {
                typedTarget.name = (java.lang.StringBuffer) value;
                return;
            }
            if ("size".equals(name)) {
                typedTarget.size = ((Number) value).longValue();
                return;
            }
            if ("userId".equals(name)) {
                typedTarget.userId = toIntValue(value);
                return;
            }
            if ("userName".equals(name)) {
                typedTarget.userName = (java.lang.StringBuffer) value;
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
