package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_io {
    private GeneratedAccess_java_io() {
    }

    public static Class<?> findClass(String name) {
        if ("java.io.ByteArrayInputStream".equals(name)) return java.io.ByteArrayInputStream.class;
        if ("java.io.ByteArrayOutputStream".equals(name)) return java.io.ByteArrayOutputStream.class;
        if ("java.io.DataInput".equals(name)) return java.io.DataInput.class;
        if ("java.io.DataInputStream".equals(name)) return java.io.DataInputStream.class;
        if ("java.io.DataOutput".equals(name)) return java.io.DataOutput.class;
        if ("java.io.DataOutputStream".equals(name)) return java.io.DataOutputStream.class;
        if ("java.io.EOFException".equals(name)) return java.io.EOFException.class;
        if ("java.io.Flushable".equals(name)) return java.io.Flushable.class;
        if ("java.io.IOException".equals(name)) return java.io.IOException.class;
        if ("java.io.InputStream".equals(name)) return java.io.InputStream.class;
        if ("java.io.InputStreamReader".equals(name)) return java.io.InputStreamReader.class;
        if ("java.io.InterruptedIOException".equals(name)) return java.io.InterruptedIOException.class;
        if ("java.io.OutputStream".equals(name)) return java.io.OutputStream.class;
        if ("java.io.OutputStreamWriter".equals(name)) return java.io.OutputStreamWriter.class;
        if ("java.io.PrintStream".equals(name)) return java.io.PrintStream.class;
        if ("java.io.Reader".equals(name)) return java.io.Reader.class;
        if ("java.io.Serializable".equals(name)) return java.io.Serializable.class;
        if ("java.io.StringReader".equals(name)) return java.io.StringReader.class;
        if ("java.io.StringWriter".equals(name)) return java.io.StringWriter.class;
        if ("java.io.UTFDataFormatException".equals(name)) return java.io.UTFDataFormatException.class;
        if ("java.io.UnsupportedEncodingException".equals(name)) return java.io.UnsupportedEncodingException.class;
        if ("java.io.Writer".equals(name)) return java.io.Writer.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.io.ByteArrayInputStream.class) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return new java.io.ByteArrayInputStream((byte[]) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new java.io.ByteArrayInputStream((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if (type == java.io.ByteArrayOutputStream.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.ByteArrayOutputStream();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.io.ByteArrayOutputStream(((Number) safeArgs[0]).intValue());
            }
        }
        if (type == java.io.EOFException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.EOFException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.io.EOFException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == java.io.IOException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.IOException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.io.IOException((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return new java.io.IOException((java.lang.Throwable) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                return new java.io.IOException((java.lang.String) safeArgs[0], (java.lang.Throwable) safeArgs[1]);
            }
        }
        if (type == java.io.InterruptedIOException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.InterruptedIOException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.io.InterruptedIOException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == java.io.StringReader.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.io.StringReader((java.lang.String) safeArgs[0]);
            }
        }
        if (type == java.io.StringWriter.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.StringWriter();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.io.StringWriter(((Number) safeArgs[0]).intValue());
            }
        }
        if (type == java.io.UTFDataFormatException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.UTFDataFormatException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.io.UTFDataFormatException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == java.io.UnsupportedEncodingException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.io.UnsupportedEncodingException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.io.UnsupportedEncodingException((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedStatic(type, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof java.io.ByteArrayInputStream) {
            try {
                return invoke0((java.io.ByteArrayInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.ByteArrayOutputStream) {
            try {
                return invoke1((java.io.ByteArrayOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.DataInputStream) {
            try {
                return invoke2((java.io.DataInputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.DataOutputStream) {
            try {
                return invoke3((java.io.DataOutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.EOFException) {
            try {
                return invoke4((java.io.EOFException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.InputStreamReader) {
            try {
                return invoke5((java.io.InputStreamReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.InterruptedIOException) {
            try {
                return invoke6((java.io.InterruptedIOException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.OutputStreamWriter) {
            try {
                return invoke7((java.io.OutputStreamWriter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.PrintStream) {
            try {
                return invoke8((java.io.PrintStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.StringReader) {
            try {
                return invoke9((java.io.StringReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.StringWriter) {
            try {
                return invoke10((java.io.StringWriter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.UTFDataFormatException) {
            try {
                return invoke11((java.io.UTFDataFormatException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.UnsupportedEncodingException) {
            try {
                return invoke12((java.io.UnsupportedEncodingException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.IOException) {
            try {
                return invoke13((java.io.IOException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.InputStream) {
            try {
                return invoke14((java.io.InputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.OutputStream) {
            try {
                return invoke15((java.io.OutputStream) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.Reader) {
            try {
                return invoke16((java.io.Reader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.Writer) {
            try {
                return invoke17((java.io.Writer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.DataInput) {
            try {
                return invoke18((java.io.DataInput) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.DataOutput) {
            try {
                return invoke19((java.io.DataOutput) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.io.Flushable) {
            try {
                return invoke20((java.io.Flushable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.io.ByteArrayInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke1(java.io.ByteArrayOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.reset(); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toByteArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toByteArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
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

    private static Object invoke2(java.io.DataInputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("readBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readBoolean();
            }
        }
        if ("readByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readByte();
            }
        }
        if ("readChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readChar();
            }
        }
        if ("readDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readDouble();
            }
        }
        if ("readFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readFloat();
            }
        }
        if ("readFully".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.readFully((byte[]) safeArgs[0]); return null;
            }
        }
        if ("readFully".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.readFully((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("readInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readInt();
            }
        }
        if ("readLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readLong();
            }
        }
        if ("readShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readShort();
            }
        }
        if ("readUTF".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readUTF();
            }
        }
        if ("readUnsignedByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readUnsignedByte();
            }
        }
        if ("readUnsignedShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readUnsignedShort();
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
        if ("skipBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.skipBytes(((Number) safeArgs[0]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(java.io.DataOutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("writeBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.writeBoolean(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("writeByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeByte(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeChar(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeChars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.writeChars((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("writeDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.writeDouble(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("writeFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.writeFloat(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("writeInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeInt(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.writeLong(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("writeShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeShort(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeUTF".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.writeUTF((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(java.io.EOFException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke5(java.io.InputStreamReader typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return typedTarget.read((char[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.ready();
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

    private static Object invoke6(java.io.InterruptedIOException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke7(java.io.OutputStreamWriter typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                typedTarget.write((char[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.write((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(java.io.PrintStream typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("checkError".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.checkError();
            }
        }
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
        if ("print".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.print(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("print".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.print(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("print".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.print(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("print".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.print((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("print".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.print((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("print".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.print(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.println(); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.println(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.println(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.println(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.println(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.println(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.println((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.println((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("println".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.println(((Number) safeArgs[0]).longValue()); return null;
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

    private static Object invoke9(java.io.StringReader typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return typedTarget.read((char[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.ready();
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

    private static Object invoke10(java.io.StringWriter typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("getBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBuffer();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                typedTarget.write((char[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.write((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(java.io.UTFDataFormatException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke12(java.io.UnsupportedEncodingException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke13(java.io.IOException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke14(java.io.InputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke15(java.io.OutputStream typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke16(java.io.Reader typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return typedTarget.read((char[]) safeArgs[0]);
            }
        }
        if ("read".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.read((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.ready();
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

    private static Object invoke17(java.io.Writer typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                typedTarget.write((char[]) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.write(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.write((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.write((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(java.io.DataInput typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("readBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readBoolean();
            }
        }
        if ("readByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readByte();
            }
        }
        if ("readChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readChar();
            }
        }
        if ("readDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readDouble();
            }
        }
        if ("readFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readFloat();
            }
        }
        if ("readFully".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.readFully((byte[]) safeArgs[0]); return null;
            }
        }
        if ("readFully".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.readFully((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("readInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readInt();
            }
        }
        if ("readLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readLong();
            }
        }
        if ("readShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readShort();
            }
        }
        if ("readUTF".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readUTF();
            }
        }
        if ("readUnsignedByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readUnsignedByte();
            }
        }
        if ("readUnsignedShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.readUnsignedShort();
            }
        }
        if ("skipBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.skipBytes(((Number) safeArgs[0]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(java.io.DataOutput typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("writeBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.writeBoolean(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("writeByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeByte(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeChar(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeChars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.writeChars((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("writeDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                typedTarget.writeDouble(((Number) safeArgs[0]).doubleValue()); return null;
            }
        }
        if ("writeFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.writeFloat(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("writeInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeInt(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.writeLong(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("writeShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.writeShort(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("writeUTF".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.writeUTF((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(java.io.Flushable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("flush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flush(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof java.io.InterruptedIOException) {
            java.io.InterruptedIOException typedTarget = (java.io.InterruptedIOException) target;
            if ("bytesTransferred".equals(name)) return typedTarget.bytesTransferred;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof java.io.InterruptedIOException) {
            java.io.InterruptedIOException typedTarget = (java.io.InterruptedIOException) target;
            if ("bytesTransferred".equals(name)) {
                typedTarget.bytesTransferred = ((Number) value).intValue();
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
