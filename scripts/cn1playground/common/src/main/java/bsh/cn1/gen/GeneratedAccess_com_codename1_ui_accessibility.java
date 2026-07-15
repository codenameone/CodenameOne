package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_accessibility {
    private GeneratedAccess_com_codename1_ui_accessibility() {
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
        if ("AccessibilityAction".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityAction.class;
        }
        if ("Handler".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityAction.Handler.class;
        }
        if ("AccessibilityAssertions".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityAssertions.class;
        }
        if ("AccessibilityCheckedState".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityCheckedState.class;
        }
        if ("AccessibilityChildProvider".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityChildProvider.class;
        }
        if ("AccessibilityCollectionInfo".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityCollectionInfo.class;
        }
        if ("AccessibilityCollectionItemInfo".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityCollectionItemInfo.class;
        }
        if ("AccessibilityGrouping".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityGrouping.class;
        }
        if ("AccessibilityInspector".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityInspector.class;
        }
        if ("AccessibilityIssue".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityIssue.class;
        }
        if ("Severity".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityIssue.Severity.class;
        }
        if ("AccessibilityLiveRegion".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityLiveRegion.class;
        }
        if ("AccessibilityManager".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityManager.class;
        }
        if ("AccessibilityNode".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityNode.class;
        }
        if ("AccessibilityNodeSnapshot".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityNodeSnapshot.class;
        }
        if ("AccessibilityRange".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityRange.class;
        }
        if ("AccessibilityRole".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityRole.class;
        }
        if ("AccessibilityTreeSnapshot".equals(simpleName)) {
            return com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.accessibility.AccessibilityAction.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.accessibility.AccessibilityAction.Handler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.accessibility.AccessibilityAction.Handler.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityAction((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.accessibility.AccessibilityAction.Handler) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.accessibility.AccessibilityAction.Handler.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.accessibility.AccessibilityAction.Handler.class, java.lang.Boolean.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityAction((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.accessibility.AccessibilityAction.Handler) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if (type == com.codename1.ui.accessibility.AccessibilityCollectionInfo.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityCollectionInfo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityCollectionInfo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue(), toIntValue(adaptedArgs[3]));
            }
        }
        if (type == com.codename1.ui.accessibility.AccessibilityCollectionItemInfo.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityCollectionItemInfo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityCollectionItemInfo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), ((Boolean) adaptedArgs[7]).booleanValue());
            }
        }
        if (type == com.codename1.ui.accessibility.AccessibilityIssue.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityIssue.Severity.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityIssue.Severity.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityIssue((com.codename1.ui.accessibility.AccessibilityIssue.Severity) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Number) adaptedArgs[3]).longValue());
            }
        }
        if (type == com.codename1.ui.accessibility.AccessibilityNode.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.accessibility.AccessibilityNode();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityNode((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityNode((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.accessibility.AccessibilityRange.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityRange(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityRange(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class}, false);
                return new com.codename1.ui.accessibility.AccessibilityRange(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue(), (java.lang.String) adaptedArgs[4]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.accessibility.AccessibilityAssertions.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.accessibility.AccessibilityInspector.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.accessibility.AccessibilityManager.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("assertNoErrors".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class}, false);
                com.codename1.ui.accessibility.AccessibilityAssertions.assertNoErrors((com.codename1.ui.accessibility.AccessibilityTreeSnapshot) adaptedArgs[0]); return null;
            }
        }
        if ("assertNoUnlabeledInteractiveNodes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class}, false);
                com.codename1.ui.accessibility.AccessibilityAssertions.assertNoUnlabeledInteractiveNodes((com.codename1.ui.accessibility.AccessibilityTreeSnapshot) adaptedArgs[0]); return null;
            }
        }
        if ("audit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityTreeSnapshot.class}, false);
                return com.codename1.ui.accessibility.AccessibilityAssertions.audit((com.codename1.ui.accessibility.AccessibilityTreeSnapshot) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.accessibility.AccessibilityAssertions.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("currentSnapshot".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.accessibility.AccessibilityInspector.currentSnapshot();
            }
        }
        if ("snapshot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                return com.codename1.ui.accessibility.AccessibilityInspector.snapshot((com.codename1.ui.Form) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.accessibility.AccessibilityInspector.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.accessibility.AccessibilityManager.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.ui.accessibility.AccessibilityManager.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.accessibility.AccessibilityAction) {
            try {
                return invoke0((com.codename1.ui.accessibility.AccessibilityAction) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityCollectionInfo) {
            try {
                return invoke1((com.codename1.ui.accessibility.AccessibilityCollectionInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityCollectionItemInfo) {
            try {
                return invoke2((com.codename1.ui.accessibility.AccessibilityCollectionItemInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityIssue) {
            try {
                return invoke3((com.codename1.ui.accessibility.AccessibilityIssue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityManager) {
            try {
                return invoke4((com.codename1.ui.accessibility.AccessibilityManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityNode) {
            try {
                return invoke5((com.codename1.ui.accessibility.AccessibilityNode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityNodeSnapshot) {
            try {
                return invoke6((com.codename1.ui.accessibility.AccessibilityNodeSnapshot) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityRange) {
            try {
                return invoke7((com.codename1.ui.accessibility.AccessibilityRange) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityTreeSnapshot) {
            try {
                return invoke8((com.codename1.ui.accessibility.AccessibilityTreeSnapshot) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityAction.Handler) {
            try {
                return invoke9((com.codename1.ui.accessibility.AccessibilityAction.Handler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.accessibility.AccessibilityChildProvider) {
            try {
                return invoke10((com.codename1.ui.accessibility.AccessibilityChildProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.accessibility.AccessibilityAction typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("perform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false);
                return typedTarget.perform((com.codename1.ui.Component) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.accessibility.AccessibilityCollectionInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getColumnCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColumnCount();
            }
        }
        if ("getRowCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRowCount();
            }
        }
        if ("getSelectionMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectionMode();
            }
        }
        if ("isHierarchical".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHierarchical();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.accessibility.AccessibilityCollectionItemInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getColumnIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColumnIndex();
            }
        }
        if ("getColumnSpan".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColumnSpan();
            }
        }
        if ("getLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLevel();
            }
        }
        if ("getPositionInSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPositionInSet();
            }
        }
        if ("getRowIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRowIndex();
            }
        }
        if ("getRowSpan".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRowSpan();
            }
        }
        if ("getSetSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSetSize();
            }
        }
        if ("isHeading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHeading();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.accessibility.AccessibilityIssue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCode();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getNodeId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNodeId();
            }
        }
        if ("getSeverity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeverity();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.accessibility.AccessibilityManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCurrentSnapshot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentSnapshot();
            }
        }
        if ("getPendingChanges".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPendingChanges();
            }
        }
        if ("getSnapshot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Form.class}, false);
                return typedTarget.getSnapshot((com.codename1.ui.Form) adaptedArgs[0]);
            }
        }
        if ("invalidate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.invalidate((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("invalidateAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidateAll(); return null;
            }
        }
        if ("performAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.performAction(((Number) adaptedArgs[0]).longValue(), (java.lang.String) adaptedArgs[1], (java.lang.Object) adaptedArgs[2]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.accessibility.AccessibilityNode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityAction.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityAction.class}, false);
                return typedTarget.addAction((com.codename1.ui.accessibility.AccessibilityAction) adaptedArgs[0]);
            }
        }
        if ("addChild".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityNode.class}, false);
                return typedTarget.addChild((com.codename1.ui.accessibility.AccessibilityNode) adaptedArgs[0]);
            }
        }
        if ("clearChildren".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearChildren();
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
            }
        }
        if ("getBusy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBusy();
            }
        }
        if ("getChecked".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChecked();
            }
        }
        if ("getChildProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildProvider();
            }
        }
        if ("getChildren".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildren();
            }
        }
        if ("getCollectionInfo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollectionInfo();
            }
        }
        if ("getCollectionItemInfo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollectionItemInfo();
            }
        }
        if ("getCurrent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrent();
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnabled();
            }
        }
        if ("getExpanded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpanded();
            }
        }
        if ("getGrouping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGrouping();
            }
        }
        if ("getHeadingLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeadingLevel();
            }
        }
        if ("getHint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHint();
            }
        }
        if ("getIdentifier".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdentifier();
            }
        }
        if ("getInvalid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInvalid();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getLiveRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLiveRegion();
            }
        }
        if ("getMultiline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMultiline();
            }
        }
        if ("getObscured".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObscured();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getPaneTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaneTitle();
            }
        }
        if ("getPressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressed();
            }
        }
        if ("getRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRange();
            }
        }
        if ("getReadOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadOnly();
            }
        }
        if ("getRequired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequired();
            }
        }
        if ("getRole".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRole();
            }
        }
        if ("getRoleDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoleDescription();
            }
        }
        if ("getSelected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelected();
            }
        }
        if ("getSortKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSortKey();
            }
        }
        if ("getTraversalAfter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTraversalAfter();
            }
        }
        if ("getTraversalBefore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTraversalBefore();
            }
        }
        if ("getValidationError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidationError();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getVirtualKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVirtualKey();
            }
        }
        if ("hasExplicitConfiguration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasExplicitConfiguration();
            }
        }
        if ("isModal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isModal();
            }
        }
        if ("removeAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.removeAction((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.setBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("setBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setBusy(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setChecked".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityCheckedState.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityCheckedState.class}, false);
                return typedTarget.setChecked((com.codename1.ui.accessibility.AccessibilityCheckedState) adaptedArgs[0]);
            }
        }
        if ("setChildProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityChildProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityChildProvider.class}, false);
                return typedTarget.setChildProvider((com.codename1.ui.accessibility.AccessibilityChildProvider) adaptedArgs[0]);
            }
        }
        if ("setCollectionInfo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityCollectionInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityCollectionInfo.class}, false);
                return typedTarget.setCollectionInfo((com.codename1.ui.accessibility.AccessibilityCollectionInfo) adaptedArgs[0]);
            }
        }
        if ("setCollectionItemInfo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityCollectionItemInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityCollectionItemInfo.class}, false);
                return typedTarget.setCollectionItemInfo((com.codename1.ui.accessibility.AccessibilityCollectionItemInfo) adaptedArgs[0]);
            }
        }
        if ("setCurrent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setCurrent(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDescription((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setEnabled(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setExpanded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setExpanded(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setGrouping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityGrouping.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityGrouping.class}, false);
                return typedTarget.setGrouping((com.codename1.ui.accessibility.AccessibilityGrouping) adaptedArgs[0]);
            }
        }
        if ("setHeadingLevel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setHeadingLevel(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setHint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setHint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setIdentifier((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setInvalid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setInvalid(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLabel((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setLiveRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityLiveRegion.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityLiveRegion.class}, false);
                return typedTarget.setLiveRegion((com.codename1.ui.accessibility.AccessibilityLiveRegion) adaptedArgs[0]);
            }
        }
        if ("setModal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setModal(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setMultiline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setMultiline(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setObscured".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setObscured(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setPaneTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setPaneTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setPressed(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityRange.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityRange.class}, false);
                return typedTarget.setRange((com.codename1.ui.accessibility.AccessibilityRange) adaptedArgs[0]);
            }
        }
        if ("setReadOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setReadOnly(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setRequired".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setRequired(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setRole".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityRole.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.accessibility.AccessibilityRole.class}, false);
                return typedTarget.setRole((com.codename1.ui.accessibility.AccessibilityRole) adaptedArgs[0]);
            }
        }
        if ("setRoleDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setRoleDescription((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSelected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setSelected(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue()));
            }
        }
        if ("setSortKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.setSortKey(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("setTraversalAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.setTraversalAfter((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("setTraversalBefore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.setTraversalBefore((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("setValidationError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setValidationError((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setVirtualKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setVirtualKey((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.accessibility.AccessibilityNodeSnapshot typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getAction((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
            }
        }
        if ("getBusy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBusy();
            }
        }
        if ("getChecked".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChecked();
            }
        }
        if ("getChildIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildIds();
            }
        }
        if ("getCollectionInfo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollectionInfo();
            }
        }
        if ("getCollectionItemInfo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCollectionItemInfo();
            }
        }
        if ("getComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponent();
            }
        }
        if ("getCurrent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrent();
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnabled();
            }
        }
        if ("getExpanded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpanded();
            }
        }
        if ("getHeadingLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeadingLevel();
            }
        }
        if ("getHint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHint();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getIdentifier".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIdentifier();
            }
        }
        if ("getInvalid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInvalid();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getLiveRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLiveRegion();
            }
        }
        if ("getMultiline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMultiline();
            }
        }
        if ("getObscured".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObscured();
            }
        }
        if ("getPaneTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaneTitle();
            }
        }
        if ("getParentId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParentId();
            }
        }
        if ("getPressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressed();
            }
        }
        if ("getRange".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRange();
            }
        }
        if ("getReadOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReadOnly();
            }
        }
        if ("getRequired".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRequired();
            }
        }
        if ("getRole".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRole();
            }
        }
        if ("getRoleDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoleDescription();
            }
        }
        if ("getSelected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelected();
            }
        }
        if ("getValidationError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValidationError();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("getVirtualKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVirtualKey();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isFocused".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocused();
            }
        }
        if ("isModal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isModal();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.accessibility.AccessibilityRange typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCurrent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrent();
            }
        }
        if ("getMaximum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaximum();
            }
        }
        if ("getMinimum".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimum();
            }
        }
        if ("getStep".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStep();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("isValid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValid();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.accessibility.AccessibilityTreeSnapshot typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getGeneration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGeneration();
            }
        }
        if ("getNode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.getNode(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("getNodeAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getNodeAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getNodeByIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getNodeByIdentifier((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getNodes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNodes();
            }
        }
        if ("getRootIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRootIds();
            }
        }
        if ("toJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJson();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.accessibility.AccessibilityAction.Handler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("perform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Object.class}, false);
                return typedTarget.perform((com.codename1.ui.Component) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.accessibility.AccessibilityChildProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessibilityChildren".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getAccessibilityChildren((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.accessibility.AccessibilityAction.class) return getStaticField0(name);
        if (type == com.codename1.ui.accessibility.AccessibilityCheckedState.class) return getStaticField1(name);
        if (type == com.codename1.ui.accessibility.AccessibilityCollectionInfo.class) return getStaticField2(name);
        if (type == com.codename1.ui.accessibility.AccessibilityGrouping.class) return getStaticField3(name);
        if (type == com.codename1.ui.accessibility.AccessibilityIssue.Severity.class) return getStaticField4(name);
        if (type == com.codename1.ui.accessibility.AccessibilityLiveRegion.class) return getStaticField5(name);
        if (type == com.codename1.ui.accessibility.AccessibilityManager.class) return getStaticField6(name);
        if (type == com.codename1.ui.accessibility.AccessibilityRole.class) return getStaticField7(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ACTIVATE".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.ACTIVATE;
        if ("COLLAPSE".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.COLLAPSE;
        if ("COPY".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.COPY;
        if ("CUT".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.CUT;
        if ("DECREMENT".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.DECREMENT;
        if ("DISMISS".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.DISMISS;
        if ("EXPAND".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.EXPAND;
        if ("FOCUS".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.FOCUS;
        if ("INCREMENT".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.INCREMENT;
        if ("LONG_PRESS".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.LONG_PRESS;
        if ("MOVE_CURSOR_BACKWARD_BY_CHARACTER".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.MOVE_CURSOR_BACKWARD_BY_CHARACTER;
        if ("MOVE_CURSOR_BACKWARD_BY_WORD".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.MOVE_CURSOR_BACKWARD_BY_WORD;
        if ("MOVE_CURSOR_FORWARD_BY_CHARACTER".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.MOVE_CURSOR_FORWARD_BY_CHARACTER;
        if ("MOVE_CURSOR_FORWARD_BY_WORD".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.MOVE_CURSOR_FORWARD_BY_WORD;
        if ("PASTE".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.PASTE;
        if ("SCROLL_BACKWARD".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.SCROLL_BACKWARD;
        if ("SCROLL_FORWARD".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.SCROLL_FORWARD;
        if ("SET_SELECTION".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.SET_SELECTION;
        if ("SET_TEXT".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.SET_TEXT;
        if ("SET_VALUE".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.SET_VALUE;
        if ("SHOW_ON_SCREEN".equals(name)) return com.codename1.ui.accessibility.AccessibilityAction.SHOW_ON_SCREEN;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityAction.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("CHECKED".equals(name)) return com.codename1.ui.accessibility.AccessibilityCheckedState.CHECKED;
        if ("MIXED".equals(name)) return com.codename1.ui.accessibility.AccessibilityCheckedState.MIXED;
        if ("UNCHECKED".equals(name)) return com.codename1.ui.accessibility.AccessibilityCheckedState.UNCHECKED;
        if ("UNSPECIFIED".equals(name)) return com.codename1.ui.accessibility.AccessibilityCheckedState.UNSPECIFIED;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityCheckedState.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("SELECTION_MULTIPLE".equals(name)) return com.codename1.ui.accessibility.AccessibilityCollectionInfo.SELECTION_MULTIPLE;
        if ("SELECTION_NONE".equals(name)) return com.codename1.ui.accessibility.AccessibilityCollectionInfo.SELECTION_NONE;
        if ("SELECTION_SINGLE".equals(name)) return com.codename1.ui.accessibility.AccessibilityCollectionInfo.SELECTION_SINGLE;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityCollectionInfo.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.ui.accessibility.AccessibilityGrouping.AUTO;
        if ("EXCLUDE".equals(name)) return com.codename1.ui.accessibility.AccessibilityGrouping.EXCLUDE;
        if ("EXCLUDE_SUBTREE".equals(name)) return com.codename1.ui.accessibility.AccessibilityGrouping.EXCLUDE_SUBTREE;
        if ("GROUP".equals(name)) return com.codename1.ui.accessibility.AccessibilityGrouping.GROUP;
        if ("LEAF".equals(name)) return com.codename1.ui.accessibility.AccessibilityGrouping.LEAF;
        if ("MERGE_DESCENDANTS".equals(name)) return com.codename1.ui.accessibility.AccessibilityGrouping.MERGE_DESCENDANTS;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityGrouping.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("ERROR".equals(name)) return com.codename1.ui.accessibility.AccessibilityIssue.Severity.ERROR;
        if ("WARNING".equals(name)) return com.codename1.ui.accessibility.AccessibilityIssue.Severity.WARNING;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityIssue.Severity.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("ASSERTIVE".equals(name)) return com.codename1.ui.accessibility.AccessibilityLiveRegion.ASSERTIVE;
        if ("OFF".equals(name)) return com.codename1.ui.accessibility.AccessibilityLiveRegion.OFF;
        if ("POLITE".equals(name)) return com.codename1.ui.accessibility.AccessibilityLiveRegion.POLITE;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityLiveRegion.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("CHANGE_ACTIONS".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_ACTIONS;
        if ("CHANGE_ALL".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_ALL;
        if ("CHANGE_BOUNDS".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_BOUNDS;
        if ("CHANGE_CONTENT".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_CONTENT;
        if ("CHANGE_FOCUS".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_FOCUS;
        if ("CHANGE_LIVE_REGION".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_LIVE_REGION;
        if ("CHANGE_PANE".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_PANE;
        if ("CHANGE_STATE".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_STATE;
        if ("CHANGE_STRUCTURE".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_STRUCTURE;
        if ("CHANGE_VALUE".equals(name)) return com.codename1.ui.accessibility.AccessibilityManager.CHANGE_VALUE;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityManager.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("ALERT".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.ALERT;
        if ("BUTTON".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.BUTTON;
        if ("CELL".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.CELL;
        if ("CHECKBOX".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.CHECKBOX;
        if ("COLUMN_HEADER".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.COLUMN_HEADER;
        if ("COMBO_BOX".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.COMBO_BOX;
        if ("DIALOG".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.DIALOG;
        if ("GENERIC".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.GENERIC;
        if ("GRID".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.GRID;
        if ("HEADING".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.HEADING;
        if ("IMAGE".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.IMAGE;
        if ("LINK".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.LINK;
        if ("LIST".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.LIST;
        if ("LIST_ITEM".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.LIST_ITEM;
        if ("MENU".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.MENU;
        if ("MENU_ITEM".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.MENU_ITEM;
        if ("NONE".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.NONE;
        if ("PROGRESS_BAR".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.PROGRESS_BAR;
        if ("RADIO_BUTTON".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.RADIO_BUTTON;
        if ("ROW".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.ROW;
        if ("ROW_HEADER".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.ROW_HEADER;
        if ("SCROLL_BAR".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.SCROLL_BAR;
        if ("SEARCH_FIELD".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.SEARCH_FIELD;
        if ("SEPARATOR".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.SEPARATOR;
        if ("SLIDER".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.SLIDER;
        if ("SPIN_BUTTON".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.SPIN_BUTTON;
        if ("STATIC_TEXT".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.STATIC_TEXT;
        if ("SWITCH".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.SWITCH;
        if ("TAB".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TAB;
        if ("TAB_LIST".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TAB_LIST;
        if ("TAB_PANEL".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TAB_PANEL;
        if ("TEXT_FIELD".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TEXT_FIELD;
        if ("TOGGLE_BUTTON".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TOGGLE_BUTTON;
        if ("TOOLBAR".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TOOLBAR;
        if ("TREE".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TREE;
        if ("TREE_ITEM".equals(name)) return com.codename1.ui.accessibility.AccessibilityRole.TREE_ITEM;
        throw unsupportedStaticField(com.codename1.ui.accessibility.AccessibilityRole.class, name);
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
