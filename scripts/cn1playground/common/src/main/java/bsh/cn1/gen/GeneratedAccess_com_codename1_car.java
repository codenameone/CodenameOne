package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_car {
    private GeneratedAccess_com_codename1_car() {
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
        if ("Car".equals(simpleName)) {
            return com.codename1.car.Car.class;
        }
        if ("CarAction".equals(simpleName)) {
            return com.codename1.car.CarAction.class;
        }
        if ("CarActionListener".equals(simpleName)) {
            return com.codename1.car.CarActionListener.class;
        }
        if ("CarActionStrip".equals(simpleName)) {
            return com.codename1.car.CarActionStrip.class;
        }
        if ("CarApplication".equals(simpleName)) {
            return com.codename1.car.CarApplication.class;
        }
        if ("CarColor".equals(simpleName)) {
            return com.codename1.car.CarColor.class;
        }
        if ("CarConnectionListener".equals(simpleName)) {
            return com.codename1.car.CarConnectionListener.class;
        }
        if ("CarContext".equals(simpleName)) {
            return com.codename1.car.CarContext.class;
        }
        if ("CarGridItem".equals(simpleName)) {
            return com.codename1.car.CarGridItem.class;
        }
        if ("CarGridTemplate".equals(simpleName)) {
            return com.codename1.car.CarGridTemplate.class;
        }
        if ("CarListTemplate".equals(simpleName)) {
            return com.codename1.car.CarListTemplate.class;
        }
        if ("CarMessageTemplate".equals(simpleName)) {
            return com.codename1.car.CarMessageTemplate.class;
        }
        if ("CarNavigationTemplate".equals(simpleName)) {
            return com.codename1.car.CarNavigationTemplate.class;
        }
        if ("CarNowPlayingTemplate".equals(simpleName)) {
            return com.codename1.car.CarNowPlayingTemplate.class;
        }
        if ("CarPaneTemplate".equals(simpleName)) {
            return com.codename1.car.CarPaneTemplate.class;
        }
        if ("CarRow".equals(simpleName)) {
            return com.codename1.car.CarRow.class;
        }
        if ("CarScreen".equals(simpleName)) {
            return com.codename1.car.CarScreen.class;
        }
        if ("CarSection".equals(simpleName)) {
            return com.codename1.car.CarSection.class;
        }
        if ("CarSurfaceCallback".equals(simpleName)) {
            return com.codename1.car.CarSurfaceCallback.class;
        }
        if ("CarTemplate".equals(simpleName)) {
            return com.codename1.car.CarTemplate.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.car.CarAction.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarAction();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.car.CarAction((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.car.CarActionStrip.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarActionStrip();
            }
        }
        if (type == com.codename1.car.CarGridItem.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarGridItem();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class}, false);
                return new com.codename1.car.CarGridItem((java.lang.String) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.car.CarGridTemplate.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarGridTemplate();
            }
        }
        if (type == com.codename1.car.CarListTemplate.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarListTemplate();
            }
        }
        if (type == com.codename1.car.CarMessageTemplate.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarMessageTemplate();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.car.CarMessageTemplate((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.car.CarNavigationTemplate.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarNavigationTemplate();
            }
        }
        if (type == com.codename1.car.CarNowPlayingTemplate.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarNowPlayingTemplate();
            }
        }
        if (type == com.codename1.car.CarPaneTemplate.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarPaneTemplate();
            }
        }
        if (type == com.codename1.car.CarRow.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarRow();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.car.CarRow((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.car.CarSection.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.car.CarSection();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.car.CarSection((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.car.Car.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addConnectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarConnectionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarConnectionListener.class}, false);
                com.codename1.car.Car.addConnectionListener((com.codename1.car.CarConnectionListener) adaptedArgs[0]); return null;
            }
        }
        if ("endSession".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.car.Car.endSession(); return null;
            }
        }
        if ("getApplication".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.car.Car.getApplication();
            }
        }
        if ("getCurrentContext".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.car.Car.getCurrentContext();
            }
        }
        if ("isCarConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.car.Car.isCarConnected();
            }
        }
        if ("removeConnectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarConnectionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarConnectionListener.class}, false);
                com.codename1.car.Car.removeConnectionListener((com.codename1.car.CarConnectionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setApplication".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarApplication.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarApplication.class}, false);
                com.codename1.car.Car.setApplication((com.codename1.car.CarApplication) adaptedArgs[0]); return null;
            }
        }
        if ("startSession".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.spi.CarBridge.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.spi.CarBridge.class}, false);
                return com.codename1.car.Car.startSession((com.codename1.car.spi.CarBridge) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.car.Car.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.car.CarGridTemplate) {
            try {
                return invoke0((com.codename1.car.CarGridTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarListTemplate) {
            try {
                return invoke1((com.codename1.car.CarListTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarMessageTemplate) {
            try {
                return invoke2((com.codename1.car.CarMessageTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarNavigationTemplate) {
            try {
                return invoke3((com.codename1.car.CarNavigationTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarNowPlayingTemplate) {
            try {
                return invoke4((com.codename1.car.CarNowPlayingTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarPaneTemplate) {
            try {
                return invoke5((com.codename1.car.CarPaneTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarAction) {
            try {
                return invoke6((com.codename1.car.CarAction) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarActionStrip) {
            try {
                return invoke7((com.codename1.car.CarActionStrip) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarApplication) {
            try {
                return invoke8((com.codename1.car.CarApplication) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarContext) {
            try {
                return invoke9((com.codename1.car.CarContext) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarGridItem) {
            try {
                return invoke10((com.codename1.car.CarGridItem) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarRow) {
            try {
                return invoke11((com.codename1.car.CarRow) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarScreen) {
            try {
                return invoke12((com.codename1.car.CarScreen) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarSection) {
            try {
                return invoke13((com.codename1.car.CarSection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarTemplate) {
            try {
                return invoke14((com.codename1.car.CarTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarActionListener) {
            try {
                return invoke15((com.codename1.car.CarActionListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarConnectionListener) {
            try {
                return invoke16((com.codename1.car.CarConnectionListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.car.CarSurfaceCallback) {
            try {
                return invoke17((com.codename1.car.CarSurfaceCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.car.CarGridTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarGridItem.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarGridItem.class}, false);
                return typedTarget.addItem((com.codename1.car.CarGridItem) adaptedArgs[0]);
            }
        }
        if ("getHeaderActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaderActions();
            }
        }
        if ("getItems".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItems();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isLoading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoading();
            }
        }
        if ("setHeaderActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false);
                return typedTarget.setHeaderActions((com.codename1.car.CarActionStrip) adaptedArgs[0]);
            }
        }
        if ("setLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setLoading(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.car.CarListTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarRow.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarRow.class}, false);
                return typedTarget.addRow((com.codename1.car.CarRow) adaptedArgs[0]);
            }
        }
        if ("addSection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarSection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarSection.class}, false);
                return typedTarget.addSection((com.codename1.car.CarSection) adaptedArgs[0]);
            }
        }
        if ("getHeaderActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaderActions();
            }
        }
        if ("getSections".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSections();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isLoading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoading();
            }
        }
        if ("setHeaderActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false);
                return typedTarget.setHeaderActions((com.codename1.car.CarActionStrip) adaptedArgs[0]);
            }
        }
        if ("setLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setLoading(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.car.CarMessageTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false);
                return typedTarget.addAction((com.codename1.car.CarAction) adaptedArgs[0]);
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getHeaderActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaderActions();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isLoading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoading();
            }
        }
        if ("setHeaderActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false);
                return typedTarget.setHeaderActions((com.codename1.car.CarActionStrip) adaptedArgs[0]);
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("setLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setLoading(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setMessage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.car.CarNavigationTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDistanceRemaining".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDistanceRemaining();
            }
        }
        if ("getHeaderActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaderActions();
            }
        }
        if ("getMapActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMapActions();
            }
        }
        if ("getNextManeuver".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextManeuver();
            }
        }
        if ("getSurfaceCallback".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSurfaceCallback();
            }
        }
        if ("getTimeRemaining".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeRemaining();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isNavigating".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNavigating();
            }
        }
        if ("setDistanceRemaining".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDistanceRemaining((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setHeaderActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false);
                return typedTarget.setHeaderActions((com.codename1.car.CarActionStrip) adaptedArgs[0]);
            }
        }
        if ("setMapActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false);
                return typedTarget.setMapActions((com.codename1.car.CarActionStrip) adaptedArgs[0]);
            }
        }
        if ("setNavigating".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setNavigating(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setNextManeuver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setNextManeuver((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSurfaceCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarSurfaceCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarSurfaceCallback.class}, false);
                return typedTarget.setSurfaceCallback((com.codename1.car.CarSurfaceCallback) adaptedArgs[0]);
            }
        }
        if ("setTimeRemaining".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTimeRemaining((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.car.CarNowPlayingTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false);
                return typedTarget.addAction((com.codename1.car.CarAction) adaptedArgs[0]);
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isUpNextVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUpNextVisible();
            }
        }
        if ("setUpNextVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setUpNextVisible(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.car.CarPaneTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false);
                return typedTarget.addAction((com.codename1.car.CarAction) adaptedArgs[0]);
            }
        }
        if ("addRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarRow.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarRow.class}, false);
                return typedTarget.addRow((com.codename1.car.CarRow) adaptedArgs[0]);
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getHeaderActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaderActions();
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isLoading".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLoading();
            }
        }
        if ("setHeaderActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionStrip.class}, false);
                return typedTarget.setHeaderActions((com.codename1.car.CarActionStrip) adaptedArgs[0]);
            }
        }
        if ("setLoading".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setLoading(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.car.CarAction typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBackgroundColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundColor();
            }
        }
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getOnAction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOnAction();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("setBackgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarColor.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarColor.class}, false);
                return typedTarget.setBackgroundColor((com.codename1.car.CarColor) adaptedArgs[0]);
            }
        }
        if ("setIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.setIcon((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("setOnAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionListener.class}, false);
                return typedTarget.setOnAction((com.codename1.car.CarActionListener) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.car.CarActionStrip typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarAction.class}, false);
                return typedTarget.addAction((com.codename1.car.CarAction) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.car.CarActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.car.CarActionListener.class}, false);
                return typedTarget.addAction((java.lang.String) adaptedArgs[0], (com.codename1.car.CarActionListener) adaptedArgs[1]);
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.car.CarApplication typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onCarConnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false);
                typedTarget.onCarConnected((com.codename1.car.CarContext) adaptedArgs[0]); return null;
            }
        }
        if ("onCarDisconnected".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.onCarDisconnected(); return null;
            }
        }
        if ("onCreateRootScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false);
                return typedTarget.onCreateRootScreen((com.codename1.car.CarContext) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.car.CarContext typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getGridItemLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGridItemLimit();
            }
        }
        if ("getListRowLimit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getListRowLimit();
            }
        }
        if ("getTopScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTopScreen();
            }
        }
        if ("isConnected".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConnected();
            }
        }
        if ("popScreen".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.popScreen(); return null;
            }
        }
        if ("pushScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarScreen.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarScreen.class}, false);
                typedTarget.pushScreen((com.codename1.car.CarScreen) adaptedArgs[0]); return null;
            }
        }
        if ("showToast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.showToast((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                typedTarget.showToast((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.car.CarGridItem typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImage();
            }
        }
        if ("getOnAction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOnAction();
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
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.setImage((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("setOnAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionListener.class}, false);
                return typedTarget.setOnAction((com.codename1.car.CarActionListener) adaptedArgs[0]);
            }
        }
        if ("setText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setText((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.car.CarRow typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImage();
            }
        }
        if ("getOnAction".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOnAction();
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
        if ("isBrowsable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBrowsable();
            }
        }
        if ("setBrowsable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setBrowsable(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.setImage((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("setOnAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarActionListener.class}, false);
                return typedTarget.setOnAction((com.codename1.car.CarActionListener) adaptedArgs[0]);
            }
        }
        if ("setText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setText((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.car.CarScreen typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("dispatchCreate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dispatchCreate(); return null;
            }
        }
        if ("dispatchCreateTemplate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.dispatchCreateTemplate();
            }
        }
        if ("dispatchDestroy".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dispatchDestroy(); return null;
            }
        }
        if ("dispatchPause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dispatchPause(); return null;
            }
        }
        if ("dispatchResume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dispatchResume(); return null;
            }
        }
        if ("finish".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.finish(); return null;
            }
        }
        if ("getContext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContext();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.car.CarSection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarRow.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarRow.class}, false);
                return typedTarget.addRow((com.codename1.car.CarRow) adaptedArgs[0]);
            }
        }
        if ("getHeader".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeader();
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("setHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setHeader((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.car.CarTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.car.CarActionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("actionPerformed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false);
                typedTarget.actionPerformed((com.codename1.car.CarContext) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.car.CarConnectionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("carConnected".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.car.CarContext.class}, false);
                typedTarget.carConnected((com.codename1.car.CarContext) adaptedArgs[0]); return null;
            }
        }
        if ("carDisconnected".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.carDisconnected(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.car.CarSurfaceCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("renderSurface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.renderSurface((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("surfaceAvailable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.surfaceAvailable(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("surfaceDestroyed".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.surfaceDestroyed(); return null;
            }
        }
        if ("visibleAreaChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.visibleAreaChanged(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.car.CarColor.class) return getStaticField0(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("BLUE".equals(name)) return com.codename1.car.CarColor.BLUE;
        if ("DEFAULT".equals(name)) return com.codename1.car.CarColor.DEFAULT;
        if ("GREEN".equals(name)) return com.codename1.car.CarColor.GREEN;
        if ("PRIMARY".equals(name)) return com.codename1.car.CarColor.PRIMARY;
        if ("RED".equals(name)) return com.codename1.car.CarColor.RED;
        if ("YELLOW".equals(name)) return com.codename1.car.CarColor.YELLOW;
        throw unsupportedStaticField(com.codename1.car.CarColor.class, name);
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
