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
package java.util;

/**
 *
 * @author shannah
 */
class NumberComparator {
    static class I implements Comparator<Integer>{

        public int compare(Integer object1, Integer object2) {
            return object1.intValue()-object2.intValue();
        }
    }
    static class D implements Comparator<Double>{

        public int compare(Double object1, Double object2) {
            double diff = object1.doubleValue()-object2.doubleValue();
            return diff==0 ? 0 : (diff<0 ? -1 : 1);
        }
    }
    
    static class F implements Comparator<Float>{

        public int compare(Float object1, Float object2) {
            float diff = object1.floatValue()-object2.floatValue();
            return diff==0 ? 0 : (diff<0 ? -1 : 1);
        }
    }
    
    static class S implements Comparator<Short>{

        public int compare(Short object1, Short object2) {
            return object1.shortValue()-object2.shortValue();
            
        }
    }
    
    static class B implements Comparator<Byte>{

        public int compare(Byte object1, Byte object2) {
            return object1.byteValue()-object2.byteValue();
        }
    }
    
    static class Z implements Comparator<Boolean>{

        public int compare(Boolean object1, Boolean object2) {
            int i1 = object1.booleanValue() ? 1 : 0;
            int i2 = object2.booleanValue() ? 1 : 0;
            return i1-i2;
        }
    }
    
    static class L implements Comparator<Long>{

        public int compare(Long object1, Long object2) {
            return (int)(object1.longValue()-object2.longValue());
        }
    }
    
    static class C implements Comparator<Character>{

        public int compare(Character object1, Character object2) {
            return object1.charValue()-object2.charValue();
        }
    }
    
    static boolean isNumber(Object o){
        if ( o == null ){
            return false;
        }
        Class c = o.getClass();
        return (Double.class.equals(c) || 
                Float.class.equals(c) || 
                Integer.class.equals(c) || 
                Long.class.equals(c) || 
                Short.class.equals(c) || 
                Boolean.class.equals(c) || 
                Byte.class.equals(c) || 
                Character.class.equals(c) 
                );
        
    }
    
    
    static Comparator createComparator(Class cls){
        if ( cls == null ){
            return null;
        } else if ( Double.class.equals(cls)){
            return new D();
        } else if ( Float.class.equals(cls)){
            return new F();
        } else if ( Integer.class.equals(cls)){
            return new I();
        } else if ( Short.class.equals(cls)){
            return new S();
        } else if ( Byte.class.equals(cls)){
            return new B();
        } else if ( Character.class.equals(cls)){
            return new C();
        } else if ( Long.class.equals(cls)){
            return new L();
        } else if ( Boolean.class.equals(cls)){
            return new Z();
        }
        return null;
    }
    
    
}
