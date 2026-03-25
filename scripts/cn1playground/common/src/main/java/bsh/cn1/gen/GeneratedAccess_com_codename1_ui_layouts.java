package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_layouts {
    private GeneratedAccess_com_codename1_ui_layouts() {
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
        if ("BorderLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.BorderLayout.class;
        }
        if ("BoxLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.BoxLayout.class;
        }
        if ("CoordinateLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.CoordinateLayout.class;
        }
        if ("FlowLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.FlowLayout.class;
        }
        if ("GridBagConstraints".equals(simpleName)) {
            return com.codename1.ui.layouts.GridBagConstraints.class;
        }
        if ("GridBagLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.GridBagLayout.class;
        }
        if ("GridLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.GridLayout.class;
        }
        if ("GroupLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.GroupLayout.class;
        }
        if ("Insets".equals(simpleName)) {
            return com.codename1.ui.layouts.Insets.class;
        }
        if ("LayeredLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.LayeredLayout.class;
        }
        if ("Layout".equals(simpleName)) {
            return com.codename1.ui.layouts.Layout.class;
        }
        if ("LayoutStyle".equals(simpleName)) {
            return com.codename1.ui.layouts.LayoutStyle.class;
        }
        if ("TextModeLayout".equals(simpleName)) {
            return com.codename1.ui.layouts.TextModeLayout.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.layouts.BorderLayout.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.BorderLayout();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.BorderLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if (type == com.codename1.ui.layouts.BoxLayout.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.BoxLayout(((Number) adaptedArgs[0]).intValue());
            }
        }
        if (type == com.codename1.ui.layouts.CoordinateLayout.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.CoordinateLayout();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                return new com.codename1.ui.layouts.CoordinateLayout((com.codename1.ui.geom.Dimension) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.CoordinateLayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if (type == com.codename1.ui.layouts.FlowLayout.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.FlowLayout();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.FlowLayout(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.FlowLayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return new com.codename1.ui.layouts.FlowLayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if (type == com.codename1.ui.layouts.GridBagConstraints.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.GridBagConstraints();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.layouts.Insets.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.layouts.Insets.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.GridBagConstraints(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), ((Number) adaptedArgs[4]).doubleValue(), ((Number) adaptedArgs[5]).doubleValue(), ((Number) adaptedArgs[6]).intValue(), ((Number) adaptedArgs[7]).intValue(), (com.codename1.ui.layouts.Insets) adaptedArgs[8], ((Number) adaptedArgs[9]).intValue(), ((Number) adaptedArgs[10]).intValue());
            }
        }
        if (type == com.codename1.ui.layouts.GridBagLayout.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.layouts.GridBagLayout();
            }
        }
        if (type == com.codename1.ui.layouts.GridLayout.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.GridLayout(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.GridLayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.GridLayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
        }
        if (type == com.codename1.ui.layouts.GroupLayout.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return new com.codename1.ui.layouts.GroupLayout((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.layouts.Insets.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.Insets(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue());
            }
        }
        if (type == com.codename1.ui.layouts.TextModeLayout.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ui.layouts.TextModeLayout(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.layouts.BorderLayout.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.layouts.BoxLayout.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.layouts.FlowLayout.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ui.layouts.GridLayout.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ui.layouts.LayeredLayout.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.ui.layouts.LayoutStyle.class) return invokeStatic5(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("absolute".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BorderLayout.absolute();
            }
        }
        if ("center".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BorderLayout.center();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.center((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("centerAbsolute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerAbsolute((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("centerAbsoluteEastWest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerAbsoluteEastWest((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]);
            }
        }
        if ("centerCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerCenter((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("centerCenterEastWest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerCenterEastWest((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]);
            }
        }
        if ("centerEastWest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerEastWest((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]);
            }
        }
        if ("centerTotalBelow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerTotalBelow((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("centerTotalBelowEastWest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.centerTotalBelowEastWest((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]);
            }
        }
        if ("east".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.east((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("north".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.north((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("south".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.south((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("totalBelow".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BorderLayout.totalBelow();
            }
        }
        if ("west".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return com.codename1.ui.layouts.BorderLayout.west((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.BorderLayout.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("encloseX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseX(varArgs);
            }
        }
        if ("encloseXCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseXCenter(varArgs);
            }
        }
        if ("encloseXNoGrow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseXNoGrow(varArgs);
            }
        }
        if ("encloseXRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseXRight(varArgs);
            }
        }
        if ("encloseY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseY(varArgs);
            }
        }
        if ("encloseYBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseYBottom(varArgs);
            }
        }
        if ("encloseYBottomLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseYBottomLast(varArgs);
            }
        }
        if ("encloseYCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.BoxLayout.encloseYCenter(varArgs);
            }
        }
        if ("x".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.x();
            }
        }
        if ("xCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.xCenter();
            }
        }
        if ("xRight".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.xRight();
            }
        }
        if ("y".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.y();
            }
        }
        if ("yBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.yBottom();
            }
        }
        if ("yCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.yCenter();
            }
        }
        if ("yLast".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.BoxLayout.yLast();
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.BoxLayout.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("encloseBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseBottom(varArgs);
            }
        }
        if ("encloseBottomByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseBottomByRow(varArgs);
            }
        }
        if ("encloseCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseCenter(varArgs);
            }
        }
        if ("encloseCenterBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseCenterBottom(varArgs);
            }
        }
        if ("encloseCenterBottomByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseCenterBottomByRow(varArgs);
            }
        }
        if ("encloseCenterMiddle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseCenterMiddle(varArgs);
            }
        }
        if ("encloseCenterMiddleByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseCenterMiddleByRow(varArgs);
            }
        }
        if ("encloseIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseIn(varArgs);
            }
        }
        if ("encloseLeftMiddle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseLeftMiddle(varArgs);
            }
        }
        if ("encloseLeftMiddleByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseLeftMiddleByRow(varArgs);
            }
        }
        if ("encloseMiddle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseMiddle(varArgs);
            }
        }
        if ("encloseMiddleByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseMiddleByRow(varArgs);
            }
        }
        if ("encloseRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseRight(varArgs);
            }
        }
        if ("encloseRightBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseRightBottom(varArgs);
            }
        }
        if ("encloseRightBottomByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseRightBottomByRow(varArgs);
            }
        }
        if ("encloseRightMiddle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseRightMiddle(varArgs);
            }
        }
        if ("encloseRightMiddleByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.FlowLayout.encloseRightMiddleByRow(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.FlowLayout.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("autoFit".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.GridLayout.autoFit();
            }
        }
        if ("encloseIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.GridLayout.encloseIn(varArgs);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.GridLayout.encloseIn(((Number) adaptedArgs[0]).intValue(), varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.GridLayout.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("encloseIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return com.codename1.ui.layouts.LayeredLayout.encloseIn(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.LayeredLayout.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("getSharedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.layouts.LayoutStyle.getSharedInstance();
            }
        }
        if ("setSharedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.LayoutStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.LayoutStyle.class}, false);
                com.codename1.ui.layouts.LayoutStyle.setSharedInstance((com.codename1.ui.layouts.LayoutStyle) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.layouts.LayoutStyle.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.layouts.BorderLayout) {
            try {
                return invoke0((com.codename1.ui.layouts.BorderLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.BoxLayout) {
            try {
                return invoke1((com.codename1.ui.layouts.BoxLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.CoordinateLayout) {
            try {
                return invoke2((com.codename1.ui.layouts.CoordinateLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.FlowLayout) {
            try {
                return invoke3((com.codename1.ui.layouts.FlowLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.GridBagLayout) {
            try {
                return invoke4((com.codename1.ui.layouts.GridBagLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.GridLayout) {
            try {
                return invoke5((com.codename1.ui.layouts.GridLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.GroupLayout) {
            try {
                return invoke6((com.codename1.ui.layouts.GroupLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.LayeredLayout) {
            try {
                return invoke7((com.codename1.ui.layouts.LayeredLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.TextModeLayout) {
            try {
                return invoke8((com.codename1.ui.layouts.TextModeLayout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.Insets) {
            try {
                return invoke9((com.codename1.ui.layouts.Insets) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.Layout) {
            try {
                return invoke10((com.codename1.ui.layouts.Layout) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.layouts.LayoutStyle) {
            try {
                return invoke11((com.codename1.ui.layouts.LayoutStyle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.layouts.BorderLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("defineLandscapeSwap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.defineLandscapeSwap((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenter();
            }
        }
        if ("getCenterBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterBehavior();
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getEast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEast();
            }
        }
        if ("getLandscapeSwap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLandscapeSwap((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getNorth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNorth();
            }
        }
        if ("getOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOverlay();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getSouth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSouth();
            }
        }
        if ("getWest".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWest();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isAbsoluteCenter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAbsoluteCenter();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("isScaleEdges".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScaleEdges();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setAbsoluteCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAbsoluteCenter(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCenterBehavior".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCenterBehavior(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setScaleEdges".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScaleEdges(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.layouts.BoxLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlign();
            }
        }
        if ("getAxis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAxis();
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.layouts.CoordinateLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.layouts.FlowLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAlign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlign();
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getValign".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValign();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isFillRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFillRows();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("isValignByRow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValignByRow();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setAlign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAlign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setFillRows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFillRows(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setValign".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setValign(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setValignByRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setValignByRow(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.layouts.GridBagLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getLayoutDimensions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutDimensions();
            }
        }
        if ("getLayoutWeights".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWeights();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("invalidateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.invalidateLayout((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setConstraints".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.layouts.GridBagConstraints.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.layouts.GridBagConstraints.class}, false);
                typedTarget.setConstraints((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.layouts.GridBagConstraints) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.layouts.GridLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getColumns".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColumns();
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getRows".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRows();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isAutoFit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoFit();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isFillLastRow".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFillLastRow();
            }
        }
        if ("isHideZeroSized".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideZeroSized();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setAutoFit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutoFit(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFillLastRow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFillLastRow(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideZeroSized".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideZeroSized(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.layouts.GroupLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("createBaselineGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return typedTarget.createBaselineGroup(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("createParallelGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createParallelGroup();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createParallelGroup(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.createParallelGroup(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("createSequentialGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createSequentialGroup();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAutocreateContainerGaps".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAutocreateContainerGaps();
            }
        }
        if ("getAutocreateGaps".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAutocreateGaps();
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getHonorsVisibility".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHonorsVisibility();
            }
        }
        if ("getHorizontalGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalGroup();
            }
        }
        if ("getLayoutStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutStyle();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getVerticalGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalGroup();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("linkSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, false);
                typedTarget.linkSize((com.codename1.ui.Component[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class, java.lang.Integer.class}, false);
                typedTarget.linkSize((com.codename1.ui.Component[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("setAutocreateContainerGaps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocreateContainerGaps(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAutocreateGaps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutocreateGaps(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHonorsVisibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHonorsVisibility(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false);
                typedTarget.setHonorsVisibility((com.codename1.ui.Component) adaptedArgs[0], Boolean.valueOf(((Boolean) adaptedArgs[1]).booleanValue())); return null;
            }
        }
        if ("setHorizontalGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.GroupLayout.Group.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.GroupLayout.Group.class}, false);
                typedTarget.setHorizontalGroup((com.codename1.ui.layouts.GroupLayout.Group) adaptedArgs[0]); return null;
            }
        }
        if ("setLayoutStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.LayoutStyle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.LayoutStyle.class}, false);
                typedTarget.setLayoutStyle((com.codename1.ui.layouts.LayoutStyle) adaptedArgs[0]); return null;
            }
        }
        if ("setVerticalGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.GroupLayout.Group.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.GroupLayout.Group.class}, false);
                typedTarget.setVerticalGroup((com.codename1.ui.layouts.GroupLayout.Group) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.layouts.LayeredLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("createConstraint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createConstraint();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.createConstraint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getBottomInsetAsString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getBottomInsetAsString((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getInset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                return typedTarget.getInset((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("getInsetsAsString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Boolean.class}, false);
                return typedTarget.getInsetsAsString((com.codename1.ui.Component) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getLayeredLayoutConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getLayeredLayoutConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getLeftInsetAsString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getLeftInsetAsString((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getOrCreateConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getOrCreateConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPercentInsetAnchorHorizontal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getPercentInsetAnchorHorizontal((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPercentInsetAnchorVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getPercentInsetAnchorVertical((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredHeightMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredHeightMM();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("getPreferredWidthMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredWidthMM();
            }
        }
        if ("getRightInsetAsString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getRightInsetAsString((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getTopInsetAsString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getTopInsetAsString((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setInsetBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setInsetBottom((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setInsetLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setInsetLeft((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setInsetRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setInsetRight((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setInsetTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setInsetTop((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setInsets((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setPercentInsetAnchorHorizontal".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setPercentInsetAnchorHorizontal((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setPercentInsetAnchorVertical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setPercentInsetAnchorVertical((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setPreferredHeightMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPreferredHeightMM(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setPreferredSizeMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setPreferredSizeMM(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setPreferredWidthMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPreferredWidthMM(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setReferenceComponentBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return typedTarget.setReferenceComponentBottom((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferenceComponentBottom((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setReferenceComponentLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return typedTarget.setReferenceComponentLeft((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferenceComponentLeft((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setReferenceComponentRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return typedTarget.setReferenceComponentRight((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferenceComponentRight((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setReferenceComponentTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class}, false);
                return typedTarget.setReferenceComponentTop((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferenceComponentTop((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setReferenceComponents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setReferenceComponents((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.setReferenceComponents((com.codename1.ui.Component) adaptedArgs[0], varArgs);
            }
        }
        if ("setReferencePositionBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferencePositionBottom((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setReferencePositionLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferencePositionLeft((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setReferencePositionRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferencePositionRight((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setReferencePositionTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Float.class}, false);
                return typedTarget.setReferencePositionTop((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("setReferencePositions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.String.class}, false);
                return typedTarget.setReferencePositions((com.codename1.ui.Component) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, float[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, float[].class}, true);
                float[] varArgs = new float[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).floatValue();
                }
                return typedTarget.setReferencePositions((com.codename1.ui.Component) adaptedArgs[0], varArgs);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.layouts.TextModeLayout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cc".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.cc();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.cc(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("createConstraint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createConstraint();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createConstraint(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isAutoGrouping".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAutoGrouping();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setAutoGrouping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAutoGrouping(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.layouts.Insets typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.set(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.layouts.Layout typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class, com.codename1.ui.Container.class}, false);
                typedTarget.addLayoutComponent((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.Container) adaptedArgs[2]); return null;
            }
        }
        if ("cloneConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.cloneConstraint((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getComponentConstraint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentConstraint((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredSize((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isConstraintTracking".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isConstraintTracking();
            }
        }
        if ("isOverlapSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverlapSupported();
            }
        }
        if ("layoutContainer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                typedTarget.layoutContainer((com.codename1.ui.Container) adaptedArgs[0]); return null;
            }
        }
        if ("obscuresPotential".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.obscuresPotential((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("overridesTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.overridesTabIndices((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("removeLayoutComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeLayoutComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class, java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices((com.codename1.ui.Container) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ui.layouts.LayoutStyle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getContainerGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, com.codename1.ui.Container.class}, false);
                return typedTarget.getContainerGap((com.codename1.ui.Component) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), (com.codename1.ui.Container) adaptedArgs[2]);
            }
        }
        if ("getPreferredGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Container.class}, false);
                return typedTarget.getPreferredGap((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.Container) adaptedArgs[4]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.layouts.BorderLayout.class) {
            if ("CENTER".equals(name)) return com.codename1.ui.layouts.BorderLayout.CENTER;
            if ("CENTER_BEHAVIOR_CENTER".equals(name)) return com.codename1.ui.layouts.BorderLayout.CENTER_BEHAVIOR_CENTER;
            if ("CENTER_BEHAVIOR_CENTER_ABSOLUTE".equals(name)) return com.codename1.ui.layouts.BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE;
            if ("CENTER_BEHAVIOR_SCALE".equals(name)) return com.codename1.ui.layouts.BorderLayout.CENTER_BEHAVIOR_SCALE;
            if ("CENTER_BEHAVIOR_TOTAL_BELLOW".equals(name)) return com.codename1.ui.layouts.BorderLayout.CENTER_BEHAVIOR_TOTAL_BELLOW;
            if ("CENTER_BEHAVIOR_TOTAL_BELOW".equals(name)) return com.codename1.ui.layouts.BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW;
            if ("EAST".equals(name)) return com.codename1.ui.layouts.BorderLayout.EAST;
            if ("NORTH".equals(name)) return com.codename1.ui.layouts.BorderLayout.NORTH;
            if ("OVERLAY".equals(name)) return com.codename1.ui.layouts.BorderLayout.OVERLAY;
            if ("SOUTH".equals(name)) return com.codename1.ui.layouts.BorderLayout.SOUTH;
            if ("WEST".equals(name)) return com.codename1.ui.layouts.BorderLayout.WEST;
        }
        if (type == com.codename1.ui.layouts.BoxLayout.class) {
            if ("X_AXIS".equals(name)) return com.codename1.ui.layouts.BoxLayout.X_AXIS;
            if ("X_AXIS_NO_GROW".equals(name)) return com.codename1.ui.layouts.BoxLayout.X_AXIS_NO_GROW;
            if ("Y_AXIS".equals(name)) return com.codename1.ui.layouts.BoxLayout.Y_AXIS;
            if ("Y_AXIS_BOTTOM_LAST".equals(name)) return com.codename1.ui.layouts.BoxLayout.Y_AXIS_BOTTOM_LAST;
        }
        if (type == com.codename1.ui.layouts.GridBagConstraints.class) {
            if ("BOTH".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.BOTH;
            if ("CENTER".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.CENTER;
            if ("EAST".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.EAST;
            if ("FIRST_LINE_END".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.FIRST_LINE_END;
            if ("FIRST_LINE_START".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.FIRST_LINE_START;
            if ("HORIZONTAL".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.HORIZONTAL;
            if ("LAST_LINE_END".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.LAST_LINE_END;
            if ("LAST_LINE_START".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.LAST_LINE_START;
            if ("LINE_END".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.LINE_END;
            if ("LINE_START".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.LINE_START;
            if ("NONE".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.NONE;
            if ("NORTH".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.NORTH;
            if ("NORTHEAST".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.NORTHEAST;
            if ("NORTHWEST".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.NORTHWEST;
            if ("PAGE_END".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.PAGE_END;
            if ("PAGE_START".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.PAGE_START;
            if ("RELATIVE".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.RELATIVE;
            if ("REMAINDER".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.REMAINDER;
            if ("SOUTH".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.SOUTH;
            if ("SOUTHEAST".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.SOUTHEAST;
            if ("SOUTHWEST".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.SOUTHWEST;
            if ("VERTICAL".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.VERTICAL;
            if ("WEST".equals(name)) return com.codename1.ui.layouts.GridBagConstraints.WEST;
        }
        if (type == com.codename1.ui.layouts.GroupLayout.class) {
            if ("BASELINE".equals(name)) return com.codename1.ui.layouts.GroupLayout.BASELINE;
            if ("CENTER".equals(name)) return com.codename1.ui.layouts.GroupLayout.CENTER;
            if ("DEFAULT_SIZE".equals(name)) return com.codename1.ui.layouts.GroupLayout.DEFAULT_SIZE;
            if ("EAST".equals(name)) return com.codename1.ui.layouts.GroupLayout.EAST;
            if ("HORIZONTAL".equals(name)) return com.codename1.ui.layouts.GroupLayout.HORIZONTAL;
            if ("LEADING".equals(name)) return com.codename1.ui.layouts.GroupLayout.LEADING;
            if ("NORTH".equals(name)) return com.codename1.ui.layouts.GroupLayout.NORTH;
            if ("PREFERRED_SIZE".equals(name)) return com.codename1.ui.layouts.GroupLayout.PREFERRED_SIZE;
            if ("SOUTH".equals(name)) return com.codename1.ui.layouts.GroupLayout.SOUTH;
            if ("TRAILING".equals(name)) return com.codename1.ui.layouts.GroupLayout.TRAILING;
            if ("VERTICAL".equals(name)) return com.codename1.ui.layouts.GroupLayout.VERTICAL;
            if ("WEST".equals(name)) return com.codename1.ui.layouts.GroupLayout.WEST;
        }
        if (type == com.codename1.ui.layouts.LayeredLayout.class) {
            if ("UNIT_AUTO".equals(name)) return com.codename1.ui.layouts.LayeredLayout.UNIT_AUTO;
            if ("UNIT_BASELINE".equals(name)) return com.codename1.ui.layouts.LayeredLayout.UNIT_BASELINE;
            if ("UNIT_DIPS".equals(name)) return com.codename1.ui.layouts.LayeredLayout.UNIT_DIPS;
            if ("UNIT_PERCENT".equals(name)) return com.codename1.ui.layouts.LayeredLayout.UNIT_PERCENT;
            if ("UNIT_PIXELS".equals(name)) return com.codename1.ui.layouts.LayeredLayout.UNIT_PIXELS;
        }
        if (type == com.codename1.ui.layouts.LayoutStyle.class) {
            if ("INDENT".equals(name)) return com.codename1.ui.layouts.LayoutStyle.INDENT;
            if ("RELATED".equals(name)) return com.codename1.ui.layouts.LayoutStyle.RELATED;
            if ("UNRELATED".equals(name)) return com.codename1.ui.layouts.LayoutStyle.UNRELATED;
        }
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        if (target instanceof com.codename1.ui.layouts.GridBagLayout) {
            com.codename1.ui.layouts.GridBagLayout typedTarget = (com.codename1.ui.layouts.GridBagLayout) target;
            if ("columnWeights".equals(name)) return typedTarget.columnWeights;
            if ("columnWidths".equals(name)) return typedTarget.columnWidths;
            if ("rowHeights".equals(name)) return typedTarget.rowHeights;
            if ("rowWeights".equals(name)) return typedTarget.rowWeights;
        }
        if (target instanceof com.codename1.ui.layouts.TextModeLayout) {
            com.codename1.ui.layouts.TextModeLayout typedTarget = (com.codename1.ui.layouts.TextModeLayout) target;
            if ("table".equals(name)) return typedTarget.table;
        }
        if (target instanceof com.codename1.ui.layouts.GridBagConstraints) {
            com.codename1.ui.layouts.GridBagConstraints typedTarget = (com.codename1.ui.layouts.GridBagConstraints) target;
            if ("anchor".equals(name)) return typedTarget.anchor;
            if ("fill".equals(name)) return typedTarget.fill;
            if ("gridheight".equals(name)) return typedTarget.gridheight;
            if ("gridwidth".equals(name)) return typedTarget.gridwidth;
            if ("gridx".equals(name)) return typedTarget.gridx;
            if ("gridy".equals(name)) return typedTarget.gridy;
            if ("insets".equals(name)) return typedTarget.insets;
            if ("ipadx".equals(name)) return typedTarget.ipadx;
            if ("ipady".equals(name)) return typedTarget.ipady;
            if ("weightx".equals(name)) return typedTarget.weightx;
            if ("weighty".equals(name)) return typedTarget.weighty;
        }
        if (target instanceof com.codename1.ui.layouts.Insets) {
            com.codename1.ui.layouts.Insets typedTarget = (com.codename1.ui.layouts.Insets) target;
            if ("bottom".equals(name)) return typedTarget.bottom;
            if ("left".equals(name)) return typedTarget.left;
            if ("right".equals(name)) return typedTarget.right;
            if ("top".equals(name)) return typedTarget.top;
        }
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        if (target instanceof com.codename1.ui.layouts.GridBagLayout) {
            com.codename1.ui.layouts.GridBagLayout typedTarget = (com.codename1.ui.layouts.GridBagLayout) target;
            if ("columnWeights".equals(name)) {
                typedTarget.columnWeights = (double[]) value;
                return;
            }
            if ("columnWidths".equals(name)) {
                typedTarget.columnWidths = (int[]) value;
                return;
            }
            if ("rowHeights".equals(name)) {
                typedTarget.rowHeights = (int[]) value;
                return;
            }
            if ("rowWeights".equals(name)) {
                typedTarget.rowWeights = (double[]) value;
                return;
            }
        }
        if (target instanceof com.codename1.ui.layouts.GridBagConstraints) {
            com.codename1.ui.layouts.GridBagConstraints typedTarget = (com.codename1.ui.layouts.GridBagConstraints) target;
            if ("anchor".equals(name)) {
                typedTarget.anchor = ((Number) value).intValue();
                return;
            }
            if ("fill".equals(name)) {
                typedTarget.fill = ((Number) value).intValue();
                return;
            }
            if ("gridheight".equals(name)) {
                typedTarget.gridheight = ((Number) value).intValue();
                return;
            }
            if ("gridwidth".equals(name)) {
                typedTarget.gridwidth = ((Number) value).intValue();
                return;
            }
            if ("gridx".equals(name)) {
                typedTarget.gridx = ((Number) value).intValue();
                return;
            }
            if ("gridy".equals(name)) {
                typedTarget.gridy = ((Number) value).intValue();
                return;
            }
            if ("insets".equals(name)) {
                typedTarget.insets = (com.codename1.ui.layouts.Insets) value;
                return;
            }
            if ("ipadx".equals(name)) {
                typedTarget.ipadx = ((Number) value).intValue();
                return;
            }
            if ("ipady".equals(name)) {
                typedTarget.ipady = ((Number) value).intValue();
                return;
            }
            if ("weightx".equals(name)) {
                typedTarget.weightx = ((Number) value).doubleValue();
                return;
            }
            if ("weighty".equals(name)) {
                typedTarget.weighty = ((Number) value).doubleValue();
                return;
            }
        }
        if (target instanceof com.codename1.ui.layouts.Insets) {
            com.codename1.ui.layouts.Insets typedTarget = (com.codename1.ui.layouts.Insets) target;
            if ("bottom".equals(name)) {
                typedTarget.bottom = ((Number) value).intValue();
                return;
            }
            if ("left".equals(name)) {
                typedTarget.left = ((Number) value).intValue();
                return;
            }
            if ("right".equals(name)) {
                typedTarget.right = ((Number) value).intValue();
                return;
            }
            if ("top".equals(name)) {
                typedTarget.top = ((Number) value).intValue();
                return;
            }
        }
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
