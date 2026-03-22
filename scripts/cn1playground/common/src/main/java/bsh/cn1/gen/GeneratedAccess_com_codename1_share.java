package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_share {
    private GeneratedAccess_com_codename1_share() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.share.EmailShare".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.share -> com.codename1.share.EmailShare");
            }
            return com.codename1.share.EmailShare.class;
        }
        if ("com.codename1.share.FacebookShare".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.share -> com.codename1.share.FacebookShare");
            }
            return com.codename1.share.FacebookShare.class;
        }
        if ("com.codename1.share.SMSShare".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.share -> com.codename1.share.SMSShare");
            }
            return com.codename1.share.SMSShare.class;
        }
        if ("com.codename1.share.ShareService".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.share -> com.codename1.share.ShareService");
            }
            return com.codename1.share.ShareService.class;
        }
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.share.EmailShare.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.share.EmailShare();
            }
        }
        if (type == com.codename1.share.FacebookShare.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.share.FacebookShare();
            }
        }
        if (type == com.codename1.share.SMSShare.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.share.SMSShare();
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
        if (target instanceof com.codename1.share.EmailShare) {
            try {
                return invoke0((com.codename1.share.EmailShare) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.FacebookShare) {
            try {
                return invoke1((com.codename1.share.FacebookShare) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.SMSShare) {
            try {
                return invoke2((com.codename1.share.SMSShare) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.ShareService) {
            try {
                return invoke3((com.codename1.share.ShareService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.share.EmailShare typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) safeArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getClientProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.putClientProperty((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCommandName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setDisabledIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisposesDialog(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setIconFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setIconGapMM(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.setImage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.setMaterialIcon(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMaterialIconSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setMessage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                typedTarget.setOriginalForm((com.codename1.ui.Form) safeArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPressedIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setRolloverIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.share.FacebookShare typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) safeArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getClientProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.putClientProperty((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCommandName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setDisabledIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisposesDialog(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setIconFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setIconGapMM(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.setImage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.setMaterialIcon(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMaterialIconSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setMessage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                typedTarget.setOriginalForm((com.codename1.ui.Form) safeArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPressedIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setRolloverIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.share.SMSShare typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) safeArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getClientProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.putClientProperty((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCommandName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setDisabledIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisposesDialog(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setIconFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setIconGapMM(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.setImage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.setMaterialIcon(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMaterialIconSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setMessage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                typedTarget.setOriginalForm((com.codename1.ui.Form) safeArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPressedIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setRolloverIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.share.ShareService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) safeArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getClientProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                typedTarget.putClientProperty((java.lang.String) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCommandName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setDisabledIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDisposesDialog(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setEnabled(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setIconFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setIconGapMM(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.setImage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.setMaterialIcon(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMaterialIconSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setMessage((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                typedTarget.setOriginalForm((com.codename1.ui.Form) safeArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPressedIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setRolloverIcon((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.share((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
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
