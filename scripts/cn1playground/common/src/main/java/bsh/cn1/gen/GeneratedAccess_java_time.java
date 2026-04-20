package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_time {
    private GeneratedAccess_java_time() {
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
        if ("Clock".equals(simpleName)) {
            return java.time.Clock.class;
        }
        if ("Duration".equals(simpleName)) {
            return java.time.Duration.class;
        }
        if ("Instant".equals(simpleName)) {
            return java.time.Instant.class;
        }
        if ("LocalDate".equals(simpleName)) {
            return java.time.LocalDate.class;
        }
        if ("LocalDateTime".equals(simpleName)) {
            return java.time.LocalDateTime.class;
        }
        if ("LocalTime".equals(simpleName)) {
            return java.time.LocalTime.class;
        }
        if ("OffsetDateTime".equals(simpleName)) {
            return java.time.OffsetDateTime.class;
        }
        if ("Period".equals(simpleName)) {
            return java.time.Period.class;
        }
        if ("ZoneId".equals(simpleName)) {
            return java.time.ZoneId.class;
        }
        if ("ZoneOffset".equals(simpleName)) {
            return java.time.ZoneOffset.class;
        }
        if ("ZonedDateTime".equals(simpleName)) {
            return java.time.ZonedDateTime.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.time.Clock.class) return invokeStatic0(name, safeArgs);
        if (type == java.time.Duration.class) return invokeStatic1(name, safeArgs);
        if (type == java.time.Instant.class) return invokeStatic2(name, safeArgs);
        if (type == java.time.LocalDate.class) return invokeStatic3(name, safeArgs);
        if (type == java.time.LocalDateTime.class) return invokeStatic4(name, safeArgs);
        if (type == java.time.LocalTime.class) return invokeStatic5(name, safeArgs);
        if (type == java.time.OffsetDateTime.class) return invokeStatic6(name, safeArgs);
        if (type == java.time.Period.class) return invokeStatic7(name, safeArgs);
        if (type == java.time.ZoneId.class) return invokeStatic8(name, safeArgs);
        if (type == java.time.ZoneOffset.class) return invokeStatic9(name, safeArgs);
        if (type == java.time.ZonedDateTime.class) return invokeStatic10(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("fixed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false);
                return java.time.Clock.fixed((java.time.Instant) adaptedArgs[0], (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("systemDefaultZone".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.Clock.systemDefaultZone();
            }
        }
        if ("systemUTC".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.Clock.systemUTC();
            }
        }
        throw unsupportedStatic(java.time.Clock.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("ofDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Duration.ofDays(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ofHours".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Duration.ofHours(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ofMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Duration.ofMillis(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ofMinutes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Duration.ofMinutes(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ofSeconds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Duration.ofSeconds(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return java.time.Duration.ofSeconds(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        throw unsupportedStatic(java.time.Duration.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("now".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.Instant.now();
            }
        }
        if ("ofEpochMilli".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Instant.ofEpochMilli(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ofEpochSecond".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.Instant.ofEpochSecond(((Number) adaptedArgs[0]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class}, false);
                return java.time.Instant.ofEpochSecond(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return java.time.Instant.parse((java.lang.CharSequence) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.time.Instant.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("now".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.LocalDate.now();
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Clock.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Clock.class}, false);
                return java.time.LocalDate.now((java.time.Clock) adaptedArgs[0]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalDate.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("ofEpochDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.LocalDate.ofEpochDay(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return java.time.LocalDate.parse((java.lang.CharSequence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false);
                return java.time.LocalDate.parse((java.lang.CharSequence) adaptedArgs[0], (java.time.format.DateTimeFormatter) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.time.LocalDate.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("now".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.LocalDateTime.now();
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Clock.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Clock.class}, false);
                return java.time.LocalDateTime.now((java.time.Clock) adaptedArgs[0]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.LocalDate.class, java.time.LocalTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalDate.class, java.time.LocalTime.class}, false);
                return java.time.LocalDateTime.of((java.time.LocalDate) adaptedArgs[0], (java.time.LocalTime) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalDateTime.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalDateTime.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalDateTime.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]));
            }
        }
        if ("ofInstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false);
                return java.time.LocalDateTime.ofInstant((java.time.Instant) adaptedArgs[0], (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return java.time.LocalDateTime.parse((java.lang.CharSequence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false);
                return java.time.LocalDateTime.parse((java.lang.CharSequence) adaptedArgs[0], (java.time.format.DateTimeFormatter) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.time.LocalDateTime.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("now".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.LocalTime.now();
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Clock.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Clock.class}, false);
                return java.time.LocalTime.now((java.time.Clock) adaptedArgs[0]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalTime.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalTime.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.LocalTime.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("ofNanoOfDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.LocalTime.ofNanoOfDay(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("ofSecondOfDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return java.time.LocalTime.ofSecondOfDay(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return java.time.LocalTime.parse((java.lang.CharSequence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false);
                return java.time.LocalTime.parse((java.lang.CharSequence) adaptedArgs[0], (java.time.format.DateTimeFormatter) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.time.LocalTime.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.LocalDateTime.class, java.time.ZoneOffset.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalDateTime.class, java.time.ZoneOffset.class}, false);
                return java.time.OffsetDateTime.of((java.time.LocalDateTime) adaptedArgs[0], (java.time.ZoneOffset) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.time.LocalDate.class, java.time.LocalTime.class, java.time.ZoneOffset.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalDate.class, java.time.LocalTime.class, java.time.ZoneOffset.class}, false);
                return java.time.OffsetDateTime.of((java.time.LocalDate) adaptedArgs[0], (java.time.LocalTime) adaptedArgs[1], (java.time.ZoneOffset) adaptedArgs[2]);
            }
        }
        if ("ofInstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false);
                return java.time.OffsetDateTime.ofInstant((java.time.Instant) adaptedArgs[0], (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return java.time.OffsetDateTime.parse((java.lang.CharSequence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false);
                return java.time.OffsetDateTime.parse((java.lang.CharSequence) adaptedArgs[0], (java.time.format.DateTimeFormatter) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.time.OffsetDateTime.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.Period.of(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        if ("ofDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.time.Period.ofDays(toIntValue(adaptedArgs[0]));
            }
        }
        if ("ofMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.time.Period.ofMonths(toIntValue(adaptedArgs[0]));
            }
        }
        if ("ofYears".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.time.Period.ofYears(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(java.time.Period.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.time.ZoneId.of((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("systemDefault".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.ZoneId.systemDefault();
            }
        }
        throw unsupportedStatic(java.time.ZoneId.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.time.ZoneOffset.of((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("ofHours".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.time.ZoneOffset.ofHours(toIntValue(adaptedArgs[0]));
            }
        }
        if ("ofHoursMinutes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.time.ZoneOffset.ofHoursMinutes(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("ofTotalSeconds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return java.time.ZoneOffset.ofTotalSeconds(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedStatic(java.time.ZoneOffset.class, name, safeArgs);
    }

    private static Object invokeStatic10(String name, Object[] safeArgs) throws Exception {
        if ("now".equals(name)) {
            if (safeArgs.length == 0) {
                return java.time.ZonedDateTime.now();
            }
            if (matches(safeArgs, new Class<?>[]{java.time.Clock.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Clock.class}, false);
                return java.time.ZonedDateTime.now((java.time.Clock) adaptedArgs[0]);
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.LocalDateTime.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalDateTime.class, java.time.ZoneId.class}, false);
                return java.time.ZonedDateTime.of((java.time.LocalDateTime) adaptedArgs[0], (java.time.ZoneId) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.time.LocalDate.class, java.time.LocalTime.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalDate.class, java.time.LocalTime.class, java.time.ZoneId.class}, false);
                return java.time.ZonedDateTime.of((java.time.LocalDate) adaptedArgs[0], (java.time.LocalTime) adaptedArgs[1], (java.time.ZoneId) adaptedArgs[2]);
            }
        }
        if ("ofInstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false);
                return java.time.ZonedDateTime.ofInstant((java.time.Instant) adaptedArgs[0], (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("parse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class}, false);
                return java.time.ZonedDateTime.parse((java.lang.CharSequence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.CharSequence.class, java.time.format.DateTimeFormatter.class}, false);
                return java.time.ZonedDateTime.parse((java.lang.CharSequence) adaptedArgs[0], (java.time.format.DateTimeFormatter) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.time.ZonedDateTime.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof java.time.ZoneOffset) {
            try {
                return invoke0((java.time.ZoneOffset) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.Clock) {
            try {
                return invoke1((java.time.Clock) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.Duration) {
            try {
                return invoke2((java.time.Duration) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.Instant) {
            try {
                return invoke3((java.time.Instant) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.LocalDate) {
            try {
                return invoke4((java.time.LocalDate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.LocalDateTime) {
            try {
                return invoke5((java.time.LocalDateTime) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.LocalTime) {
            try {
                return invoke6((java.time.LocalTime) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.OffsetDateTime) {
            try {
                return invoke7((java.time.OffsetDateTime) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.Period) {
            try {
                return invoke8((java.time.Period) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.ZoneId) {
            try {
                return invoke9((java.time.ZoneId) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.time.ZonedDateTime) {
            try {
                return invoke10((java.time.ZonedDateTime) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.time.ZoneOffset typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTotalSeconds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalSeconds();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(java.time.Clock typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getZone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZone();
            }
        }
        if ("instant".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.instant();
            }
        }
        if ("millis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.millis();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(java.time.Duration typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.compareTo((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getNano".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNano();
            }
        }
        if ("getSeconds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeconds();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("minus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.minus((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("plus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.plus((java.time.Duration) adaptedArgs[0]);
            }
        }
        if ("toMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toMillis();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(java.time.Instant typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return typedTarget.compareTo((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getEpochSecond".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEpochSecond();
            }
        }
        if ("getNano".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNano();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("minusMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.minusMillis(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("minusSeconds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.minusSeconds(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusMillis".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusMillis(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusSeconds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusSeconds(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("toEpochMilli".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toEpochMilli();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(java.time.LocalDate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("atStartOfDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.ZoneId.class}, false);
                return typedTarget.atStartOfDay((java.time.ZoneId) adaptedArgs[0]);
            }
        }
        if ("atTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.LocalTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalTime.class}, false);
                return typedTarget.atTime((java.time.LocalTime) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.atTime(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.atTime(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.atTime(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false);
                return typedTarget.format((java.time.format.DateTimeFormatter) adaptedArgs[0]);
            }
        }
        if ("getDayOfMonth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDayOfMonth();
            }
        }
        if ("getMonthValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMonthValue();
            }
        }
        if ("getYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYear();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isLeapYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLeapYear();
            }
        }
        if ("lengthOfMonth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lengthOfMonth();
            }
        }
        if ("minusDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.minusDays(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusDays(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusMonths".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusMonths(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusYears".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusYears(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("toEpochDay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toEpochDay();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(java.time.LocalDateTime typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false);
                return typedTarget.format((java.time.format.DateTimeFormatter) adaptedArgs[0]);
            }
        }
        if ("getDayOfMonth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDayOfMonth();
            }
        }
        if ("getHour".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHour();
            }
        }
        if ("getMinute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinute();
            }
        }
        if ("getMonthValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMonthValue();
            }
        }
        if ("getNano".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNano();
            }
        }
        if ("getSecond".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSecond();
            }
        }
        if ("getYear".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYear();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("plusDays".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusDays(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusHours".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusHours(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusMinutes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusMinutes(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusSeconds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusSeconds(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("toInstant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.ZoneOffset.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.ZoneOffset.class}, false);
                return typedTarget.toInstant((java.time.ZoneOffset) adaptedArgs[0]);
            }
        }
        if ("toLocalDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toLocalDate();
            }
        }
        if ("toLocalTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toLocalTime();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(java.time.LocalTime typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.LocalTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalTime.class}, false);
                return typedTarget.compareTo((java.time.LocalTime) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false);
                return typedTarget.format((java.time.format.DateTimeFormatter) adaptedArgs[0]);
            }
        }
        if ("getHour".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHour();
            }
        }
        if ("getMinute".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinute();
            }
        }
        if ("getNano".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNano();
            }
        }
        if ("getSecond".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSecond();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("plusHours".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusHours(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusMinutes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusMinutes(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("plusSeconds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.plusSeconds(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("toNanoOfDay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toNanoOfDay();
            }
        }
        if ("toSecondOfDay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toSecondOfDay();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(java.time.OffsetDateTime typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.OffsetDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.OffsetDateTime.class}, false);
                return typedTarget.compareTo((java.time.OffsetDateTime) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false);
                return typedTarget.format((java.time.format.DateTimeFormatter) adaptedArgs[0]);
            }
        }
        if ("getOffset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOffset();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toInstant".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toInstant();
            }
        }
        if ("toLocalDateTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toLocalDateTime();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(java.time.Period typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDays".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDays();
            }
        }
        if ("getMonths".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMonths();
            }
        }
        if ("getYears".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYears();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(java.time.ZoneId typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(java.time.ZonedDateTime typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("format".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.format.DateTimeFormatter.class}, false);
                return typedTarget.format((java.time.format.DateTimeFormatter) adaptedArgs[0]);
            }
        }
        if ("getOffset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOffset();
            }
        }
        if ("getZone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getZone();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("toInstant".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toInstant();
            }
        }
        if ("toLocalDateTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toLocalDateTime();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == java.time.LocalTime.class) return getStaticField0(name);
        if (type == java.time.ZoneOffset.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("MIDNIGHT".equals(name)) return java.time.LocalTime.MIDNIGHT;
        throw unsupportedStaticField(java.time.LocalTime.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("UTC".equals(name)) return java.time.ZoneOffset.UTC;
        throw unsupportedStaticField(java.time.ZoneOffset.class, name);
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
