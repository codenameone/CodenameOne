package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_util {
    private GeneratedAccess_com_codename1_util() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.util.AsyncResource".equals(name)) return com.codename1.util.AsyncResource.class;
        if ("com.codename1.util.AsyncResult".equals(name)) return com.codename1.util.AsyncResult.class;
        if ("com.codename1.util.Base64".equals(name)) return com.codename1.util.Base64.class;
        if ("com.codename1.util.BigDecimal".equals(name)) return com.codename1.util.BigDecimal.class;
        if ("com.codename1.util.BigInteger".equals(name)) return com.codename1.util.BigInteger.class;
        if ("com.codename1.util.CStringBuilder".equals(name)) return com.codename1.util.CStringBuilder.class;
        if ("com.codename1.util.Callback".equals(name)) return com.codename1.util.Callback.class;
        if ("com.codename1.util.CallbackAdapter".equals(name)) return com.codename1.util.CallbackAdapter.class;
        if ("com.codename1.util.CallbackDispatcher".equals(name)) return com.codename1.util.CallbackDispatcher.class;
        if ("com.codename1.util.CaseInsensitiveOrder".equals(name)) return com.codename1.util.CaseInsensitiveOrder.class;
        if ("com.codename1.util.DateUtil".equals(name)) return com.codename1.util.DateUtil.class;
        if ("com.codename1.util.EasyThread".equals(name)) return com.codename1.util.EasyThread.class;
        if ("com.codename1.util.FailureCallback".equals(name)) return com.codename1.util.FailureCallback.class;
        if ("com.codename1.util.LazyValue".equals(name)) return com.codename1.util.LazyValue.class;
        if ("com.codename1.util.MathUtil".equals(name)) return com.codename1.util.MathUtil.class;
        if ("com.codename1.util.OnComplete".equals(name)) return com.codename1.util.OnComplete.class;
        if ("com.codename1.util.RunnableWithResult".equals(name)) return com.codename1.util.RunnableWithResult.class;
        if ("com.codename1.util.RunnableWithResultSync".equals(name)) return com.codename1.util.RunnableWithResultSync.class;
        if ("com.codename1.util.StringUtil".equals(name)) return com.codename1.util.StringUtil.class;
        if ("com.codename1.util.SuccessCallback".equals(name)) return com.codename1.util.SuccessCallback.class;
        if ("com.codename1.util.Wrapper".equals(name)) return com.codename1.util.Wrapper.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.AsyncResource.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.util.AsyncResource();
            }
        }
        if (type == com.codename1.util.BigDecimal.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, java.lang.Integer.class}, false)) {
                return new com.codename1.util.BigDecimal((com.codename1.util.BigInteger) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if (type == com.codename1.util.BigInteger.class) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return new com.codename1.util.BigInteger((byte[]) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.util.BigInteger((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, byte[].class}, false)) {
                return new com.codename1.util.BigInteger(((Number) safeArgs[0]).intValue(), (byte[]) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Random.class}, false)) {
                return new com.codename1.util.BigInteger(((Number) safeArgs[0]).intValue(), (java.util.Random) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return new com.codename1.util.BigInteger((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.util.Random.class}, false)) {
                return new com.codename1.util.BigInteger(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (java.util.Random) safeArgs[2]);
            }
        }
        if (type == com.codename1.util.CStringBuilder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.util.CStringBuilder();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.util.CStringBuilder(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.util.CStringBuilder((java.lang.String) safeArgs[0]);
            }
        }
        if (type == com.codename1.util.DateUtil.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.util.DateUtil();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false)) {
                return new com.codename1.util.DateUtil((java.util.TimeZone) safeArgs[0]);
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
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource[].class}, true)) {
                com.codename1.util.AsyncResource[] varArgs = new com.codename1.util.AsyncResource[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.util.AsyncResource) safeArgs[i];
                }
                return com.codename1.util.AsyncResource.all(varArgs);
            }
        }
        if ("all".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return com.codename1.util.AsyncResource.all((java.util.Collection) safeArgs[0]);
            }
        }
        if ("await".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResource[].class}, true)) {
                com.codename1.util.AsyncResource[] varArgs = new com.codename1.util.AsyncResource[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.util.AsyncResource) safeArgs[i];
                }
                com.codename1.util.AsyncResource.await(varArgs); return null;
            }
        }
        if ("await".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                com.codename1.util.AsyncResource.await((java.util.Collection) safeArgs[0]); return null;
            }
        }
        if ("isCancelled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return com.codename1.util.AsyncResource.isCancelled((java.lang.Throwable) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.AsyncResource.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.util.Base64.decode((byte[]) safeArgs[0]);
            }
        }
        if ("decode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return com.codename1.util.Base64.decode((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("encode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.util.Base64.encode((byte[]) safeArgs[0]);
            }
        }
        if ("encodeNoNewline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.util.Base64.encodeNoNewline((byte[]) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.Base64.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, java.lang.Integer.class}, false)) {
                return com.codename1.util.BigDecimal.getInstance((com.codename1.util.BigInteger) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.BigDecimal.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("probablePrime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Random.class}, false)) {
                return com.codename1.util.BigInteger.probablePrime(((Number) safeArgs[0]).intValue(), (java.util.Random) safeArgs[1]);
            }
        }
        if ("valueOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return com.codename1.util.BigInteger.valueOf(((Number) safeArgs[0]).longValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.BigInteger.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("dispatchError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.FailureCallback.class, java.lang.Throwable.class}, false)) {
                com.codename1.util.CallbackDispatcher.dispatchError((com.codename1.util.FailureCallback) safeArgs[0], (java.lang.Throwable) safeArgs[1]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.util.CallbackDispatcher.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return com.codename1.util.DateUtil.compare((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("compareByDateField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return com.codename1.util.DateUtil.compareByDateField(((Number) safeArgs[0]).longValue());
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date[].class}, true)) {
                java.util.Date[] varArgs = new java.util.Date[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.util.Date) safeArgs[i];
                }
                return com.codename1.util.DateUtil.max(varArgs);
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date[].class}, true)) {
                java.util.Date[] varArgs = new java.util.Date[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.util.Date) safeArgs[i];
                }
                return com.codename1.util.DateUtil.min(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.util.DateUtil.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("addGlobalErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                com.codename1.util.EasyThread.addGlobalErrorListener((com.codename1.util.EasyThread.ErrorListener) safeArgs[0]); return null;
            }
        }
        if ("removeGlobalErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                com.codename1.util.EasyThread.removeGlobalErrorListener((com.codename1.util.EasyThread.ErrorListener) safeArgs[0]); return null;
            }
        }
        if ("start".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.util.EasyThread.start((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.EasyThread.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("acos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.acos(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("asin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.asin(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("atan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.atan(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("atan2".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.atan2(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.compare(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                return com.codename1.util.MathUtil.compare(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("copySign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.copySign(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("copysign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.copysign(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("exp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.exp(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.floor(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return com.codename1.util.MathUtil.floor(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("log".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.log(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("log10".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.log10(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("nextAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.nextAfter(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("pow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.pow(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("round".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.round(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("round".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return com.codename1.util.MathUtil.round(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("scalb".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                return com.codename1.util.MathUtil.scalb(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("scalbn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                return com.codename1.util.MathUtil.scalbn(((Number) safeArgs[0]).doubleValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("ulp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return com.codename1.util.MathUtil.ulp(((Number) safeArgs[0]).doubleValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.MathUtil.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("getBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.getBytes((java.lang.String) safeArgs[0]);
            }
        }
        if ("join".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Iterable.class, java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.join((java.lang.Iterable) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("join".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.join((java.lang.Object[]) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("newString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return com.codename1.util.StringUtil.newString((byte[]) safeArgs[0]);
            }
        }
        if ("newString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.util.StringUtil.newString((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("replaceAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.replaceAll((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
        }
        if ("replaceFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.replaceFirst((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
        }
        if ("setImplementation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.impl.CodenameOneImplementation.class}, false)) {
                com.codename1.util.StringUtil.setImplementation((com.codename1.impl.CodenameOneImplementation) safeArgs[0]); return null;
            }
        }
        if ("tokenize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false)) {
                return com.codename1.util.StringUtil.tokenize((java.lang.String) safeArgs[0], ((Character) safeArgs[1]).charValue());
            }
        }
        if ("tokenize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.tokenize((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("tokenizeString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false)) {
                return com.codename1.util.StringUtil.tokenizeString((java.lang.String) safeArgs[0], ((Character) safeArgs[1]).charValue());
            }
        }
        if ("tokenizeString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.util.StringUtil.tokenizeString((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
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
        if (target instanceof com.codename1.util.BigDecimal) {
            try {
                return invoke1((com.codename1.util.BigDecimal) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.BigInteger) {
            try {
                return invoke2((com.codename1.util.BigInteger) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CStringBuilder) {
            try {
                return invoke3((com.codename1.util.CStringBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CallbackAdapter) {
            try {
                return invoke4((com.codename1.util.CallbackAdapter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CallbackDispatcher) {
            try {
                return invoke5((com.codename1.util.CallbackDispatcher) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.CaseInsensitiveOrder) {
            try {
                return invoke6((com.codename1.util.CaseInsensitiveOrder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.DateUtil) {
            try {
                return invoke7((com.codename1.util.DateUtil) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.EasyThread) {
            try {
                return invoke8((com.codename1.util.EasyThread) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.Callback) {
            try {
                return invoke9((com.codename1.util.Callback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.FailureCallback) {
            try {
                return invoke10((com.codename1.util.FailureCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.RunnableWithResult) {
            try {
                return invoke11((com.codename1.util.RunnableWithResult) target, name, safeArgs);
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
                typedTarget.addListener((com.codename1.util.AsyncResource) safeArgs[0]); return null;
            }
        }
        if ("addObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                typedTarget.addObserver((java.util.Observer) safeArgs[0]); return null;
            }
        }
        if ("asPromise".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.asPromise();
            }
        }
        if ("await".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.await(); return null;
            }
        }
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.cancel(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("countObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.countObservers();
            }
        }
        if ("deleteObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                typedTarget.deleteObserver((java.util.Observer) safeArgs[0]); return null;
            }
        }
        if ("deleteObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.deleteObservers(); return null;
            }
        }
        if ("error".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.error((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        if ("except".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.except((com.codename1.util.SuccessCallback) safeArgs[0]);
            }
        }
        if ("except".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                return typedTarget.except((com.codename1.util.SuccessCallback) safeArgs[0], (com.codename1.util.EasyThread) safeArgs[1]);
            }
        }
        if ("hasChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasChanged();
            }
        }
        if ("isCancelled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isCancelled();
            }
        }
        if ("isDone".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDone();
            }
        }
        if ("isReady".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReady();
            }
        }
        if ("notifyObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.notifyObservers(); return null;
            }
        }
        if ("notifyObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.notifyObservers((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.AsyncResult.class}, false)) {
                typedTarget.onResult((com.codename1.util.AsyncResult) safeArgs[0]); return null;
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                return typedTarget.ready((com.codename1.util.SuccessCallback) safeArgs[0]);
            }
        }
        if ("ready".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class, com.codename1.util.EasyThread.class}, false)) {
                return typedTarget.ready((com.codename1.util.SuccessCallback) safeArgs[0], (com.codename1.util.EasyThread) safeArgs[1]);
            }
        }
        if ("waitFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.waitFor(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.util.BigDecimal typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                return typedTarget.add((com.codename1.util.BigDecimal) safeArgs[0]);
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.add((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("adjustScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.adjustScale(((Number) safeArgs[0]).intValue());
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                return typedTarget.compareTo((com.codename1.util.BigDecimal) safeArgs[0]);
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.compareTo((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("divide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                return typedTarget.divide((com.codename1.util.BigDecimal) safeArgs[0]);
            }
        }
        if ("divide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.divide((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.floor();
            }
        }
        if ("getScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getScale();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.intValue();
            }
        }
        if ("longValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.longValue();
            }
        }
        if ("multiply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                return typedTarget.multiply((com.codename1.util.BigDecimal) safeArgs[0]);
            }
        }
        if ("multiply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.multiply((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("negate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.negate();
            }
        }
        if ("round".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.round();
            }
        }
        if ("shiftLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shiftLeft(((Number) safeArgs[0]).intValue());
            }
        }
        if ("subtract".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigDecimal.class}, false)) {
                return typedTarget.subtract((com.codename1.util.BigDecimal) safeArgs[0]);
            }
        }
        if ("subtract".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.subtract((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.util.BigInteger typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("abs".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.abs();
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.add((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("and".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.and((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("andNot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.andNot((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("bitCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.bitCount();
            }
        }
        if ("bitLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.bitLength();
            }
        }
        if ("byteValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.byteValue();
            }
        }
        if ("clearBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.clearBit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.compareTo((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.compareTo((java.lang.Object) safeArgs[0]);
            }
        }
        if ("divide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.divide((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("divideAndRemainder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.divideAndRemainder((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("flipBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.flipBit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("gcd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.gcd((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("getLowestSetBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLowestSetBit();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("intValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.intValue();
            }
        }
        if ("isProbablePrime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.isProbablePrime(((Number) safeArgs[0]).intValue());
            }
        }
        if ("longValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.longValue();
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.max((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.min((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("mod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.mod((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("modInverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.modInverse((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("modPow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class, com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.modPow((com.codename1.util.BigInteger) safeArgs[0], (com.codename1.util.BigInteger) safeArgs[1]);
            }
        }
        if ("multiply".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.multiply((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("negate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.negate();
            }
        }
        if ("not".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.not();
            }
        }
        if ("or".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.or((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("pow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.pow(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remainder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.remainder((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("setBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.setBit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shiftLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shiftLeft(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shiftRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shiftRight(((Number) safeArgs[0]).intValue());
            }
        }
        if ("signum".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.signum();
            }
        }
        if ("subtract".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.subtract((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        if ("testBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.testBit(((Number) safeArgs[0]).intValue());
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
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.toString(((Number) safeArgs[0]).intValue());
            }
        }
        if ("xor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.BigInteger.class}, false)) {
                return typedTarget.xor((com.codename1.util.BigInteger) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.util.CStringBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.append(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                return typedTarget.append(((Character) safeArgs[0]).charValue());
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return typedTarget.append((char[]) safeArgs[0]);
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                return typedTarget.append(((Number) safeArgs[0]).doubleValue());
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.append(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.append(((Number) safeArgs[0]).intValue());
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.append((java.lang.Object) safeArgs[0]);
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.append((java.lang.String) safeArgs[0]);
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.append(((Number) safeArgs[0]).longValue());
            }
        }
        if ("append".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.append((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("appendCodePoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.appendCodePoint(((Number) safeArgs[0]).intValue());
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.delete(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("deleteCharAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.deleteCharAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Character.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), ((Character) safeArgs[1]).charValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), (char[]) safeArgs[1]);
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Double.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), (java.lang.Object) safeArgs[1]);
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1]);
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).longValue());
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.insert(((Number) safeArgs[0]).intValue(), (char[]) safeArgs[1], ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                return typedTarget.replace(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (java.lang.String) safeArgs[2]);
            }
        }
        if ("reverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.reverse();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.util.CallbackAdapter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                typedTarget.onError((java.lang.Object) safeArgs[0], (java.lang.Throwable) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.util.CallbackDispatcher typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.run(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.util.CaseInsensitiveOrder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.compare((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.util.DateUtil typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.getOffset(((Number) safeArgs[0]).longValue());
            }
        }
        if ("getTimeAgo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                return typedTarget.getTimeAgo((java.util.Date) safeArgs[0]);
            }
        }
        if ("inDaylightTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                return typedTarget.inDaylightTime((java.util.Date) safeArgs[0]);
            }
        }
        if ("isSameDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameDay((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameDay((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("isSameHour".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameHour((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameHour".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameHour((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("isSameMinute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameMinute((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameMinute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameMinute((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("isSameMonth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameMonth((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameMonth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameMonth((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("isSameSecond".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameSecond((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameSecond".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameSecond((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("isSameTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameTime((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameTime((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        if ("isSameYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Calendar.class, java.util.Calendar.class}, false)) {
                return typedTarget.isSameYear((java.util.Calendar) safeArgs[0], (java.util.Calendar) safeArgs[1]);
            }
        }
        if ("isSameYear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.util.Date.class}, false)) {
                return typedTarget.isSameYear((java.util.Date) safeArgs[0], (java.util.Date) safeArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.util.EasyThread typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                typedTarget.addErrorListener((com.codename1.util.EasyThread.ErrorListener) safeArgs[0]); return null;
            }
        }
        if ("isThisIt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isThisIt();
            }
        }
        if ("kill".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.kill(); return null;
            }
        }
        if ("removeErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.EasyThread.ErrorListener.class}, false)) {
                typedTarget.removeErrorListener((com.codename1.util.EasyThread.ErrorListener) safeArgs[0]); return null;
            }
        }
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                typedTarget.run((java.lang.Runnable) safeArgs[0]); return null;
            }
        }
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.RunnableWithResult.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.run((com.codename1.util.RunnableWithResult) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("runAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                typedTarget.runAndWait((java.lang.Runnable) safeArgs[0]); return null;
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPriority(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.util.Callback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                typedTarget.onError((java.lang.Object) safeArgs[0], (java.lang.Throwable) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.util.FailureCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Throwable.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                typedTarget.onError((java.lang.Object) safeArgs[0], (java.lang.Throwable) safeArgs[1], ((Number) safeArgs[2]).intValue(), (java.lang.String) safeArgs[3]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.util.RunnableWithResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("run".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.run((com.codename1.util.SuccessCallback) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.util.BigInteger.class) {
            if ("ONE".equals(name)) return com.codename1.util.BigInteger.ONE;
            if ("ZERO".equals(name)) return com.codename1.util.BigInteger.ZERO;
        }
        if (type == com.codename1.util.DateUtil.class) {
            if ("DAY".equals(name)) return com.codename1.util.DateUtil.DAY;
            if ("HOUR".equals(name)) return com.codename1.util.DateUtil.HOUR;
            if ("MILLISECOND".equals(name)) return com.codename1.util.DateUtil.MILLISECOND;
            if ("MINUTE".equals(name)) return com.codename1.util.DateUtil.MINUTE;
            if ("MONTH".equals(name)) return com.codename1.util.DateUtil.MONTH;
            if ("SECOND".equals(name)) return com.codename1.util.DateUtil.SECOND;
            if ("YEAR".equals(name)) return com.codename1.util.DateUtil.YEAR;
        }
        throw unsupportedStaticField(type, name);
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
