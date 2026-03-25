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
            return com.codename1.ui.validation.Constraint.class;
        }
        if ("ExistInConstraint".equals(simpleName)) {
            return com.codename1.ui.validation.ExistInConstraint.class;
        }
        if ("GroupConstraint".equals(simpleName)) {
            return com.codename1.ui.validation.GroupConstraint.class;
        }
        if ("LengthConstraint".equals(simpleName)) {
            return com.codename1.ui.validation.LengthConstraint.class;
        }
        if ("NotConstraint".equals(simpleName)) {
            return com.codename1.ui.validation.NotConstraint.class;
        }
        if ("NumericConstraint".equals(simpleName)) {
            return com.codename1.ui.validation.NumericConstraint.class;
        }
        if ("RegexConstraint".equals(simpleName)) {
            return com.codename1.ui.validation.RegexConstraint.class;
        }
        if ("Validator".equals(simpleName)) {
            return com.codename1.ui.validation.Validator.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.validation.ExistInConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return new com.codename1.ui.validation.ExistInConstraint((java.util.List) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                return new com.codename1.ui.validation.ExistInConstraint((java.lang.String[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.ExistInConstraint((java.util.List) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.ExistInConstraint((java.lang.String[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Boolean.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Boolean.class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.ExistInConstraint((java.util.List) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.String) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.ExistInConstraint((java.lang.String[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ui.validation.GroupConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Constraint[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.validation.Constraint[].class}, true);
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.validation.Constraint) adaptedArgs[i];
                }
                return new com.codename1.ui.validation.GroupConstraint(varArgs);
            }
        }
        if (type == com.codename1.ui.validation.LengthConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.validation.LengthConstraint(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.LengthConstraint(((Number) adaptedArgs[0]).intValue(), (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ui.validation.NotConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Constraint[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.validation.Constraint[].class}, true);
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.validation.Constraint) adaptedArgs[i];
                }
                return new com.codename1.ui.validation.NotConstraint(varArgs);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.validation.Constraint[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.validation.Constraint[].class}, true);
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.ui.validation.Constraint) adaptedArgs[i];
                }
                return new com.codename1.ui.validation.NotConstraint((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if (type == com.codename1.ui.validation.NumericConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return new com.codename1.ui.validation.NumericConstraint(((Boolean) adaptedArgs[0]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.NumericConstraint(((Boolean) adaptedArgs[0]).booleanValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), (java.lang.String) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.ui.validation.RegexConstraint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ui.validation.RegexConstraint((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ui.validation.Validator.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
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
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.RegexConstraint.validEmail();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.validation.RegexConstraint.validEmail((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("validURL".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.RegexConstraint.validURL();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.validation.RegexConstraint.validURL((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.validation.RegexConstraint.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getDefaultValidationEmblemPositionX".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.Validator.getDefaultValidationEmblemPositionX();
            }
        }
        if ("getDefaultValidationEmblemPositionY".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.Validator.getDefaultValidationEmblemPositionY();
            }
        }
        if ("getDefaultValidationFailedEmblem".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.Validator.getDefaultValidationFailedEmblem();
            }
        }
        if ("getDefaultValidationFailureHighlightMode".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.Validator.getDefaultValidationFailureHighlightMode();
            }
        }
        if ("isValidateOnEveryKey".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.validation.Validator.isValidateOnEveryKey();
            }
        }
        if ("setDefaultValidationEmblemPositionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                com.codename1.ui.validation.Validator.setDefaultValidationEmblemPositionX(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setDefaultValidationEmblemPositionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                com.codename1.ui.validation.Validator.setDefaultValidationEmblemPositionY(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setDefaultValidationFailedEmblem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                com.codename1.ui.validation.Validator.setDefaultValidationFailedEmblem((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultValidationFailureHighlightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Validator.HighlightMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.validation.Validator.HighlightMode.class}, false);
                com.codename1.ui.validation.Validator.setDefaultValidationFailureHighlightMode((com.codename1.ui.validation.Validator.HighlightMode) adaptedArgs[0]); return null;
            }
        }
        if ("setValidateOnEveryKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.ui.validation.Validator.setValidateOnEveryKey(((Boolean) adaptedArgs[0]).booleanValue()); return null;
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
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.validation.GroupConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.validation.LengthConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.validation.NotConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.validation.NumericConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.validation.RegexConstraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.validation.Validator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.validation.Constraint[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.validation.Constraint[].class}, true);
                com.codename1.ui.validation.Constraint[] varArgs = new com.codename1.ui.validation.Constraint[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.ui.validation.Constraint) adaptedArgs[i];
                }
                return typedTarget.addConstraint((com.codename1.ui.Component) adaptedArgs[0], varArgs);
            }
        }
        if ("addSubmitButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addSubmitButtons(varArgs);
            }
        }
        if ("bindDataListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.bindDataListener((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("getErrorMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getErrorMessage((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getErrorMessageUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrorMessageUIID();
            }
        }
        if ("getValidationEmblemPositionX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidationEmblemPositionX();
            }
        }
        if ("getValidationEmblemPositionY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidationEmblemPositionY();
            }
        }
        if ("getValidationFailedEmblem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidationFailedEmblem();
            }
        }
        if ("getValidationFailureHighlightMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidationFailureHighlightMode();
            }
        }
        if ("isShowErrorMessageForFocusedComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowErrorMessageForFocusedComponent();
            }
        }
        if ("isValid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValid();
            }
        }
        if ("setErrorMessageUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setErrorMessageUIID((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShowErrorMessageForFocusedComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShowErrorMessageForFocusedComponent(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setValidationEmblemPositionX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setValidationEmblemPositionX(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setValidationEmblemPositionY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setValidationEmblemPositionY(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setValidationFailedEmblem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setValidationFailedEmblem((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setValidationFailureHighlightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.validation.Validator.HighlightMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.validation.Validator.HighlightMode.class}, false);
                typedTarget.setValidationFailureHighlightMode((com.codename1.ui.validation.Validator.HighlightMode) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.validation.Constraint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDefaultFailMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFailMessage();
            }
        }
        if ("isValid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.isValid((java.lang.Object) adaptedArgs[0]);
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
