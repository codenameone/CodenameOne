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
import java.util.*;

public class InnerClasses {
    private int counter = 10;
    private static String prefix = "p";

    class Inner {
        int value = counter * 2;

        int outerPlus(int x) {
            return counter + x + value;
        }

        class Deeper {
            String describe() {
                return prefix + ":" + counter + ":" + value;
            }
        }
    }

    static class Nested {
        private final String name;

        Nested(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Nested(" + name + ")";
        }
    }

    interface Greeter {
        String greet(String who);
    }

    Greeter make(final String greeting) {
        int local = 3;
        class LocalGreeter implements Greeter {
            int times = local;

            public String greet(String who) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < times; i++) {
                    b.append(greeting).append(' ').append(who).append(counter);
                }
                return b.toString();
            }
        }
        return new LocalGreeter();
    }

    Greeter anon(String suffix) {
        return new Greeter() {
            int calls;

            @Override
            public String greet(String who) {
                calls++;
                counter++;
                return who + suffix + calls + counter;
            }
        };
    }

    abstract static class Shape {
        abstract double area();

        public String toString() {
            return getClass().getSimpleName() + "=" + area();
        }
    }

    class Sub extends Inner {
        int outerPlus(int x) {
            return super.outerPlus(x) * 2;
        }
    }

    public static void main(String[] args) {
        InnerClasses o = new InnerClasses();
        InnerClasses.Inner in = o.new Inner();
        System.out.println(in.outerPlus(5));
        InnerClasses.Inner.Deeper d = in.new Deeper();
        System.out.println(d.describe());
        System.out.println(new Nested("n"));
        System.out.println(o.make("hi").greet("bob"));
        Greeter g = o.anon("!");
        System.out.println(g.greet("a"));
        System.out.println(g.greet("b"));
        System.out.println(o.new Sub().outerPlus(1));
        Shape s = new Shape() {
            double area() {
                return 2.5;
            }
        };
        System.out.println(s.area());
        List<Integer> list = new ArrayList<>(Arrays.asList(5, 3, 9, 1));
        Collections.sort(list, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return b - a;
            }
        });
        System.out.println(list);
        Iterator<String> it = new Iterator<String>() {
            int i = 0;

            public boolean hasNext() {
                return i < 3;
            }

            public String next() {
                return "item" + i++;
            }
        };
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        int base = 100;
        Runnable r = new Runnable() {
            public void run() {
                Runnable inner = new Runnable() {
                    public void run() {
                        System.out.println("nested anon " + base + o.counter);
                    }
                };
                inner.run();
            }
        };
        r.run();
    }
}
