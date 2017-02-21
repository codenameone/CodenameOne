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
public abstract class MethodHandle {
  public MethodHandle() {
  }

  public java.lang.invoke.MethodType type() {
      return null;
  }

  public final java.lang.Object invokeExact(java.lang.Object... a) throws java.lang.Throwable {
      return null;
  }

  public final java.lang.Object invoke(java.lang.Object... a) throws java.lang.Throwable {
      return null;
  }

  public java.lang.Object invokeWithArguments(java.lang.Object... a) throws java.lang.Throwable {
      return null;
  }

  public java.lang.Object invokeWithArguments(java.util.List<?> a) throws java.lang.Throwable {
      return null;
  }

  public java.lang.invoke.MethodHandle asType(java.lang.invoke.MethodType a) {
      return null;
  }

  public java.lang.invoke.MethodHandle asSpreader(java.lang.Class<?> a, int b) {
      return null;
  }

  public java.lang.invoke.MethodHandle asCollector(java.lang.Class<?> a, int b) {
      return null;
  }

  public java.lang.invoke.MethodHandle asVarargsCollector(java.lang.Class<?> a) {
      return null;
  }

  public boolean isVarargsCollector() {
      return false;
  }

  public java.lang.invoke.MethodHandle asFixedArity() {
      return null;
  }
  public java.lang.invoke.MethodHandle bindTo(java.lang.Object a) {
      return null;
  }
}
