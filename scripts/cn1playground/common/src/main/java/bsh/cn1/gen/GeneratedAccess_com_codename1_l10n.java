package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_l10n {
    private GeneratedAccess_com_codename1_l10n() {
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
        if ("DateFormat".equals(simpleName)) {
            return com.codename1.l10n.DateFormat.class;
        }
        if ("DateFormatPatterns".equals(simpleName)) {
            return com.codename1.l10n.DateFormatPatterns.class;
        }
        if ("DateFormatSymbols".equals(simpleName)) {
            return com.codename1.l10n.DateFormatSymbols.class;
        }
        if ("Format".equals(simpleName)) {
            return com.codename1.l10n.Format.class;
        }
        if ("L10NManager".equals(simpleName)) {
            return com.codename1.l10n.L10NManager.class;
        }
        if ("ParseException".equals(simpleName)) {
            return com.codename1.l10n.ParseException.class;
        }
        if ("SimpleDateFormat".equals(simpleName)) {
            return com.codename1.l10n.SimpleDateFormat.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.l10n.ParseException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.l10n.ParseException((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.l10n.SimpleDateFormat.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.l10n.SimpleDateFormat();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.l10n.SimpleDateFormat((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.l10n.DateFormat.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.l10n.L10NManager.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.l10n.SimpleDateFormat.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getDateInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.l10n.DateFormat.getDateInstance();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.l10n.DateFormat.getDateInstance(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getDateTimeInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return com.codename1.l10n.DateFormat.getDateTimeInstance(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.l10n.DateFormat.getInstance();
            }
        }
        if ("getTimeInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.l10n.DateFormat.getTimeInstance();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.l10n.DateFormat.getTimeInstance(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(com.codename1.l10n.DateFormat.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.l10n.L10NManager.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.l10n.L10NManager.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("isRestrictMonthNameLength".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.l10n.SimpleDateFormat.isRestrictMonthNameLength();
            }
        }
        if ("setRestrictMonthNameLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.l10n.SimpleDateFormat.setRestrictMonthNameLength(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.l10n.SimpleDateFormat.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.l10n.SimpleDateFormat) {
            try {
                return invoke0((com.codename1.l10n.SimpleDateFormat) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.l10n.DateFormat) {
            try {
                return invoke1((com.codename1.l10n.DateFormat) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.l10n.DateFormatSymbols) {
            try {
                return invoke2((com.codename1.l10n.DateFormatSymbols) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.l10n.Format) {
            try {
                return invoke3((com.codename1.l10n.Format) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.l10n.L10NManager) {
            try {
                return invoke4((com.codename1.l10n.L10NManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.l10n.ParseException) {
            try {
                return invoke5((com.codename1.l10n.ParseException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.l10n.SimpleDateFormat typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("applyPattern".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.applyPattern((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.format((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("getDateFormatSymbols".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDateFormatSymbols();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parse((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseObject((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDateFormatSymbols".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.l10n.DateFormatSymbols.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.l10n.DateFormatSymbols.class}, false);
                typedTarget.setDateFormatSymbols((com.codename1.l10n.DateFormatSymbols) adaptedArgs[0]); return null;
            }
        }
        if ("toPattern".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toPattern();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.l10n.DateFormat typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.format((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parse((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseObject((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.l10n.DateFormatSymbols typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addZoneMapping".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.addZoneMapping((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4]); return null;
            }
        }
        if ("getAmPmStrings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAmPmStrings();
            }
        }
        if ("getEras".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEras();
            }
        }
        if ("getMonths".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMonths();
            }
        }
        if ("getResourceBundle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResourceBundle();
            }
        }
        if ("getShortMonths".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShortMonths();
            }
        }
        if ("getShortWeekdays".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getShortWeekdays();
            }
        }
        if ("getWeekdays".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWeekdays();
            }
        }
        if ("getZoneLongName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getZoneLongName((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getZoneLongNameDST".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getZoneLongNameDST((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getZoneShortName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getZoneShortName((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getZoneShortNameDST".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getZoneShortNameDST((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getZoneStrings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZoneStrings();
            }
        }
        if ("isLocalized".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLocalized();
            }
        }
        if ("setAmPmStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setAmPmStrings((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setEras".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setEras((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setLocalized".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setLocalized(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setMonths((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setResourceBundle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.setResourceBundle((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("setShortMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setShortMonths((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setShortWeekdays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setShortWeekdays((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setWeekdays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setWeekdays((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setZoneStrings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[][].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[][].class}, false);
                typedTarget.setZoneStrings((java.lang.String[][]) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.l10n.Format typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.format((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("parseObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseObject((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.l10n.L10NManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.format(((Number) adaptedArgs[0]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.format(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Integer.class}, false);
                return typedTarget.format(((Number) adaptedArgs[0]).doubleValue(), toIntValue(adaptedArgs[1]));
            }
        }
        if ("formatCurrency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                return typedTarget.formatCurrency(((Number) adaptedArgs[0]).doubleValue());
            }
        }
        if ("formatDateLongStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.formatDateLongStyle((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("formatDateShortStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.formatDateShortStyle((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("formatDateTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.formatDateTime((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("formatDateTimeMedium".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.formatDateTimeMedium((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("formatDateTimeShort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.formatDateTimeShort((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("formatTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.formatTime((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("getCurrencySymbol".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrencySymbol();
            }
        }
        if ("getLanguage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguage();
            }
        }
        if ("getLocale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocale();
            }
        }
        if ("getLongMonthName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.getLongMonthName((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("getShortMonthName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.getShortMonthName((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("isRTLLocale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTLLocale();
            }
        }
        if ("parseCurrency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseCurrency((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseDouble((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseInt((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("parseLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.parseLong((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setLocale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setLocale((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.l10n.ParseException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getErrorOffset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getErrorOffset();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.l10n.DateFormat.class) return getStaticField0(name);
        if (type == com.codename1.l10n.DateFormatPatterns.class) return getStaticField1(name);
        if (type == com.codename1.l10n.DateFormatSymbols.class) return getStaticField2(name);
        if (type == com.codename1.l10n.SimpleDateFormat.class) return getStaticField3(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DEFAULT".equals(name)) return com.codename1.l10n.DateFormat.DEFAULT;
        if ("FULL".equals(name)) return com.codename1.l10n.DateFormat.FULL;
        if ("LONG".equals(name)) return com.codename1.l10n.DateFormat.LONG;
        if ("MEDIUM".equals(name)) return com.codename1.l10n.DateFormat.MEDIUM;
        if ("SHORT".equals(name)) return com.codename1.l10n.DateFormat.SHORT;
        throw unsupportedStaticField(com.codename1.l10n.DateFormat.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ISO8601".equals(name)) return com.codename1.l10n.DateFormatPatterns.ISO8601;
        if ("RFC2822".equals(name)) return com.codename1.l10n.DateFormatPatterns.RFC2822;
        if ("RFC822".equals(name)) return com.codename1.l10n.DateFormatPatterns.RFC822;
        if ("TWITTER_SEARCH".equals(name)) return com.codename1.l10n.DateFormatPatterns.TWITTER_SEARCH;
        if ("TWITTER_TIMELINE".equals(name)) return com.codename1.l10n.DateFormatPatterns.TWITTER_TIMELINE;
        if ("VERBOSE_DATE".equals(name)) return com.codename1.l10n.DateFormatPatterns.VERBOSE_DATE;
        if ("VERBOSE_TIME".equals(name)) return com.codename1.l10n.DateFormatPatterns.VERBOSE_TIME;
        if ("VERBOSE_TIMESTAMP".equals(name)) return com.codename1.l10n.DateFormatPatterns.VERBOSE_TIMESTAMP;
        throw unsupportedStaticField(com.codename1.l10n.DateFormatPatterns.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("ZONE_ID".equals(name)) return com.codename1.l10n.DateFormatSymbols.ZONE_ID;
        if ("ZONE_LONGNAME".equals(name)) return com.codename1.l10n.DateFormatSymbols.ZONE_LONGNAME;
        if ("ZONE_LONGNAME_DST".equals(name)) return com.codename1.l10n.DateFormatSymbols.ZONE_LONGNAME_DST;
        if ("ZONE_SHORTNAME".equals(name)) return com.codename1.l10n.DateFormatSymbols.ZONE_SHORTNAME;
        if ("ZONE_SHORTNAME_DST".equals(name)) return com.codename1.l10n.DateFormatSymbols.ZONE_SHORTNAME_DST;
        throw unsupportedStaticField(com.codename1.l10n.DateFormatSymbols.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("DEFAULT".equals(name)) return com.codename1.l10n.SimpleDateFormat.DEFAULT;
        if ("FULL".equals(name)) return com.codename1.l10n.SimpleDateFormat.FULL;
        if ("LONG".equals(name)) return com.codename1.l10n.SimpleDateFormat.LONG;
        if ("MEDIUM".equals(name)) return com.codename1.l10n.SimpleDateFormat.MEDIUM;
        if ("SHORT".equals(name)) return com.codename1.l10n.SimpleDateFormat.SHORT;
        throw unsupportedStaticField(com.codename1.l10n.SimpleDateFormat.class, name);
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
