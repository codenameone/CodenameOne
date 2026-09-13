/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.TryCatch;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class Util {

    public static String getCType(PrimitiveType type) {
        return type == null ? null : type.getCType();
    }

    public static String getSigType(PrimitiveType type) {
        return type == null ? null : type.getSigType();
    }

    public static List<ByteCodeMethodArg> getMethodArgs(String methodDesc) {
        List<ByteCodeMethodArg> arguments = new ArrayList<ByteCodeMethodArg>();
        int currentArrayDim = 0;

        String desc = methodDesc;
        int pos = desc.lastIndexOf(')');
        desc = desc.substring(1, pos);
        for (int i = 0; i < desc.length(); i++) {
            char currentType = desc.charAt(i);
            switch (currentType) {
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    //if(!dependentClasses.contains(objectType)) {
                    //    dependentClasses.add(objectType);
                    //}
                    i = idx;
                    arguments.add(new ByteCodeMethodArg(objectType, currentArrayDim));
                    break;
                case 'I':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.INT, currentArrayDim));
                    break;
                case 'J':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.LONG, currentArrayDim));
                    break;
                case 'B':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.BYTE, currentArrayDim));
                    break;
                case 'S':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.SHORT, currentArrayDim));
                    break;
                case 'F':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.FLOAT, currentArrayDim));
                    break;
                case 'D':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.DOUBLE, currentArrayDim));
                    break;
                case 'Z':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.BOOLEAN, currentArrayDim));
                    break;
                case 'C':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.CHAR, currentArrayDim));
                    break;
            }
            currentArrayDim = 0;
        }
        return arguments;
    }

    public static String resolveInvokeSpecialOwner(String owner, String name, String desc) {
        if (owner == null) {
            return owner;
        }
        ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
        if (bc == null) {
            return owner;
        }
        ByteCodeClass resolvedOwner = bc.findMethodOwner(name, desc);
        if (resolvedOwner == null) {
            return owner;
        }
        return resolvedOwner.getOriginalClassName();
    }

    public static char[] getStackInputTypes(Instruction instr) {
        char[] out = instr.getStackInputTypes();
        if (out != null) {
            return out;
        }

        switch (instr.getOpcode()) {
            
                case Opcodes.NOP:
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_M1:
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                case Opcodes.SIPUSH:
                case Opcodes.BIPUSH:    
                    return new char[0];
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.IALOAD:
                case Opcodes.SALOAD:
                case Opcodes.LALOAD:
                case Opcodes.FALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                    return new char[]{'i', 'o'};
                
                case Opcodes.BASTORE:
                case Opcodes.CASTORE:
                case Opcodes.SASTORE:
                case Opcodes.IASTORE:
                    return new char[]{'i','i','o'};
                case Opcodes.LASTORE:
                    return new char[]{'l', 'i', 'o'};
                case Opcodes.FASTORE:
                    return new char[]{'f','i','o'};
                case Opcodes.DASTORE:
                    return new char[]{'d', 'i', 'o'};
                case Opcodes.AASTORE:
                    return new char[]{'o','i','o'};

                case Opcodes.POP:
                    return new char[]{'*'};

                case Opcodes.POP2:
                    return new char[]{'*','*'};

                case Opcodes.DUP:
                    return new char[]{'0'};

                case Opcodes.DUP2:
                case Opcodes.DUP_X2:
                case Opcodes.DUP2_X2:
                    return null; // DUP2 depends on the types on the stack so we don't statically know the input types

                case Opcodes.DUP_X1:
                case Opcodes.DUP2_X1:
                    return new char[]{'0','1'};
                case Opcodes.SWAP:
                    return new char[]{'0','1'};
                case Opcodes.IADD:
                case Opcodes.ISUB:
                case Opcodes.IMUL:
                case Opcodes.IDIV:
                case Opcodes.IREM:
                case Opcodes.ISHL:
                case Opcodes.ISHR:
                case Opcodes.IUSHR:
                case Opcodes.IAND:
                case Opcodes.IOR:
                case Opcodes.IXOR:
                    return new char[]{'i','i'};
               
                case Opcodes.LADD:
                case Opcodes.LSUB:
                case Opcodes.LMUL:
                case Opcodes.LDIV:
                case Opcodes.LREM:
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LAND:
                case Opcodes.LOR:
                case Opcodes.LXOR:
                case Opcodes.LCMP:
                    return new char[]{'l','l'};
                case Opcodes.FADD:
                case Opcodes.FSUB:
                case Opcodes.FMUL:
                case Opcodes.FDIV:
                case Opcodes.FREM:
                case Opcodes.FCMPG:
                case Opcodes.FCMPL:
                    return new char[]{'f','f'};
                case Opcodes.DADD:
                case Opcodes.DSUB:
                case Opcodes.DMUL:
                case Opcodes.DDIV:
                case Opcodes.DREM:
                case Opcodes.DCMPL:
                case Opcodes.DCMPG:
                    return new char[]{'d','d'};
                    
                case Opcodes.INEG:
                case Opcodes.I2L:
                case Opcodes.I2F:
                case Opcodes.I2D:
                case Opcodes.I2B:
                case Opcodes.I2C:
                case Opcodes.I2S:
                case Opcodes.NEWARRAY:
                    return new char[]{'i'};
                case Opcodes.LNEG:
                case Opcodes.L2I:
                case Opcodes.L2F:
                case Opcodes.L2D:
                    return new char[]{'l'};
                    
                case Opcodes.FNEG:
                case Opcodes.F2I:
                case Opcodes.F2L:
                case Opcodes.F2D:
                    return new char[]{'f'};
                case Opcodes.DNEG:
                case Opcodes.D2I:
                case Opcodes.D2L:
                case Opcodes.D2F:
                    return new char[]{'d'};
                case Opcodes.LUSHR: 
                     return new char[]{'i','l'};
                    
                case Opcodes.ARRAYLENGTH:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                case Opcodes.ATHROW:
                    return new char[]{'o'};
                default: 
                    return null;
                
                    
        }
             
        

    }

    public static char[] getStackOutputTypes(Instruction instr) {
        char[] out = instr.getStackOutputTypes();
        if (out != null) {
            return out;
        }
        
        switch(instr.getOpcode()) {
            case Opcodes.NOP:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.POP:
            case Opcodes.POP2:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
            case Opcodes.ATHROW:
                return new char[0];
                
            case Opcodes.ACONST_NULL:
            case Opcodes.AALOAD:
            case Opcodes.NEWARRAY:
                return new char[]{'o'};
                
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.IALOAD:
            case Opcodes.SALOAD:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.INEG:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.F2I:
            case Opcodes.D2I:
            case Opcodes.L2I:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.LCMP:
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.SIPUSH:
            case Opcodes.BIPUSH:
                return new char[]{'i'};
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.LALOAD:
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LNEG:
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return new char[]{'l'};
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.FALOAD:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.FNEG:
            case Opcodes.I2F:
            case Opcodes.D2F:
            case Opcodes.L2F:
                return new char[]{'f'};
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
            case Opcodes.DALOAD:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.DNEG:
            case Opcodes.I2D:
            case Opcodes.F2D:
            case Opcodes.L2D:
                return new char[]{'d'};
            case Opcodes.DUP:
                return new char[]{'0','0'};
                
            case Opcodes.DUP2:
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X2:
                return null;
                
            case Opcodes.DUP_X1:
            case Opcodes.DUP2_X1:
                return new char[]{'0','1','0'};
            case Opcodes.SWAP:
                    return new char[]{'1','0'};
                
            default:
                return null;
        }
         
    }

    /**
     * Writes {@code data} to {@code target}, replacing it.
     *
     * Stands in for {@code Files.write(Path, byte[])}. ParparVM's JavaAPI has no
     * java.nio.file, and the translator has to compile against it to be able to
     * translate itself, so the whole translator stays on java.io.
     */
    public static void writeBytes(File target, byte[] data) throws IOException {
        OutputStream out = new FileOutputStream(target);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    /**
     * The path of {@code f} relative to {@code root}, with '/' separators.
     *
     * Stands in for {@code root.toPath().relativize(f.toPath())} for the one case
     * that needs it: {@code f} is always found by walking {@code root}, so it is
     * always underneath it and no ".." segment can arise.
     */
    public static String relativePath(File root, File f) {
        String rootPath = root.getAbsolutePath();
        String filePath = f.getAbsolutePath();
        if (filePath.startsWith(rootPath)) {
            filePath = filePath.substring(rootPath.length());
        }
        filePath = filePath.replace('\\', '/');
        while (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        return filePath;
    }

    /**
     * Java's {@code \s}: the six characters the regex engine treats as whitespace.
     * Deliberately not Character.isWhitespace, which differs -- it excludes the
     * vertical tab and accepts many Unicode separators.
     *
     * 0x0B rather than an escape because a raw control byte in a source file is
     * what check-control-characters.py exists to reject.
     */
    private static boolean isRegexWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r';
    }

    /**
     * Equivalent of {@code s.split(String.valueOf(separator))} for a separator that
     * is not a regex metacharacter, including the trailing-empty-string removal
     * String.split does at the default limit of zero.
     *
     * The translator has to compile against ParparVM's JavaAPI in order to translate
     * itself, and String.split is not declared there. It is one of the methods
     * BytecodeComplianceMojo rewrites onto com.codename1.util.regex precisely
     * because JavaAPI lacks it, so adding it there would leave two regex engines and
     * a rewrite rule whose premise had become false. The few call sites here lose
     * the regex instead.
     */
    public static String[] splitLiteral(String s, char separator) {
        // String.split returns { s } when the pattern never matches, WITHOUT the
        // trailing-empty removal below -- so "".split(";") is { "" }, not { }. Missing
        // this is the one way a hand-written splitter and the regex part company on
        // an input a caller can actually produce (an unset build hint).
        if (s.indexOf(separator) < 0) {
            return new String[] { s };
        }
        List<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == separator) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        int end = parts.size();
        while (end > 0 && parts.get(end - 1).isEmpty()) {
            end--;
        }
        return parts.subList(0, end).toArray(new String[end]);
    }

    /**
     * Equivalent of {@code s.split("\\s+")}, including the leading empty string
     * String.split produces when the input starts with whitespace, and the removal
     * of trailing empty strings. See {@link #splitLiteral} for why this is not a
     * regex.
     */
    public static String[] splitWhitespace(String s) {
        // See splitLiteral: no match means { s }, trailing-empty removal skipped.
        boolean matched = false;
        for (int j = 0; j < s.length(); j++) {
            if (isRegexWhitespace(s.charAt(j))) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return new String[] { s };
        }
        List<String> parts = new ArrayList<String>();
        int i = 0;
        int start = 0;
        while (i < s.length()) {
            if (isRegexWhitespace(s.charAt(i))) {
                parts.add(s.substring(start, i));
                while (i < s.length() && isRegexWhitespace(s.charAt(i))) {
                    i++;
                }
                start = i;
            } else {
                i++;
            }
        }
        parts.add(s.substring(start));
        int end = parts.size();
        while (end > 0 && parts.get(end - 1).isEmpty()) {
            end--;
        }
        return parts.subList(0, end).toArray(new String[end]);
    }

    /**
     * Equivalent of {@code s.replaceAll("\\s+", " ")}. See {@link #splitLiteral}
     * for why this is not a regex.
     */
    public static String collapseWhitespace(String s) {
        StringBuilder b = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (isRegexWhitespace(c)) {
                b.append(' ');
                while (i < s.length() && isRegexWhitespace(s.charAt(i))) {
                    i++;
                }
            } else {
                b.append(c);
                i++;
            }
        }
        return b.toString();
    }

    /**
     * Equivalent of
     * {@code s.replaceAll("locals\\[(\\d+)\\]\\.data\\.o", "olocals_$1_")}: rewrites
     * an indexed object local into the scalar-replaced name the barebone path emits.
     *
     * Besides removing the regex (see {@link #splitLiteral}), this drops a Pattern
     * compile that used to happen once per barebone method in every build.
     */
    public static String rewriteLocalObjectRefs(String s) {
        final String prefix = "locals[";
        final String suffix = "].data.o";
        int at = s.indexOf(prefix);
        if (at < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        int from = 0;
        while (at >= 0) {
            int digits = at + prefix.length();
            int end = digits;
            while (end < s.length() && s.charAt(end) >= '0' && s.charAt(end) <= '9') {
                end++;
            }
            if (end > digits && s.startsWith(suffix, end)) {
                b.append(s, from, at);
                b.append("olocals_").append(s, digits, end).append('_');
                from = end + suffix.length();
            } else {
                // \d+ needs at least one digit and "].data.o" must follow it, so this
                // occurrence is not a match; copy it through and keep scanning after it.
                b.append(s, from, digits);
                from = digits;
            }
            at = s.indexOf(prefix, from);
        }
        b.append(s, from, s.length());
        return b.toString();
    }

    /**
     * {@code System.getProperty(key, defaultValue)}, falling back to the
     * environment.
     *
     * ParparVM's JavaAPI declares only the one-argument form, and it returns null
     * unconditionally -- a native binary has no -D to read. The translator has to
     * compile against that JavaAPI in order to translate itself, so the two-argument
     * form is provided here instead of being added to JavaAPI, and every knob gains
     * an environment spelling that works in a translated build. cn1.sqlite is read
     * from CN1_SQLITE, INCLUDE_NPE_CHECKS from INCLUDE_NPE_CHECKS.
     *
     * NativeSignatureVerifier.mode() already reached for getenv for exactly this
     * reason; this generalizes it rather than adding a second convention.
     */
    /**
     * Memoized ParparVM name mangling: '/' and '$' both become '_'.
     *
     * The tree contains 95 hand-written copies of
     * {@code x.replace('/', '_').replace('$', '_')}, 54 of them in the
     * per-instruction emit classes (Invoke, Field, CustomInvoke, Ldc), so the
     * SAME owner string is re-mangled once per emitted instruction. The distinct
     * inputs are bounded by the class count (5782 on the hellocodenameone
     * corpus) while the calls run into the millions.
     *
     * String.replace already returns {@code this} when the character is absent,
     * so the '$' pass is usually free; the '/' pass is the one that allocates a
     * char[] and a String every time. Caching turns that into one lookup.
     *
     * Not synchronized: the translator parses and emits on a single thread --
     * Parser.writeOutput is one sequential loop with no executor and a single
     * writeFile call site.
     */
    private static final java.util.Map<String, String> MANGLE_CACHE =
            new java.util.HashMap<String, String>();

    public static String mangle(String name) {
        if (name == null) {
            return null;
        }
        String m = MANGLE_CACHE.get(name);
        if (m == null) {
            m = name.replace('/', '_').replace('$', '_');
            MANGLE_CACHE.put(name, m);
        }
        return m;
    }

    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(environmentName(key));
        }
        return value == null ? defaultValue : value;
    }

    /**
     * "cn1.sqlite" -&gt; "CN1_SQLITE". Folded by hand: String.toUpperCase is locale
     * sensitive and CN1 has no java.util.Locale to ask for the root locale, so on a
     * Turkish device the 'i' of "cn1.sqlite" would not fold to 'I' and the variable
     * would never be found.
     */
    private static String environmentName(String key) {
        StringBuilder b = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c >= 'a' && c <= 'z') {
                b.append((char) (c - 'a' + 'A'));
            } else if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                b.append(c);
            } else {
                b.append('_');
            }
        }
        return b.toString();
    }

    /**
     * Stands in for {@code java.io.FileFilter}, which ParparVM's JavaAPI does not
     * declare. Kept as a functional interface so the call sites keep their lambdas.
     */
    public interface FileMatcher {
        boolean accept(File file);
    }

    /**
     * Stands in for {@code java.io.FilenameFilter}.
     */
    public interface FileNameMatcher {
        boolean accept(File dir, String name);
    }

    /**
     * {@code dir.listFiles(filter)}, including its null return when {@code dir} is
     * not a directory -- callers test for it.
     */
    public static File[] listFiles(File dir, FileMatcher matcher) {
        File[] all = dir.listFiles();
        if (all == null) {
            return null;
        }
        List<File> kept = new ArrayList<File>(all.length);
        for (int i = 0; i < all.length; i++) {
            if (matcher.accept(all[i])) {
                kept.add(all[i]);
            }
        }
        return kept.toArray(new File[kept.size()]);
    }

    /**
     * {@code dir.list(filter)}, including its null return when {@code dir} is not a
     * directory.
     */
    public static String[] list(File dir, FileNameMatcher matcher) {
        String[] all = dir.list();
        if (all == null) {
            return null;
        }
        List<String> kept = new ArrayList<String>(all.length);
        for (int i = 0; i < all.length; i++) {
            if (matcher.accept(dir, all[i])) {
                kept.add(all[i]);
            }
        }
        return kept.toArray(new String[kept.size()]);
    }

    /**
     * {@code Character.forDigit(digit, 16)} for a digit already known to be in
     * range. JavaAPI has no forDigit.
     */
    public static char hexDigit(int digit) {
        return (char) (digit < 10 ? '0' + digit : 'a' - 10 + digit);
    }
}
