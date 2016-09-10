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
public class MethodType {
    public MethodType() {}
  public static java.lang.invoke.MethodType methodType(java.lang.Class<?> a, java.lang.Class<?>[] b) {
      return null;
  }

  public static java.lang.invoke.MethodType methodType(java.lang.Class<?> a, java.util.List<java.lang.Class<?>> b) {
      return null;
  }

  public static java.lang.invoke.MethodType methodType(java.lang.Class<?> a, java.lang.Class<?> b, java.lang.Class<?>... c) {
      return null;
  }

  public static java.lang.invoke.MethodType methodType(java.lang.Class<?> a) {
      return null;
  }

  public static java.lang.invoke.MethodType methodType(java.lang.Class<?> a, java.lang.Class<?> b) {
      return null;
  }

  public static java.lang.invoke.MethodType methodType(java.lang.Class<?> a, java.lang.invoke.MethodType b) {
      return null;
  }

  public static java.lang.invoke.MethodType genericMethodType(int a, boolean b) {
      return null;
  }

  public static java.lang.invoke.MethodType genericMethodType(int a) {
      return null;
  }

  public java.lang.invoke.MethodType changeParameterType(int a, java.lang.Class<?> b) {
      return null;
  }

  public java.lang.invoke.MethodType insertParameterTypes(int a, java.lang.Class<?>... b) {
      return null;
  }

  public java.lang.invoke.MethodType appendParameterTypes(java.lang.Class<?>... a) {
      return null;
  }

  public java.lang.invoke.MethodType insertParameterTypes(int a, java.util.List<java.lang.Class<?>> b) {
      return null;
  }

  public java.lang.invoke.MethodType appendParameterTypes(java.util.List<java.lang.Class<?>> a) {
      return null;
  }

  public java.lang.invoke.MethodType dropParameterTypes(int a, int b) {
      return null;
  }

  public java.lang.invoke.MethodType changeReturnType(java.lang.Class<?> a) {
      return null;
  }

  public boolean hasPrimitives() {
      return false;
  }

  public boolean hasWrappers() {
      return false;
  }

  public java.lang.invoke.MethodType erase() {
      return null;
  }

  public java.lang.invoke.MethodType generic() {
      return null;
  }

  public java.lang.invoke.MethodType wrap() {
      return null;
  }

  public java.lang.invoke.MethodType unwrap() {
      return null;
  }

  public java.lang.Class<?> parameterType(int a) {
      return null;
  }

  public int parameterCount() {
      return 0;
  }

  public java.lang.Class<?> returnType() {
      return null;
  }

  public java.util.List<java.lang.Class<?>> parameterList() {
      return null;
  }

  public java.lang.Class<?>[] parameterArray() {
      return null;
  }

    
}
