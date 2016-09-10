/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.lang.invoke;

/**
 * @deprecated these classes are used internally for Lambda compatibility
 */
public class LambdaMetafactory {
  public static final int FLAG_SERIALIZABLE = 1;
  public static final int FLAG_MARKERS = 2;
  public static final int FLAG_BRIDGES  = 4;
  public LambdaMetafactory() {}
  public static CallSite metafactory(java.lang.invoke.MethodHandles.Lookup a, java.lang.String f, java.lang.invoke.MethodType b, 
          java.lang.invoke.MethodType c, java.lang.invoke.MethodHandle d, java.lang.invoke.MethodType e) throws java.lang.invoke.LambdaConversionException {
      return null;
  }
  public static java.lang.invoke.CallSite altMetafactory(java.lang.invoke.MethodHandles.Lookup a, java.lang.String b, java.lang.invoke.MethodType c, java.lang.Object... d) throws java.lang.invoke.LambdaConversionException {
      return null;
  }
}
