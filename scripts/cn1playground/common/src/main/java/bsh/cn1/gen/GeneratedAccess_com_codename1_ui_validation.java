package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_validation {
    private GeneratedAccess_com_codename1_ui_validation() {
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
        if ("Constraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.Constraint");
            }
            return com.codename1.ui.validation.Constraint.class;
        }
        if ("ExistInConstraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.ExistInConstraint");
            }
            return com.codename1.ui.validation.ExistInConstraint.class;
        }
        if ("GroupConstraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.GroupConstraint");
            }
            return com.codename1.ui.validation.GroupConstraint.class;
        }
        if ("LengthConstraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.LengthConstraint");
            }
            return com.codename1.ui.validation.LengthConstraint.class;
        }
        if ("NotConstraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.NotConstraint");
            }
            return com.codename1.ui.validation.NotConstraint.class;
        }
        if ("NumericConstraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.NumericConstraint");
            }
            return com.codename1.ui.validation.NumericConstraint.class;
        }
        if ("RegexConstraint".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.RegexConstraint");
            }
            return com.codename1.ui.validation.RegexConstraint.class;
        }
        if ("Validator".equals(simpleName)) {
            if (simpleName != null) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.validation -> com.codename1.ui.validation.Validator");
            }
            return com.codename1.ui.validation.Validator.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.validation.ExistInConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                return new com.codename1.ui.validation.ExistInConstraint((java.lang.String[]) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                return new com.codename1.ui.validation.ExistInConstraint((java.util.List) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.ExistInConstraint((java.lang.String[]) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.ExistInConstraint((java.util.List) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.ExistInConstraint((java.lang.String[]) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue(), (java.lang.String) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Boolean.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.ExistInConstraint((java.util.List) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue(), (java.lang.String) safeArgs[2]);
            }
        }
        if (type == com.codename1.ui.validation.GroupConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Constraint[].class}, true)) {
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.validation.Constraint) safeArgs[i];
                }
                return new com.codename1.ui.validation.GroupConstraint(varArgs);
            }
        }
        if (type == com.codename1.ui.validation.LengthConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new com.codename1.ui.validation.LengthConstraint(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.LengthConstraint(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.ui.validation.NotConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Constraint[].class}, true)) {
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.validation.Constraint) safeArgs[i];
                }
                return new com.codename1.ui.validation.NotConstraint(varArgs);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.validation.Constraint[].class}, true)) {
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.ui.validation.Constraint) safeArgs[i];
                }
                return new com.codename1.ui.validation.NotConstraint((java.lang.String) safeArgs[0], varArgs);
            }
        }
        if (type == com.codename1.ui.validation.NumericConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return new com.codename1.ui.validation.NumericConstraint(((Boolean) safeArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.NumericConstraint(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), (java.lang.String) safeArgs[3]);
            }
        }
        if (type == com.codename1.ui.validation.RegexConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.validation.RegexConstraint((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.ui.validation.Validator.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.validation.Validator();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.validation.RegexConstraint.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.validation.Validator.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("validEmail".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.RegexConstraint.validEmail();
            }
        }
        if ("validEmail".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.validation.RegexConstraint.validEmail((java.lang.String) safeArgs[0]);
            }
        }
        if ("validURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.RegexConstraint.validURL();
            }
        }
        if ("validURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.validation.RegexConstraint.validURL((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.validation.RegexConstraint.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getDefaultValidationEmblemPositionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.Validator.getDefaultValidationEmblemPositionX();
            }
        }
        if ("getDefaultValidationEmblemPositionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.Validator.getDefaultValidationEmblemPositionY();
            }
        }
        if ("getDefaultValidationFailedEmblem".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.Validator.getDefaultValidationFailedEmblem();
            }
        }
        if ("getDefaultValidationFailureHighlightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.Validator.getDefaultValidationFailureHighlightMode();
            }
        }
        if ("isValidateOnEveryKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.validation.Validator.isValidateOnEveryKey();
            }
        }
        if ("setDefaultValidationEmblemPositionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                com.codename1.ui.validation.Validator.setDefaultValidationEmblemPositionX(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setDefaultValidationEmblemPositionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                com.codename1.ui.validation.Validator.setDefaultValidationEmblemPositionY(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setDefaultValidationFailedEmblem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                com.codename1.ui.validation.Validator.setDefaultValidationFailedEmblem((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setDefaultValidationFailureHighlightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Validator.HighlightMode.class}, false)) {
                com.codename1.ui.validation.Validator.setDefaultValidationFailureHighlightMode((com.codename1.ui.validation.Validator.HighlightMode) safeArgs[0]); return null;
            }
        }
        if ("setValidateOnEveryKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                com.codename1.ui.validation.Validator.setValidateOnEveryKey(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.validation.Validator.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.validation.ExistInConstraint) {
            try {
                return invoke0((com.codename1.ui.validation.ExistInConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.GroupConstraint) {
            try {
                return invoke1((com.codename1.ui.validation.GroupConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.LengthConstraint) {
            try {
                return invoke2((com.codename1.ui.validation.LengthConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.NotConstraint) {
            try {
                return invoke3((com.codename1.ui.validation.NotConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.NumericConstraint) {
            try {
                return invoke4((com.codename1.ui.validation.NumericConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.RegexConstraint) {
            try {
                return invoke5((com.codename1.ui.validation.RegexConstraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.Validator) {
            try {
                return invoke6((com.codename1.ui.validation.Validator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.validation.Constraint) {
            try {
                return invoke7((com.codename1.ui.validation.Constraint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.validation.ExistInConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.validation.GroupConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.validation.LengthConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.validation.NotConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.validation.NumericConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.validation.RegexConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.validation.Validator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.validation.Constraint[].class}, true)) {
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.ui.validation.Constraint) safeArgs[i];
                }
                return typedTarget.addConstraint((com.codename1.ui.Component) safeArgs[0], varArgs);
            }
        }
        if ("addSubmitButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) safeArgs[i];
                }
                return typedTarget.addSubmitButtons(varArgs);
            }
        }
        if ("bindDataListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.bindDataListener((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("getErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                return typedTarget.getErrorMessage((com.codename1.ui.Component) safeArgs[0]);
            }
        }
        if ("getErrorMessageUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getErrorMessageUIID();
            }
        }
        if ("getValidationEmblemPositionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValidationEmblemPositionX();
            }
        }
        if ("getValidationEmblemPositionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValidationEmblemPositionY();
            }
        }
        if ("getValidationFailedEmblem".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValidationFailedEmblem();
            }
        }
        if ("getValidationFailureHighlightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getValidationFailureHighlightMode();
            }
        }
        if ("isShowErrorMessageForFocusedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShowErrorMessageForFocusedComponent();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isValid();
            }
        }
        if ("setErrorMessageUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setErrorMessageUIID((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setShowErrorMessageForFocusedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setShowErrorMessageForFocusedComponent(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setValidationEmblemPositionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setValidationEmblemPositionX(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setValidationEmblemPositionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setValidationEmblemPositionY(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setValidationFailedEmblem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setValidationFailedEmblem((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setValidationFailureHighlightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Validator.HighlightMode.class}, false)) {
                typedTarget.setValidationFailureHighlightMode((com.codename1.ui.validation.Validator.HighlightMode) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.validation.Constraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.isValid((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
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
