package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_plaf {
    private GeneratedAccess_com_codename1_ui_plaf() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.ui.plaf.Border".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.Border");
            }
            return com.codename1.ui.plaf.Border.class;
        }
        if ("com.codename1.ui.plaf.CSSBorder".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.CSSBorder");
            }
            return com.codename1.ui.plaf.CSSBorder.class;
        }
        if ("com.codename1.ui.plaf.DefaultLookAndFeel".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.DefaultLookAndFeel");
            }
            return com.codename1.ui.plaf.DefaultLookAndFeel.class;
        }
        if ("com.codename1.ui.plaf.LookAndFeel".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.LookAndFeel");
            }
            return com.codename1.ui.plaf.LookAndFeel.class;
        }
        if ("com.codename1.ui.plaf.RoundBorder".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.RoundBorder");
            }
            return com.codename1.ui.plaf.RoundBorder.class;
        }
        if ("com.codename1.ui.plaf.RoundRectBorder".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.RoundRectBorder");
            }
            return com.codename1.ui.plaf.RoundRectBorder.class;
        }
        if ("com.codename1.ui.plaf.Style".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.Style");
            }
            return com.codename1.ui.plaf.Style.class;
        }
        if ("com.codename1.ui.plaf.StyleParser".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.StyleParser");
            }
            return com.codename1.ui.plaf.StyleParser.class;
        }
        if ("com.codename1.ui.plaf.UIManager".equals(name)) {
            if (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components.")) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access helper hit com.codename1.ui.plaf -> com.codename1.ui.plaf.UIManager");
            }
            return com.codename1.ui.plaf.UIManager.class;
        }
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.plaf.CSSBorder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.plaf.CSSBorder();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                return new com.codename1.ui.plaf.CSSBorder((com.codename1.ui.util.Resources) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.ui.plaf.CSSBorder((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class}, false)) {
                return new com.codename1.ui.plaf.CSSBorder((com.codename1.ui.util.Resources) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == com.codename1.ui.plaf.DefaultLookAndFeel.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                return new com.codename1.ui.plaf.DefaultLookAndFeel((com.codename1.ui.plaf.UIManager) safeArgs[0]);
            }
        }
        if (type == com.codename1.ui.plaf.Style.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.ui.plaf.Style();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                return new com.codename1.ui.plaf.Style((com.codename1.ui.plaf.Style) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Font.class, java.lang.Byte.class}, false)) {
                return new com.codename1.ui.plaf.Style(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (com.codename1.ui.Font) safeArgs[2], ((Number) safeArgs[3]).byteValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Font.class, java.lang.Byte.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                return new com.codename1.ui.plaf.Style(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (com.codename1.ui.Font) safeArgs[2], ((Number) safeArgs[3]).byteValue(), (com.codename1.ui.Image) safeArgs[4], ((Number) safeArgs[5]).byteValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.plaf.Border.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ui.plaf.DefaultLookAndFeel.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ui.plaf.RoundBorder.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ui.plaf.RoundRectBorder.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ui.plaf.Style.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.ui.plaf.StyleParser.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.ui.plaf.UIManager.class) return invokeStatic6(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("createBevelLowered".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.createBevelLowered();
            }
        }
        if ("createBevelLowered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createBevelLowered(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("createBevelRaised".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.createBevelRaised();
            }
        }
        if ("createBevelRaised".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createBevelRaised(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("createCompoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class}, false)) {
                return com.codename1.ui.plaf.Border.createCompoundBorder((com.codename1.ui.plaf.Border) safeArgs[0], (com.codename1.ui.plaf.Border) safeArgs[1], (com.codename1.ui.plaf.Border) safeArgs[2], (com.codename1.ui.plaf.Border) safeArgs[3]);
            }
        }
        if ("createDashedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createDashedBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createDashedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createDashedBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createDottedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createDottedBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createDottedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createDottedBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createDoubleBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createDoubleBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createDoubleBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createDoubleBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.createEmpty();
            }
        }
        if ("createEtchedLowered".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.createEtchedLowered();
            }
        }
        if ("createEtchedLowered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createEtchedLowered(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createEtchedRaised".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.createEtchedRaised();
            }
        }
        if ("createEtchedRaised".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createEtchedRaised(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createGrooveBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createGrooveBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createGrooveBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createGrooveBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createHorizonalImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.plaf.Border.createHorizonalImageBorder((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2]);
            }
        }
        if ("createImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.plaf.Border.createImageBorder((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2]);
            }
        }
        if ("createImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.plaf.Border.createImageBorder((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.Image) safeArgs[4], (com.codename1.ui.Image) safeArgs[5], (com.codename1.ui.Image) safeArgs[6], (com.codename1.ui.Image) safeArgs[7], (com.codename1.ui.Image) safeArgs[8]);
            }
        }
        if ("createImageScaledBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.plaf.Border.createImageScaledBorder((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.Image) safeArgs[4], (com.codename1.ui.Image) safeArgs[5], (com.codename1.ui.Image) safeArgs[6], (com.codename1.ui.Image) safeArgs[7], (com.codename1.ui.Image) safeArgs[8]);
            }
        }
        if ("createImageSplicedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                return com.codename1.ui.plaf.Border.createImageSplicedBorder((com.codename1.ui.Image) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), ((Number) safeArgs[2]).doubleValue(), ((Number) safeArgs[3]).doubleValue(), ((Number) safeArgs[4]).doubleValue());
            }
        }
        if ("createInsetBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createInsetBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createInsetBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createInsetBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) safeArgs[0]).intValue(), (java.lang.String) safeArgs[1]);
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), (java.lang.String) safeArgs[2]);
            }
        }
        if ("createOutsetBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createOutsetBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createOutsetBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createOutsetBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createRidgeBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createRidgeBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createRidgeBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createRidgeBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createRoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createRoundBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createRoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return com.codename1.ui.plaf.Border.createRoundBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("createRoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createRoundBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("createRoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return com.codename1.ui.plaf.Border.createRoundBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue());
            }
        }
        if ("createUndelineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return com.codename1.ui.plaf.Border.createUndelineBorder(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("createUndelineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createUndelineBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createUnderlineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return com.codename1.ui.plaf.Border.createUnderlineBorder(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("createUnderlineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createUnderlineBorder(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createUnderlineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createUnderlineBorder(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createUnderlineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.Border.createUnderlineBorder(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("createVerticalImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                return com.codename1.ui.plaf.Border.createVerticalImageBorder((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2]);
            }
        }
        if ("getDefaultBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.getDefaultBorder();
            }
        }
        if ("getEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.Border.getEmpty();
            }
        }
        if ("setDefaultBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                com.codename1.ui.plaf.Border.setDefaultBorder((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.Border.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("reverseAlignForBidi".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                return com.codename1.ui.plaf.DefaultLookAndFeel.reverseAlignForBidi((com.codename1.ui.Component) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.DefaultLookAndFeel.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.RoundBorder.create();
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.RoundBorder.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.RoundRectBorder.create();
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.RoundRectBorder.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("createProxyStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style[].class}, true)) {
                com.codename1.ui.plaf.Style[] varArgs = new com.codename1.ui.plaf.Style[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.plaf.Style) safeArgs[i];
                }
                return com.codename1.ui.plaf.Style.createProxyStyle(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.Style.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("getBackgroundTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.StyleParser.getBackgroundTypes();
            }
        }
        if ("getSupportedBackgroundTypes".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.StyleParser.getSupportedBackgroundTypes();
            }
        }
        if ("parseScalarValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.plaf.StyleParser.parseScalarValue((java.lang.String) safeArgs[0]);
            }
        }
        if ("validateScalarValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.plaf.StyleParser.validateScalarValue((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.StyleParser.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("createInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.UIManager.createInstance();
            }
        }
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.ui.plaf.UIManager.getInstance();
            }
        }
        if ("initFirstTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.ui.plaf.UIManager.initFirstTheme((java.lang.String) safeArgs[0]);
            }
        }
        if ("initNamedTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.ui.plaf.UIManager.initNamedTheme((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.UIManager.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ui.plaf.CSSBorder) {
            try {
                return invoke0((com.codename1.ui.plaf.CSSBorder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.DefaultLookAndFeel) {
            try {
                return invoke1((com.codename1.ui.plaf.DefaultLookAndFeel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.RoundBorder) {
            try {
                return invoke2((com.codename1.ui.plaf.RoundBorder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.RoundRectBorder) {
            try {
                return invoke3((com.codename1.ui.plaf.RoundRectBorder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.Border) {
            try {
                return invoke4((com.codename1.ui.plaf.Border) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.LookAndFeel) {
            try {
                return invoke5((com.codename1.ui.plaf.LookAndFeel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.Style) {
            try {
                return invoke6((com.codename1.ui.plaf.Style) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.UIManager) {
            try {
                return invoke7((com.codename1.ui.plaf.UIManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ui.plaf.CSSBorder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("backgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.backgroundColor((java.lang.String) safeArgs[0]);
            }
        }
        if ("backgroundImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image[].class}, true)) {
                com.codename1.ui.Image[] varArgs = new com.codename1.ui.Image[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Image) safeArgs[i];
                }
                return typedTarget.backgroundImage(varArgs);
            }
        }
        if ("backgroundImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.backgroundImage((java.lang.String) safeArgs[0]);
            }
        }
        if ("backgroundPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.backgroundPosition(varArgs);
            }
        }
        if ("backgroundRepeat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.backgroundRepeat(varArgs);
            }
        }
        if ("borderColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.borderColor(varArgs);
            }
        }
        if ("borderImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, double[].class}, true)) {
                double[] varArgs = new double[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = ((Number) safeArgs[i]).doubleValue();
                }
                return typedTarget.borderImage((com.codename1.ui.Image) safeArgs[0], varArgs);
            }
        }
        if ("borderImageWithName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, double[].class}, true)) {
                double[] varArgs = new double[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = ((Number) safeArgs[i]).doubleValue();
                }
                return typedTarget.borderImageWithName((java.lang.String) safeArgs[0], varArgs);
            }
        }
        if ("borderRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.borderRadius((java.lang.String) safeArgs[0]);
            }
        }
        if ("borderStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.borderStroke(varArgs);
            }
        }
        if ("borderStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.borderStyle(varArgs);
            }
        }
        if ("borderWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.borderWidth(varArgs);
            }
        }
        if ("boxShadow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.boxShadow((java.lang.String) safeArgs[0]);
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThickness();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangleType();
            }
        }
        if ("lock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.Component) safeArgs[4]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.geom.Rectangle) safeArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPaintOuterBorderFirst(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setThickness(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("toCSSString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toCSSString();
            }
        }
        if ("unlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.plaf.DefaultLookAndFeel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.bind((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("calculateLabelSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class}, false)) {
                return typedTarget.calculateLabelSpan((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.Label) safeArgs[1]);
            }
        }
        if ("calculateSpanForLabelText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.calculateSpanForLabelText((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.Label) safeArgs[1], (java.lang.String) safeArgs[2], ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).intValue());
            }
        }
        if ("calculateTextAreaSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                return typedTarget.calculateTextAreaSpan((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]);
            }
        }
        if ("calculateTextFieldSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                return typedTarget.calculateTextFieldSpan((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]);
            }
        }
        if ("drawButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                typedTarget.drawButton((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Button) safeArgs[1]); return null;
            }
        }
        if ("drawCheckBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                typedTarget.drawCheckBox((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Button) safeArgs[1]); return null;
            }
        }
        if ("drawComboBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                typedTarget.drawComboBox((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.List) safeArgs[1]); return null;
            }
        }
        if ("drawHorizontalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.drawHorizontalScroll((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("drawLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Label.class}, false)) {
                typedTarget.drawLabel((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Label) safeArgs[1]); return null;
            }
        }
        if ("drawList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                typedTarget.drawList((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.List) safeArgs[1]); return null;
            }
        }
        if ("drawPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                typedTarget.drawPullToRefresh((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("drawRadioButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                typedTarget.drawRadioButton((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Button) safeArgs[1]); return null;
            }
        }
        if ("drawTextArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                typedTarget.drawTextArea((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]); return null;
            }
        }
        if ("drawTextField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                typedTarget.drawTextField((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]); return null;
            }
        }
        if ("drawTextFieldCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                typedTarget.drawTextFieldCursor((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]); return null;
            }
        }
        if ("drawVerticalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.drawVerticalScroll((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("focusGained".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.focusGained((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("focusLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.focusLost((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("getButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                return typedTarget.getButtonPreferredSize((com.codename1.ui.Button) safeArgs[0]);
            }
        }
        if ("getCheckBoxFocusImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCheckBoxFocusImages();
            }
        }
        if ("getCheckBoxImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCheckBoxImages();
            }
        }
        if ("getCheckBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                return typedTarget.getCheckBoxPreferredSize((com.codename1.ui.Button) safeArgs[0]);
            }
        }
        if ("getComboBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                return typedTarget.getComboBoxPreferredSize((com.codename1.ui.List) safeArgs[0]);
            }
        }
        if ("getDefaultDialogTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultDialogTransitionIn();
            }
        }
        if ("getDefaultDialogTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultDialogTransitionOut();
            }
        }
        if ("getDefaultFormTintColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFormTintColor();
            }
        }
        if ("getDefaultFormTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFormTransitionIn();
            }
        }
        if ("getDefaultFormTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFormTransitionOut();
            }
        }
        if ("getDefaultMenuTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMenuTransitionIn();
            }
        }
        if ("getDefaultMenuTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMenuTransitionOut();
            }
        }
        if ("getDefaultSmoothScrollingSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultSmoothScrollingSpeed();
            }
        }
        if ("getDisableColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisableColor();
            }
        }
        if ("getFadeScrollBarSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFadeScrollBarSpeed();
            }
        }
        if ("getFadeScrollEdgeLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFadeScrollEdgeLength();
            }
        }
        if ("getHorizontalScrollHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHorizontalScrollHeight();
            }
        }
        if ("getLabelPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                return typedTarget.getLabelPreferredSize((com.codename1.ui.Label) safeArgs[0]);
            }
        }
        if ("getListPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                return typedTarget.getListPreferredSize((com.codename1.ui.List) safeArgs[0]);
            }
        }
        if ("getMenuBarClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMenuBarClass();
            }
        }
        if ("getMenuIcons".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMenuIcons();
            }
        }
        if ("getMenuRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMenuRenderer();
            }
        }
        if ("getPullToRefreshHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPullToRefreshHeight();
            }
        }
        if ("getRadioButtonFocusImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRadioButtonFocusImages();
            }
        }
        if ("getRadioButtonImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRadioButtonImages();
            }
        }
        if ("getRadioButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                return typedTarget.getRadioButtonPreferredSize((com.codename1.ui.Button) safeArgs[0]);
            }
        }
        if ("getTactileTouchDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTactileTouchDuration();
            }
        }
        if ("getTextAreaSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class, java.lang.Boolean.class}, false)) {
                return typedTarget.getTextAreaSize((com.codename1.ui.TextArea) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("getTextFieldCursorColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextFieldCursorColor();
            }
        }
        if ("getTextFieldPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class}, false)) {
                return typedTarget.getTextFieldPreferredSize((com.codename1.ui.TextArea) safeArgs[0]);
            }
        }
        if ("getTickerSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTickerSpeed();
            }
        }
        if ("getVerticalScrollWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVerticalScrollWidth();
            }
        }
        if ("isBackgroundImageDetermineSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackgroundImageDetermineSize();
            }
        }
        if ("isDefaultAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultAlwaysTensile();
            }
        }
        if ("isDefaultEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultEndsWith3Points();
            }
        }
        if ("isDefaultSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultSmoothScrolling();
            }
        }
        if ("isDefaultSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultSnapToGrid();
            }
        }
        if ("isDefaultTensileDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultTensileDrag();
            }
        }
        if ("isDefaultTensileHighlight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultTensileHighlight();
            }
        }
        if ("isFadeScrollBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFadeScrollBar();
            }
        }
        if ("isFadeScrollEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFadeScrollEdge();
            }
        }
        if ("isFocusScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFocusScrolling();
            }
        }
        if ("isRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRTL();
            }
        }
        if ("isReverseSoftButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReverseSoftButtons();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isTickWhenFocused".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTickWhenFocused();
            }
        }
        if ("isTouchMenus".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTouchMenus();
            }
        }
        if ("paintTensileHighlight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                typedTarget.paintTensileHighlight((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Graphics) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.refreshTheme(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBackgroundImageDetermineSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundImageDetermineSize(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCheckBoxFocusImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setCheckBoxFocusImages((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3]); return null;
            }
        }
        if ("setCheckBoxImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setCheckBoxImages((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setCheckBoxImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setCheckBoxImages((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3]); return null;
            }
        }
        if ("setComboBoxImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setComboBoxImage((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setDefaultAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultAlwaysTensile(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultDialogTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultDialogTransitionIn((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultDialogTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultDialogTransitionOut((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultEndsWith3Points(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultFormTintColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDefaultFormTintColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDefaultFormTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultFormTransitionIn((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultFormTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultFormTransitionOut((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultMenuTransitionIn((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultMenuTransitionOut((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultSmoothScrolling(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultSmoothScrollingSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDefaultSmoothScrollingSpeed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDefaultSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultSnapToGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultTensileDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultTensileDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisableColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDisableColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFG".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.setFG((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("setFadeScrollBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFadeScrollBar(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollBarSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFadeScrollBarSpeed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFadeScrollEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFadeScrollEdge(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollEdgeLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFadeScrollEdgeLength(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFocusScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFocusScrolling(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMenuBarClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                typedTarget.setMenuBarClass((java.lang.Class) safeArgs[0]); return null;
            }
        }
        if ("setMenuIcons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setMenuIcons((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2]); return null;
            }
        }
        if ("setMenuRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false)) {
                typedTarget.setMenuRenderer((com.codename1.ui.list.ListCellRenderer) safeArgs[0]); return null;
            }
        }
        if ("setPasswordChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                typedTarget.setPasswordChar(((Character) safeArgs[0]).charValue()); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setRTL(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRadioButtonFocusImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setRadioButtonFocusImages((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3]); return null;
            }
        }
        if ("setRadioButtonImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setRadioButtonImages((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1]); return null;
            }
        }
        if ("setRadioButtonImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setRadioButtonImages((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3]); return null;
            }
        }
        if ("setReverseSoftButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReverseSoftButtons(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTactileTouchDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTactileTouchDuration(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTextFieldCursorColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTextFieldCursorColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTickWhenFocused".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setTickWhenFocused(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTickerSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setTickerSpeed(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setTouchMenus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setTouchMenus(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("uninstall".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.uninstall(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.plaf.RoundBorder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("color".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.color(((Number) safeArgs[0]).intValue());
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getColor();
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOpacity();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getShadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowBlur();
            }
        }
        if ("getShadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowOpacity();
            }
        }
        if ("getShadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowSpread();
            }
        }
        if ("getShadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowX();
            }
        }
        if ("getShadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowY();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStrokeOpacity();
            }
        }
        if ("getStrokeThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStrokeThickness();
            }
        }
        if ("getThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThickness();
            }
        }
        if ("getUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUIID();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isOnlyLeftRounded".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isOnlyLeftRounded();
            }
        }
        if ("isOnlyRightRounded".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isOnlyRightRounded();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangle();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangleType();
            }
        }
        if ("isShadowMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isShadowMM();
            }
        }
        if ("isStrokeMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isStrokeMM();
            }
        }
        if ("lock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("onlyLeftRounded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.onlyLeftRounded(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("onlyRightRounded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.onlyRightRounded(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("opacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.opacity(((Number) safeArgs[0]).intValue());
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("rectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.rectangle(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.Component) safeArgs[4]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.geom.Rectangle) safeArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPaintOuterBorderFirst(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setThickness(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("shadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowBlur(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("shadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shadowOpacity(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shadowSpread(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                return typedTarget.shadowSpread(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("shadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowX(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("shadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowY(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("stroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Stroke.class}, false)) {
                return typedTarget.stroke((com.codename1.ui.Stroke) safeArgs[0]);
            }
        }
        if ("stroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                return typedTarget.stroke(((Number) safeArgs[0]).floatValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("strokeAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.strokeAngle(((Number) safeArgs[0]).intValue());
            }
        }
        if ("strokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.strokeColor(((Number) safeArgs[0]).intValue());
            }
        }
        if ("strokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.strokeOpacity(((Number) safeArgs[0]).intValue());
            }
        }
        if ("uiid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.uiid(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("unlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.plaf.RoundRectBorder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("arrowSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.arrowSize(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("bezierCorners".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.bezierCorners(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("bottomLeftMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.bottomLeftMode(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("bottomOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.bottomOnlyMode(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("bottomRightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.bottomRightMode(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("cornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.cornerRadius(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getShadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowBlur();
            }
        }
        if ("getShadowColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowColor();
            }
        }
        if ("getShadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowOpacity();
            }
        }
        if ("getShadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowSpread();
            }
        }
        if ("getShadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowX();
            }
        }
        if ("getShadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getShadowY();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStrokeOpacity();
            }
        }
        if ("getStrokeThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStrokeThickness();
            }
        }
        if ("getThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThickness();
            }
        }
        if ("getTrackComponentHorizontalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTrackComponentHorizontalPosition();
            }
        }
        if ("getTrackComponentSide".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTrackComponentSide();
            }
        }
        if ("getTrackComponentVerticalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTrackComponentVerticalPosition();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isBezierCorners".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBezierCorners();
            }
        }
        if ("isBottomLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBottomLeft();
            }
        }
        if ("isBottomOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBottomOnlyMode();
            }
        }
        if ("isBottomRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBottomRight();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangleType();
            }
        }
        if ("isStrokeMM".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isStrokeMM();
            }
        }
        if ("isTopLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTopLeft();
            }
        }
        if ("isTopOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTopOnlyMode();
            }
        }
        if ("isTopRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTopRight();
            }
        }
        if ("isUseCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUseCache();
            }
        }
        if ("lock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("setArrowSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setArrowSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.Component) safeArgs[4]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.geom.Rectangle) safeArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPaintOuterBorderFirst(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setThickness(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("shadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowBlur(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("shadowColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shadowColor(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shadowOpacity(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowSpread(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("shadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.shadowSpread(((Number) safeArgs[0]).intValue());
            }
        }
        if ("shadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowX(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("shadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.shadowY(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("stroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Stroke.class}, false)) {
                return typedTarget.stroke((com.codename1.ui.Stroke) safeArgs[0]);
            }
        }
        if ("stroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                return typedTarget.stroke(((Number) safeArgs[0]).floatValue(), ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("strokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.strokeColor(((Number) safeArgs[0]).intValue());
            }
        }
        if ("strokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.strokeOpacity(((Number) safeArgs[0]).intValue());
            }
        }
        if ("topLeftMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.topLeftMode(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("topOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.topOnlyMode(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("topRightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.topRightMode(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("trackComponentHorizontalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.trackComponentHorizontalPosition(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("trackComponentSide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.trackComponentSide(((Number) safeArgs[0]).intValue());
            }
        }
        if ("trackComponentVerticalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                return typedTarget.trackComponentVerticalPosition(((Number) safeArgs[0]).floatValue());
            }
        }
        if ("unlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.unlock(); return null;
            }
        }
        if ("useCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.useCache(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.plaf.Border typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getProperty((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThickness();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRectangleType();
            }
        }
        if ("lock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paint((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.Component) safeArgs[4]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2], (com.codename1.ui.Image) safeArgs[3], (com.codename1.ui.geom.Rectangle) safeArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setPaintOuterBorderFirst(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setThickness(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) safeArgs[0]); return null;
            }
        }
        if ("unlock".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.plaf.LookAndFeel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                typedTarget.bind((com.codename1.ui.Component) safeArgs[0]); return null;
            }
        }
        if ("calculateLabelSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class}, false)) {
                return typedTarget.calculateLabelSpan((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.Label) safeArgs[1]);
            }
        }
        if ("calculateTextAreaSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                return typedTarget.calculateTextAreaSpan((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]);
            }
        }
        if ("calculateTextFieldSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                return typedTarget.calculateTextFieldSpan((com.codename1.ui.TextSelection) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]);
            }
        }
        if ("drawButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                typedTarget.drawButton((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Button) safeArgs[1]); return null;
            }
        }
        if ("drawCheckBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                typedTarget.drawCheckBox((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Button) safeArgs[1]); return null;
            }
        }
        if ("drawComboBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                typedTarget.drawComboBox((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.List) safeArgs[1]); return null;
            }
        }
        if ("drawHorizontalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.drawHorizontalScroll((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("drawLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Label.class}, false)) {
                typedTarget.drawLabel((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Label) safeArgs[1]); return null;
            }
        }
        if ("drawList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                typedTarget.drawList((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.List) safeArgs[1]); return null;
            }
        }
        if ("drawPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                typedTarget.drawPullToRefresh((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("drawRadioButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                typedTarget.drawRadioButton((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Button) safeArgs[1]); return null;
            }
        }
        if ("drawTextArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                typedTarget.drawTextArea((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]); return null;
            }
        }
        if ("drawTextField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                typedTarget.drawTextField((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]); return null;
            }
        }
        if ("drawTextFieldCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                typedTarget.drawTextFieldCursor((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.TextArea) safeArgs[1]); return null;
            }
        }
        if ("drawVerticalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.drawVerticalScroll((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1], ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("getButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                return typedTarget.getButtonPreferredSize((com.codename1.ui.Button) safeArgs[0]);
            }
        }
        if ("getCheckBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                return typedTarget.getCheckBoxPreferredSize((com.codename1.ui.Button) safeArgs[0]);
            }
        }
        if ("getComboBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                return typedTarget.getComboBoxPreferredSize((com.codename1.ui.List) safeArgs[0]);
            }
        }
        if ("getDefaultDialogTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultDialogTransitionIn();
            }
        }
        if ("getDefaultDialogTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultDialogTransitionOut();
            }
        }
        if ("getDefaultFormTintColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFormTintColor();
            }
        }
        if ("getDefaultFormTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFormTransitionIn();
            }
        }
        if ("getDefaultFormTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultFormTransitionOut();
            }
        }
        if ("getDefaultMenuTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMenuTransitionIn();
            }
        }
        if ("getDefaultMenuTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultMenuTransitionOut();
            }
        }
        if ("getDefaultSmoothScrollingSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDefaultSmoothScrollingSpeed();
            }
        }
        if ("getDisableColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisableColor();
            }
        }
        if ("getFadeScrollBarSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFadeScrollBarSpeed();
            }
        }
        if ("getFadeScrollEdgeLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFadeScrollEdgeLength();
            }
        }
        if ("getHorizontalScrollHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHorizontalScrollHeight();
            }
        }
        if ("getLabelPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                return typedTarget.getLabelPreferredSize((com.codename1.ui.Label) safeArgs[0]);
            }
        }
        if ("getListPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                return typedTarget.getListPreferredSize((com.codename1.ui.List) safeArgs[0]);
            }
        }
        if ("getMenuBarClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMenuBarClass();
            }
        }
        if ("getMenuIcons".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMenuIcons();
            }
        }
        if ("getMenuRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMenuRenderer();
            }
        }
        if ("getPullToRefreshHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPullToRefreshHeight();
            }
        }
        if ("getRadioButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                return typedTarget.getRadioButtonPreferredSize((com.codename1.ui.Button) safeArgs[0]);
            }
        }
        if ("getTactileTouchDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTactileTouchDuration();
            }
        }
        if ("getTextAreaSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class, java.lang.Boolean.class}, false)) {
                return typedTarget.getTextAreaSize((com.codename1.ui.TextArea) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("getTextFieldCursorColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextFieldCursorColor();
            }
        }
        if ("getTextFieldPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class}, false)) {
                return typedTarget.getTextFieldPreferredSize((com.codename1.ui.TextArea) safeArgs[0]);
            }
        }
        if ("getTickerSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTickerSpeed();
            }
        }
        if ("getVerticalScrollWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVerticalScrollWidth();
            }
        }
        if ("isBackgroundImageDetermineSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isBackgroundImageDetermineSize();
            }
        }
        if ("isDefaultAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultAlwaysTensile();
            }
        }
        if ("isDefaultEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultEndsWith3Points();
            }
        }
        if ("isDefaultSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultSmoothScrolling();
            }
        }
        if ("isDefaultSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultSnapToGrid();
            }
        }
        if ("isDefaultTensileDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultTensileDrag();
            }
        }
        if ("isDefaultTensileHighlight".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isDefaultTensileHighlight();
            }
        }
        if ("isFadeScrollBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFadeScrollBar();
            }
        }
        if ("isFadeScrollEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFadeScrollEdge();
            }
        }
        if ("isFocusScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isFocusScrolling();
            }
        }
        if ("isRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRTL();
            }
        }
        if ("isReverseSoftButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isReverseSoftButtons();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isTouchMenus".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isTouchMenus();
            }
        }
        if ("paintTensileHighlight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                typedTarget.paintTensileHighlight((com.codename1.ui.Component) safeArgs[0], (com.codename1.ui.Graphics) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.refreshTheme(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBackgroundImageDetermineSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundImageDetermineSize(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultAlwaysTensile(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultDialogTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultDialogTransitionIn((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultDialogTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultDialogTransitionOut((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultEndsWith3Points(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultFormTintColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDefaultFormTintColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDefaultFormTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultFormTransitionIn((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultFormTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultFormTransitionOut((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultMenuTransitionIn((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                typedTarget.setDefaultMenuTransitionOut((com.codename1.ui.animations.Transition) safeArgs[0]); return null;
            }
        }
        if ("setDefaultSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultSmoothScrolling(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultSmoothScrollingSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDefaultSmoothScrollingSpeed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setDefaultSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultSnapToGrid(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultTensileDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setDefaultTensileDrag(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisableColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setDisableColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFG".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                typedTarget.setFG((com.codename1.ui.Graphics) safeArgs[0], (com.codename1.ui.Component) safeArgs[1]); return null;
            }
        }
        if ("setFadeScrollBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFadeScrollBar(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollBarSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFadeScrollBarSpeed(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFadeScrollEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFadeScrollEdge(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollEdgeLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFadeScrollEdgeLength(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFocusScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setFocusScrolling(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMenuBarClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                typedTarget.setMenuBarClass((java.lang.Class) safeArgs[0]); return null;
            }
        }
        if ("setMenuIcons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                typedTarget.setMenuIcons((com.codename1.ui.Image) safeArgs[0], (com.codename1.ui.Image) safeArgs[1], (com.codename1.ui.Image) safeArgs[2]); return null;
            }
        }
        if ("setMenuRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false)) {
                typedTarget.setMenuRenderer((com.codename1.ui.list.ListCellRenderer) safeArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setRTL(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReverseSoftButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setReverseSoftButtons(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTactileTouchDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTactileTouchDuration(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTextFieldCursorColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTextFieldCursorColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTickerSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setTickerSpeed(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setTouchMenus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setTouchMenus(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("uninstall".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.uninstall(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.plaf.Style typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addStyleListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.StyleListener.class}, false)) {
                typedTarget.addStyleListener((com.codename1.ui.events.StyleListener) safeArgs[0]); return null;
            }
        }
        if ("cacheMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.cacheMargins(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("flushMarginsCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.flushMarginsCache(); return null;
            }
        }
        if ("getAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackgroundGradientEndColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundGradientEndColor();
            }
        }
        if ("getBackgroundGradientRelativeSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundGradientRelativeSize();
            }
        }
        if ("getBackgroundGradientRelativeX".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundGradientRelativeX();
            }
        }
        if ("getBackgroundGradientRelativeY".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundGradientRelativeY();
            }
        }
        if ("getBackgroundGradientStartColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundGradientStartColor();
            }
        }
        if ("getBackgroundType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBackgroundType();
            }
        }
        if ("getBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBgColor();
            }
        }
        if ("getBgImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBgImage();
            }
        }
        if ("getBgPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBgPainter();
            }
        }
        if ("getBgTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBgTransparency();
            }
        }
        if ("getBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBorder();
            }
        }
        if ("getElevation".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getElevation();
            }
        }
        if ("getFgAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFgAlpha();
            }
        }
        if ("getFgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFgColor();
            }
        }
        if ("getFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFont();
            }
        }
        if ("getHorizontalMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHorizontalMargins();
            }
        }
        if ("getHorizontalPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getHorizontalPadding();
            }
        }
        if ("getIconGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconGap();
            }
        }
        if ("getIconGapUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIconGapUnit();
            }
        }
        if ("getMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getMargin(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return typedTarget.getMargin(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getMarginBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMarginBottom();
            }
        }
        if ("getMarginFloatValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return typedTarget.getMarginFloatValue(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getMarginLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.getMarginLeft(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getMarginLeftNoRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMarginLeftNoRTL();
            }
        }
        if ("getMarginRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.getMarginRight(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getMarginRightNoRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMarginRightNoRTL();
            }
        }
        if ("getMarginTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMarginTop();
            }
        }
        if ("getMarginUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMarginUnit();
            }
        }
        if ("getMarginValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return typedTarget.getMarginValue(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOpacity();
            }
        }
        if ("getPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getPadding(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return typedTarget.getPadding(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingFloatValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return typedTarget.getPaddingFloatValue(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.getPaddingLeft(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getPaddingLeftNoRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPaddingLeftNoRTL();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return typedTarget.getPaddingRight(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("getPaddingRightNoRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPaddingRightNoRTL();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getPaddingUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPaddingUnit();
            }
        }
        if ("getPaddingValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                return typedTarget.getPaddingValue(((Boolean) safeArgs[0]).booleanValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getTextDecoration".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextDecoration();
            }
        }
        if ("getVerticalMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVerticalMargins();
            }
        }
        if ("getVerticalPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVerticalPadding();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("is3DTextNorth".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.is3DTextNorth();
            }
        }
        if ("isLowered3DText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isLowered3DText();
            }
        }
        if ("isModified".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isModified();
            }
        }
        if ("isOverline".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isOverline();
            }
        }
        if ("isRaised3DText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRaised3DText();
            }
        }
        if ("isRendererStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRendererStyle();
            }
        }
        if ("isStrikeThru".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isStrikeThru();
            }
        }
        if ("isSuppressChangeEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSuppressChangeEvents();
            }
        }
        if ("isSurface".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSurface();
            }
        }
        if ("isUnderline".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUnderline();
            }
        }
        if ("markAsRendererStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.markAsRendererStyle(); return null;
            }
        }
        if ("merge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.merge((com.codename1.ui.plaf.Style) safeArgs[0]); return null;
            }
        }
        if ("removeListeners".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeListeners(); return null;
            }
        }
        if ("removeStyleListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.StyleListener.class}, false)) {
                typedTarget.removeStyleListener((com.codename1.ui.events.StyleListener) safeArgs[0]); return null;
            }
        }
        if ("restoreCachedMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.restoreCachedMargins(); return null;
            }
        }
        if ("set3DText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.set3DText(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("set3DTextNorth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.set3DTextNorth(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setAlignment(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setAlignment(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientEndColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBackgroundGradientEndColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBackgroundGradientEndColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundGradientEndColor(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setBackgroundGradientRelativeSize(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundGradientRelativeSize(((Number) safeArgs[0]).floatValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setBackgroundGradientRelativeX(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundGradientRelativeX(((Number) safeArgs[0]).floatValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setBackgroundGradientRelativeY(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundGradientRelativeY(((Number) safeArgs[0]).floatValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientStartColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBackgroundGradientStartColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBackgroundGradientStartColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundGradientStartColor(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setBackgroundType(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setBackgroundType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBackgroundType(((Number) safeArgs[0]).byteValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBgColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBgColor(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBgImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setBgImage((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setBgImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBgImage((com.codename1.ui.Image) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBgPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Painter.class}, false)) {
                typedTarget.setBgPainter((com.codename1.ui.Painter) safeArgs[0]); return null;
            }
        }
        if ("setBgTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setBgTransparency(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setBgTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setBgTransparency(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setBgTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBgTransparency(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                typedTarget.setBorder((com.codename1.ui.plaf.Border) safeArgs[0]); return null;
            }
        }
        if ("setBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class, java.lang.Boolean.class}, false)) {
                typedTarget.setBorder((com.codename1.ui.plaf.Border) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setElevation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setElevation(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setElevation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setElevation(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setFgAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFgAlpha(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFgAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setFgAlpha(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setFgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setFgColor(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setFgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setFgColor(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                typedTarget.setFont((com.codename1.ui.Font) safeArgs[0]); return null;
            }
        }
        if ("setFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Boolean.class}, false)) {
                typedTarget.setFont((com.codename1.ui.Font) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setIconGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setIconGap(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setIconGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.setIconGap(((Number) safeArgs[0]).floatValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setIconGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Byte.class}, false)) {
                typedTarget.setIconGap(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).byteValue()); return null;
            }
        }
        if ("setIconGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Byte.class, java.lang.Boolean.class}, false)) {
                typedTarget.setIconGap(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).byteValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setIconGapUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setIconGapUnit(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setIconGapUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Boolean.class}, false)) {
                typedTarget.setIconGapUnit(((Number) safeArgs[0]).byteValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                typedTarget.setMargin(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setMargin(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.setMargin(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setMargin(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.setMargin(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setMargin(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("setMarginBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMarginBottom(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMarginBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setMarginBottom(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setMarginLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMarginLeft(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMarginLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setMarginLeft(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setMarginRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMarginRight(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMarginRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setMarginRight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setMarginTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setMarginTop(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setMarginTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setMarginTop(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setMarginUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, true)) {
                byte[] varArgs = new byte[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = ((Number) safeArgs[i]).byteValue();
                }
                typedTarget.setMarginUnit(varArgs); return null;
            }
        }
        if ("setMarginUnitBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setMarginUnitBottom(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setMarginUnitLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setMarginUnitLeft(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setMarginUnitRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setMarginUnitRight(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setMarginUnitTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setMarginUnitTop(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setOpacity(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setOpacity(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setOverline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setOverline(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                typedTarget.setPadding(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setPadding(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                typedTarget.setPadding(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setPadding(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                typedTarget.setPadding(((Number) safeArgs[0]).floatValue(), ((Number) safeArgs[1]).floatValue(), ((Number) safeArgs[2]).floatValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.setPadding(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("setPaddingBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setPaddingBottom(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setPaddingBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPaddingBottom(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPaddingLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setPaddingLeft(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setPaddingLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPaddingLeft(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPaddingRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setPaddingRight(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setPaddingRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPaddingRight(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPaddingTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                typedTarget.setPaddingTop(((Number) safeArgs[0]).floatValue()); return null;
            }
        }
        if ("setPaddingTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setPaddingTop(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setPaddingUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, true)) {
                byte[] varArgs = new byte[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = ((Number) safeArgs[i]).byteValue();
                }
                typedTarget.setPaddingUnit(varArgs); return null;
            }
        }
        if ("setPaddingUnitBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPaddingUnitBottom(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setPaddingUnitLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPaddingUnitLeft(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setPaddingUnitRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPaddingUnitRight(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setPaddingUnitTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                typedTarget.setPaddingUnitTop(((Number) safeArgs[0]).byteValue()); return null;
            }
        }
        if ("setStrikeThru".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setStrikeThru(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSuppressChangeEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSuppressChangeEvents(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSurface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setSurface(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSurface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                typedTarget.setSurface(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setTextDecoration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setTextDecoration(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setTextDecoration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.setTextDecoration(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("setUnderline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setUnderline(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.stripMarginAndPadding(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.plaf.UIManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addThemeProps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                typedTarget.addThemeProps((java.util.Hashtable) safeArgs[0]); return null;
            }
        }
        if ("addThemeRefreshListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.addThemeRefreshListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("getBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBundle();
            }
        }
        if ("getComponentCustomStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.getComponentCustomStyle((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getComponentSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getComponentSelectedStyle((java.lang.String) safeArgs[0]);
            }
        }
        if ("getComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getComponentStyle((java.lang.String) safeArgs[0]);
            }
        }
        if ("getIconUIIDFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getIconUIIDFor((java.lang.String) safeArgs[0]);
            }
        }
        if ("getLookAndFeel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLookAndFeel();
            }
        }
        if ("getResourceBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getResourceBundle();
            }
        }
        if ("getThemeConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                return typedTarget.getThemeConstant((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("getThemeConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.getThemeConstant((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("getThemeImageConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getThemeImageConstant((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThemeMaskConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.getThemeMaskConstant((java.lang.String) safeArgs[0]);
            }
        }
        if ("getThemeName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getThemeName();
            }
        }
        if ("isThemeConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isThemeConstant((java.lang.String) safeArgs[0]);
            }
        }
        if ("isThemeConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.isThemeConstant((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue());
            }
        }
        if ("isUseLargerTextScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUseLargerTextScale();
            }
        }
        if ("localize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.localize((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if ("parseComponentCustomStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 4];
                for (int i = 4; i < safeArgs.length; i++) {
                    varArgs[i - 4] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.parseComponentCustomStyle((com.codename1.ui.util.Resources) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], varArgs);
            }
        }
        if ("parseComponentSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 3];
                for (int i = 3; i < safeArgs.length; i++) {
                    varArgs[i - 3] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.parseComponentSelectedStyle((com.codename1.ui.util.Resources) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], varArgs);
            }
        }
        if ("parseComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 3];
                for (int i = 3; i < safeArgs.length; i++) {
                    varArgs[i - 3] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.parseComponentStyle((com.codename1.ui.util.Resources) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], varArgs);
            }
        }
        if ("removeThemeRefreshListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                typedTarget.removeThemeRefreshListener((com.codename1.ui.events.ActionListener) safeArgs[0]); return null;
            }
        }
        if ("setBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.setBundle((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("setComponentSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.setComponentSelectedStyle((java.lang.String) safeArgs[0], (com.codename1.ui.plaf.Style) safeArgs[1]); return null;
            }
        }
        if ("setComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                typedTarget.setComponentStyle((java.lang.String) safeArgs[0], (com.codename1.ui.plaf.Style) safeArgs[1]); return null;
            }
        }
        if ("setComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class, java.lang.String.class}, false)) {
                typedTarget.setComponentStyle((java.lang.String) safeArgs[0], (com.codename1.ui.plaf.Style) safeArgs[1], (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("setLookAndFeel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.LookAndFeel.class}, false)) {
                typedTarget.setLookAndFeel((com.codename1.ui.plaf.LookAndFeel) safeArgs[0]); return null;
            }
        }
        if ("setResourceBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                typedTarget.setResourceBundle((java.util.Hashtable) safeArgs[0]); return null;
            }
        }
        if ("setThemeProps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                typedTarget.setThemeProps((java.util.Hashtable) safeArgs[0]); return null;
            }
        }
        if ("setUseLargerTextScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setUseLargerTextScale(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("wasThemeInstalled".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.wasThemeInstalled();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.plaf.CSSBorder.class) {
            if ("HPOSITION_CENTER".equals(name)) return com.codename1.ui.plaf.CSSBorder.HPOSITION_CENTER;
            if ("HPOSITION_LEFT".equals(name)) return com.codename1.ui.plaf.CSSBorder.HPOSITION_LEFT;
            if ("HPOSITION_OTHER".equals(name)) return com.codename1.ui.plaf.CSSBorder.HPOSITION_OTHER;
            if ("HPOSITION_RIGHT".equals(name)) return com.codename1.ui.plaf.CSSBorder.HPOSITION_RIGHT;
            if ("REPEAT_BOTH".equals(name)) return com.codename1.ui.plaf.CSSBorder.REPEAT_BOTH;
            if ("REPEAT_NONE".equals(name)) return com.codename1.ui.plaf.CSSBorder.REPEAT_NONE;
            if ("REPEAT_X".equals(name)) return com.codename1.ui.plaf.CSSBorder.REPEAT_X;
            if ("REPEAT_Y".equals(name)) return com.codename1.ui.plaf.CSSBorder.REPEAT_Y;
            if ("SIZE_AUTO".equals(name)) return com.codename1.ui.plaf.CSSBorder.SIZE_AUTO;
            if ("SIZE_CONTAIN".equals(name)) return com.codename1.ui.plaf.CSSBorder.SIZE_CONTAIN;
            if ("SIZE_COVER".equals(name)) return com.codename1.ui.plaf.CSSBorder.SIZE_COVER;
            if ("SIZE_OTHER".equals(name)) return com.codename1.ui.plaf.CSSBorder.SIZE_OTHER;
            if ("STYLE_DASHED".equals(name)) return com.codename1.ui.plaf.CSSBorder.STYLE_DASHED;
            if ("STYLE_DOTTED".equals(name)) return com.codename1.ui.plaf.CSSBorder.STYLE_DOTTED;
            if ("STYLE_HIDDEN".equals(name)) return com.codename1.ui.plaf.CSSBorder.STYLE_HIDDEN;
            if ("STYLE_NONE".equals(name)) return com.codename1.ui.plaf.CSSBorder.STYLE_NONE;
            if ("STYLE_SOLID".equals(name)) return com.codename1.ui.plaf.CSSBorder.STYLE_SOLID;
            if ("UNIT_EM".equals(name)) return com.codename1.ui.plaf.CSSBorder.UNIT_EM;
            if ("UNIT_MM".equals(name)) return com.codename1.ui.plaf.CSSBorder.UNIT_MM;
            if ("UNIT_PERCENT".equals(name)) return com.codename1.ui.plaf.CSSBorder.UNIT_PERCENT;
            if ("UNIT_PIXELS".equals(name)) return com.codename1.ui.plaf.CSSBorder.UNIT_PIXELS;
            if ("VPOSITION_BOTTOM".equals(name)) return com.codename1.ui.plaf.CSSBorder.VPOSITION_BOTTOM;
            if ("VPOSITION_CENTER".equals(name)) return com.codename1.ui.plaf.CSSBorder.VPOSITION_CENTER;
            if ("VPOSITION_OTHER".equals(name)) return com.codename1.ui.plaf.CSSBorder.VPOSITION_OTHER;
            if ("VPOSITION_TOP".equals(name)) return com.codename1.ui.plaf.CSSBorder.VPOSITION_TOP;
        }
        if (type == com.codename1.ui.plaf.Style.class) {
            if ("ALIGNMENT".equals(name)) return com.codename1.ui.plaf.Style.ALIGNMENT;
            if ("BACKGROUND_ALIGNMENT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_ALIGNMENT;
            if ("BACKGROUND_GRADIENT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_GRADIENT;
            if ("BACKGROUND_GRADIENT_LINEAR_HORIZONTAL".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL;
            if ("BACKGROUND_GRADIENT_LINEAR_VERTICAL".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL;
            if ("BACKGROUND_GRADIENT_RADIAL".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_GRADIENT_RADIAL;
            if ("BACKGROUND_IMAGE_ALIGNED_BOTTOM".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM;
            if ("BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT;
            if ("BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT;
            if ("BACKGROUND_IMAGE_ALIGNED_CENTER".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_CENTER;
            if ("BACKGROUND_IMAGE_ALIGNED_LEFT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_LEFT;
            if ("BACKGROUND_IMAGE_ALIGNED_RIGHT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_RIGHT;
            if ("BACKGROUND_IMAGE_ALIGNED_TOP".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_TOP;
            if ("BACKGROUND_IMAGE_ALIGNED_TOP_LEFT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT;
            if ("BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT;
            if ("BACKGROUND_IMAGE_SCALED".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED;
            if ("BACKGROUND_IMAGE_SCALED_FILL".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED_FILL;
            if ("BACKGROUND_IMAGE_SCALED_FIT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED_FIT;
            if ("BACKGROUND_IMAGE_TILE_BOTH".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_BOTH;
            if ("BACKGROUND_IMAGE_TILE_HORIZONTAL".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_HORIZONTAL;
            if ("BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM;
            if ("BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER;
            if ("BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP;
            if ("BACKGROUND_IMAGE_TILE_VERTICAL".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_VERTICAL;
            if ("BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER;
            if ("BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT;
            if ("BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT;
            if ("BACKGROUND_NONE".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_NONE;
            if ("BACKGROUND_TYPE".equals(name)) return com.codename1.ui.plaf.Style.BACKGROUND_TYPE;
            if ("BG_COLOR".equals(name)) return com.codename1.ui.plaf.Style.BG_COLOR;
            if ("BG_IMAGE".equals(name)) return com.codename1.ui.plaf.Style.BG_IMAGE;
            if ("BORDER".equals(name)) return com.codename1.ui.plaf.Style.BORDER;
            if ("ELEVATION".equals(name)) return com.codename1.ui.plaf.Style.ELEVATION;
            if ("FG_ALPHA".equals(name)) return com.codename1.ui.plaf.Style.FG_ALPHA;
            if ("FG_COLOR".equals(name)) return com.codename1.ui.plaf.Style.FG_COLOR;
            if ("FONT".equals(name)) return com.codename1.ui.plaf.Style.FONT;
            if ("ICON_GAP".equals(name)) return com.codename1.ui.plaf.Style.ICON_GAP;
            if ("ICON_GAP_UNIT".equals(name)) return com.codename1.ui.plaf.Style.ICON_GAP_UNIT;
            if ("MARGIN".equals(name)) return com.codename1.ui.plaf.Style.MARGIN;
            if ("MARGIN_UNIT".equals(name)) return com.codename1.ui.plaf.Style.MARGIN_UNIT;
            if ("OPACITY".equals(name)) return com.codename1.ui.plaf.Style.OPACITY;
            if ("PADDING".equals(name)) return com.codename1.ui.plaf.Style.PADDING;
            if ("PADDING_UNIT".equals(name)) return com.codename1.ui.plaf.Style.PADDING_UNIT;
            if ("PAINTER".equals(name)) return com.codename1.ui.plaf.Style.PAINTER;
            if ("SURFACE".equals(name)) return com.codename1.ui.plaf.Style.SURFACE;
            if ("TEXT_DECORATION".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION;
            if ("TEXT_DECORATION_3D".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_3D;
            if ("TEXT_DECORATION_3D_LOWERED".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_3D_LOWERED;
            if ("TEXT_DECORATION_3D_SHADOW_NORTH".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_3D_SHADOW_NORTH;
            if ("TEXT_DECORATION_NONE".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_NONE;
            if ("TEXT_DECORATION_OVERLINE".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_OVERLINE;
            if ("TEXT_DECORATION_STRIKETHRU".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_STRIKETHRU;
            if ("TEXT_DECORATION_UNDERLINE".equals(name)) return com.codename1.ui.plaf.Style.TEXT_DECORATION_UNDERLINE;
            if ("TRANSPARENCY".equals(name)) return com.codename1.ui.plaf.Style.TRANSPARENCY;
            if ("UNIT_TYPE_DIPS".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS;
            if ("UNIT_TYPE_PIXELS".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS;
            if ("UNIT_TYPE_REM".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_REM;
            if ("UNIT_TYPE_SCREEN_PERCENTAGE".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_SCREEN_PERCENTAGE;
            if ("UNIT_TYPE_VH".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_VH;
            if ("UNIT_TYPE_VMAX".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_VMAX;
            if ("UNIT_TYPE_VMIN".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_VMIN;
            if ("UNIT_TYPE_VW".equals(name)) return com.codename1.ui.plaf.Style.UNIT_TYPE_VW;
        }
        if (type == com.codename1.ui.plaf.StyleParser.class) {
            if ("UNIT_INHERIT".equals(name)) return com.codename1.ui.plaf.StyleParser.UNIT_INHERIT;
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
