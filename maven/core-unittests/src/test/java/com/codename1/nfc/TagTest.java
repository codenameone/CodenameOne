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
package com.codename1.nfc;

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the concrete behaviour baked into the abstract {@link Tag} base
 * class: the defensively-copied technology set and UID, the default technology
 * accessors (all {@code null}), and the default NDEF operations that fail with
 * {@link NfcError#UNSUPPORTED_TAG}. A minimal subclass stands in for a port's
 * native tag.
 */
class TagTest {

    /** Bare subclass exercising only the base-class behaviour. */
    private static final class MinimalTag extends Tag {
        MinimalTag(Set<TagType> types, byte[] id) {
            super(types, id);
        }
    }

    private static Set<TagType> setOf(TagType... t) {
        Set<TagType> s = new HashSet<TagType>();
        for (TagType x : t) {
            s.add(x);
        }
        return s;
    }

    /** Extracts the (synchronously delivered) error from an already-failed resource. */
    private static Throwable errorOf(AsyncResource<?> r) {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        r.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err.set(t);
            }
        });
        return err.get();
    }

    @Test
    void typesAreExposedAndMembershipQueryable() {
        MinimalTag tag = new MinimalTag(setOf(TagType.NDEF, TagType.ISO_DEP), new byte[]{1});
        assertEquals(2, tag.getTypes().size());
        assertTrue(tag.supports(TagType.NDEF));
        assertTrue(tag.supports(TagType.ISO_DEP));
        assertFalse(tag.supports(TagType.MIFARE_CLASSIC));
    }

    @Test
    void typesSetIsUnmodifiable() {
        final MinimalTag tag = new MinimalTag(setOf(TagType.NDEF), new byte[0]);
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                tag.getTypes().add(TagType.NFC_A);
            }
        });
    }

    @Test
    void nullTypesYieldEmptySet() {
        MinimalTag tag = new MinimalTag(null, null);
        assertTrue(tag.getTypes().isEmpty());
        assertFalse(tag.supports(TagType.NDEF));
    }

    @Test
    void idIsDefensivelyCopiedOnConstructionAndAccess() {
        byte[] src = {10, 20, 30};
        MinimalTag tag = new MinimalTag(setOf(TagType.NDEF), src);
        // Mutating the source after construction must not change the stored UID.
        src[0] = 99;
        assertArrayEquals(new byte[]{10, 20, 30}, tag.getId());
        // Mutating a returned copy must not change the stored UID either.
        tag.getId()[1] = 99;
        assertArrayEquals(new byte[]{10, 20, 30}, tag.getId());
    }

    @Test
    void nullIdYieldsEmptyArray() {
        assertEquals(0, new MinimalTag(setOf(TagType.NDEF), null).getId().length);
    }

    @Test
    void defaultScalarsMatchBaseContract() {
        MinimalTag tag = new MinimalTag(setOf(TagType.NDEF), new byte[0]);
        assertEquals(-1, tag.getMaxNdefSize());
        assertFalse(tag.isWritable());
        // hasNdef defaults to supports(NDEF).
        assertTrue(tag.hasNdef());
        assertFalse(new MinimalTag(setOf(TagType.ISO_DEP), new byte[0]).hasNdef());
    }

    @Test
    void technologyAccessorsDefaultToNull() {
        MinimalTag tag = new MinimalTag(setOf(TagType.NDEF), new byte[0]);
        assertNull(tag.getIsoDep());
        assertNull(tag.getMifareClassic());
        assertNull(tag.getMifareUltralight());
        assertNull(tag.getNfcA());
        assertNull(tag.getNfcB());
        assertNull(tag.getNfcF());
        assertNull(tag.getNfcV());
    }

    @Test
    void readNdefFailsUnsupportedByDefault() {
        Throwable t = errorOf(new MinimalTag(setOf(TagType.NDEF), new byte[0]).readNdef());
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) t).getError());
    }

    @Test
    void writeNdefFailsUnsupportedByDefault() {
        Throwable t = errorOf(new MinimalTag(setOf(TagType.NDEF), new byte[0]).writeNdef(null));
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) t).getError());
    }

    @Test
    void makeReadOnlyFailsUnsupportedByDefault() {
        Throwable t = errorOf(new MinimalTag(setOf(TagType.NDEF), new byte[0]).makeReadOnly());
        assertTrue(t instanceof NfcException);
        assertEquals(NfcError.UNSUPPORTED_TAG, ((NfcException) t).getError());
    }
}
