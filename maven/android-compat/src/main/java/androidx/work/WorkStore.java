/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package androidx.work;

import com.codename1.io.Storage;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// The persisted form of the work this application has not finished, kept in
/// the application's private `Storage` so it survives a restart, and the
/// binary encoding of [Data] that the size limit is measured against.
final class WorkStore {

    static final String FILE = "cn1compat-work.bin";
    private static final int VERSION = 1;

    private static final int T_NULL = 0;
    private static final int T_BOOLEAN = 1;
    private static final int T_BYTE = 2;
    private static final int T_INT = 3;
    private static final int T_LONG = 4;
    private static final int T_FLOAT = 5;
    private static final int T_DOUBLE = 6;
    private static final int T_STRING = 7;
    private static final int T_BOOLEAN_ARRAY = 8;
    private static final int T_BYTE_ARRAY = 9;
    private static final int T_INT_ARRAY = 10;
    private static final int T_LONG_ARRAY = 11;
    private static final int T_FLOAT_ARRAY = 12;
    private static final int T_DOUBLE_ARRAY = 13;
    private static final int T_STRING_ARRAY = 14;

    private WorkStore() {
    }

    // ------------------------------------------------------------- values

    static boolean isSupportedValue(Object v) {
        return typeOf(v) > T_NULL;
    }

    private static int typeOf(Object v) {
        if (v == null) {
            return T_NULL;
        }
        if (v instanceof Boolean) {
            return T_BOOLEAN;
        }
        if (v instanceof Byte) {
            return T_BYTE;
        }
        if (v instanceof Integer) {
            return T_INT;
        }
        if (v instanceof Long) {
            return T_LONG;
        }
        if (v instanceof Float) {
            return T_FLOAT;
        }
        if (v instanceof Double) {
            return T_DOUBLE;
        }
        if (v instanceof String) {
            return T_STRING;
        }
        if (v instanceof boolean[]) {
            return T_BOOLEAN_ARRAY;
        }
        if (v instanceof byte[]) {
            return T_BYTE_ARRAY;
        }
        if (v instanceof int[]) {
            return T_INT_ARRAY;
        }
        if (v instanceof long[]) {
            return T_LONG_ARRAY;
        }
        if (v instanceof float[]) {
            return T_FLOAT_ARRAY;
        }
        if (v instanceof double[]) {
            return T_DOUBLE_ARRAY;
        }
        if (v instanceof String[]) {
            return T_STRING_ARRAY;
        }
        return -1;
    }

