/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_share {
    private GeneratedAccess_com_codename1_share() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("EmailShare".equals(simpleName)) {
            return com.codename1.share.EmailShare.class;
        }
        if ("FacebookShare".equals(simpleName)) {
            return com.codename1.share.FacebookShare.class;
        }
        if ("SMSShare".equals(simpleName)) {
            return com.codename1.share.SMSShare.class;
        }
        if ("ShareResult".equals(simpleName)) {
            return com.codename1.share.ShareResult.class;
        }
        if ("ShareResultListener".equals(simpleName)) {
            return com.codename1.share.ShareResultListener.class;
        }
        if ("ShareService".equals(simpleName)) {
            return com.codename1.share.ShareService.class;
        }
        if ("SharedContent".equals(simpleName)) {
            return com.codename1.share.SharedContent.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.share.SharedContent.Builder.class;
        }
        if ("Item".equals(simpleName)) {
            return com.codename1.share.SharedContent.Item.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.share.EmailShare.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.share.EmailShare();
            }
        }
        if (type == com.codename1.share.FacebookShare.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.share.FacebookShare();
            }
        }
        if (type == com.codename1.share.SMSShare.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.share.SMSShare();
            }
        }
        if (type == com.codename1.share.SharedContent.Builder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.share.SharedContent.Builder();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.share.ShareResult.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.share.SharedContent.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("dismissed".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.share.ShareResult.dismissed();
            }
        }
        if ("failed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.share.ShareResult.failed((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("sharedTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.share.ShareResult.sharedTo((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.share.ShareResult.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("builder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.share.SharedContent.builder();
            }
        }
        throw unsupportedStatic(com.codename1.share.SharedContent.class, name, safeArgs);
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
        if (target instanceof com.codename1.share.ShareResult) {
            try {
                return invoke3((com.codename1.share.ShareResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.ShareService) {
            try {
                return invoke4((com.codename1.share.ShareService) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.SharedContent) {
            try {
                return invoke5((com.codename1.share.SharedContent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.SharedContent.Builder) {
            try {
                return invoke6((com.codename1.share.SharedContent.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.SharedContent.Item) {
            try {
                return invoke7((com.codename1.share.SharedContent.Item) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.share.ShareResultListener) {
            try {
                return invoke8((com.codename1.share.ShareResultListener) target, name, safeArgs);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false);
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) adaptedArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDesktopMenu".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopMenu();
            }
        }
        if ("getDesktopShortcutKeyChar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutKeyChar();
            }
        }
        if ("getDesktopShortcutModifiers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutModifiers();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("getShareResultListener".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShareResultListener();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCommandName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDesktopMenu".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDesktopMenu((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDesktopShortcut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setDisabledIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisposesDialog(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setIconFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setIconGapMM(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setImage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setMaterialIcon(((Character) adaptedArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaterialIconSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setMessage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                typedTarget.setOriginalForm((com.codename1.ui.Form) adaptedArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPressedIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setRolloverIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setShareResultListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false);
                typedTarget.setShareResultListener((com.codename1.share.ShareResultListener) adaptedArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.share.FacebookShare typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false);
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) adaptedArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDesktopMenu".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopMenu();
            }
        }
        if ("getDesktopShortcutKeyChar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutKeyChar();
            }
        }
        if ("getDesktopShortcutModifiers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutModifiers();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("getShareResultListener".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShareResultListener();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCommandName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDesktopMenu".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDesktopMenu((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDesktopShortcut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setDisabledIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisposesDialog(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setIconFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setIconGapMM(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setImage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setMaterialIcon(((Character) adaptedArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaterialIconSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setMessage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                typedTarget.setOriginalForm((com.codename1.ui.Form) adaptedArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPressedIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setRolloverIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setShareResultListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false);
                typedTarget.setShareResultListener((com.codename1.share.ShareResultListener) adaptedArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.share.SMSShare typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false);
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) adaptedArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDesktopMenu".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopMenu();
            }
        }
        if ("getDesktopShortcutKeyChar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutKeyChar();
            }
        }
        if ("getDesktopShortcutModifiers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutModifiers();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("getShareResultListener".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShareResultListener();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCommandName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDesktopMenu".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDesktopMenu((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDesktopShortcut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setDisabledIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisposesDialog(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setIconFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setIconGapMM(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setImage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setMaterialIcon(((Character) adaptedArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaterialIconSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setMessage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                typedTarget.setOriginalForm((com.codename1.ui.Form) adaptedArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPressedIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setRolloverIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setShareResultListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false);
                typedTarget.setShareResultListener((com.codename1.share.ShareResultListener) adaptedArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.share.ShareResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getPackageName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPackageName();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        if ("isDismissed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDismissed();
            }
        }
        if ("isFailed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFailed();
            }
        }
        if ("isSharedTo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSharedTo();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.share.ShareService typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionEvent.class}, false);
                typedTarget.actionPerformed((com.codename1.ui.events.ActionEvent) adaptedArgs[0]); return null;
            }
        }
        if ("canShareImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.canShareImage();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getCommandName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommandName();
            }
        }
        if ("getDesktopMenu".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopMenu();
            }
        }
        if ("getDesktopShortcutKeyChar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutKeyChar();
            }
        }
        if ("getDesktopShortcutModifiers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDesktopShortcutModifiers();
            }
        }
        if ("getDisabledIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledIcon();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getIconFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconFont();
            }
        }
        if ("getIconGapMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconGapMM();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaterialIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIcon();
            }
        }
        if ("getMaterialIconSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaterialIconSize();
            }
        }
        if ("getOriginal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOriginal();
            }
        }
        if ("getPressedIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedIcon();
            }
        }
        if ("getRolloverIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRolloverIcon();
            }
        }
        if ("getShareResultListener".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShareResultListener();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isDisposesDialog".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDisposesDialog();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCommandName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCommandName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDesktopMenu".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDesktopMenu((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDesktopShortcut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class, java.lang.Integer.class}, false);
                return typedTarget.setDesktopShortcut(((Character) adaptedArgs[0]).charValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setDisabledIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setDisabledIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDisposesDialog".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDisposesDialog(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setIconFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setIconFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
        }
        if ("setIconGapMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setIconGapMM(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setImage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setMaterialIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setMaterialIcon(((Character) adaptedArgs[0]).charValue()); return null;
            }
        }
        if ("setMaterialIconSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMaterialIconSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setMessage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOriginalForm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                typedTarget.setOriginalForm((com.codename1.ui.Form) adaptedArgs[0]); return null;
            }
        }
        if ("setPressedIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPressedIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setRolloverIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setRolloverIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setShareResultListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.share.ShareResultListener.class}, false);
                typedTarget.setShareResultListener((com.codename1.share.ShareResultListener) adaptedArgs[0]); return null;
            }
        }
        if ("share".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.share((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.share.SharedContent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFirstItem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirstItem();
            }
        }
        if ("getItems".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItems();
            }
        }
        if ("getSubject".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubject();
            }
        }
        if ("hasFiles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFiles();
            }
        }
        if ("hasText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasText();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.share.SharedContent.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.addFile((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("addImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.addImage((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("addText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.addText((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("addUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.addUrl((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("subject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.subject((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.share.SharedContent.Item typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getFilePath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFilePath();
            }
        }
        if ("getMimeType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMimeType();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.share.ShareResultListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.share.ShareResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.share.ShareResult.class}, false);
                typedTarget.onResult((com.codename1.share.ShareResult) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.share.EmailShare.class) return getStaticField0(name);
        if (type == com.codename1.share.FacebookShare.class) return getStaticField1(name);
        if (type == com.codename1.share.SMSShare.class) return getStaticField2(name);
        if (type == com.codename1.share.ShareResult.class) return getStaticField3(name);
        if (type == com.codename1.share.ShareService.class) return getStaticField4(name);
        if (type == com.codename1.share.SharedContent.class) return getStaticField5(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DESKTOP_MENU".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU;
        if ("DESKTOP_MENU_ABOUT".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_ABOUT;
        if ("DESKTOP_MENU_APP".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_APP;
        if ("DESKTOP_MENU_EDIT".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_EDIT;
        if ("DESKTOP_MENU_FILE".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_FILE;
        if ("DESKTOP_MENU_HELP".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_HELP;
        if ("DESKTOP_MENU_PREFERENCES".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_PREFERENCES;
        if ("DESKTOP_MENU_QUIT".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_QUIT;
        if ("DESKTOP_MENU_VIEW".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_VIEW;
        if ("DESKTOP_MENU_WINDOW".equals(name)) return com.codename1.share.EmailShare.DESKTOP_MENU_WINDOW;
        if ("DESKTOP_SHORTCUT_KEY".equals(name)) return com.codename1.share.EmailShare.DESKTOP_SHORTCUT_KEY;
        if ("DESKTOP_SHORTCUT_MODIFIERS".equals(name)) return com.codename1.share.EmailShare.DESKTOP_SHORTCUT_MODIFIERS;
        if ("DESKTOP_SHORTCUT_MODIFIER_ALT".equals(name)) return com.codename1.share.EmailShare.DESKTOP_SHORTCUT_MODIFIER_ALT;
        if ("DESKTOP_SHORTCUT_MODIFIER_PRIMARY".equals(name)) return com.codename1.share.EmailShare.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
        if ("DESKTOP_SHORTCUT_MODIFIER_SHIFT".equals(name)) return com.codename1.share.EmailShare.DESKTOP_SHORTCUT_MODIFIER_SHIFT;
        throw unsupportedStaticField(com.codename1.share.EmailShare.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("DESKTOP_MENU".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU;
        if ("DESKTOP_MENU_ABOUT".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_ABOUT;
        if ("DESKTOP_MENU_APP".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_APP;
        if ("DESKTOP_MENU_EDIT".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_EDIT;
        if ("DESKTOP_MENU_FILE".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_FILE;
        if ("DESKTOP_MENU_HELP".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_HELP;
        if ("DESKTOP_MENU_PREFERENCES".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_PREFERENCES;
        if ("DESKTOP_MENU_QUIT".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_QUIT;
        if ("DESKTOP_MENU_VIEW".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_VIEW;
        if ("DESKTOP_MENU_WINDOW".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_MENU_WINDOW;
        if ("DESKTOP_SHORTCUT_KEY".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_SHORTCUT_KEY;
        if ("DESKTOP_SHORTCUT_MODIFIERS".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_SHORTCUT_MODIFIERS;
        if ("DESKTOP_SHORTCUT_MODIFIER_ALT".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_SHORTCUT_MODIFIER_ALT;
        if ("DESKTOP_SHORTCUT_MODIFIER_PRIMARY".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
        if ("DESKTOP_SHORTCUT_MODIFIER_SHIFT".equals(name)) return com.codename1.share.FacebookShare.DESKTOP_SHORTCUT_MODIFIER_SHIFT;
        throw unsupportedStaticField(com.codename1.share.FacebookShare.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("DESKTOP_MENU".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU;
        if ("DESKTOP_MENU_ABOUT".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_ABOUT;
        if ("DESKTOP_MENU_APP".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_APP;
        if ("DESKTOP_MENU_EDIT".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_EDIT;
        if ("DESKTOP_MENU_FILE".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_FILE;
        if ("DESKTOP_MENU_HELP".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_HELP;
        if ("DESKTOP_MENU_PREFERENCES".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_PREFERENCES;
        if ("DESKTOP_MENU_QUIT".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_QUIT;
        if ("DESKTOP_MENU_VIEW".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_VIEW;
        if ("DESKTOP_MENU_WINDOW".equals(name)) return com.codename1.share.SMSShare.DESKTOP_MENU_WINDOW;
        if ("DESKTOP_SHORTCUT_KEY".equals(name)) return com.codename1.share.SMSShare.DESKTOP_SHORTCUT_KEY;
        if ("DESKTOP_SHORTCUT_MODIFIERS".equals(name)) return com.codename1.share.SMSShare.DESKTOP_SHORTCUT_MODIFIERS;
        if ("DESKTOP_SHORTCUT_MODIFIER_ALT".equals(name)) return com.codename1.share.SMSShare.DESKTOP_SHORTCUT_MODIFIER_ALT;
        if ("DESKTOP_SHORTCUT_MODIFIER_PRIMARY".equals(name)) return com.codename1.share.SMSShare.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
        if ("DESKTOP_SHORTCUT_MODIFIER_SHIFT".equals(name)) return com.codename1.share.SMSShare.DESKTOP_SHORTCUT_MODIFIER_SHIFT;
        throw unsupportedStaticField(com.codename1.share.SMSShare.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("STATUS_DISMISSED".equals(name)) return com.codename1.share.ShareResult.STATUS_DISMISSED;
        if ("STATUS_FAILED".equals(name)) return com.codename1.share.ShareResult.STATUS_FAILED;
        if ("STATUS_SHARED_TO".equals(name)) return com.codename1.share.ShareResult.STATUS_SHARED_TO;
        throw unsupportedStaticField(com.codename1.share.ShareResult.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("DESKTOP_MENU".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU;
        if ("DESKTOP_MENU_ABOUT".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_ABOUT;
        if ("DESKTOP_MENU_APP".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_APP;
        if ("DESKTOP_MENU_EDIT".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_EDIT;
        if ("DESKTOP_MENU_FILE".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_FILE;
        if ("DESKTOP_MENU_HELP".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_HELP;
        if ("DESKTOP_MENU_PREFERENCES".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_PREFERENCES;
        if ("DESKTOP_MENU_QUIT".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_QUIT;
        if ("DESKTOP_MENU_VIEW".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_VIEW;
        if ("DESKTOP_MENU_WINDOW".equals(name)) return com.codename1.share.ShareService.DESKTOP_MENU_WINDOW;
        if ("DESKTOP_SHORTCUT_KEY".equals(name)) return com.codename1.share.ShareService.DESKTOP_SHORTCUT_KEY;
        if ("DESKTOP_SHORTCUT_MODIFIERS".equals(name)) return com.codename1.share.ShareService.DESKTOP_SHORTCUT_MODIFIERS;
        if ("DESKTOP_SHORTCUT_MODIFIER_ALT".equals(name)) return com.codename1.share.ShareService.DESKTOP_SHORTCUT_MODIFIER_ALT;
        if ("DESKTOP_SHORTCUT_MODIFIER_PRIMARY".equals(name)) return com.codename1.share.ShareService.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
        if ("DESKTOP_SHORTCUT_MODIFIER_SHIFT".equals(name)) return com.codename1.share.ShareService.DESKTOP_SHORTCUT_MODIFIER_SHIFT;
        throw unsupportedStaticField(com.codename1.share.ShareService.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("TYPE_FILE".equals(name)) return com.codename1.share.SharedContent.TYPE_FILE;
        if ("TYPE_IMAGE".equals(name)) return com.codename1.share.SharedContent.TYPE_IMAGE;
        if ("TYPE_TEXT".equals(name)) return com.codename1.share.SharedContent.TYPE_TEXT;
        if ("TYPE_URL".equals(name)) return com.codename1.share.SharedContent.TYPE_URL;
        throw unsupportedStaticField(com.codename1.share.SharedContent.class, name);
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
        if (type == com.codename1.printing.PrintResultListener.class) {
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
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
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
