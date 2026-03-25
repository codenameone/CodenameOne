package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_properties {
    private GeneratedAccess_com_codename1_properties() {
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
        if ("BooleanProperty".equals(simpleName)) {
            return com.codename1.properties.BooleanProperty.class;
        }
        if ("ByteProperty".equals(simpleName)) {
            return com.codename1.properties.ByteProperty.class;
        }
        if ("CharProperty".equals(simpleName)) {
            return com.codename1.properties.CharProperty.class;
        }
        if ("CollectionProperty".equals(simpleName)) {
            return com.codename1.properties.CollectionProperty.class;
        }
        if ("DoubleProperty".equals(simpleName)) {
            return com.codename1.properties.DoubleProperty.class;
        }
        if ("FloatProperty".equals(simpleName)) {
            return com.codename1.properties.FloatProperty.class;
        }
        if ("InstantUI".equals(simpleName)) {
            return com.codename1.properties.InstantUI.class;
        }
        if ("IntProperty".equals(simpleName)) {
            return com.codename1.properties.IntProperty.class;
        }
        if ("ListProperty".equals(simpleName)) {
            return com.codename1.properties.ListProperty.class;
        }
        if ("LongProperty".equals(simpleName)) {
            return com.codename1.properties.LongProperty.class;
        }
        if ("MapAdapter".equals(simpleName)) {
            return com.codename1.properties.MapAdapter.class;
        }
        if ("MapProperty".equals(simpleName)) {
            return com.codename1.properties.MapProperty.class;
        }
        if ("NumericProperty".equals(simpleName)) {
            return com.codename1.properties.NumericProperty.class;
        }
        if ("PreferencesObject".equals(simpleName)) {
            return com.codename1.properties.PreferencesObject.class;
        }
        if ("Property".equals(simpleName)) {
            return com.codename1.properties.Property.class;
        }
        if ("PropertyBase".equals(simpleName)) {
            return com.codename1.properties.PropertyBase.class;
        }
        if ("PropertyBusinessObject".equals(simpleName)) {
            return com.codename1.properties.PropertyBusinessObject.class;
        }
        if ("PropertyChangeListener".equals(simpleName)) {
            return com.codename1.properties.PropertyChangeListener.class;
        }
        if ("PropertyIndex".equals(simpleName)) {
            return com.codename1.properties.PropertyIndex.class;
        }
        if ("SQLMap".equals(simpleName)) {
            return com.codename1.properties.SQLMap.class;
        }
        if ("SetProperty".equals(simpleName)) {
            return com.codename1.properties.SetProperty.class;
        }
        if ("UiBinding".equals(simpleName)) {
            return com.codename1.properties.UiBinding.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.properties.BooleanProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.BooleanProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return new com.codename1.properties.BooleanProperty((java.lang.String) adaptedArgs[0], Boolean.valueOf(((Boolean) adaptedArgs[1]).booleanValue()));
            }
        }
        if (type == com.codename1.properties.ByteProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.ByteProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Byte.class}, false);
                return new com.codename1.properties.ByteProperty((java.lang.String) adaptedArgs[0], Byte.valueOf(((Number) adaptedArgs[1]).byteValue()));
            }
        }
        if (type == com.codename1.properties.CharProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.CharProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Character.class}, false);
                return new com.codename1.properties.CharProperty((java.lang.String) adaptedArgs[0], Character.valueOf(((Character) adaptedArgs[1]).charValue()));
            }
        }
        if (type == com.codename1.properties.DoubleProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.DoubleProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class}, false);
                return new com.codename1.properties.DoubleProperty((java.lang.String) adaptedArgs[0], Double.valueOf(((Number) adaptedArgs[1]).doubleValue()));
            }
        }
        if (type == com.codename1.properties.FloatProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.FloatProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class}, false);
                return new com.codename1.properties.FloatProperty((java.lang.String) adaptedArgs[0], Float.valueOf(((Number) adaptedArgs[1]).floatValue()));
            }
        }
        if (type == com.codename1.properties.IntProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.IntProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.properties.IntProperty((java.lang.String) adaptedArgs[0], Integer.valueOf(((Number) adaptedArgs[1]).intValue()));
            }
        }
        if (type == com.codename1.properties.ListProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.ListProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) adaptedArgs[i];
                }
                return new com.codename1.properties.ListProperty((java.lang.String) adaptedArgs[0], varArgs);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 2];
                for (int i = 2; i < adaptedArgs.length; i++) {
                    varArgs[i - 2] = (java.lang.Object) adaptedArgs[i];
                }
                return new com.codename1.properties.ListProperty((java.lang.String) adaptedArgs[0], (java.lang.Class) adaptedArgs[1], varArgs);
            }
        }
        if (type == com.codename1.properties.LongProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.LongProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class}, false);
                return new com.codename1.properties.LongProperty((java.lang.String) adaptedArgs[0], Long.valueOf(((Number) adaptedArgs[1]).longValue()));
            }
        }
        if (type == com.codename1.properties.MapProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.MapProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Class.class}, false);
                return new com.codename1.properties.MapProperty((java.lang.String) adaptedArgs[0], (java.lang.Class) adaptedArgs[1], (java.lang.Class) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.properties.Property.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.Property((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return new com.codename1.properties.Property((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class}, false);
                return new com.codename1.properties.Property((java.lang.String) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Object.class}, false);
                return new com.codename1.properties.Property((java.lang.String) adaptedArgs[0], (java.lang.Class) adaptedArgs[1], (java.lang.Object) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.properties.PropertyIndex.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, java.lang.String.class, com.codename1.properties.PropertyBase[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, java.lang.String.class, com.codename1.properties.PropertyBase[].class}, true);
                com.codename1.properties.PropertyBase[] varArgs = new com.codename1.properties.PropertyBase[adaptedArgs.length - 2];
                for (int i = 2; i < adaptedArgs.length; i++) {
                    varArgs[i - 2] = (com.codename1.properties.PropertyBase) adaptedArgs[i];
                }
                return new com.codename1.properties.PropertyIndex((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (java.lang.String) adaptedArgs[1], varArgs);
            }
        }
        if (type == com.codename1.properties.SetProperty.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.properties.SetProperty((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) adaptedArgs[i];
                }
                return new com.codename1.properties.SetProperty((java.lang.String) adaptedArgs[0], varArgs);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Class.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 2];
                for (int i = 2; i < adaptedArgs.length; i++) {
                    varArgs[i - 2] = (java.lang.Object) adaptedArgs[i];
                }
                return new com.codename1.properties.SetProperty((java.lang.String) adaptedArgs[0], (java.lang.Class) adaptedArgs[1], varArgs);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.properties.PreferencesObject.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.properties.PropertyBase.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.properties.PropertyIndex.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.properties.SQLMap.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.properties.UiBinding.class) return invokeStatic4(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                return com.codename1.properties.PreferencesObject.create((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.properties.PreferencesObject.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("bindGlobalGetListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                com.codename1.properties.PropertyBase.bindGlobalGetListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("bindGlobalSetListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                com.codename1.properties.PropertyBase.bindGlobalSetListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.properties.PropertyBase.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("storeJSONList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.List.class}, false);
                com.codename1.properties.PropertyIndex.storeJSONList((java.lang.String) adaptedArgs[0], (java.util.List) adaptedArgs[1]); return null;
            }
        }
        if ("toJSONList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return com.codename1.properties.PropertyIndex.toJSONList((java.util.List) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.properties.PropertyIndex.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.db.Database.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.db.Database.class}, false);
                return com.codename1.properties.SQLMap.create((com.codename1.db.Database) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.properties.SQLMap.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("unbind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                com.codename1.properties.UiBinding.unbind((com.codename1.properties.PropertyBase) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                com.codename1.properties.UiBinding.unbind((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.properties.UiBinding.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.properties.ByteProperty) {
            try {
                return invoke0((com.codename1.properties.ByteProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.CharProperty) {
            try {
                return invoke1((com.codename1.properties.CharProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.DoubleProperty) {
            try {
                return invoke2((com.codename1.properties.DoubleProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.FloatProperty) {
            try {
                return invoke3((com.codename1.properties.FloatProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.IntProperty) {
            try {
                return invoke4((com.codename1.properties.IntProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.LongProperty) {
            try {
                return invoke5((com.codename1.properties.LongProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.BooleanProperty) {
            try {
                return invoke6((com.codename1.properties.BooleanProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.ListProperty) {
            try {
                return invoke7((com.codename1.properties.ListProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.NumericProperty) {
            try {
                return invoke8((com.codename1.properties.NumericProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.SetProperty) {
            try {
                return invoke9((com.codename1.properties.SetProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.CollectionProperty) {
            try {
                return invoke10((com.codename1.properties.CollectionProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.MapProperty) {
            try {
                return invoke11((com.codename1.properties.MapProperty) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.Property) {
            try {
                return invoke12((com.codename1.properties.Property) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.InstantUI) {
            try {
                return invoke13((com.codename1.properties.InstantUI) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.MapAdapter) {
            try {
                return invoke14((com.codename1.properties.MapAdapter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.PreferencesObject) {
            try {
                return invoke15((com.codename1.properties.PreferencesObject) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.PropertyBase) {
            try {
                return invoke16((com.codename1.properties.PropertyBase) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.PropertyIndex) {
            try {
                return invoke17((com.codename1.properties.PropertyIndex) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.SQLMap) {
            try {
                return invoke18((com.codename1.properties.SQLMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.UiBinding) {
            try {
                return invoke19((com.codename1.properties.UiBinding) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.PropertyBusinessObject) {
            try {
                return invoke20((com.codename1.properties.PropertyBusinessObject) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.properties.PropertyChangeListener) {
            try {
                return invoke21((com.codename1.properties.PropertyChangeListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.properties.ByteProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getByte".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getByte();
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.properties.CharProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getChar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChar();
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.properties.DoubleProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getDouble".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDouble();
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.properties.FloatProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getFloat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFloat();
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.properties.IntProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getInt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInt();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.properties.LongProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getLong".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLong();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.properties.BooleanProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getBoolean".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBoolean();
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.properties.ListProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.add(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("asExplodedList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asExplodedList();
            }
        }
        if ("asList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asList();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.set((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(((Number) adaptedArgs[0]).intValue(), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.setList((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.properties.NumericProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isNullable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNullable();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNullable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNullable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.properties.SetProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("asExplodedList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asExplodedList();
            }
        }
        if ("asList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asList();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.set((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.properties.CollectionProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("asExplodedList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asExplodedList();
            }
        }
        if ("asList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asList();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.set((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.properties.MapProperty typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("asExplodedMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asExplodedMap();
            }
        }
        if ("asMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asMap();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getKeyType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getValueType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValueType();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return typedTarget.setMap((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("valueSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.valueSet();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.properties.Property typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
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
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.set((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.properties.InstantUI typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createEditUI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, java.lang.Boolean.class}, false);
                return typedTarget.createEditUI((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("excludeProperties".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase[].class}, true);
                com.codename1.properties.PropertyBase[] varArgs = new com.codename1.properties.PropertyBase[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.properties.PropertyBase) adaptedArgs[i];
                }
                typedTarget.excludeProperties(varArgs); return null;
            }
        }
        if ("excludeProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                typedTarget.excludeProperty((com.codename1.properties.PropertyBase) adaptedArgs[0]); return null;
            }
        }
        if ("getBindings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getBindings((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getOrder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOrder();
            }
        }
        if ("getTextFieldConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.getTextFieldConstraint((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("isExcludedProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.isExcludedProperty((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("setComponentClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Class.class}, false);
                typedTarget.setComponentClass((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]); return null;
            }
        }
        if ("setMultiChoiceLabels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.setMultiChoiceLabels((com.codename1.properties.PropertyBase) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("setMultiChoiceValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) adaptedArgs[i];
                }
                typedTarget.setMultiChoiceValues((com.codename1.properties.PropertyBase) adaptedArgs[0], varArgs); return null;
            }
        }
        if ("setOrder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase[].class}, true);
                com.codename1.properties.PropertyBase[] varArgs = new com.codename1.properties.PropertyBase[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.properties.PropertyBase) adaptedArgs[i];
                }
                typedTarget.setOrder(varArgs); return null;
            }
        }
        if ("setTextFieldConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Integer.class}, false);
                typedTarget.setTextFieldConstraint((com.codename1.properties.PropertyBase) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.properties.MapAdapter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("placeInMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.util.Map.class}, false);
                typedTarget.placeInMap((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.util.Map) adaptedArgs[1]); return null;
            }
        }
        if ("setFromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.util.Map.class}, false);
                typedTarget.setFromMap((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.util.Map) adaptedArgs[1]); return null;
            }
        }
        if ("useAdapterFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.useAdapterFor((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.properties.PreferencesObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.bind();
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.String.class}, false);
                return typedTarget.setName((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setPrefix".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setPrefix((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.properties.PropertyBase typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getGenericType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGenericType();
            }
        }
        if ("getLabel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabel();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.properties.PropertyChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("setLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLabel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("stopListening".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopListening(); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.properties.PropertyIndex typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asElement();
            }
        }
        if ("asExternalizable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asExternalizable();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("fromJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.fromJSON((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("fromXml".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.xml.Element.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.xml.Element.class}, false);
                typedTarget.fromXml((com.codename1.xml.Element) adaptedArgs[0]); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.get((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getIgnoreCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getIgnoreCase((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getMetaDataOfClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getMetaDataOfClass((java.lang.String) adaptedArgs[0]);
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
        if ("getXmlTextElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getXmlTextElement();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("init".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.Object) adaptedArgs[i];
                }
                typedTarget.init(varArgs); return null;
            }
        }
        if ("isExcludeFromJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.isExcludeFromJSON((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("isExcludeFromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.isExcludeFromMap((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("isXmlTextElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.isXmlTextElement((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("loadJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.loadJSON((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("loadJSONList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.loadJSONList((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("newInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.newInstance();
            }
        }
        if ("populateFromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.populateFromMap((java.util.Map) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class, java.lang.Class.class}, false);
                typedTarget.populateFromMap((java.util.Map) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]); return null;
            }
        }
        if ("putMetaDataOfClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putMetaDataOfClass((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("registerExternalizable".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.registerExternalizable(); return null;
            }
        }
        if ("setExcludeFromJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Boolean.class}, false);
                typedTarget.setExcludeFromJSON((com.codename1.properties.PropertyBase) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setExcludeFromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Boolean.class}, false);
                typedTarget.setExcludeFromMap((com.codename1.properties.PropertyBase) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setSimpleObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Object.class}, false);
                return typedTarget.setSimpleObject((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setXmlTextElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Boolean.class}, false);
                typedTarget.setXmlTextElement((com.codename1.properties.PropertyBase) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("storeJSON".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.storeJSON((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toJSON".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toJSON();
            }
        }
        if ("toMapRepresentation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toMapRepresentation();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.toString(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("toXML".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toXML();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.properties.SQLMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createTable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                return typedTarget.createTable((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]);
            }
        }
        if ("delete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                typedTarget.delete((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]); return null;
            }
        }
        if ("dropTable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                typedTarget.dropTable((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]); return null;
            }
        }
        if ("getColumnName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.getColumnName((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("getSqlType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                return typedTarget.getSqlType((com.codename1.properties.PropertyBase) adaptedArgs[0]);
            }
        }
        if ("getTableName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                return typedTarget.getTableName((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]);
            }
        }
        if ("insert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                typedTarget.insert((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]); return null;
            }
        }
        if ("select".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.select((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (com.codename1.properties.Property) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue());
            }
        }
        if ("selectBuild".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.selectBuild();
            }
        }
        if ("selectNot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.selectNot((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (com.codename1.properties.Property) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).intValue());
            }
        }
        if ("setColumnName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.String.class}, false);
                typedTarget.setColumnName((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setPrimaryKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class}, false);
                typedTarget.setPrimaryKey((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (com.codename1.properties.Property) adaptedArgs[1]); return null;
            }
        }
        if ("setPrimaryKeyAutoIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.properties.Property.class}, false);
                typedTarget.setPrimaryKeyAutoIncrement((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (com.codename1.properties.Property) adaptedArgs[1]); return null;
            }
        }
        if ("setSqlType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, com.codename1.properties.SQLMap.SqlType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, com.codename1.properties.SQLMap.SqlType.class}, false);
                typedTarget.setSqlType((com.codename1.properties.PropertyBase) adaptedArgs[0], (com.codename1.properties.SQLMap.SqlType) adaptedArgs[1]); return null;
            }
        }
        if ("setTableName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, java.lang.String.class}, false);
                typedTarget.setTableName((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setVerbose".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVerbose(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class}, false);
                typedTarget.update((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.properties.UiBinding typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, com.codename1.ui.Component.class}, false);
                return typedTarget.bind((com.codename1.properties.PropertyBase) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBusinessObject.class, com.codename1.ui.Container.class}, false);
                return typedTarget.bind((com.codename1.properties.PropertyBusinessObject) adaptedArgs[0], (com.codename1.ui.Container) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, com.codename1.ui.Component.class, com.codename1.properties.UiBinding.ComponentAdapter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, com.codename1.ui.Component.class, com.codename1.properties.UiBinding.ComponentAdapter.class}, false);
                return typedTarget.bind((com.codename1.properties.PropertyBase) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.properties.UiBinding.ComponentAdapter) adaptedArgs[2]);
            }
        }
        if ("bindGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Object[].class, com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class, java.lang.Object[].class, com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 2];
                for (int i = 2; i < adaptedArgs.length; i++) {
                    varArgs[i - 2] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.bindGroup((com.codename1.properties.PropertyBase) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1], varArgs);
            }
        }
        if ("bindInteger".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.Property.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.Property.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.bindInteger((com.codename1.properties.Property) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("bindString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.Property.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.Property.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.bindString((com.codename1.properties.Property) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("createTableModel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.CollectionProperty.class, com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.CollectionProperty.class, com.codename1.properties.PropertyBusinessObject.class}, false);
                return typedTarget.createTableModel((com.codename1.properties.CollectionProperty) adaptedArgs[0], (com.codename1.properties.PropertyBusinessObject) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, com.codename1.properties.PropertyBusinessObject.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, com.codename1.properties.PropertyBusinessObject.class}, false);
                return typedTarget.createTableModel((java.util.List) adaptedArgs[0], (com.codename1.properties.PropertyBusinessObject) adaptedArgs[1]);
            }
        }
        if ("isAutoCommit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoCommit();
            }
        }
        if ("setAutoCommit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutoCommit(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.properties.PropertyBusinessObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPropertyIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyIndex();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.properties.PropertyChangeListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("propertyChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.properties.PropertyBase.class}, false);
                typedTarget.propertyChanged((com.codename1.properties.PropertyBase) adaptedArgs[0]); return null;
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
