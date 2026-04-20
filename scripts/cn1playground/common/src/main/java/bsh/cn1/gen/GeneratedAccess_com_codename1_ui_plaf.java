package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ui_plaf {
    private GeneratedAccess_com_codename1_ui_plaf() {
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
        if ("Border".equals(simpleName)) {
            return com.codename1.ui.plaf.Border.class;
        }
        if ("CSSBorder".equals(simpleName)) {
            return com.codename1.ui.plaf.CSSBorder.class;
        }
        if ("DefaultLookAndFeel".equals(simpleName)) {
            return com.codename1.ui.plaf.DefaultLookAndFeel.class;
        }
        if ("LookAndFeel".equals(simpleName)) {
            return com.codename1.ui.plaf.LookAndFeel.class;
        }
        if ("RoundBorder".equals(simpleName)) {
            return com.codename1.ui.plaf.RoundBorder.class;
        }
        if ("RoundRectBorder".equals(simpleName)) {
            return com.codename1.ui.plaf.RoundRectBorder.class;
        }
        if ("Style".equals(simpleName)) {
            return com.codename1.ui.plaf.Style.class;
        }
        if ("StyleParser".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.class;
        }
        if ("BorderInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.BorderInfo.class;
        }
        if ("BoxInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.BoxInfo.class;
        }
        if ("FontInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.FontInfo.class;
        }
        if ("ImageInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.ImageInfo.class;
        }
        if ("MarginInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.MarginInfo.class;
        }
        if ("PaddingInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.PaddingInfo.class;
        }
        if ("ScalarValue".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.ScalarValue.class;
        }
        if ("StyleInfo".equals(simpleName)) {
            return com.codename1.ui.plaf.StyleParser.StyleInfo.class;
        }
        if ("UIManager".equals(simpleName)) {
            return com.codename1.ui.plaf.UIManager.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ui.plaf.CSSBorder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.plaf.CSSBorder();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                return new com.codename1.ui.plaf.CSSBorder((com.codename1.ui.util.Resources) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ui.plaf.CSSBorder((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class}, false);
                return new com.codename1.ui.plaf.CSSBorder((com.codename1.ui.util.Resources) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ui.plaf.DefaultLookAndFeel.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                return new com.codename1.ui.plaf.DefaultLookAndFeel((com.codename1.ui.plaf.UIManager) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.plaf.Style.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.plaf.Style();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                return new com.codename1.ui.plaf.Style((com.codename1.ui.plaf.Style) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Font.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Font.class, java.lang.Byte.class}, false);
                return new com.codename1.ui.plaf.Style(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (com.codename1.ui.Font) adaptedArgs[2], (byte) toIntValue(adaptedArgs[3]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Font.class, java.lang.Byte.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Font.class, java.lang.Byte.class, com.codename1.ui.Image.class, java.lang.Byte.class}, false);
                return new com.codename1.ui.plaf.Style(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (com.codename1.ui.Font) adaptedArgs[2], (byte) toIntValue(adaptedArgs[3]), (com.codename1.ui.Image) adaptedArgs[4], (byte) toIntValue(adaptedArgs[5]));
            }
        }
        if (type == com.codename1.ui.plaf.StyleParser.BoxInfo.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false);
                return new com.codename1.ui.plaf.StyleParser.BoxInfo((com.codename1.ui.plaf.StyleParser.ScalarValue[]) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.plaf.StyleParser.ImageInfo.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ui.plaf.StyleParser.ImageInfo((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.plaf.StyleParser.MarginInfo.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false);
                return new com.codename1.ui.plaf.StyleParser.MarginInfo((com.codename1.ui.plaf.StyleParser.ScalarValue[]) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.plaf.StyleParser.PaddingInfo.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false);
                return new com.codename1.ui.plaf.StyleParser.PaddingInfo((com.codename1.ui.plaf.StyleParser.ScalarValue[]) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ui.plaf.StyleParser.ScalarValue.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.plaf.StyleParser.ScalarValue();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Byte.class}, false);
                return new com.codename1.ui.plaf.StyleParser.ScalarValue(((Number) adaptedArgs[0]).doubleValue(), (byte) toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.ui.plaf.StyleParser.StyleInfo.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ui.plaf.StyleParser.StyleInfo();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.StyleInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.StyleInfo.class}, false);
                return new com.codename1.ui.plaf.StyleParser.StyleInfo((com.codename1.ui.plaf.StyleParser.StyleInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return new com.codename1.ui.plaf.StyleParser.StyleInfo((java.util.Map) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return new com.codename1.ui.plaf.StyleParser.StyleInfo(varArgs);
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
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.createBevelLowered();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createBevelLowered(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("createBevelRaised".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.createBevelRaised();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createBevelRaised(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("createCompoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class, com.codename1.ui.plaf.Border.class}, false);
                return com.codename1.ui.plaf.Border.createCompoundBorder((com.codename1.ui.plaf.Border) adaptedArgs[0], (com.codename1.ui.plaf.Border) adaptedArgs[1], (com.codename1.ui.plaf.Border) adaptedArgs[2], (com.codename1.ui.plaf.Border) adaptedArgs[3]);
            }
        }
        if ("createDashedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createDashedBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createDashedBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createDottedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createDottedBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createDottedBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createDoubleBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createDoubleBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createDoubleBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.createEmpty();
            }
        }
        if ("createEtchedLowered".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.createEtchedLowered();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createEtchedLowered(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createEtchedRaised".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.createEtchedRaised();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createEtchedRaised(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createGrooveBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createGrooveBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createGrooveBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createHorizonalImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                return com.codename1.ui.plaf.Border.createHorizonalImageBorder((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]);
            }
        }
        if ("createImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                return com.codename1.ui.plaf.Border.createImageBorder((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                return com.codename1.ui.plaf.Border.createImageBorder((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.Image) adaptedArgs[4], (com.codename1.ui.Image) adaptedArgs[5], (com.codename1.ui.Image) adaptedArgs[6], (com.codename1.ui.Image) adaptedArgs[7], (com.codename1.ui.Image) adaptedArgs[8]);
            }
        }
        if ("createImageScaledBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                return com.codename1.ui.plaf.Border.createImageScaledBorder((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.Image) adaptedArgs[4], (com.codename1.ui.Image) adaptedArgs[5], (com.codename1.ui.Image) adaptedArgs[6], (com.codename1.ui.Image) adaptedArgs[7], (com.codename1.ui.Image) adaptedArgs[8]);
            }
        }
        if ("createImageSplicedBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class}, false);
                return com.codename1.ui.plaf.Border.createImageSplicedBorder((com.codename1.ui.Image) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).doubleValue(), ((Number) adaptedArgs[4]).doubleValue());
            }
        }
        if ("createInsetBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createInsetBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createInsetBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createLineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createLineBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createLineBorder(((Number) adaptedArgs[0]).floatValue(), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createLineBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return com.codename1.ui.plaf.Border.createLineBorder(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class}, false);
                return com.codename1.ui.plaf.Border.createLineBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("createOutsetBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createOutsetBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createOutsetBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createRidgeBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createRidgeBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createRidgeBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createRoundBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createRoundBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.plaf.Border.createRoundBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createRoundBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                return com.codename1.ui.plaf.Border.createRoundBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("createUndelineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.ui.plaf.Border.createUndelineBorder(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createUndelineBorder(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createUnderlineBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return com.codename1.ui.plaf.Border.createUnderlineBorder(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createUnderlineBorder(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createUnderlineBorder(((Number) adaptedArgs[0]).floatValue(), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.Border.createUnderlineBorder(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createVerticalImageBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                return com.codename1.ui.plaf.Border.createVerticalImageBorder((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]);
            }
        }
        if ("getDefaultBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.getDefaultBorder();
            }
        }
        if ("getEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.Border.getEmpty();
            }
        }
        if ("setDefaultBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                com.codename1.ui.plaf.Border.setDefaultBorder((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.Border.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("reverseAlignForBidi".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                return com.codename1.ui.plaf.DefaultLookAndFeel.reverseAlignForBidi((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.DefaultLookAndFeel.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.RoundBorder.create();
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.RoundBorder.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("create".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.RoundRectBorder.create();
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.RoundRectBorder.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("createProxyStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style[].class}, true);
                com.codename1.ui.plaf.Style[] varArgs = new com.codename1.ui.plaf.Style[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.plaf.Style) adaptedArgs[i];
                }
                return com.codename1.ui.plaf.Style.createProxyStyle(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.Style.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("getBackgroundTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.StyleParser.getBackgroundTypes();
            }
        }
        if ("getSupportedBackgroundTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.StyleParser.getSupportedBackgroundTypes();
            }
        }
        if ("parseScalarValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.plaf.StyleParser.parseScalarValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("validateScalarValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.plaf.StyleParser.validateScalarValue((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ui.plaf.StyleParser.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("createInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.UIManager.createInstance();
            }
        }
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ui.plaf.UIManager.getInstance();
            }
        }
        if ("initFirstTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ui.plaf.UIManager.initFirstTheme((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("initNamedTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ui.plaf.UIManager.initNamedTheme((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
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
        if (target instanceof com.codename1.ui.plaf.StyleParser.MarginInfo) {
            try {
                return invoke4((com.codename1.ui.plaf.StyleParser.MarginInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.PaddingInfo) {
            try {
                return invoke5((com.codename1.ui.plaf.StyleParser.PaddingInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.Border) {
            try {
                return invoke6((com.codename1.ui.plaf.Border) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.LookAndFeel) {
            try {
                return invoke7((com.codename1.ui.plaf.LookAndFeel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.Style) {
            try {
                return invoke8((com.codename1.ui.plaf.Style) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.BorderInfo) {
            try {
                return invoke9((com.codename1.ui.plaf.StyleParser.BorderInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.BoxInfo) {
            try {
                return invoke10((com.codename1.ui.plaf.StyleParser.BoxInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.FontInfo) {
            try {
                return invoke11((com.codename1.ui.plaf.StyleParser.FontInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.ImageInfo) {
            try {
                return invoke12((com.codename1.ui.plaf.StyleParser.ImageInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.ScalarValue) {
            try {
                return invoke13((com.codename1.ui.plaf.StyleParser.ScalarValue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.StyleParser.StyleInfo) {
            try {
                return invoke14((com.codename1.ui.plaf.StyleParser.StyleInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ui.plaf.UIManager) {
            try {
                return invoke15((com.codename1.ui.plaf.UIManager) target, name, safeArgs);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("backgroundColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.backgroundColor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("backgroundImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.backgroundImage((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image[].class}, true);
                com.codename1.ui.Image[] varArgs = new com.codename1.ui.Image[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Image) adaptedArgs[i];
                }
                return typedTarget.backgroundImage(varArgs);
            }
        }
        if ("backgroundPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.backgroundPosition(varArgs);
            }
        }
        if ("backgroundRepeat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.backgroundRepeat(varArgs);
            }
        }
        if ("borderColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.borderColor(varArgs);
            }
        }
        if ("borderImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, double[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, double[].class}, true);
                double[] varArgs = new double[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).doubleValue();
                }
                return typedTarget.borderImage((com.codename1.ui.Image) adaptedArgs[0], varArgs);
            }
        }
        if ("borderImageWithName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, double[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, double[].class}, true);
                double[] varArgs = new double[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = ((Number) adaptedArgs[i]).doubleValue();
                }
                return typedTarget.borderImageWithName((java.lang.String) adaptedArgs[0], varArgs);
            }
        }
        if ("borderRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.borderRadius((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("borderStroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.borderStroke(varArgs);
            }
        }
        if ("borderStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.borderStyle(varArgs);
            }
        }
        if ("borderWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.borderWidth(varArgs);
            }
        }
        if ("boxShadow".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.boxShadow((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThickness();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRectangleType();
            }
        }
        if ("lock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.geom.Rectangle) adaptedArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPaintOuterBorderFirst(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setThickness(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("toCSSString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toCSSString();
            }
        }
        if ("unlock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ui.plaf.DefaultLookAndFeel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.bind((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("calculateLabelSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class}, false);
                return typedTarget.calculateLabelSpan((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1]);
            }
        }
        if ("calculateSpanForLabelText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.calculateSpanForLabelText((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1], (java.lang.String) adaptedArgs[2], toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]));
            }
        }
        if ("calculateTextAreaSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false);
                return typedTarget.calculateTextAreaSpan((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]);
            }
        }
        if ("calculateTextFieldSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false);
                return typedTarget.calculateTextFieldSpan((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]);
            }
        }
        if ("drawButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false);
                typedTarget.drawButton((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Button) adaptedArgs[1]); return null;
            }
        }
        if ("drawCheckBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false);
                typedTarget.drawCheckBox((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Button) adaptedArgs[1]); return null;
            }
        }
        if ("drawComboBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false);
                typedTarget.drawComboBox((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1]); return null;
            }
        }
        if ("drawHorizontalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.drawHorizontalScroll((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("drawLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Label.class}, false);
                typedTarget.drawLabel((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1]); return null;
            }
        }
        if ("drawList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false);
                typedTarget.drawList((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1]); return null;
            }
        }
        if ("drawPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Boolean.class}, false);
                typedTarget.drawPullToRefresh((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("drawRadioButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false);
                typedTarget.drawRadioButton((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Button) adaptedArgs[1]); return null;
            }
        }
        if ("drawTextArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.drawTextArea((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("drawTextField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.drawTextField((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("drawTextFieldCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.drawTextFieldCursor((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("drawVerticalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.drawVerticalScroll((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("focusGained".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.focusGained((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("focusLost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.focusLost((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("getButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.getButtonPreferredSize((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("getCheckBoxFocusImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCheckBoxFocusImages();
            }
        }
        if ("getCheckBoxImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCheckBoxImages();
            }
        }
        if ("getCheckBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.getCheckBoxPreferredSize((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("getComboBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false);
                return typedTarget.getComboBoxPreferredSize((com.codename1.ui.List) adaptedArgs[0]);
            }
        }
        if ("getDefaultDialogTransitionIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultDialogTransitionIn();
            }
        }
        if ("getDefaultDialogTransitionOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultDialogTransitionOut();
            }
        }
        if ("getDefaultFormTintColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFormTintColor();
            }
        }
        if ("getDefaultFormTransitionIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFormTransitionIn();
            }
        }
        if ("getDefaultFormTransitionOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFormTransitionOut();
            }
        }
        if ("getDefaultMenuTransitionIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMenuTransitionIn();
            }
        }
        if ("getDefaultMenuTransitionOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMenuTransitionOut();
            }
        }
        if ("getDefaultSmoothScrollingSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultSmoothScrollingSpeed();
            }
        }
        if ("getDisableColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisableColor();
            }
        }
        if ("getFadeScrollBarSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFadeScrollBarSpeed();
            }
        }
        if ("getFadeScrollEdgeLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFadeScrollEdgeLength();
            }
        }
        if ("getHorizontalScrollHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalScrollHeight();
            }
        }
        if ("getLabelPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                return typedTarget.getLabelPreferredSize((com.codename1.ui.Label) adaptedArgs[0]);
            }
        }
        if ("getListPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false);
                return typedTarget.getListPreferredSize((com.codename1.ui.List) adaptedArgs[0]);
            }
        }
        if ("getMenuBarClass".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMenuBarClass();
            }
        }
        if ("getMenuIcons".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMenuIcons();
            }
        }
        if ("getMenuRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMenuRenderer();
            }
        }
        if ("getPullToRefreshHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPullToRefreshHeight();
            }
        }
        if ("getRadioButtonFocusImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadioButtonFocusImages();
            }
        }
        if ("getRadioButtonImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadioButtonImages();
            }
        }
        if ("getRadioButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.getRadioButtonPreferredSize((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("getTactileTouchDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTactileTouchDuration();
            }
        }
        if ("getTextAreaSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class, java.lang.Boolean.class}, false);
                return typedTarget.getTextAreaSize((com.codename1.ui.TextArea) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getTextFieldCursorColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextFieldCursorColor();
            }
        }
        if ("getTextFieldPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class}, false);
                return typedTarget.getTextFieldPreferredSize((com.codename1.ui.TextArea) adaptedArgs[0]);
            }
        }
        if ("getTickerSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTickerSpeed();
            }
        }
        if ("getVerticalScrollWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalScrollWidth();
            }
        }
        if ("isBackgroundImageDetermineSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundImageDetermineSize();
            }
        }
        if ("isDefaultAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultAlwaysTensile();
            }
        }
        if ("isDefaultEndsWith3Points".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultEndsWith3Points();
            }
        }
        if ("isDefaultSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultSmoothScrolling();
            }
        }
        if ("isDefaultSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultSnapToGrid();
            }
        }
        if ("isDefaultTensileDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultTensileDrag();
            }
        }
        if ("isDefaultTensileHighlight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultTensileHighlight();
            }
        }
        if ("isFadeScrollBar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFadeScrollBar();
            }
        }
        if ("isFadeScrollEdge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFadeScrollEdge();
            }
        }
        if ("isFocusScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusScrolling();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isReverseSoftButtons".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReverseSoftButtons();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isTickWhenFocused".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTickWhenFocused();
            }
        }
        if ("isTouchMenus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouchMenus();
            }
        }
        if ("paintTensileHighlight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                typedTarget.paintTensileHighlight((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Graphics) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBackgroundImageDetermineSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBackgroundImageDetermineSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCheckBoxFocusImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setCheckBoxFocusImages((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3]); return null;
            }
        }
        if ("setCheckBoxImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setCheckBoxImages((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setCheckBoxImages((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3]); return null;
            }
        }
        if ("setComboBoxImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setComboBoxImage((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultDialogTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultDialogTransitionIn((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultDialogTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultDialogTransitionOut((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultEndsWith3Points(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultFormTintColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDefaultFormTintColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDefaultFormTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultFormTransitionIn((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultFormTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultFormTransitionOut((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultMenuTransitionIn((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultMenuTransitionOut((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultSmoothScrollingSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDefaultSmoothScrollingSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDefaultSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultTensileDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultTensileDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisableColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDisableColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setFG".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.setFG((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("setFadeScrollBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFadeScrollBar(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollBarSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFadeScrollBarSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setFadeScrollEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFadeScrollEdge(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollEdgeLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFadeScrollEdgeLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setFocusScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMenuBarClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                typedTarget.setMenuBarClass((java.lang.Class) adaptedArgs[0]); return null;
            }
        }
        if ("setMenuIcons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setMenuIcons((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]); return null;
            }
        }
        if ("setMenuRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false);
                typedTarget.setMenuRenderer((com.codename1.ui.list.ListCellRenderer) adaptedArgs[0]); return null;
            }
        }
        if ("setPasswordChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                typedTarget.setPasswordChar(((Character) adaptedArgs[0]).charValue()); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRadioButtonFocusImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setRadioButtonFocusImages((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3]); return null;
            }
        }
        if ("setRadioButtonImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setRadioButtonImages((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setRadioButtonImages((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3]); return null;
            }
        }
        if ("setReverseSoftButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReverseSoftButtons(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTactileTouchDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTactileTouchDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTextFieldCursorColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTextFieldCursorColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTickWhenFocused".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTickWhenFocused(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTickerSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setTickerSpeed(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setTouchMenus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTouchMenus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("uninstall".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.uninstall(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ui.plaf.RoundBorder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("color".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.color(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpacity();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getShadowBlur".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowBlur();
            }
        }
        if ("getShadowOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowOpacity();
            }
        }
        if ("getShadowSpread".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowSpread();
            }
        }
        if ("getShadowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowX();
            }
        }
        if ("getShadowY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowY();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeOpacity();
            }
        }
        if ("getStrokeThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeThickness();
            }
        }
        if ("getThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThickness();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isOnlyLeftRounded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOnlyLeftRounded();
            }
        }
        if ("isOnlyRightRounded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOnlyRightRounded();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRectangle();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRectangleType();
            }
        }
        if ("isShadowMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShadowMM();
            }
        }
        if ("isStrokeMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStrokeMM();
            }
        }
        if ("lock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("onlyLeftRounded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.onlyLeftRounded(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("onlyRightRounded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.onlyRightRounded(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("opacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.opacity(toIntValue(adaptedArgs[0]));
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("rectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.rectangle(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.geom.Rectangle) adaptedArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPaintOuterBorderFirst(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setThickness(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("shadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowBlur(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("shadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shadowOpacity(toIntValue(adaptedArgs[0]));
            }
        }
        if ("shadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shadowSpread(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                return typedTarget.shadowSpread(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("shadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowX(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("shadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowY(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("stroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Stroke.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Stroke.class}, false);
                return typedTarget.stroke((com.codename1.ui.Stroke) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false);
                return typedTarget.stroke(((Number) adaptedArgs[0]).floatValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("strokeAngle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.strokeAngle(toIntValue(adaptedArgs[0]));
            }
        }
        if ("strokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.strokeColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("strokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.strokeOpacity(toIntValue(adaptedArgs[0]));
            }
        }
        if ("uiid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.uiid(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("unlock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ui.plaf.RoundRectBorder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("arrowSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.arrowSize(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("bezierCorners".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.bezierCorners(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("bottomLeftMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.bottomLeftMode(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("bottomOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.bottomOnlyMode(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("bottomRightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.bottomRightMode(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("cornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.cornerRadius(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getShadowBlur".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowBlur();
            }
        }
        if ("getShadowColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowColor();
            }
        }
        if ("getShadowOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowOpacity();
            }
        }
        if ("getShadowSpread".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowSpread();
            }
        }
        if ("getShadowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowX();
            }
        }
        if ("getShadowY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowY();
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeOpacity();
            }
        }
        if ("getStrokeThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeThickness();
            }
        }
        if ("getThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThickness();
            }
        }
        if ("getTrackComponentHorizontalPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackComponentHorizontalPosition();
            }
        }
        if ("getTrackComponentSide".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackComponentSide();
            }
        }
        if ("getTrackComponentVerticalPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackComponentVerticalPosition();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isBezierCorners".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBezierCorners();
            }
        }
        if ("isBottomLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBottomLeft();
            }
        }
        if ("isBottomOnlyMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBottomOnlyMode();
            }
        }
        if ("isBottomRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBottomRight();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRectangleType();
            }
        }
        if ("isStrokeMM".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStrokeMM();
            }
        }
        if ("isTopLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTopLeft();
            }
        }
        if ("isTopOnlyMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTopOnlyMode();
            }
        }
        if ("isTopRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTopRight();
            }
        }
        if ("isUseCache".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseCache();
            }
        }
        if ("lock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("setArrowSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setArrowSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.geom.Rectangle) adaptedArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPaintOuterBorderFirst(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setThickness(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("shadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowBlur(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("shadowColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shadowColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("shadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shadowOpacity(toIntValue(adaptedArgs[0]));
            }
        }
        if ("shadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowSpread(((Number) adaptedArgs[0]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.shadowSpread(toIntValue(adaptedArgs[0]));
            }
        }
        if ("shadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowX(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("shadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.shadowY(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("stroke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Stroke.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Stroke.class}, false);
                return typedTarget.stroke((com.codename1.ui.Stroke) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false);
                return typedTarget.stroke(((Number) adaptedArgs[0]).floatValue(), ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("strokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.strokeColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("strokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.strokeOpacity(toIntValue(adaptedArgs[0]));
            }
        }
        if ("topLeftMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.topLeftMode(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("topOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.topOnlyMode(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("topRightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.topRightMode(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("trackComponentHorizontalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.trackComponentHorizontalPosition(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("trackComponentSide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.trackComponentSide(toIntValue(adaptedArgs[0]));
            }
        }
        if ("trackComponentVerticalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.trackComponentVerticalPosition(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("unlock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlock(); return null;
            }
        }
        if ("useCache".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.useCache(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ui.plaf.StyleParser.MarginInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValues();
            }
        }
        if ("setValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false);
                typedTarget.setValues((com.codename1.ui.plaf.StyleParser.ScalarValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toString(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ui.plaf.StyleParser.PaddingInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValues();
            }
        }
        if ("setValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false);
                typedTarget.setValues((com.codename1.ui.plaf.StyleParser.ScalarValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toString(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ui.plaf.Border typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addOuterBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.addOuterBorder((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("clearImageBorderSpecialTile".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearImageBorderSpecialTile(); return null;
            }
        }
        if ("createPressedVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createPressedVersion();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCompoundBorders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompoundBorders();
            }
        }
        if ("getFocusedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFocusedInstance();
            }
        }
        if ("getMinimumHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumHeight();
            }
        }
        if ("getMinimumWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumWidth();
            }
        }
        if ("getPressedInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedInstance();
            }
        }
        if ("getProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThickness();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isBackgroundPainter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundPainter();
            }
        }
        if ("isEmptyBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmptyBorder();
            }
        }
        if ("isPaintOuterBorderFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPaintOuterBorderFirst();
            }
        }
        if ("isRectangleType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRectangleType();
            }
        }
        if ("lock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.lock(); return null;
            }
        }
        if ("mirrorBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mirrorBorder();
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("paintBorderBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.paintBorderBackground((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("setFocusedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setFocusedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setImageBorderSpecialTile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Component.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setImageBorderSpecialTile((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2], (com.codename1.ui.Image) adaptedArgs[3], (com.codename1.ui.geom.Rectangle) adaptedArgs[4]); return null;
            }
        }
        if ("setPaintOuterBorderFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPaintOuterBorderFirst(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPressedInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setPressedInstance((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
        }
        if ("setThickness".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setThickness(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTrackComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setTrackComponent((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("unlock".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.unlock(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ui.plaf.LookAndFeel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.bind((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("calculateLabelSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.Label.class}, false);
                return typedTarget.calculateLabelSpan((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1]);
            }
        }
        if ("calculateTextAreaSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false);
                return typedTarget.calculateTextAreaSpan((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]);
            }
        }
        if ("calculateTextFieldSpan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextSelection.class, com.codename1.ui.TextArea.class}, false);
                return typedTarget.calculateTextFieldSpan((com.codename1.ui.TextSelection) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]);
            }
        }
        if ("drawButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false);
                typedTarget.drawButton((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Button) adaptedArgs[1]); return null;
            }
        }
        if ("drawCheckBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false);
                typedTarget.drawCheckBox((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Button) adaptedArgs[1]); return null;
            }
        }
        if ("drawComboBox".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false);
                typedTarget.drawComboBox((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1]); return null;
            }
        }
        if ("drawHorizontalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.drawHorizontalScroll((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("drawLabel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Label.class}, false);
                typedTarget.drawLabel((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1]); return null;
            }
        }
        if ("drawList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.List.class}, false);
                typedTarget.drawList((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.List) adaptedArgs[1]); return null;
            }
        }
        if ("drawPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Boolean.class}, false);
                typedTarget.drawPullToRefresh((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("drawRadioButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Button.class}, false);
                typedTarget.drawRadioButton((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Button) adaptedArgs[1]); return null;
            }
        }
        if ("drawTextArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.drawTextArea((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("drawTextField".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.drawTextField((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("drawTextFieldCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.TextArea.class}, false);
                typedTarget.drawTextFieldCursor((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.TextArea) adaptedArgs[1]); return null;
            }
        }
        if ("drawVerticalScroll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.drawVerticalScroll((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
        }
        if ("getButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.getButtonPreferredSize((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("getCheckBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.getCheckBoxPreferredSize((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("getComboBoxPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false);
                return typedTarget.getComboBoxPreferredSize((com.codename1.ui.List) adaptedArgs[0]);
            }
        }
        if ("getDefaultDialogTransitionIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultDialogTransitionIn();
            }
        }
        if ("getDefaultDialogTransitionOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultDialogTransitionOut();
            }
        }
        if ("getDefaultFormTintColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFormTintColor();
            }
        }
        if ("getDefaultFormTransitionIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFormTransitionIn();
            }
        }
        if ("getDefaultFormTransitionOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultFormTransitionOut();
            }
        }
        if ("getDefaultMenuTransitionIn".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMenuTransitionIn();
            }
        }
        if ("getDefaultMenuTransitionOut".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultMenuTransitionOut();
            }
        }
        if ("getDefaultSmoothScrollingSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDefaultSmoothScrollingSpeed();
            }
        }
        if ("getDisableColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisableColor();
            }
        }
        if ("getFadeScrollBarSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFadeScrollBarSpeed();
            }
        }
        if ("getFadeScrollEdgeLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFadeScrollEdgeLength();
            }
        }
        if ("getHorizontalScrollHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalScrollHeight();
            }
        }
        if ("getLabelPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                return typedTarget.getLabelPreferredSize((com.codename1.ui.Label) adaptedArgs[0]);
            }
        }
        if ("getListPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.List.class}, false);
                return typedTarget.getListPreferredSize((com.codename1.ui.List) adaptedArgs[0]);
            }
        }
        if ("getMenuBarClass".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMenuBarClass();
            }
        }
        if ("getMenuIcons".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMenuIcons();
            }
        }
        if ("getMenuRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMenuRenderer();
            }
        }
        if ("getPullToRefreshHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPullToRefreshHeight();
            }
        }
        if ("getRadioButtonPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Button.class}, false);
                return typedTarget.getRadioButtonPreferredSize((com.codename1.ui.Button) adaptedArgs[0]);
            }
        }
        if ("getTactileTouchDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTactileTouchDuration();
            }
        }
        if ("getTextAreaSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class, java.lang.Boolean.class}, false);
                return typedTarget.getTextAreaSize((com.codename1.ui.TextArea) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("getTextFieldCursorColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextFieldCursorColor();
            }
        }
        if ("getTextFieldPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.TextArea.class}, false);
                return typedTarget.getTextFieldPreferredSize((com.codename1.ui.TextArea) adaptedArgs[0]);
            }
        }
        if ("getTickerSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTickerSpeed();
            }
        }
        if ("getVerticalScrollWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalScrollWidth();
            }
        }
        if ("isBackgroundImageDetermineSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundImageDetermineSize();
            }
        }
        if ("isDefaultAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultAlwaysTensile();
            }
        }
        if ("isDefaultEndsWith3Points".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultEndsWith3Points();
            }
        }
        if ("isDefaultSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultSmoothScrolling();
            }
        }
        if ("isDefaultSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultSnapToGrid();
            }
        }
        if ("isDefaultTensileDrag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultTensileDrag();
            }
        }
        if ("isDefaultTensileHighlight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDefaultTensileHighlight();
            }
        }
        if ("isFadeScrollBar".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFadeScrollBar();
            }
        }
        if ("isFadeScrollEdge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFadeScrollEdge();
            }
        }
        if ("isFocusScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusScrolling();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isReverseSoftButtons".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReverseSoftButtons();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isTouchMenus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTouchMenus();
            }
        }
        if ("paintTensileHighlight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Graphics.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                typedTarget.paintTensileHighlight((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Graphics) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBackgroundImageDetermineSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBackgroundImageDetermineSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultDialogTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultDialogTransitionIn((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultDialogTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultDialogTransitionOut((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultEndsWith3Points".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultEndsWith3Points(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultFormTintColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDefaultFormTintColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDefaultFormTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultFormTransitionIn((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultFormTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultFormTransitionOut((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionIn".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultMenuTransitionIn((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultMenuTransitionOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.animations.Transition.class}, false);
                typedTarget.setDefaultMenuTransitionOut((com.codename1.ui.animations.Transition) adaptedArgs[0]); return null;
            }
        }
        if ("setDefaultSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultSmoothScrollingSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDefaultSmoothScrollingSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDefaultSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDefaultTensileDrag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDefaultTensileDrag(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDisableColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setDisableColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setFG".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, com.codename1.ui.Component.class}, false);
                typedTarget.setFG((com.codename1.ui.Graphics) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
        }
        if ("setFadeScrollBar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFadeScrollBar(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollBarSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFadeScrollBarSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setFadeScrollEdge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFadeScrollEdge(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFadeScrollEdgeLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFadeScrollEdgeLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setFocusScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMenuBarClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                typedTarget.setMenuBarClass((java.lang.Class) adaptedArgs[0]); return null;
            }
        }
        if ("setMenuIcons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, com.codename1.ui.Image.class, com.codename1.ui.Image.class}, false);
                typedTarget.setMenuIcons((com.codename1.ui.Image) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], (com.codename1.ui.Image) adaptedArgs[2]); return null;
            }
        }
        if ("setMenuRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.list.ListCellRenderer.class}, false);
                typedTarget.setMenuRenderer((com.codename1.ui.list.ListCellRenderer) adaptedArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setReverseSoftButtons".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setReverseSoftButtons(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTactileTouchDuration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTactileTouchDuration(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTextFieldCursorColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTextFieldCursorColor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTickerSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setTickerSpeed(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setTouchMenus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTouchMenus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("uninstall".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.uninstall(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ui.plaf.Style typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addStyleListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.StyleListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.StyleListener.class}, false);
                typedTarget.addStyleListener((com.codename1.ui.events.StyleListener) adaptedArgs[0]); return null;
            }
        }
        if ("cacheMargins".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.cacheMargins(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("flushMarginsCache".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushMarginsCache(); return null;
            }
        }
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getBackgroundGradientEndColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundGradientEndColor();
            }
        }
        if ("getBackgroundGradientRelativeSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundGradientRelativeSize();
            }
        }
        if ("getBackgroundGradientRelativeX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundGradientRelativeX();
            }
        }
        if ("getBackgroundGradientRelativeY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundGradientRelativeY();
            }
        }
        if ("getBackgroundGradientStartColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundGradientStartColor();
            }
        }
        if ("getBackgroundType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackgroundType();
            }
        }
        if ("getBgColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgColor();
            }
        }
        if ("getBgImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgImage();
            }
        }
        if ("getBgPainter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgPainter();
            }
        }
        if ("getBgTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgTransparency();
            }
        }
        if ("getBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBorder();
            }
        }
        if ("getElevation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getElevation();
            }
        }
        if ("getFgAlpha".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFgAlpha();
            }
        }
        if ("getFgColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFgColor();
            }
        }
        if ("getFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFont();
            }
        }
        if ("getHorizontalMargins".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalMargins();
            }
        }
        if ("getHorizontalPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHorizontalPadding();
            }
        }
        if ("getIconGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconGap();
            }
        }
        if ("getIconGapUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconGapUnit();
            }
        }
        if ("getMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getMargin(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.getMargin(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMarginBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMarginBottom();
            }
        }
        if ("getMarginFloatValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.getMarginFloatValue(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMarginLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getMarginLeft(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getMarginLeftNoRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMarginLeftNoRTL();
            }
        }
        if ("getMarginRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getMarginRight(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getMarginRightNoRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMarginRightNoRTL();
            }
        }
        if ("getMarginTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMarginTop();
            }
        }
        if ("getMarginUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMarginUnit();
            }
        }
        if ("getMarginValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.getMarginValue(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpacity();
            }
        }
        if ("getPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getPadding(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.getPadding(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getPaddingBottom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingBottom();
            }
        }
        if ("getPaddingFloatValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.getPaddingFloatValue(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getPaddingLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getPaddingLeft(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getPaddingLeftNoRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingLeftNoRTL();
            }
        }
        if ("getPaddingRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getPaddingRight(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getPaddingRightNoRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingRightNoRTL();
            }
        }
        if ("getPaddingTop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingTop();
            }
        }
        if ("getPaddingUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPaddingUnit();
            }
        }
        if ("getPaddingValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Integer.class}, false);
                return typedTarget.getPaddingValue(((Boolean) adaptedArgs[0]).booleanValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getTextDecoration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextDecoration();
            }
        }
        if ("getVerticalMargins".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalMargins();
            }
        }
        if ("getVerticalPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVerticalPadding();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("is3DTextNorth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.is3DTextNorth();
            }
        }
        if ("isLowered3DText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLowered3DText();
            }
        }
        if ("isModified".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isModified();
            }
        }
        if ("isOverline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOverline();
            }
        }
        if ("isRaised3DText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRaised3DText();
            }
        }
        if ("isRendererStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRendererStyle();
            }
        }
        if ("isStrikeThru".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isStrikeThru();
            }
        }
        if ("isSuppressChangeEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSuppressChangeEvents();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isUnderline".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUnderline();
            }
        }
        if ("markAsRendererStyle".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.markAsRendererStyle(); return null;
            }
        }
        if ("merge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.merge((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("removeListeners".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeListeners(); return null;
            }
        }
        if ("removeStyleListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.StyleListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.StyleListener.class}, false);
                typedTarget.removeStyleListener((com.codename1.ui.events.StyleListener) adaptedArgs[0]); return null;
            }
        }
        if ("restoreCachedMargins".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.restoreCachedMargins(); return null;
            }
        }
        if ("set3DText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.set3DText(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("set3DTextNorth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.set3DTextNorth(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setAlignment(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setAlignment(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientEndColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBackgroundGradientEndColor(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setBackgroundGradientEndColor(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setBackgroundGradientRelativeSize(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false);
                typedTarget.setBackgroundGradientRelativeSize(((Number) adaptedArgs[0]).floatValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setBackgroundGradientRelativeX(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false);
                typedTarget.setBackgroundGradientRelativeX(((Number) adaptedArgs[0]).floatValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientRelativeY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setBackgroundGradientRelativeY(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false);
                typedTarget.setBackgroundGradientRelativeY(((Number) adaptedArgs[0]).floatValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundGradientStartColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBackgroundGradientStartColor(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setBackgroundGradientStartColor(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBackgroundType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setBackgroundType((byte) toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Boolean.class}, false);
                typedTarget.setBackgroundType((byte) toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBgColor(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setBgColor(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBgImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setBgImage((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Boolean.class}, false);
                typedTarget.setBgImage((com.codename1.ui.Image) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBgPainter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Painter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Painter.class}, false);
                typedTarget.setBgPainter((com.codename1.ui.Painter) adaptedArgs[0]); return null;
            }
        }
        if ("setBgTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setBgTransparency((byte) toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBgTransparency(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setBgTransparency(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class}, false);
                typedTarget.setBorder((com.codename1.ui.plaf.Border) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Border.class, java.lang.Boolean.class}, false);
                typedTarget.setBorder((com.codename1.ui.plaf.Border) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setElevation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setElevation(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setElevation(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setFgAlpha".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFgAlpha(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setFgAlpha(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setFgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setFgColor(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setFgColor(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class}, false);
                typedTarget.setFont((com.codename1.ui.Font) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Font.class, java.lang.Boolean.class}, false);
                typedTarget.setFont((com.codename1.ui.Font) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setIconGap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setIconGap(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Boolean.class}, false);
                typedTarget.setIconGap(((Number) adaptedArgs[0]).floatValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Byte.class}, false);
                typedTarget.setIconGap(((Number) adaptedArgs[0]).floatValue(), (byte) toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Byte.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Byte.class, java.lang.Boolean.class}, false);
                typedTarget.setIconGap(((Number) adaptedArgs[0]).floatValue(), (byte) toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("setIconGapUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setIconGapUnit((byte) toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class, java.lang.Boolean.class}, false);
                typedTarget.setIconGapUnit((byte) toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setMargin(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setMargin(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false);
                typedTarget.setMargin(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue(), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setMargin(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setMargin(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setMargin(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("setMarginBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMarginBottom(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMarginBottom(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMarginLeft(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMarginLeft(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMarginRight(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMarginRight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setMarginTop(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMarginTop(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, true);
                byte[] varArgs = new byte[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (byte) toIntValue(adaptedArgs[i]);
                }
                typedTarget.setMarginUnit(varArgs); return null;
            }
        }
        if ("setMarginUnitBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setMarginUnitBottom((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginUnitLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setMarginUnitLeft((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginUnitRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setMarginUnitRight((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setMarginUnitTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setMarginUnitTop((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setOpacity(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setOpacity(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setOverline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOverline(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setPadding(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false);
                typedTarget.setPadding(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue(), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setPadding(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setPadding(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("setPaddingBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPaddingBottom(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPaddingBottom(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPaddingLeft(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPaddingLeft(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPaddingRight(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPaddingRight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setPaddingTop(((Number) adaptedArgs[0]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPaddingTop(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, true);
                byte[] varArgs = new byte[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (byte) toIntValue(adaptedArgs[i]);
                }
                typedTarget.setPaddingUnit(varArgs); return null;
            }
        }
        if ("setPaddingUnitBottom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPaddingUnitBottom((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingUnitLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPaddingUnitLeft((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingUnitRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPaddingUnitRight((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPaddingUnitTop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setPaddingUnitTop((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setStrikeThru".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setStrikeThru(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSuppressChangeEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSuppressChangeEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSurface".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSurface(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setSurface(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setTextDecoration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTextDecoration(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.setTextDecoration(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setUnderline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUnderline(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stripMarginAndPadding(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ui.plaf.StyleParser.BorderInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("colorString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.colorString();
            }
        }
        if ("createBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                return typedTarget.createBorder((com.codename1.ui.util.Resources) adaptedArgs[0]);
            }
        }
        if ("getBottomLeftMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomLeftMode();
            }
        }
        if ("getBottomOnlyMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomOnlyMode();
            }
        }
        if ("getBottomRightMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomRightMode();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getCornerRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCornerRadius();
            }
        }
        if ("getImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImages();
            }
        }
        if ("getOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpacity();
            }
        }
        if ("getRectangle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRectangle();
            }
        }
        if ("getShadowBlur".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowBlur();
            }
        }
        if ("getShadowOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowOpacity();
            }
        }
        if ("getShadowSpread".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowSpread();
            }
        }
        if ("getShadowX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowX();
            }
        }
        if ("getShadowY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShadowY();
            }
        }
        if ("getSpliceImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpliceImage();
            }
        }
        if ("getSpliceInsets".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSpliceInsets();
            }
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return typedTarget.getSpliceInsets((double[]) adaptedArgs[0]);
            }
        }
        if ("getStrokeColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeColor();
            }
        }
        if ("getStrokeOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStrokeOpacity();
            }
        }
        if ("getThickness".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThickness();
            }
        }
        if ("getTopLeftMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTopLeftMode();
            }
        }
        if ("getTopOnlyMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTopOnlyMode();
            }
        }
        if ("getTopRightMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTopRightMode();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("getWidthInPixels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthInPixels();
            }
        }
        if ("getWidthUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidthUnit();
            }
        }
        if ("setBottomLeftMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBottomLeftMode(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setBottomOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBottomOnlyMode(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setBottomRightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBottomRightMode(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setColor(Integer.valueOf(toIntValue(adaptedArgs[0]))); return null;
            }
        }
        if ("setCornerRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue.class}, false);
                typedTarget.setCornerRadius((com.codename1.ui.plaf.StyleParser.ScalarValue) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setCornerRadius(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCornerRadius((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setImages((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setOpacity(Integer.valueOf(toIntValue(adaptedArgs[0]))); return null;
            }
        }
        if ("setRectangle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRectangle(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setShadowBlur".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setShadowBlur(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setShadowOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setShadowOpacity(Integer.valueOf(toIntValue(adaptedArgs[0]))); return null;
            }
        }
        if ("setShadowSpread".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue.class}, false);
                typedTarget.setShadowSpread((com.codename1.ui.plaf.StyleParser.ScalarValue) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setShadowSpread((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setShadowX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setShadowX(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setShadowY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setShadowY(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setSpliceImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSpliceImage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSpliceInsets".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSpliceInsets((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                typedTarget.setSpliceInsets((double[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                typedTarget.setSpliceInsets((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setStrokeColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStrokeColor(Integer.valueOf(toIntValue(adaptedArgs[0]))); return null;
            }
        }
        if ("setStrokeOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStrokeOpacity(Integer.valueOf(toIntValue(adaptedArgs[0]))); return null;
            }
        }
        if ("setTopLeftMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTopLeftMode(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setTopOnlyMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTopOnlyMode(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setTopRightMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTopRightMode(Boolean.valueOf(((Boolean) adaptedArgs[0]).booleanValue())); return null;
            }
        }
        if ("setType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setType((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setWidth(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setWidthUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setWidthUnit((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("widthString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.widthString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ui.plaf.StyleParser.BoxInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getValue(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValues();
            }
        }
        if ("setValues".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.StyleParser.ScalarValue[].class}, false);
                typedTarget.setValues((com.codename1.ui.plaf.StyleParser.ScalarValue[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toString(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ui.plaf.StyleParser.FontInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                return typedTarget.createFont((com.codename1.ui.plaf.Style) adaptedArgs[0]);
            }
        }
        if ("getFile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFile();
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
        if ("getSizeInPixels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                return typedTarget.getSizeInPixels((com.codename1.ui.plaf.Style) adaptedArgs[0]);
            }
        }
        if ("getSizeUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSizeUnit();
            }
        }
        if ("setFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setFile((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setSize(Float.valueOf(((Number) adaptedArgs[0]).floatValue())); return null;
            }
        }
        if ("setSizeUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setSizeUnit((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("sizeString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.sizeString((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.ui.plaf.StyleParser.ImageInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                return typedTarget.getImage((com.codename1.ui.util.Resources) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.ui.plaf.StyleParser.ScalarValue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPixelValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPixelValue();
            }
        }
        if ("getUnit".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnit();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("setUnit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setUnit((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setValue(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toString(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.ui.plaf.StyleParser.StyleInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlignment".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignment();
            }
        }
        if ("getAlignmentAsString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlignmentAsString();
            }
        }
        if ("getBgColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgColor();
            }
        }
        if ("getBgImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgImage();
            }
        }
        if ("getBgType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgType();
            }
        }
        if ("getBgTypeAsString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBgTypeAsString();
            }
        }
        if ("getBorder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBorder();
            }
        }
        if ("getFgColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFgColor();
            }
        }
        if ("getFont".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFont();
            }
        }
        if ("getMargin".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMargin();
            }
        }
        if ("getOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOpacity();
            }
        }
        if ("getPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPadding();
            }
        }
        if ("getTextDecoration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextDecoration();
            }
        }
        if ("getTextDecorationAsString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextDecorationAsString();
            }
        }
        if ("getTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTransparency();
            }
        }
        if ("setAlignment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setAlignment(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAlignment((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setBgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setBgColor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setBgImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setBgImage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setBgType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setBgType(Integer.valueOf(toIntValue(adaptedArgs[0])));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setBgType((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setBorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setBorder((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setFgColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setFgColor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setFont".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setFont((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setFontName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setFontName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setFontSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setFontSize((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setMargin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setMargin((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setOpacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setOpacity((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPadding".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setPadding((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTransparency((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toStyleString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toStyleString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.ui.plaf.UIManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addThemeProps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.addThemeProps((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("addThemeRefreshListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addThemeRefreshListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("getBundle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBundle();
            }
        }
        if ("getComponentCustomStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getComponentCustomStyle((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getComponentSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getComponentSelectedStyle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getComponentStyle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getIconUIIDFor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getIconUIIDFor((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getLookAndFeel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLookAndFeel();
            }
        }
        if ("getResourceBundle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResourceBundle();
            }
        }
        if ("getThemeConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.getThemeConstant((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getThemeConstant((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getThemeImageConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getThemeImageConstant((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getThemeMaskConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getThemeMaskConstant((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getThemeName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getThemeName();
            }
        }
        if ("isThemeConstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isThemeConstant((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.isThemeConstant((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("isUseLargerTextScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUseLargerTextScale();
            }
        }
        if ("localize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.localize((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("parseComponentCustomStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 4];
                for (int i = 4; i < adaptedArgs.length; i++) {
                    varArgs[i - 4] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.parseComponentCustomStyle((com.codename1.ui.util.Resources) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], varArgs);
            }
        }
        if ("parseComponentSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 3];
                for (int i = 3; i < adaptedArgs.length; i++) {
                    varArgs[i - 3] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.parseComponentSelectedStyle((com.codename1.ui.util.Resources) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], varArgs);
            }
        }
        if ("parseComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class, java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 3];
                for (int i = 3; i < adaptedArgs.length; i++) {
                    varArgs[i - 3] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.parseComponentStyle((com.codename1.ui.util.Resources) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], varArgs);
            }
        }
        if ("removeThemeRefreshListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeThemeRefreshListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.setBundle((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setComponentSelectedStyle((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("setComponentStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setComponentStyle((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class, java.lang.String.class}, false);
                typedTarget.setComponentStyle((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("setLookAndFeel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.LookAndFeel.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.LookAndFeel.class}, false);
                typedTarget.setLookAndFeel((com.codename1.ui.plaf.LookAndFeel) adaptedArgs[0]); return null;
            }
        }
        if ("setResourceBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.setResourceBundle((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("setThemeProps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.setThemeProps((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("setUseLargerTextScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setUseLargerTextScale(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("wasThemeInstalled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.wasThemeInstalled();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ui.plaf.CSSBorder.class) return getStaticField0(name);
        if (type == com.codename1.ui.plaf.Style.class) return getStaticField1(name);
        if (type == com.codename1.ui.plaf.StyleParser.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
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
        throw unsupportedStaticField(com.codename1.ui.plaf.CSSBorder.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
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
        throw unsupportedStaticField(com.codename1.ui.plaf.Style.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("UNIT_INHERIT".equals(name)) return com.codename1.ui.plaf.StyleParser.UNIT_INHERIT;
        throw unsupportedStaticField(com.codename1.ui.plaf.StyleParser.class, name);
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
