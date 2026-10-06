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
/** Class-file metadata a reader of the class file sees: records, sealed types, generic signatures, throws. */
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
public class Metadata {
    sealed interface Shape permits Circle, Square {}
    record Circle(double r) implements Shape {}
    record Square(double side) implements Shape {}
    record Pair<A, B extends Comparable<B>>(A first, List<? extends B> rest) {}
    static class Box<T extends Number & Comparable<T>> implements Comparable<Box<T>> {
        T value;
        Map<String, List<T>> index = new HashMap<>();
        int[] raw;
        List<?>[] lists;
        public int compareTo(Box<T> o) { return 0; }
        <E extends Exception> T read(List<? super T> sink, E error) throws IOException, E { return value; }
        void plain() throws IOException, InterruptedException {}
        abstract static class Inner<U> { abstract U get() throws Exception; }
    }
    class Member<V> { Member(V v, List<V> more) {} }
    static String names(Type[] ts) {
        StringBuilder b = new StringBuilder("[");
        for (Type t : ts) { b.append(t.getTypeName()).append(";"); }
        return b.append("]").toString();
    }
    static String names(Class<?>[] cs) {
        StringBuilder b = new StringBuilder("[");
        for (Class<?> c : cs) { b.append(c.getSimpleName()).append(";"); }
        return b.append("]").toString();
    }
    static Method method(Class<?> c, String name) {
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(name) && !m.isBridge()) return m;
        }
        throw new IllegalStateException(name);
    }
    public static void main(String[] args) throws Exception {
        System.out.println("record " + Circle.class.isRecord() + " " + Shape.class.isRecord());
        for (RecordComponent rc : Pair.class.getRecordComponents()) {
            System.out.println("component " + rc.getName() + " " + rc.getGenericType().getTypeName());
        }
        System.out.println("sealed " + Shape.class.isSealed() + " " + names(Shape.class.getPermittedSubclasses()));
        System.out.println("params " + names(Box.class.getTypeParameters()) + " bounds "
                + names(Box.class.getTypeParameters()[0].getBounds()));
        System.out.println("super " + names(Box.class.getGenericInterfaces()));
        for (String f : new String[] {"value", "index", "raw", "lists"}) {
            System.out.println("field " + f + " " + Box.class.getDeclaredField(f).getGenericType().getTypeName());
        }
        Method read = method(Box.class, "read");
        System.out.println("read " + names(read.getTypeParameters()) + " " + read.getGenericReturnType().getTypeName()
                + " " + names(read.getGenericParameterTypes()) + " throws " + names(read.getGenericExceptionTypes())
                + " / " + names(read.getExceptionTypes()));
        Method plain = method(Box.class, "plain");
        System.out.println("plain throws " + names(plain.getExceptionTypes()));
        Method get = method(Box.Inner.class, "get");
        System.out.println("abstract " + get.getGenericReturnType().getTypeName() + " throws " + names(get.getExceptionTypes()));
        Constructor<?> ctor = Member.class.getDeclaredConstructors()[0];
        System.out.println("ctor " + names(ctor.getGenericParameterTypes()));
    }
}
