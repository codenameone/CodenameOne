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

/**
 * dart:typed_data's {@code TypedDataList<E>}: what every typed list shares -- its length,
 * its window on a {@link ByteBuffer}, and the bulk writes. {@link TypedIntList} and
 * {@link TypedFloatList} implement it, so a value typed as a TypedDataList takes any of them.
 *
 * @param <E> the element type, {@code Long} or {@code Double}
 */
public interface TypedDataList<E> {

    long length();

    ByteBuffer buffer();

    long lengthInBytes();

    long offsetInBytes();

    long elementSizeInBytes();

    void setRange(long start, long end, Iterable<? extends E> from);

    void setRange(long start, long end, Iterable<? extends E> from, long skipCount);

    void fillRange(long start, long end);

    void fillRange(long start, long end, E value);
}
