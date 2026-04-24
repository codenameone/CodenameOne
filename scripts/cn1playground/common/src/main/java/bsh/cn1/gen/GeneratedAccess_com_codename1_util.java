package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_util {
    private GeneratedAccess_com_codename1_util() {
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
        if ("AsyncResource".equals(simpleName)) {
            return com.codename1.util.AsyncResource.class;
        }
        if ("AsyncExecutionException".equals(simpleName)) {
            return com.codename1.util.AsyncResource.AsyncExecutionException.class;
        }
        if ("CancellationException".equals(simpleName)) {
            return com.codename1.util.AsyncResource.CancellationException.class;
        }
        if ("AsyncResult".equals(simpleName)) {
            return com.codename1.util.AsyncResult.class;
        }
        if ("Base64".equals(simpleName)) {
            return com.codename1.util.Base64.class;
        }
        if ("BigDecimal".equals(simpleName)) {
            return com.codename1.util.BigDecimal.class;
        }
        if ("BigInteger".equals(simpleName)) {
            return com.codename1.util.BigInteger.class;
        }
        if ("CStringBuilder".equals(simpleName)) {
            return com.codename1.util.CStringBuilder.class;
        }
        if ("Callback".equals(simpleName)) {
            return com.codename1.util.Callback.class;
        }
        if ("CallbackAdapter".equals(simpleName)) {
            return com.codename1.util.CallbackAdapter.class;
        }
        if ("CallbackDispatcher".equals(simpleName)) {
            return com.codename1.util.CallbackDispatcher.class;
        }
        if ("CaseInsensitiveOrder".equals(simpleName)) {
            return com.codename1.util.CaseInsensitiveOrder.class;
        }
        if ("DateUtil".equals(simpleName)) {
            return com.codename1.util.DateUtil.class;
        }
        if ("EasyThread".equals(simpleName)) {
            return com.codename1.util.EasyThread.class;
        }
        if ("ErrorListener".equals(simpleName)) {
            return com.codename1.util.EasyThread.ErrorListener.class;
        }
        if ("FailureCallback".equals(simpleName)) {
            return com.codename1.util.FailureCallback.class;
        }
        if ("LazyValue".equals(simpleName)) {
            return com.codename1.util.LazyValue.class;
        }
        if ("MathUtil".equals(simpleName)) {
            return com.codename1.util.MathUtil.class;
        }
        if ("OnComplete".equals(simpleName)) {
            return com.codename1.util.OnComplete.class;
        }
        if ("RunnableWithResult".equals(simpleName)) {
            return com.codename1.util.RunnableWithResult.class;
        }
        if ("RunnableWithResultSync".equals(simpleName)) {
            return com.codename1.util.RunnableWithResultSync.class;
        }
        if ("StringUtil".equals(simpleName)) {
            return com.codename1.util.StringUtil.class;
        }
        if ("SuccessCallback".equals(simpleName)) {
            return com.codename1.util.SuccessCallback.class;
        }
        if ("Wrapper".equals(simpleName)) {
            return com.codename1.util.Wrapper.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.AsyncResource.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.util.AsyncResource();
            }
        }
        if (type == com.codename1.util.AsyncResource.AsyncExecutionException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return new com.codename1.util.AsyncResource.AsyncExecutionException((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.util.AsyncResource.CancellationException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.util.AsyncResource.CancellationException();
            }
        }
        if (type == com.codename1.util.BigDecimal.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, java.lang.Integer.class}, false);
                return new com.codename1.util.BigDecimal((com.codename1.util.BigInteger) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.util.BigInteger.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.util.BigInteger((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return new com.codename1.util.BigInteger((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Random.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Random.class}, false);
                return new com.codename1.util.BigInteger(toIntValue(adaptedArgs[0]), (java.util.Random) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false);
                return new com.codename1.util.BigInteger(toIntValue(adaptedArgs[0]), (byte[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.util.BigInteger((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.util.Random.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.util.Random.class}, false);
                return new com.codename1.util.BigInteger(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.util.Random) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.util.CStringBuilder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.util.CStringBuilder();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.util.CStringBuilder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.util.CStringBuilder((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.util.DateUtil.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.util.DateUtil();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false);
                return new com.codename1.util.DateUtil((java.util.TimeZone) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.util.Wrapper.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return new com.codename1.util.Wrapper((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.AsyncResource.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.util.Base64.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.util.BigDecimal.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.util.BigInteger.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.util.CallbackDispatcher.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.util.DateUtil.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.util.EasyThread.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.util.MathUtil.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.util.StringUtil.class) return invokeStatic8(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("all".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return com.codename1.util.AsyncResource.all((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource[].class}, true);
                com.codename1.util.AsyncResource[] varArgs = new com.codename1.util.AsyncResource[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.util.AsyncResource) adaptedArgs[i];
                }
                return com.codename1.util.AsyncResource.all(varArgs);
            }
        }
        if ("await".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                com.codename1.util.AsyncResource.await((java.util.Collection) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource[].class}, true);
                com.codename1.util.AsyncResource[] varArgs = new com.codename1.util.AsyncResource[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.util.AsyncResource) adaptedArgs[i];
                }
                com.codename1.util.AsyncResource.await(varArgs); return null;
            }
        }
        if ("isCancelled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return com.codename1.util.AsyncResource.isCancelled((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.AsyncResource.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.util.Base64.decode((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return com.codename1.util.Base64.decode((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.util.Base64.decode((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, byte[].class}, false);
                return com.codename1.util.Base64.decode((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), (byte[]) adaptedArgs[2]);
            }
        }
        if ("encode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.util.Base64.encode((byte[]) adaptedArgs[0]);
            }
        }
        if ("encodeNoNewline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.util.Base64.encodeNoNewline((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return com.codename1.util.Base64.encodeNoNewline((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.util.Base64.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, java.lang.Integer.class}, false);
                return com.codename1.util.BigDecimal.getInstance((com.codename1.util.BigInteger) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedStatic(com.codename1.util.BigDecimal.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("probablePrime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Random.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Random.class}, false);
                return com.codename1.util.BigInteger.probablePrime(toIntValue(adaptedArgs[0]), (java.util.Random) adaptedArgs[1]);
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.util.BigInteger.valueOf(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.BigInteger.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("dispatchError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.FailureCallback.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.FailureCallback.class, java.lang.Throwable.class}, false);
                com.codename1.util.CallbackDispatcher.dispatchError((com.codename1.util.FailureCallback) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]); return null;
            }
        }
        if ("dispatchSuccess".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, java.lang.Object.class}, false);
                com.codename1.util.CallbackDispatcher.dispatchSuccess((com.codename1.util.SuccessCallback) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.util.CallbackDispatcher.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return com.codename1.util.DateUtil.compare((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("compareByDateField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return com.codename1.util.DateUtil.compareByDateField(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date[].class}, true);
                java.util.Date[] varArgs = new java.util.Date[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.util.Date) adaptedArgs[i];
                }
                return com.codename1.util.DateUtil.max(varArgs);
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date[].class}, true);
                java.util.Date[] varArgs = new java.util.Date[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.util.Date) adaptedArgs[i];
                }
                return com.codename1.util.DateUtil.min(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.util.DateUtil.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("addGlobalErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false);
                com.codename1.util.EasyThread.addGlobalErrorListener((com.codename1.util.EasyThread.ErrorListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeGlobalErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false);
                com.codename1.util.EasyThread.removeGlobalErrorListener((com.codename1.util.EasyThread.ErrorListener) adaptedArgs[0]); return null;
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.util.EasyThread.start((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.EasyThread.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("acos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.acos(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("asin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.asin(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("atan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.atan(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("atan2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.atan2(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.compare(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.util.MathUtil.compare(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("copySign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.copySign(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("copysign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.copysign(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("exp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.exp(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.floor(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.util.MathUtil.floor(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("log".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.log(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("log10".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.log10(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("nextAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.nextAfter(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("pow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.pow(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
        }
        if ("round".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.round(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.util.MathUtil.round(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("scalb".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                return com.codename1.util.MathUtil.scalb(((Number) adaptedArgs[0]).doubleValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("scalbn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                return com.codename1.util.MathUtil.scalbn(((Number) adaptedArgs[0]).doubleValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("ulp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return com.codename1.util.MathUtil.ulp(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.MathUtil.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("getBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.util.StringUtil.getBytes((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("join".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Iterable.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Iterable.class, java.lang.String.class}, false);
                return com.codename1.util.StringUtil.join((java.lang.Iterable) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.String.class}, false);
                return com.codename1.util.StringUtil.join((java.lang.Object[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("newString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.util.StringUtil.newString((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.util.StringUtil.newString((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("replaceAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.util.StringUtil.replaceAll((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("replaceFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.util.StringUtil.replaceFirst((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("tokenize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false);
                return com.codename1.util.StringUtil.tokenize((java.lang.String) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.util.StringUtil.tokenize((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("tokenizeString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false);
                return com.codename1.util.StringUtil.tokenizeString((java.lang.String) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.util.StringUtil.tokenizeString((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.util.StringUtil.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.util.AsyncResource) {
            try {
                return invoke0((com.codename1.util.AsyncResource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.AsyncResource.AsyncExecutionException) {
            try {
                return invoke1((com.codename1.util.AsyncResource.AsyncExecutionException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.BigDecimal) {
            try {
                return invoke2((com.codename1.util.BigDecimal) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.BigInteger) {
            try {
                return invoke3((com.codename1.util.BigInteger) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CStringBuilder) {
            try {
                return invoke4((com.codename1.util.CStringBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CallbackAdapter) {
            try {
                return invoke5((com.codename1.util.CallbackAdapter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CallbackDispatcher) {
            try {
                return invoke6((com.codename1.util.CallbackDispatcher) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CaseInsensitiveOrder) {
            try {
                return invoke7((com.codename1.util.CaseInsensitiveOrder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.DateUtil) {
            try {
                return invoke8((com.codename1.util.DateUtil) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.EasyThread) {
            try {
                return invoke9((com.codename1.util.EasyThread) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.Wrapper) {
            try {
                return invoke10((com.codename1.util.Wrapper) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.AsyncResult) {
            try {
                return invoke11((com.codename1.util.AsyncResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.Callback) {
            try {
                return invoke12((com.codename1.util.Callback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.EasyThread.ErrorListener) {
            try {
                return invoke13((com.codename1.util.EasyThread.ErrorListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.FailureCallback) {
            try {
                return invoke14((com.codename1.util.FailureCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.LazyValue) {
            try {
                return invoke15((com.codename1.util.LazyValue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.OnComplete) {
            try {
                return invoke16((com.codename1.util.OnComplete) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.RunnableWithResult) {
            try {
                return invoke17((com.codename1.util.RunnableWithResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.RunnableWithResultSync) {
            try {
                return invoke18((com.codename1.util.RunnableWithResultSync) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.SuccessCallback) {
            try {
                return invoke19((com.codename1.util.SuccessCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.util.AsyncResource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource.class}, false);
                typedTarget.addListener((com.codename1.util.AsyncResource) adaptedArgs[0]); return null;
            }
        }
        if ("addObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.addObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("asPromise".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asPromise();
            }
        }
        if ("await".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.await(); return null;
            }
        }
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.cancel(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("complete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.complete((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("countObservers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.countObservers();
            }
        }
        if ("deleteObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.deleteObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("deleteObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.deleteObservers(); return null;
            }
        }
        if ("error".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.error((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("except".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.except((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false);
                return typedTarget.except((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.EasyThread) adaptedArgs[1]);
            }
        }
        if ("get".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.get();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hasChanged".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasChanged();
            }
        }
        if ("isCancelled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCancelled();
            }
        }
        if ("isDone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDone();
            }
        }
        if ("isReady".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReady();
            }
        }
        if ("notifyObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.notifyObservers(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.notifyObservers((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false);
                typedTarget.onResult((com.codename1.util.AsyncResult) adaptedArgs[0]); return null;
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                return typedTarget.ready((com.codename1.util.SuccessCallback) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false);
                return typedTarget.ready((com.codename1.util.SuccessCallback) adaptedArgs[0], (com.codename1.util.EasyThread) adaptedArgs[1]);
            }
        }
        if ("waitFor".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.waitFor(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.util.AsyncResource.AsyncExecutionException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCause();
            }
        }
        if ("isCancelled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCancelled();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.util.BigDecimal typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false);
                return typedTarget.add((com.codename1.util.BigDecimal) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.add((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("adjustScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.adjustScale(toIntValue(adaptedArgs[0]));
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false);
                return typedTarget.compareTo((com.codename1.util.BigDecimal) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.compareTo((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("divide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false);
                return typedTarget.divide((com.codename1.util.BigDecimal) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.divide((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.floor();
            }
        }
        if ("getScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScale();
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
        if ("multiply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false);
                return typedTarget.multiply((com.codename1.util.BigDecimal) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.multiply((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("negate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.negate();
            }
        }
        if ("round".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.round();
            }
        }
        if ("shiftLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shiftLeft(toIntValue(adaptedArgs[0]));
            }
        }
        if ("subtract".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false);
                return typedTarget.subtract((com.codename1.util.BigDecimal) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.subtract((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.util.BigInteger typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.abs();
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.add((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("and".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.and((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("andNot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.andNot((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("bitCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.bitCount();
            }
        }
        if ("bitLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.bitLength();
            }
        }
        if ("byteValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.byteValue();
            }
        }
        if ("clearBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.clearBit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.compareTo((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("divide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.divide((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("divideAndRemainder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.divideAndRemainder((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("flipBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.flipBit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("gcd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.gcd((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("getLowestSetBit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLowestSetBit();
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
        if ("isProbablePrime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isProbablePrime(toIntValue(adaptedArgs[0]));
            }
        }
        if ("longValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.longValue();
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.max((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.min((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("mod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.mod((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("modInverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.modInverse((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("modPow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, com.codename1.util.BigInteger.class}, false);
                return typedTarget.modPow((com.codename1.util.BigInteger) adaptedArgs[0], (com.codename1.util.BigInteger) adaptedArgs[1]);
            }
        }
        if ("multiply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.multiply((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("negate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.negate();
            }
        }
        if ("not".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.not();
            }
        }
        if ("or".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.or((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("pow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.pow(toIntValue(adaptedArgs[0]));
            }
        }
        if ("remainder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.remainder((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("setBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setBit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("shiftLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shiftLeft(toIntValue(adaptedArgs[0]));
            }
        }
        if ("shiftRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shiftRight(toIntValue(adaptedArgs[0]));
            }
        }
        if ("signum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.signum();
            }
        }
        if ("subtract".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.subtract((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        if ("testBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.testBit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("toByteArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toByteArray();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toString(toIntValue(adaptedArgs[0]));
            }
        }
        if ("xor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false);
                return typedTarget.xor((com.codename1.util.BigInteger) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.util.CStringBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
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
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return typedTarget.append((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.append((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("appendCodePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.appendCodePoint(toIntValue(adaptedArgs[0]));
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
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), (char[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.insert(toIntValue(adaptedArgs[0]), (char[]) adaptedArgs[1], toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return typedTarget.replace(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
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

    private static Object invoke5(com.codename1.util.CallbackAdapter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.onError((java.lang.Object) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3]); return null;
            }
        }
        if ("onSucess".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.onSucess((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.util.CallbackDispatcher typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("run".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.run(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.util.CaseInsensitiveOrder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.compare((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.util.DateUtil typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.getOffset(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("getTimeAgo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.getTimeAgo((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("inDaylightTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.inDaylightTime((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("isSameDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameDay((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameDay((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("isSameHour".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameHour((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameHour((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("isSameMinute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameMinute((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameMinute((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("isSameMonth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameMonth((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameMonth((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("isSameSecond".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameSecond((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameSecond((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("isSameTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameTime((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameTime((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        if ("isSameYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false);
                return typedTarget.isSameYear((java.util.Calendar) adaptedArgs[0], (java.util.Calendar) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false);
                return typedTarget.isSameYear((java.util.Date) adaptedArgs[0], (java.util.Date) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.util.EasyThread typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false);
                typedTarget.addErrorListener((com.codename1.util.EasyThread.ErrorListener) adaptedArgs[0]); return null;
            }
        }
        if ("isThisIt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isThisIt();
            }
        }
        if ("kill".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.kill(); return null;
            }
        }
        if ("removeErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false);
                typedTarget.removeErrorListener((com.codename1.util.EasyThread.ErrorListener) adaptedArgs[0]); return null;
            }
        }
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.RunnableWithResultSync.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.RunnableWithResultSync.class}, false);
                return typedTarget.run((com.codename1.util.RunnableWithResultSync) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.run((java.lang.Runnable) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.RunnableWithResult.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.RunnableWithResult.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.run((com.codename1.util.RunnableWithResult) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
        }
        if ("runAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.runAndWait((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPriority(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.util.Wrapper typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("get".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.get();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.set((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.util.AsyncResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onReady".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class}, false);
                typedTarget.onReady((java.lang.Object) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.util.Callback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.onError((java.lang.Object) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.util.EasyThread.ErrorListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.class, java.lang.Object.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.class, java.lang.Object.class, java.lang.Throwable.class}, false);
                typedTarget.onError((com.codename1.util.EasyThread) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.util.FailureCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false);
                typedTarget.onError((java.lang.Object) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.util.LazyValue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.Object) adaptedArgs[i];
                }
                return typedTarget.get(varArgs);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.util.OnComplete typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("completed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.completed((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.util.RunnableWithResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                typedTarget.run((com.codename1.util.SuccessCallback) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.util.RunnableWithResultSync typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("run".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.run();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.util.SuccessCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onSucess".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.onSucess((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.util.BigInteger.class) return getStaticField0(name);
        if (type == com.codename1.util.DateUtil.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ONE".equals(name)) return com.codename1.util.BigInteger.ONE;
        if ("ZERO".equals(name)) return com.codename1.util.BigInteger.ZERO;
        throw unsupportedStaticField(com.codename1.util.BigInteger.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("DAY".equals(name)) return com.codename1.util.DateUtil.DAY;
        if ("HOUR".equals(name)) return com.codename1.util.DateUtil.HOUR;
        if ("MILLISECOND".equals(name)) return com.codename1.util.DateUtil.MILLISECOND;
        if ("MINUTE".equals(name)) return com.codename1.util.DateUtil.MINUTE;
        if ("MONTH".equals(name)) return com.codename1.util.DateUtil.MONTH;
        if ("SECOND".equals(name)) return com.codename1.util.DateUtil.SECOND;
        if ("YEAR".equals(name)) return com.codename1.util.DateUtil.YEAR;
        throw unsupportedStaticField(com.codename1.util.DateUtil.class, name);
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
