package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_lang {
    private GeneratedAccess_java_lang() {
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
        if ("Appendable".equals(simpleName)) {
            return java.lang.Appendable.class;
        }
        if ("ArithmeticException".equals(simpleName)) {
            return java.lang.ArithmeticException.class;
        }
        if ("ArrayIndexOutOfBoundsException".equals(simpleName)) {
            return java.lang.ArrayIndexOutOfBoundsException.class;
        }
        if ("ArrayStoreException".equals(simpleName)) {
            return java.lang.ArrayStoreException.class;
        }
        if ("AssertionError".equals(simpleName)) {
            return java.lang.AssertionError.class;
        }
        if ("AutoCloseable".equals(simpleName)) {
            return java.lang.AutoCloseable.class;
        }
        if ("Boolean".equals(simpleName)) {
            return java.lang.Boolean.class;
        }
        if ("Byte".equals(simpleName)) {
            return java.lang.Byte.class;
        }
        if ("CharSequence".equals(simpleName)) {
            return java.lang.CharSequence.class;
        }
        if ("Character".equals(simpleName)) {
            return java.lang.Character.class;
        }
        if ("Class".equals(simpleName)) {
            return java.lang.Class.class;
        }
        if ("ClassCastException".equals(simpleName)) {
            return java.lang.ClassCastException.class;
        }
        if ("ClassLoader".equals(simpleName)) {
            return java.lang.ClassLoader.class;
        }
        if ("ClassNotFoundException".equals(simpleName)) {
            return java.lang.ClassNotFoundException.class;
        }
        if ("CloneNotSupportedException".equals(simpleName)) {
            return java.lang.CloneNotSupportedException.class;
        }
        if ("Cloneable".equals(simpleName)) {
            return java.lang.Cloneable.class;
        }
        if ("Comparable".equals(simpleName)) {
            return java.lang.Comparable.class;
        }
        if ("Deprecated".equals(simpleName)) {
            return java.lang.Deprecated.class;
        }
        if ("Double".equals(simpleName)) {
            return java.lang.Double.class;
        }
        if ("Enum".equals(simpleName)) {
            return java.lang.Enum.class;
        }
        if ("Error".equals(simpleName)) {
            return java.lang.Error.class;
        }
        if ("Exception".equals(simpleName)) {
            return java.lang.Exception.class;
        }
        if ("Float".equals(simpleName)) {
            return java.lang.Float.class;
        }
        if ("IllegalAccessException".equals(simpleName)) {
            return java.lang.IllegalAccessException.class;
        }
        if ("IllegalArgumentException".equals(simpleName)) {
            return java.lang.IllegalArgumentException.class;
        }
        if ("IllegalMonitorStateException".equals(simpleName)) {
            return java.lang.IllegalMonitorStateException.class;
        }
        if ("IllegalStateException".equals(simpleName)) {
            return java.lang.IllegalStateException.class;
        }
        if ("IncompatibleClassChangeError".equals(simpleName)) {
            return java.lang.IncompatibleClassChangeError.class;
        }
        if ("IndexOutOfBoundsException".equals(simpleName)) {
            return java.lang.IndexOutOfBoundsException.class;
        }
        if ("InstantiationException".equals(simpleName)) {
            return java.lang.InstantiationException.class;
        }
        if ("Integer".equals(simpleName)) {
            return java.lang.Integer.class;
        }
        if ("InterruptedException".equals(simpleName)) {
            return java.lang.InterruptedException.class;
        }
        if ("Iterable".equals(simpleName)) {
            return java.lang.Iterable.class;
        }
        if ("LinkageError".equals(simpleName)) {
            return java.lang.LinkageError.class;
        }
        if ("Long".equals(simpleName)) {
            return java.lang.Long.class;
        }
        if ("Math".equals(simpleName)) {
            return java.lang.Math.class;
        }
        if ("NegativeArraySizeException".equals(simpleName)) {
            return java.lang.NegativeArraySizeException.class;
        }
        if ("NoClassDefFoundError".equals(simpleName)) {
            return java.lang.NoClassDefFoundError.class;
        }
        if ("NoSuchFieldError".equals(simpleName)) {
            return java.lang.NoSuchFieldError.class;
        }
        if ("NullPointerException".equals(simpleName)) {
            return java.lang.NullPointerException.class;
        }
        if ("Number".equals(simpleName)) {
            return java.lang.Number.class;
        }
        if ("NumberFormatException".equals(simpleName)) {
            return java.lang.NumberFormatException.class;
        }
        if ("Object".equals(simpleName)) {
            return java.lang.Object.class;
        }
        if ("OutOfMemoryError".equals(simpleName)) {
            return java.lang.OutOfMemoryError.class;
        }
        if ("Override".equals(simpleName)) {
            return java.lang.Override.class;
        }
        if ("Runnable".equals(simpleName)) {
            return java.lang.Runnable.class;
        }
        if ("Runtime".equals(simpleName)) {
            return java.lang.Runtime.class;
        }
        if ("RuntimeException".equals(simpleName)) {
            return java.lang.RuntimeException.class;
        }
        if ("SafeVarargs".equals(simpleName)) {
            return java.lang.SafeVarargs.class;
        }
        if ("SecurityException".equals(simpleName)) {
            return java.lang.SecurityException.class;
        }
        if ("Short".equals(simpleName)) {
            return java.lang.Short.class;
        }
        if ("StackTraceElement".equals(simpleName)) {
            return java.lang.StackTraceElement.class;
        }
        if ("String".equals(simpleName)) {
            return java.lang.String.class;
        }
        if ("StringBuffer".equals(simpleName)) {
            return java.lang.StringBuffer.class;
        }
        if ("StringBuilder".equals(simpleName)) {
            return java.lang.StringBuilder.class;
        }
        if ("StringIndexOutOfBoundsException".equals(simpleName)) {
            return java.lang.StringIndexOutOfBoundsException.class;
        }
        if ("System".equals(simpleName)) {
            return java.lang.System.class;
        }
        if ("Thread".equals(simpleName)) {
            return java.lang.Thread.class;
        }
        if ("ThreadLocal".equals(simpleName)) {
            return java.lang.ThreadLocal.class;
        }
        if ("Throwable".equals(simpleName)) {
            return java.lang.Throwable.class;
        }
        if ("UnsupportedOperationException".equals(simpleName)) {
            return java.lang.UnsupportedOperationException.class;
        }
        if ("VirtualMachineError".equals(simpleName)) {
            return java.lang.VirtualMachineError.class;
        }
        if ("Void".equals(simpleName)) {
            return java.lang.Void.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.lang.ArithmeticException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.ArithmeticException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.ArithmeticException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.ArrayIndexOutOfBoundsException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.ArrayIndexOutOfBoundsException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.lang.ArrayIndexOutOfBoundsException(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.ArrayIndexOutOfBoundsException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.ArrayStoreException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.ArrayStoreException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.ArrayStoreException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.AssertionError.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.AssertionError();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return new java.lang.AssertionError(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return new java.lang.AssertionError(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return new java.lang.AssertionError(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return new java.lang.AssertionError(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.lang.AssertionError(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return new java.lang.AssertionError(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return new java.lang.AssertionError((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new java.lang.AssertionError((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == java.lang.Boolean.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return new java.lang.Boolean(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if (type == java.lang.Byte.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                return new java.lang.Byte((byte) toIntValue(adaptedArgs[0]));
            }
        }
        if (type == java.lang.Character.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return new java.lang.Character(((Character) adaptedArgs[0]).charValue());
            }
        }
        if (type == java.lang.ClassCastException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.ClassCastException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.ClassCastException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.ClassNotFoundException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.ClassNotFoundException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.ClassNotFoundException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.CloneNotSupportedException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.CloneNotSupportedException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.CloneNotSupportedException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Double.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return new java.lang.Double(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if (type == java.lang.Error.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.Error();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.Error((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Exception.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.Exception();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.Exception((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return new java.lang.Exception((java.lang.Throwable) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new java.lang.Exception((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == java.lang.Float.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return new java.lang.Float(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return new java.lang.Float(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if (type == java.lang.IllegalAccessException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.IllegalAccessException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.IllegalAccessException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.IllegalArgumentException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.IllegalArgumentException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.IllegalArgumentException((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return new java.lang.IllegalArgumentException((java.lang.Throwable) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new java.lang.IllegalArgumentException((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == java.lang.IllegalMonitorStateException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.IllegalMonitorStateException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.IllegalMonitorStateException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.IllegalStateException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.IllegalStateException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.IllegalStateException((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return new java.lang.IllegalStateException((java.lang.Throwable) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new java.lang.IllegalStateException((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == java.lang.IncompatibleClassChangeError.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.IncompatibleClassChangeError();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.IncompatibleClassChangeError((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.IndexOutOfBoundsException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.IndexOutOfBoundsException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.IndexOutOfBoundsException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.InstantiationException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.InstantiationException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.InstantiationException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Integer.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.lang.Integer(toIntValue(adaptedArgs[0]));
            }
        }
        if (type == java.lang.InterruptedException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.InterruptedException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.InterruptedException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.LinkageError.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.LinkageError();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.LinkageError((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Long.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return new java.lang.Long(((Number) adaptedArgs[0]).longValue());
            }
        }
        if (type == java.lang.NegativeArraySizeException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.NegativeArraySizeException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.NegativeArraySizeException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.NoClassDefFoundError.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.NoClassDefFoundError();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.NoClassDefFoundError((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.NoSuchFieldError.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.NoSuchFieldError();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.NoSuchFieldError((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.NullPointerException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.NullPointerException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.NullPointerException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.NumberFormatException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.NumberFormatException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.NumberFormatException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Object.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.Object();
            }
        }
        if (type == java.lang.OutOfMemoryError.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.OutOfMemoryError();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.OutOfMemoryError((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.RuntimeException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.RuntimeException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.RuntimeException((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return new java.lang.RuntimeException((java.lang.Throwable) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new java.lang.RuntimeException((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == java.lang.SecurityException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.SecurityException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.SecurityException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Short.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Short.class}, false);
                return new java.lang.Short((short) toIntValue(adaptedArgs[0]));
            }
        }
        if (type == java.lang.StackTraceElement.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false);
                return new java.lang.StackTraceElement((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], toIntValue(adaptedArgs[3]));
            }
        }
        if (type == java.lang.String.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.String();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.String((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return new java.lang.String((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return new java.lang.String((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class}, false);
                return new java.lang.String((byte[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new java.lang.String((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new java.lang.String((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return new java.lang.String((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3]);
            }
        }
        if (type == java.lang.StringBuffer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.StringBuffer();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.lang.StringBuffer(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.StringBuffer((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.StringBuilder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.StringBuilder();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.lang.StringBuilder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.StringBuilder((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return new java.lang.StringBuilder((java.lang.CharSequence) adaptedArgs[0]);
            }
        }
        if (type == java.lang.StringIndexOutOfBoundsException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.StringIndexOutOfBoundsException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.lang.StringIndexOutOfBoundsException(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.StringIndexOutOfBoundsException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.lang.Thread.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.Thread();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                return new java.lang.Thread((java.lang.Runnable) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.Thread((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class, java.lang.String.class}, false);
                return new java.lang.Thread((java.lang.Runnable) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == java.lang.ThreadLocal.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.ThreadLocal();
            }
        }
        if (type == java.lang.Throwable.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.Throwable();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.Throwable((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return new java.lang.Throwable((java.lang.Throwable) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new java.lang.Throwable((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
        }
        if (type == java.lang.UnsupportedOperationException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.lang.UnsupportedOperationException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.lang.UnsupportedOperationException((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.lang.Boolean.class) return invokeStatic0(name, safeArgs);
        if (type == java.lang.Byte.class) return invokeStatic1(name, safeArgs);
        if (type == java.lang.Character.class) return invokeStatic2(name, safeArgs);
        if (type == java.lang.Class.class) return invokeStatic3(name, safeArgs);
        if (type == java.lang.ClassLoader.class) return invokeStatic4(name, safeArgs);
        if (type == java.lang.Double.class) return invokeStatic5(name, safeArgs);
        if (type == java.lang.Enum.class) return invokeStatic6(name, safeArgs);
        if (type == java.lang.Float.class) return invokeStatic7(name, safeArgs);
        if (type == java.lang.Integer.class) return invokeStatic8(name, safeArgs);
        if (type == java.lang.Long.class) return invokeStatic9(name, safeArgs);
        if (type == java.lang.Math.class) return invokeStatic10(name, safeArgs);
        if (type == java.lang.Runtime.class) return invokeStatic11(name, safeArgs);
        if (type == java.lang.Short.class) return invokeStatic12(name, safeArgs);
        if (type == java.lang.String.class) return invokeStatic13(name, safeArgs);
        if (type == java.lang.System.class) return invokeStatic14(name, safeArgs);
        if (type == java.lang.Thread.class) return invokeStatic15(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("parseBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Boolean.parseBoolean((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return java.lang.Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Boolean.valueOf((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.lang.Boolean.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("parseByte".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Byte.parseByte((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return java.lang.Byte.parseByte((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                return java.lang.Byte.valueOf((byte) toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(java.lang.Byte.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("charCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Character.charCount(toIntValue(adaptedArgs[0]));
            }
        }
        if ("codePointAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointAt((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointAt((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointAt((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("codePointBefore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointBefore((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointBefore((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointBefore((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("codePointCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointCount((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Character.codePointCount((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("digit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false);
                return java.lang.Character.digit(((Character) adaptedArgs[0]).charValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("isDigit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isDigit(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isHighSurrogate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isHighSurrogate(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isLowSurrogate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isLowSurrogate(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isLowerCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isLowerCase(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isSpace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isSpace(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isSpaceChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isSpaceChar(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isSupplementaryCodePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Character.isSupplementaryCodePoint(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isSurrogatePair".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Character.class}, false);
                return java.lang.Character.isSurrogatePair(((Character) adaptedArgs[0]).charValue(), ((Character) adaptedArgs[1]).charValue());
            }
        }
        if ("isUpperCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isUpperCase(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isValidCodePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Character.isValidCodePoint(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isWhitespace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.isWhitespace(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Character.isWhitespace(toIntValue(adaptedArgs[0]));
            }
        }
        if ("offsetByCodePoints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Character.offsetByCodePoints((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Character.offsetByCodePoints((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]));
            }
        }
        if ("reverseBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.reverseBytes(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("toChars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Character.toChars(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class, java.lang.Integer.class}, false);
                return java.lang.Character.toChars(toIntValue(adaptedArgs[0]), (char[]) adaptedArgs[1], toIntValue(adaptedArgs[2]));
            }
        }
        if ("toCodePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Character.class}, false);
                return java.lang.Character.toCodePoint(((Character) adaptedArgs[0]).charValue(), ((Character) adaptedArgs[1]).charValue());
            }
        }
        if ("toLowerCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.toLowerCase(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("toTitleCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.toTitleCase(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Character.toTitleCase(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toUpperCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.toUpperCase(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.Character.valueOf(((Character) adaptedArgs[0]).charValue());
            }
        }
        throw unsupportedStatic(java.lang.Character.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("forName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Class.forName((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.lang.Class.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("getSystemClassLoader".equals(name)) {
            if (safeArgs.length == 0) {
                return java.lang.ClassLoader.getSystemClassLoader();
            }
        }
        throw unsupportedStatic(java.lang.ClassLoader.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return java.lang.Double.compare(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("doubleToLongBits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Double.doubleToLongBits(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isInfinite".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Double.isInfinite(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("isNaN".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Double.isNaN(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("longBitsToDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.lang.Double.longBitsToDouble(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("parseDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Double.parseDouble((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Double.toString(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Double.valueOf(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Double.valueOf((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.lang.Double.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, java.lang.String.class}, false);
                return java.lang.Enum.valueOf((java.lang.Class) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.lang.Enum.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return java.lang.Float.compare(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("floatToIntBits".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Float.floatToIntBits(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("intBitsToFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Float.intBitsToFloat(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isInfinite".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Float.isInfinite(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("isNaN".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Float.isNaN(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("parseFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Float.parseFloat((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Float.toString(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Float.valueOf(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Float.valueOf((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.lang.Float.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Integer.compare(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("numberOfLeadingZeros".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.numberOfLeadingZeros(toIntValue(adaptedArgs[0]));
            }
        }
        if ("parseInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Integer.parseInt((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return java.lang.Integer.parseInt((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("signum".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.signum(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toBinaryString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.toBinaryString(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toHexString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.toHexString(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toOctalString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.toOctalString(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.toString(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Integer.toString(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Integer.valueOf(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Integer.valueOf((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return java.lang.Integer.valueOf((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedStatic(java.lang.Integer.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return java.lang.Long.compare(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("parseLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Long.parseLong((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return java.lang.Long.parseLong((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.lang.Long.toString(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Integer.class}, false);
                return java.lang.Long.toString(((Number) adaptedArgs[0]).longValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.lang.Long.valueOf(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedStatic(java.lang.Long.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.abs(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Math.abs(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.Math.abs(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.lang.Math.abs(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ceil".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.ceil(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("cos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.cos(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.floor(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return java.lang.Math.max(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return java.lang.Math.max(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Math.max(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return java.lang.Math.max(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return java.lang.Math.min(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return java.lang.Math.min(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.Math.min(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return java.lang.Math.min(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("nextAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return java.lang.Math.nextAfter(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Double.class}, false);
                return java.lang.Math.nextAfter(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("nextDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.nextDown(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("round".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.round(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.Math.round(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("sin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.sin(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("sqrt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.sqrt(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("tan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.tan(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("toDegrees".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.toDegrees(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("toRadians".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.Math.toRadians(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        throw unsupportedStatic(java.lang.Math.class, name, safeArgs);
    }

    private static Object invokeStatic11(String name, Object[] safeArgs) throws Exception {
        if ("getRuntime".equals(name)) {
            if (safeArgs.length == 0) {
                return java.lang.Runtime.getRuntime();
            }
        }
        throw unsupportedStatic(java.lang.Runtime.class, name, safeArgs);
    }

    private static Object invokeStatic12(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Short.class, java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Short.class, java.lang.Short.class}, false);
                return java.lang.Short.compare((short) toIntValue(adaptedArgs[0]), (short) toIntValue(adaptedArgs[1]));
            }
        }
        if ("parseShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.Short.parseShort((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return java.lang.Short.parseShort((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Short.class}, false);
                return java.lang.Short.valueOf((short) toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(java.lang.Short.class, name, safeArgs);
    }

    private static Object invokeStatic13(String name, Object[] safeArgs) throws Exception {
        if ("copyValueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return java.lang.String.copyValueOf((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.String.copyValueOf((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) adaptedArgs[i];
                }
                return java.lang.String.format((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return java.lang.String.valueOf(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return java.lang.String.valueOf(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return java.lang.String.valueOf(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return java.lang.String.valueOf(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.lang.String.valueOf(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.lang.String.valueOf(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.lang.String.valueOf((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.lang.String.valueOf((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        throw unsupportedStatic(java.lang.String.class, name, safeArgs);
    }

    private static Object invokeStatic14(String name, Object[] safeArgs) throws Exception {
        if ("arraycopy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.lang.System.arraycopy((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.Object) adaptedArgs[2], toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("currentTimeMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return java.lang.System.currentTimeMillis();
            }
        }
        if ("gc".equals(name)) {
            if (safeArgs.length == 0) {
                java.lang.System.gc(); return null;
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.lang.System.getProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("identityHashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.lang.System.identityHashCode((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.lang.System.class, name, safeArgs);
    }

    private static Object invokeStatic15(String name, Object[] safeArgs) throws Exception {
        if ("activeCount".equals(name)) {
            if (safeArgs.length == 0) {
                return java.lang.Thread.activeCount();
            }
        }
        if ("currentThread".equals(name)) {
            if (safeArgs.length == 0) {
                return java.lang.Thread.currentThread();
            }
        }
        if ("sleep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                java.lang.Thread.sleep(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("yield".equals(name)) {
            if (safeArgs.length == 0) {
                java.lang.Thread.yield(); return null;
            }
        }
        throw unsupportedStatic(java.lang.Thread.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof java.lang.ArrayIndexOutOfBoundsException) {
            try {
                return invoke0((java.lang.ArrayIndexOutOfBoundsException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.NoSuchFieldError) {
            try {
                return invoke1((java.lang.NoSuchFieldError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.NumberFormatException) {
            try {
                return invoke2((java.lang.NumberFormatException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.StringIndexOutOfBoundsException) {
            try {
                return invoke3((java.lang.StringIndexOutOfBoundsException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.ArithmeticException) {
            try {
                return invoke4((java.lang.ArithmeticException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.ArrayStoreException) {
            try {
                return invoke5((java.lang.ArrayStoreException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.ClassCastException) {
            try {
                return invoke6((java.lang.ClassCastException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.IllegalArgumentException) {
            try {
                return invoke7((java.lang.IllegalArgumentException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.IllegalMonitorStateException) {
            try {
                return invoke8((java.lang.IllegalMonitorStateException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.IllegalStateException) {
            try {
                return invoke9((java.lang.IllegalStateException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.IncompatibleClassChangeError) {
            try {
                return invoke10((java.lang.IncompatibleClassChangeError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.IndexOutOfBoundsException) {
            try {
                return invoke11((java.lang.IndexOutOfBoundsException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.NegativeArraySizeException) {
            try {
                return invoke12((java.lang.NegativeArraySizeException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.NullPointerException) {
            try {
                return invoke13((java.lang.NullPointerException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.OutOfMemoryError) {
            try {
                return invoke14((java.lang.OutOfMemoryError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.SecurityException) {
            try {
                return invoke15((java.lang.SecurityException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.UnsupportedOperationException) {
            try {
                return invoke16((java.lang.UnsupportedOperationException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.AssertionError) {
            try {
                return invoke17((java.lang.AssertionError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.ClassNotFoundException) {
            try {
                return invoke18((java.lang.ClassNotFoundException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.CloneNotSupportedException) {
            try {
                return invoke19((java.lang.CloneNotSupportedException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.IllegalAccessException) {
            try {
                return invoke20((java.lang.IllegalAccessException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.InstantiationException) {
            try {
                return invoke21((java.lang.InstantiationException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.InterruptedException) {
            try {
                return invoke22((java.lang.InterruptedException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.LinkageError) {
            try {
                return invoke23((java.lang.LinkageError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.NoClassDefFoundError) {
            try {
                return invoke24((java.lang.NoClassDefFoundError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.RuntimeException) {
            try {
                return invoke25((java.lang.RuntimeException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.VirtualMachineError) {
            try {
                return invoke26((java.lang.VirtualMachineError) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Byte) {
            try {
                return invoke27((java.lang.Byte) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.ClassLoader) {
            try {
                return invoke28((java.lang.ClassLoader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Double) {
            try {
                return invoke29((java.lang.Double) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Error) {
            try {
                return invoke30((java.lang.Error) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Exception) {
            try {
                return invoke31((java.lang.Exception) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Float) {
            try {
                return invoke32((java.lang.Float) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Integer) {
            try {
                return invoke33((java.lang.Integer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Long) {
            try {
                return invoke34((java.lang.Long) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Short) {
            try {
                return invoke35((java.lang.Short) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.ThreadLocal) {
            try {
                return invoke36((java.lang.ThreadLocal) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Boolean) {
            try {
                return invoke37((java.lang.Boolean) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Character) {
            try {
                return invoke38((java.lang.Character) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Class) {
            try {
                return invoke39((java.lang.Class) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Enum) {
            try {
                return invoke40((java.lang.Enum) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Number) {
            try {
                return invoke41((java.lang.Number) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Object) {
            try {
                return invoke42((java.lang.Object) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Runtime) {
            try {
                return invoke43((java.lang.Runtime) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.StackTraceElement) {
            try {
                return invoke44((java.lang.StackTraceElement) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.String) {
            try {
                return invoke45((java.lang.String) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.StringBuffer) {
            try {
                return invoke46((java.lang.StringBuffer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.StringBuilder) {
            try {
                return invoke47((java.lang.StringBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Thread) {
            try {
                return invoke48((java.lang.Thread) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Throwable) {
            try {
                return invoke49((java.lang.Throwable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Appendable) {
            try {
                return invoke50((java.lang.Appendable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.AutoCloseable) {
            try {
                return invoke51((java.lang.AutoCloseable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.CharSequence) {
            try {
                return invoke52((java.lang.CharSequence) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Comparable) {
            try {
                return invoke53((java.lang.Comparable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Iterable) {
            try {
                return invoke54((java.lang.Iterable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.lang.Runnable) {
            try {
                return invoke55((java.lang.Runnable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.lang.ArrayIndexOutOfBoundsException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke1(java.lang.NoSuchFieldError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke2(java.lang.NumberFormatException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke3(java.lang.StringIndexOutOfBoundsException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke4(java.lang.ArithmeticException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke5(java.lang.ArrayStoreException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke6(java.lang.ClassCastException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke7(java.lang.IllegalArgumentException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke8(java.lang.IllegalMonitorStateException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke9(java.lang.IllegalStateException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke10(java.lang.IncompatibleClassChangeError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke11(java.lang.IndexOutOfBoundsException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke12(java.lang.NegativeArraySizeException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke13(java.lang.NullPointerException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke14(java.lang.OutOfMemoryError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke15(java.lang.SecurityException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke16(java.lang.UnsupportedOperationException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke17(java.lang.AssertionError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke18(java.lang.ClassNotFoundException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke19(java.lang.CloneNotSupportedException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke20(java.lang.IllegalAccessException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke21(java.lang.InstantiationException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke22(java.lang.InterruptedException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke23(java.lang.LinkageError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke24(java.lang.NoClassDefFoundError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke25(java.lang.RuntimeException typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke26(java.lang.VirtualMachineError typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke27(java.lang.Byte typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                return typedTarget.compareTo(Byte.valueOf((byte) toIntValue(adaptedArgs[0])));
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke28(java.lang.ClassLoader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke29(java.lang.Double typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.compareTo(Double.valueOf(((Number) adaptedArgs[0]).doubleValue()));
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("isInfinite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInfinite();
            }
        }
        if ("isNaN".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNaN();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke30(java.lang.Error typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke31(java.lang.Exception typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke32(java.lang.Float typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.compareTo(Float.valueOf(((Number) adaptedArgs[0]).floatValue()));
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("isInfinite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInfinite();
            }
        }
        if ("isNaN".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNaN();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke33(java.lang.Integer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.compareTo(Integer.valueOf(toIntValue(adaptedArgs[0])));
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke34(java.lang.Long typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.compareTo(Long.valueOf(((Number) adaptedArgs[0]).longValue()));
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke35(java.lang.Short typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Short.class}, false);
                return typedTarget.compareTo(Short.valueOf((short) toIntValue(adaptedArgs[0])));
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke36(java.lang.ThreadLocal typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.get();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.set((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke37(java.lang.Boolean typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("booleanValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.booleanValue();
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.compareTo(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke38(java.lang.Character typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.charValue();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke39(java.lang.Class typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asSubclass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                return typedTarget.asSubclass((java.lang.Class) adaptedArgs[0]);
            }
        }
        if ("cast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cast((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("desiredAssertionStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.desiredAssertionStatus();
            }
        }
        if ("getCanonicalName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCanonicalName();
            }
        }
        if ("getClassLoader".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClassLoader();
            }
        }
        if ("getComponentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentType();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getSimpleName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSimpleName();
            }
        }
        if ("isAnnotationPresent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                return typedTarget.isAnnotationPresent((java.lang.Class) adaptedArgs[0]);
            }
        }
        if ("isArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isArray();
            }
        }
        if ("isAssignableFrom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                return typedTarget.isAssignableFrom((java.lang.Class) adaptedArgs[0]);
            }
        }
        if ("isEnum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnum();
            }
        }
        if ("isInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isInstance((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isInterface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isInterface();
            }
        }
        if ("isLocalClass".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLocalClass();
            }
        }
        if ("isPrimitive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrimitive();
            }
        }
        if ("isSynthetic".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSynthetic();
            }
        }
        if ("newInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.newInstance();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke40(java.lang.Enum typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Enum.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Enum.class}, false);
                return typedTarget.compareTo((java.lang.Enum) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDeclaringClass".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDeclaringClass();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("name".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.name();
            }
        }
        if ("ordinal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.ordinal();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke41(java.lang.Number typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("doubleValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.doubleValue();
            }
        }
        if ("floatValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floatValue();
            }
        }
        if ("intValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intValue();
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("shortValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.shortValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke42(java.lang.Object typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke43(java.lang.Runtime typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("exit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.exit(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("freeMemory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.freeMemory();
            }
        }
        if ("gc".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.gc(); return null;
            }
        }
        if ("totalMemory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.totalMemory();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke44(java.lang.StackTraceElement typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getClassName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getClassName();
            }
        }
        if ("getFileName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFileName();
            }
        }
        if ("getLineNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLineNumber();
            }
        }
        if ("getMethodName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMethodName();
            }
        }
        if ("isNativeMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativeMethod();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke45(java.lang.String typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.compareTo((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("compareToIgnoreCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.compareToIgnoreCase((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("concat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.concat((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return typedTarget.contains((java.lang.CharSequence) adaptedArgs[0]);
            }
        }
        if ("contentEquals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StringBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StringBuffer.class}, false);
                return typedTarget.contentEquals((java.lang.StringBuffer) adaptedArgs[0]);
            }
        }
        if ("endsWith".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.endsWith((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equalsIgnoreCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.equalsIgnoreCase((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBytes();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBytes((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getChars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, char[].class, java.lang.Integer.class}, false);
                typedTarget.getChars(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (char[]) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.indexOf(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.indexOf((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.indexOf(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.indexOf((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("intern".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.intern();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.lastIndexOf(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.lastIndexOf((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.lastIndexOf(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.lastIndexOf((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("length".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.length();
            }
        }
        if ("regionMatches".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.regionMatches(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.regionMatches(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2], toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]));
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Character.class}, false);
                return typedTarget.replace(((Character) adaptedArgs[0]).charValue(), ((Character) adaptedArgs[1]).charValue());
            }
        }
        if ("startsWith".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.startsWith((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.startsWith((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("subSequence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subSequence(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("substring".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.substring(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.substring(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toCharArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toCharArray();
            }
        }
        if ("toLowerCase".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toLowerCase();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("toUpperCase".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toUpperCase();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Locale.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Locale.class}, false);
                return typedTarget.toUpperCase((java.util.Locale) adaptedArgs[0]);
            }
        }
        if ("trim".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.trim();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke46(java.lang.StringBuffer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.append(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.append(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.append(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.append(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.append(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.append(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.append((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.append((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.append((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("capacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.capacity();
            }
        }
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.delete(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("deleteCharAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deleteCharAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.ensureCapacity(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("getChars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, char[].class, java.lang.Integer.class}, false);
                typedTarget.getChars(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (char[]) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Character) adaptedArgs[1]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), (java.lang.CharSequence) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("length".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.length();
            }
        }
        if ("reverse".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.reverse();
            }
        }
        if ("setCharAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false);
                typedTarget.setCharAt(toIntValue(adaptedArgs[0]), ((Character) adaptedArgs[1]).charValue()); return null;
            }
        }
        if ("setLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("subSequence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subSequence(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke47(java.lang.StringBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.append(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.append(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.append(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.append(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.append(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.append(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.append((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.StringBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StringBuffer.class}, false);
                return typedTarget.append((java.lang.StringBuffer) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return typedTarget.append((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.append((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.append((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.delete(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("deleteCharAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.deleteCharAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Character) adaptedArgs[1]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), (java.lang.CharSequence) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("reverse".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.reverse();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke48(java.lang.Thread typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStackTrace();
            }
        }
        if ("interrupt".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.interrupt(); return null;
            }
        }
        if ("isAlive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlive();
            }
        }
        if ("join".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.join(); return null;
            }
        }
        if ("run".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.run(); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPriority(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("start".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.start(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke49(java.lang.Throwable typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke50(java.lang.Appendable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.append(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return typedTarget.append((java.lang.CharSequence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.append((java.lang.CharSequence) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke51(java.lang.AutoCloseable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke52(java.lang.CharSequence typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("length".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.length();
            }
        }
        if ("subSequence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subSequence(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke53(java.lang.Comparable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.compareTo((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke54(java.lang.Iterable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke55(java.lang.Runnable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("run".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.run(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == java.lang.Boolean.class) return getStaticField0(name);
        if (type == java.lang.Byte.class) return getStaticField1(name);
        if (type == java.lang.Character.class) return getStaticField2(name);
        if (type == java.lang.Double.class) return getStaticField3(name);
        if (type == java.lang.Float.class) return getStaticField4(name);
        if (type == java.lang.Integer.class) return getStaticField5(name);
        if (type == java.lang.Long.class) return getStaticField6(name);
        if (type == java.lang.Math.class) return getStaticField7(name);
        if (type == java.lang.Short.class) return getStaticField8(name);
        if (type == java.lang.String.class) return getStaticField9(name);
        if (type == java.lang.Thread.class) return getStaticField10(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("FALSE".equals(name)) return java.lang.Boolean.FALSE;
        if ("TRUE".equals(name)) return java.lang.Boolean.TRUE;
        throw unsupportedStaticField(java.lang.Boolean.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("MAX_VALUE".equals(name)) return java.lang.Byte.MAX_VALUE;
        if ("MIN_VALUE".equals(name)) return java.lang.Byte.MIN_VALUE;
        if ("SIZE".equals(name)) return java.lang.Byte.SIZE;
        throw unsupportedStaticField(java.lang.Byte.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("MAX_CODE_POINT".equals(name)) return java.lang.Character.MAX_CODE_POINT;
        if ("MAX_HIGH_SURROGATE".equals(name)) return java.lang.Character.MAX_HIGH_SURROGATE;
        if ("MAX_LOW_SURROGATE".equals(name)) return java.lang.Character.MAX_LOW_SURROGATE;
        if ("MAX_RADIX".equals(name)) return java.lang.Character.MAX_RADIX;
        if ("MAX_SURROGATE".equals(name)) return java.lang.Character.MAX_SURROGATE;
        if ("MAX_VALUE".equals(name)) return java.lang.Character.MAX_VALUE;
        if ("MIN_CODE_POINT".equals(name)) return java.lang.Character.MIN_CODE_POINT;
        if ("MIN_HIGH_SURROGATE".equals(name)) return java.lang.Character.MIN_HIGH_SURROGATE;
        if ("MIN_LOW_SURROGATE".equals(name)) return java.lang.Character.MIN_LOW_SURROGATE;
        if ("MIN_RADIX".equals(name)) return java.lang.Character.MIN_RADIX;
        if ("MIN_SUPPLEMENTARY_CODE_POINT".equals(name)) return java.lang.Character.MIN_SUPPLEMENTARY_CODE_POINT;
        if ("MIN_SURROGATE".equals(name)) return java.lang.Character.MIN_SURROGATE;
        if ("MIN_VALUE".equals(name)) return java.lang.Character.MIN_VALUE;
        if ("SIZE".equals(name)) return java.lang.Character.SIZE;
        throw unsupportedStaticField(java.lang.Character.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("MAX_VALUE".equals(name)) return java.lang.Double.MAX_VALUE;
        if ("MIN_VALUE".equals(name)) return java.lang.Double.MIN_VALUE;
        if ("NEGATIVE_INFINITY".equals(name)) return java.lang.Double.NEGATIVE_INFINITY;
        if ("NaN".equals(name)) return java.lang.Double.NaN;
        if ("POSITIVE_INFINITY".equals(name)) return java.lang.Double.POSITIVE_INFINITY;
        throw unsupportedStaticField(java.lang.Double.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("MAX_VALUE".equals(name)) return java.lang.Float.MAX_VALUE;
        if ("MIN_VALUE".equals(name)) return java.lang.Float.MIN_VALUE;
        if ("NEGATIVE_INFINITY".equals(name)) return java.lang.Float.NEGATIVE_INFINITY;
        if ("NaN".equals(name)) return java.lang.Float.NaN;
        if ("POSITIVE_INFINITY".equals(name)) return java.lang.Float.POSITIVE_INFINITY;
        throw unsupportedStaticField(java.lang.Float.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("MAX_VALUE".equals(name)) return java.lang.Integer.MAX_VALUE;
        if ("MIN_VALUE".equals(name)) return java.lang.Integer.MIN_VALUE;
        throw unsupportedStaticField(java.lang.Integer.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("MAX_VALUE".equals(name)) return java.lang.Long.MAX_VALUE;
        if ("MIN_VALUE".equals(name)) return java.lang.Long.MIN_VALUE;
        throw unsupportedStaticField(java.lang.Long.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("E".equals(name)) return java.lang.Math.E;
        if ("PI".equals(name)) return java.lang.Math.PI;
        throw unsupportedStaticField(java.lang.Math.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("MAX_VALUE".equals(name)) return java.lang.Short.MAX_VALUE;
        if ("MIN_VALUE".equals(name)) return java.lang.Short.MIN_VALUE;
        throw unsupportedStaticField(java.lang.Short.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("CASE_INSENSITIVE_ORDER".equals(name)) return java.lang.String.CASE_INSENSITIVE_ORDER;
        throw unsupportedStaticField(java.lang.String.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
        if ("MAX_PRIORITY".equals(name)) return java.lang.Thread.MAX_PRIORITY;
        if ("MIN_PRIORITY".equals(name)) return java.lang.Thread.MIN_PRIORITY;
        if ("NORM_PRIORITY".equals(name)) return java.lang.Thread.NORM_PRIORITY;
        throw unsupportedStaticField(java.lang.Thread.class, name);
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