    /// Arrays compare by content, as `Data.equals` does on Android.
    static boolean valueEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        int t = typeOf(a);
        if (t != typeOf(b)) {
            return false;
        }
        switch (t) {
            case T_BOOLEAN_ARRAY:
                return Arrays.equals((boolean[]) a, (boolean[]) b);
            case T_BYTE_ARRAY:
                return Arrays.equals((byte[]) a, (byte[]) b);
            case T_INT_ARRAY:
                return Arrays.equals((int[]) a, (int[]) b);
            case T_LONG_ARRAY:
                return Arrays.equals((long[]) a, (long[]) b);
            case T_FLOAT_ARRAY:
                return Arrays.equals((float[]) a, (float[]) b);
            case T_DOUBLE_ARRAY:
                return Arrays.equals((double[]) a, (double[]) b);
            case T_STRING_ARRAY:
                return Arrays.equals((Object[]) a, (Object[]) b);
            default:
                return a.equals(b);
        }
    }

    static int encodedSize(Data d) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            writeData(new DataOutputStream(bo), d);
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
        return bo.size();
    }

    static void writeData(DataOutputStream out, Data d) throws IOException {
        out.writeInt(d.values.size());
        for (Map.Entry<String, Object> e : d.values.entrySet()) {
            out.writeUTF(e.getKey());
            writeValue(out, e.getValue());
        }
    }

    static Data readData(DataInputStream in) throws IOException {
        int n = in.readInt();
        Map<String, Object> m = new HashMap<String, Object>();
        for (int i = 0; i < n; i++) {
            String k = in.readUTF();
            m.put(k, readValue(in));
        }
        return new Data(m);
    }

    private static void writeValue(DataOutputStream out, Object v) throws IOException {
        int t = typeOf(v);
        out.writeByte(t);
        switch (t) {
            case T_NULL:
                break;
            case T_BOOLEAN:
                out.writeBoolean(((Boolean) v).booleanValue());
                break;
            case T_BYTE:
                out.writeByte(((Byte) v).byteValue());
                break;
            case T_INT:
                out.writeInt(((Integer) v).intValue());
                break;
            case T_LONG:
                out.writeLong(((Long) v).longValue());
                break;
            case T_FLOAT:
                out.writeFloat(((Float) v).floatValue());
                break;
            case T_DOUBLE:
                out.writeDouble(((Double) v).doubleValue());
                break;
            case T_STRING:
                out.writeUTF((String) v);
                break;
            case T_BOOLEAN_ARRAY: {
                boolean[] a = (boolean[]) v;
                out.writeInt(a.length);
                for (int i = 0; i < a.length; i++) {
                    out.writeBoolean(a[i]);
                }
                break;
            }
            case T_BYTE_ARRAY: {
                byte[] a = (byte[]) v;
                out.writeInt(a.length);
                out.write(a);
                break;
            }
            case T_INT_ARRAY: {
                int[] a = (int[]) v;
                out.writeInt(a.length);
                for (int i = 0; i < a.length; i++) {
                    out.writeInt(a[i]);
                }
                break;
            }
            case T_LONG_ARRAY: {
                long[] a = (long[]) v;
                out.writeInt(a.length);
                for (int i = 0; i < a.length; i++) {
                    out.writeLong(a[i]);
                }
                break;
            }
            case T_FLOAT_ARRAY: {
                float[] a = (float[]) v;
                out.writeInt(a.length);
                for (int i = 0; i < a.length; i++) {
                    out.writeFloat(a[i]);
                }
                break;
            }
            case T_DOUBLE_ARRAY: {
                double[] a = (double[]) v;
                out.writeInt(a.length);
                for (int i = 0; i < a.length; i++) {
                    out.writeDouble(a[i]);
                }
                break;
            }
            case T_STRING_ARRAY: {
                String[] a = (String[]) v;
                out.writeInt(a.length);
                for (int i = 0; i < a.length; i++) {
                    out.writeBoolean(a[i] != null);
                    if (a[i] != null) {
                        out.writeUTF(a[i]);
                    }
                }
                break;
            }
            default:
                throw new IOException("unsupported value " + v.getClass());
        }
    }

    private static Object readValue(DataInputStream in) throws IOException {
        int t = in.readByte();
        int n;
        switch (t) {
            case T_NULL:
                return null;
            case T_BOOLEAN:
                return Boolean.valueOf(in.readBoolean());
            case T_BYTE:
                return Byte.valueOf(in.readByte());
            case T_INT:
                return Integer.valueOf(in.readInt());
            case T_LONG:
                return Long.valueOf(in.readLong());
            case T_FLOAT:
                return Float.valueOf(in.readFloat());
            case T_DOUBLE:
                return Double.valueOf(in.readDouble());
            case T_STRING:
                return in.readUTF();
            case T_BOOLEAN_ARRAY: {
                n = in.readInt();
                boolean[] a = new boolean[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in.readBoolean();
                }
                return a;
            }
            case T_BYTE_ARRAY: {
                n = in.readInt();
                byte[] a = new byte[n];
                in.readFully(a);
                return a;
            }
            case T_INT_ARRAY: {
                n = in.readInt();
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in.readInt();
                }
                return a;
            }
            case T_LONG_ARRAY: {
                n = in.readInt();
                long[] a = new long[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in.readLong();
                }
                return a;
            }
            case T_FLOAT_ARRAY: {
                n = in.readInt();
                float[] a = new float[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in.readFloat();
                }
                return a;
            }
            case T_DOUBLE_ARRAY: {
                n = in.readInt();
                double[] a = new double[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in.readDouble();
                }
                return a;
            }
            case T_STRING_ARRAY: {
                n = in.readInt();
                String[] a = new String[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in.readBoolean() ? in.readUTF() : null;
                }
                return a;
            }
            default:
                throw new IOException("unknown value type " + t);
        }
    }

    // -------------------------------------------------------------- specs

    private static void writeOptional(DataOutputStream out, String s) throws IOException {
        out.writeBoolean(s != null);
        if (s != null) {
            out.writeUTF(s);
        }
    }

    private static String readOptional(DataInputStream in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }

    private static void writeSpec(DataOutputStream out, WorkSpec s) throws IOException {
        out.writeUTF(s.id);
        out.writeUTF(s.workerClassName);
        out.writeInt(s.tags.size());
        for (Iterator<String> it = s.tags.iterator(); it.hasNext();) {
            out.writeUTF(it.next());
        }
        writeData(out, s.input);
        Constraints c = s.constraints;
        out.writeUTF(c.networkType.name());
        out.writeBoolean(c.requiresCharging);
        out.writeBoolean(c.requiresBatteryNotLow);
        out.writeBoolean(c.requiresDeviceIdle);
        out.writeBoolean(c.requiresStorageNotLow);
        out.writeLong(s.initialDelayMillis);
        out.writeLong(s.intervalMillis);
        out.writeLong(s.flexMillis);
        out.writeUTF(s.backoffPolicy.name());
        out.writeLong(s.backoffDelayMillis);
        out.writeBoolean(s.expedited);
        writeOptional(out, s.uniqueName);
        out.writeInt(s.prerequisites.size());
        for (int i = 0; i < s.prerequisites.size(); i++) {
            out.writeUTF(s.prerequisites.get(i));
        }
        out.writeUTF(s.state.name());
        out.writeInt(s.runAttemptCount);
        out.writeInt(s.generation);
        out.writeLong(s.nextRunAt);
        writeData(out, s.output);
    }

    private static WorkSpec readSpec(DataInputStream in) throws IOException {
        WorkSpec s = new WorkSpec();
        s.id = in.readUTF();
        s.workerClassName = in.readUTF();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            s.tags.add(in.readUTF());
        }
        s.input = readData(in);
        NetworkType net = NetworkType.valueOf(in.readUTF());
        boolean charging = in.readBoolean();
        boolean battery = in.readBoolean();
        boolean idle = in.readBoolean();
        boolean storage = in.readBoolean();
        s.constraints = new Constraints(net, charging, battery, idle, storage);
        s.initialDelayMillis = in.readLong();
        s.intervalMillis = in.readLong();
        s.flexMillis = in.readLong();
        s.backoffPolicy = BackoffPolicy.valueOf(in.readUTF());
        s.backoffDelayMillis = in.readLong();
        s.expedited = in.readBoolean();
        s.uniqueName = readOptional(in);
        n = in.readInt();
        for (int i = 0; i < n; i++) {
            s.prerequisites.add(in.readUTF());
        }
        s.state = WorkInfo.State.valueOf(in.readUTF());
        s.runAttemptCount = in.readInt();
        s.generation = in.readInt();
        s.nextRunAt = in.readLong();
        s.output = readData(in);
        return s;
    }

    static void save(List<WorkSpec> specs) {
        Storage st = Storage.getInstance();
        if (specs.isEmpty()) {
            if (st.exists(FILE)) {
                st.deleteStorageFile(FILE);
            }
            return;
        }
        OutputStream os = null;
        try {
            os = st.createOutputStream(FILE);
            DataOutputStream out = new DataOutputStream(os);
            out.writeInt(VERSION);
            out.writeInt(specs.size());
            for (int i = 0; i < specs.size(); i++) {
                writeSpec(out, specs.get(i));
            }
            out.flush();
        } catch (IOException e) {
            com.codename1.io.Log.e(e);
        } finally {
            close(os);
        }
    }

    /// The saved work, or none when nothing was saved or the file is from a
    /// format this runtime does not read.
    static List<WorkSpec> load() {
        List<WorkSpec> out = new ArrayList<WorkSpec>();
        Storage st = Storage.getInstance();
        if (!st.exists(FILE)) {
            return out;
        }
        InputStream is = null;
        try {
            is = st.createInputStream(FILE);
            DataInputStream in = new DataInputStream(is);
            if (in.readInt() != VERSION) {
                return out;
            }
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                out.add(readSpec(in));
            }
        } catch (IOException e) {
            com.codename1.io.Log.e(e);
            out.clear();
        } catch (IllegalArgumentException e) {
            // An enum constant this runtime no longer has.
            com.codename1.io.Log.e(e);
            out.clear();
        } finally {
            close(is);
        }
        return out;
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // nothing to recover
            }
        }
    }
}
