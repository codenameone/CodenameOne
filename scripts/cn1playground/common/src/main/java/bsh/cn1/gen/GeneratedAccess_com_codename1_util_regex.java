package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_util_regex {
    private GeneratedAccess_com_codename1_util_regex() {
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
        if ("CharacterArrayCharacterIterator".equals(simpleName)) {
            return com.codename1.util.regex.CharacterArrayCharacterIterator.class;
        }
        if ("CharacterIterator".equals(simpleName)) {
            return com.codename1.util.regex.CharacterIterator.class;
        }
        if ("RE".equals(simpleName)) {
            return com.codename1.util.regex.RE.class;
        }
        if ("RECharacter".equals(simpleName)) {
            return com.codename1.util.regex.RECharacter.class;
        }
        if ("RECompiler".equals(simpleName)) {
            return com.codename1.util.regex.RECompiler.class;
        }
        if ("REDebugCompiler".equals(simpleName)) {
            return com.codename1.util.regex.REDebugCompiler.class;
        }
        if ("REProgram".equals(simpleName)) {
            return com.codename1.util.regex.REProgram.class;
        }
        if ("RESyntaxException".equals(simpleName)) {
            return com.codename1.util.regex.RESyntaxException.class;
        }
        if ("REUtil".equals(simpleName)) {
            return com.codename1.util.regex.REUtil.class;
        }
        if ("ReaderCharacterIterator".equals(simpleName)) {
            return com.codename1.util.regex.ReaderCharacterIterator.class;
        }
        if ("StreamCharacterIterator".equals(simpleName)) {
            return com.codename1.util.regex.StreamCharacterIterator.class;
        }
        if ("StringCharacterIterator".equals(simpleName)) {
            return com.codename1.util.regex.StringCharacterIterator.class;
        }
        if ("StringReader".equals(simpleName)) {
            return com.codename1.util.regex.StringReader.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.regex.CharacterArrayCharacterIterator.class) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.util.regex.CharacterArrayCharacterIterator((char[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if (type == com.codename1.util.regex.RE.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.util.regex.RE();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.regex.REProgram.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.regex.REProgram.class}, false);
                return new com.codename1.util.regex.RE((com.codename1.util.regex.REProgram) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.util.regex.RE((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.regex.REProgram.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.regex.REProgram.class, java.lang.Integer.class}, false);
                return new com.codename1.util.regex.RE((com.codename1.util.regex.REProgram) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return new com.codename1.util.regex.RE((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if (type == com.codename1.util.regex.RECompiler.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.util.regex.RECompiler();
            }
        }
        if (type == com.codename1.util.regex.REProgram.class) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return new com.codename1.util.regex.REProgram((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, char[].class}, false);
                return new com.codename1.util.regex.REProgram(((Number) adaptedArgs[0]).intValue(), (char[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false);
                return new com.codename1.util.regex.REProgram((char[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if (type == com.codename1.util.regex.RESyntaxException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.util.regex.RESyntaxException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.util.regex.StringCharacterIterator.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.util.regex.StringCharacterIterator((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.util.regex.StringReader.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.util.regex.StringReader((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.util.regex.RE.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.util.regex.RECharacter.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.util.regex.REUtil.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("simplePatternToFullRegularExpression".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.util.regex.RE.simplePatternToFullRegularExpression((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.util.regex.RE.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.getType(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isDigit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isDigit(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isJavaIdentifierPart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isJavaIdentifierPart(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isJavaIdentifierStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isJavaIdentifierStart(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isLetter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isLetter(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isLetterOrDigit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isLetterOrDigit(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isSpaceChar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isSpaceChar(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("isWhitespace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.isWhitespace(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("toLowerCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.toLowerCase(((Character) adaptedArgs[0]).charValue());
            }
        }
        if ("toUpperCase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Character.class}, false);
                return com.codename1.util.regex.RECharacter.toUpperCase(((Character) adaptedArgs[0]).charValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.regex.RECharacter.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("createRE".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.util.regex.REUtil.createRE((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return com.codename1.util.regex.REUtil.createRE((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedStatic(com.codename1.util.regex.REUtil.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.util.regex.REDebugCompiler) {
            try {
                return invoke0((com.codename1.util.regex.REDebugCompiler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.CharacterArrayCharacterIterator) {
            try {
                return invoke1((com.codename1.util.regex.CharacterArrayCharacterIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.RE) {
            try {
                return invoke2((com.codename1.util.regex.RE) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.RECompiler) {
            try {
                return invoke3((com.codename1.util.regex.RECompiler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.REProgram) {
            try {
                return invoke4((com.codename1.util.regex.REProgram) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.ReaderCharacterIterator) {
            try {
                return invoke5((com.codename1.util.regex.ReaderCharacterIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.StreamCharacterIterator) {
            try {
                return invoke6((com.codename1.util.regex.StreamCharacterIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.StringCharacterIterator) {
            try {
                return invoke7((com.codename1.util.regex.StringCharacterIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.StringReader) {
            try {
                return invoke8((com.codename1.util.regex.StringReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.util.regex.CharacterIterator) {
            try {
                return invoke9((com.codename1.util.regex.CharacterIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.util.regex.REDebugCompiler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.compile((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("dumpProgram".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.dumpProgram(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.util.regex.CharacterArrayCharacterIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isEnd(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("substring".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.util.regex.RE typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getMatchFlags".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMatchFlags();
            }
        }
        if ("getParen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getParen(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getParenCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParenCount();
            }
        }
        if ("getParenEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getParenEnd(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getParenLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getParenLength(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getParenStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getParenStart(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("getProgram".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgram();
            }
        }
        if ("grep".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.grep((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("match".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.match((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.regex.CharacterIterator.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.regex.CharacterIterator.class, java.lang.Integer.class}, false);
                return typedTarget.match((com.codename1.util.regex.CharacterIterator) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.match((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue());
            }
        }
        if ("setMatchFlags".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMatchFlags(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setProgram".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.regex.REProgram.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.regex.REProgram.class}, false);
                typedTarget.setProgram((com.codename1.util.regex.REProgram) adaptedArgs[0]); return null;
            }
        }
        if ("split".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.split((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("subst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.subst((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.subst((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.util.regex.RECompiler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.compile((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.util.regex.REProgram typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getInstructions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInstructions();
            }
        }
        if ("getPrefix".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrefix();
            }
        }
        if ("setInstructions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false);
                typedTarget.setInstructions((char[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.util.regex.ReaderCharacterIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isEnd(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("substring".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.util.regex.StreamCharacterIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isEnd(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("substring".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.util.regex.StringCharacterIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isEnd(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("substring".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.util.regex.StringReader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("mark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.mark(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("markSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.markSupported();
            }
        }
        if ("read".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.read();
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return typedTarget.read((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.read((char[]) adaptedArgs[0], ((Number) adaptedArgs[1]).intValue(), ((Number) adaptedArgs[2]).intValue());
            }
        }
        if ("readLine".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readLine();
            }
        }
        if ("ready".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.ready();
            }
        }
        if ("reset".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.reset(); return null;
            }
        }
        if ("skip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.skip(((Number) adaptedArgs[0]).longValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.util.regex.CharacterIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("charAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.charAt(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("isEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.isEnd(((Number) adaptedArgs[0]).intValue());
            }
        }
        if ("substring".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.substring(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.util.regex.RE.class) {
            if ("MATCH_CASEINDEPENDENT".equals(name)) return com.codename1.util.regex.RE.MATCH_CASEINDEPENDENT;
            if ("MATCH_MULTILINE".equals(name)) return com.codename1.util.regex.RE.MATCH_MULTILINE;
            if ("MATCH_NORMAL".equals(name)) return com.codename1.util.regex.RE.MATCH_NORMAL;
            if ("MATCH_SINGLELINE".equals(name)) return com.codename1.util.regex.RE.MATCH_SINGLELINE;
            if ("REPLACE_ALL".equals(name)) return com.codename1.util.regex.RE.REPLACE_ALL;
            if ("REPLACE_BACKREFERENCES".equals(name)) return com.codename1.util.regex.RE.REPLACE_BACKREFERENCES;
            if ("REPLACE_FIRSTONLY".equals(name)) return com.codename1.util.regex.RE.REPLACE_FIRSTONLY;
        }
        if (type == com.codename1.util.regex.RECharacter.class) {
            if ("COMBINING_SPACING_MARK".equals(name)) return com.codename1.util.regex.RECharacter.COMBINING_SPACING_MARK;
            if ("CONNECTOR_PUNCTUATION".equals(name)) return com.codename1.util.regex.RECharacter.CONNECTOR_PUNCTUATION;
            if ("CONTROL".equals(name)) return com.codename1.util.regex.RECharacter.CONTROL;
            if ("CURRENCY_SYMBOL".equals(name)) return com.codename1.util.regex.RECharacter.CURRENCY_SYMBOL;
            if ("DASH_PUNCTUATION".equals(name)) return com.codename1.util.regex.RECharacter.DASH_PUNCTUATION;
            if ("DECIMAL_DIGIT_NUMBER".equals(name)) return com.codename1.util.regex.RECharacter.DECIMAL_DIGIT_NUMBER;
            if ("ENCLOSING_MARK".equals(name)) return com.codename1.util.regex.RECharacter.ENCLOSING_MARK;
            if ("END_PUNCTUATION".equals(name)) return com.codename1.util.regex.RECharacter.END_PUNCTUATION;
            if ("FORMAT".equals(name)) return com.codename1.util.regex.RECharacter.FORMAT;
            if ("LETTER_NUMBER".equals(name)) return com.codename1.util.regex.RECharacter.LETTER_NUMBER;
            if ("LINE_SEPARATOR".equals(name)) return com.codename1.util.regex.RECharacter.LINE_SEPARATOR;
            if ("LOWERCASE_LETTER".equals(name)) return com.codename1.util.regex.RECharacter.LOWERCASE_LETTER;
            if ("MATH_SYMBOL".equals(name)) return com.codename1.util.regex.RECharacter.MATH_SYMBOL;
            if ("MODIFIER_LETTER".equals(name)) return com.codename1.util.regex.RECharacter.MODIFIER_LETTER;
            if ("MODIFIER_SYMBOL".equals(name)) return com.codename1.util.regex.RECharacter.MODIFIER_SYMBOL;
            if ("NON_SPACING_MARK".equals(name)) return com.codename1.util.regex.RECharacter.NON_SPACING_MARK;
            if ("OTHER_LETTER".equals(name)) return com.codename1.util.regex.RECharacter.OTHER_LETTER;
            if ("OTHER_NUMBER".equals(name)) return com.codename1.util.regex.RECharacter.OTHER_NUMBER;
            if ("OTHER_PUNCTUATION".equals(name)) return com.codename1.util.regex.RECharacter.OTHER_PUNCTUATION;
            if ("OTHER_SYMBOL".equals(name)) return com.codename1.util.regex.RECharacter.OTHER_SYMBOL;
            if ("PARAGRAPH_SEPARATOR".equals(name)) return com.codename1.util.regex.RECharacter.PARAGRAPH_SEPARATOR;
            if ("PRIVATE_USE".equals(name)) return com.codename1.util.regex.RECharacter.PRIVATE_USE;
            if ("SPACE_SEPARATOR".equals(name)) return com.codename1.util.regex.RECharacter.SPACE_SEPARATOR;
            if ("START_PUNCTUATION".equals(name)) return com.codename1.util.regex.RECharacter.START_PUNCTUATION;
            if ("SURROGATE".equals(name)) return com.codename1.util.regex.RECharacter.SURROGATE;
            if ("TITLECASE_LETTER".equals(name)) return com.codename1.util.regex.RECharacter.TITLECASE_LETTER;
            if ("UNASSIGNED".equals(name)) return com.codename1.util.regex.RECharacter.UNASSIGNED;
            if ("UPPERCASE_LETTER".equals(name)) return com.codename1.util.regex.RECharacter.UPPERCASE_LETTER;
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
