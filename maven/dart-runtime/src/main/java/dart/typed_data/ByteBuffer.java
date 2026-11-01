/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.typed_data;

/// Dart's {@code dart:typed_data} ByteBuffer: the bytes a {@link ByteData} or a
/// {@link Uint8List} is a view over. Views made from it share those bytes, as Dart's do;
/// offsets and lengths cover the whole buffer.
public final class ByteBuffer {

    private final byte[] bytes;

    ByteBuffer(byte[] bytes) {
        this.bytes = bytes;
    }

    public long lengthInBytes() {
        return bytes.length;
    }

    /// {@code buffer.asUint8List()}: the bytes as a list, shared with this buffer.
    public Uint8List asUint8List() {
        return Uint8List.view(bytes);
    }

    public Uint8List asUint8List(long offsetInBytes) {
        return Uint8List.view(this, offsetInBytes);
    }

    public Uint8List asUint8List(long offsetInBytes, Long length) {
        return Uint8List.view(this, offsetInBytes, length);
    }

    /// {@code buffer.asByteData()}: the bytes as a ByteData, shared with this buffer.
    public ByteData asByteData() {
        return ByteData.fromBytes(bytes);
    }

    byte[] bytes() {
        return bytes;
    }

    public Int8List asInt8List() {
        return Int8List.view(this);
    }

    public Int8List asInt8List(long offsetInBytes) {
        return Int8List.view(this, offsetInBytes);
    }

    public Int8List asInt8List(long offsetInBytes, Long length) {
        return Int8List.view(this, offsetInBytes, length);
    }

    public Uint8ClampedList asUint8ClampedList() {
        return Uint8ClampedList.view(this);
    }

    public Uint8ClampedList asUint8ClampedList(long offsetInBytes) {
        return Uint8ClampedList.view(this, offsetInBytes);
    }

    public Uint8ClampedList asUint8ClampedList(long offsetInBytes, Long length) {
        return Uint8ClampedList.view(this, offsetInBytes, length);
    }

    public Int16List asInt16List() {
        return Int16List.view(this);
    }

    public Int16List asInt16List(long offsetInBytes) {
        return Int16List.view(this, offsetInBytes);
    }

    public Int16List asInt16List(long offsetInBytes, Long length) {
        return Int16List.view(this, offsetInBytes, length);
    }

    public Uint16List asUint16List() {
        return Uint16List.view(this);
    }

    public Uint16List asUint16List(long offsetInBytes) {
        return Uint16List.view(this, offsetInBytes);
    }

    public Uint16List asUint16List(long offsetInBytes, Long length) {
        return Uint16List.view(this, offsetInBytes, length);
    }

    public Int32List asInt32List() {
        return Int32List.view(this);
    }

    public Int32List asInt32List(long offsetInBytes) {
        return Int32List.view(this, offsetInBytes);
    }

    public Int32List asInt32List(long offsetInBytes, Long length) {
        return Int32List.view(this, offsetInBytes, length);
    }

    public Uint32List asUint32List() {
        return Uint32List.view(this);
    }

    public Uint32List asUint32List(long offsetInBytes) {
        return Uint32List.view(this, offsetInBytes);
    }

    public Uint32List asUint32List(long offsetInBytes, Long length) {
        return Uint32List.view(this, offsetInBytes, length);
    }

    public Int64List asInt64List() {
        return Int64List.view(this);
    }

    public Int64List asInt64List(long offsetInBytes) {
        return Int64List.view(this, offsetInBytes);
    }

    public Int64List asInt64List(long offsetInBytes, Long length) {
        return Int64List.view(this, offsetInBytes, length);
    }

    public Uint64List asUint64List() {
        return Uint64List.view(this);
    }

    public Uint64List asUint64List(long offsetInBytes) {
        return Uint64List.view(this, offsetInBytes);
    }

    public Uint64List asUint64List(long offsetInBytes, Long length) {
        return Uint64List.view(this, offsetInBytes, length);
    }

    public Float32List asFloat32List() {
        return Float32List.view(this);
    }

    public Float32List asFloat32List(long offsetInBytes) {
        return Float32List.view(this, offsetInBytes);
    }

    public Float32List asFloat32List(long offsetInBytes, Long length) {
        return Float32List.view(this, offsetInBytes, length);
    }

    public Float64List asFloat64List() {
        return Float64List.view(this);
    }

    public Float64List asFloat64List(long offsetInBytes) {
        return Float64List.view(this, offsetInBytes);
    }

    public Float64List asFloat64List(long offsetInBytes, Long length) {
        return Float64List.view(this, offsetInBytes, length);
    }
}
