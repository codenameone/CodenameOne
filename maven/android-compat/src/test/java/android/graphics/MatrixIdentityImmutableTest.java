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
package android.graphics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// `Matrix.IDENTITY_MATRIX` is immutable, as on Android. It used to be an
/// ordinary matrix, so one `postTranslate` on it changed the constant for
/// every later user in the process.
public class MatrixIdentityImmutableTest {

    private interface Mutation {
        void apply(Matrix m);
    }

    private static void assertRefused(String what, Mutation mutation) {
        try {
            mutation.apply(Matrix.IDENTITY_MATRIX);
            fail(what + " modified IDENTITY_MATRIX");
        } catch (IllegalStateException expected) {
            // as on Android
        }
        assertTrue(what, Matrix.IDENTITY_MATRIX.isIdentity());
        assertEquals(what, new Matrix(), Matrix.IDENTITY_MATRIX);
    }

    @Test
    public void everyMutatorIsRefused() {
        assertRefused("postTranslate", new Mutation() {
            public void apply(Matrix m) {
                m.postTranslate(5, 5);
            }
        });
        assertRefused("preScale", new Mutation() {
            public void apply(Matrix m) {
                m.preScale(2, 2);
            }
        });
        assertRefused("postRotate", new Mutation() {
            public void apply(Matrix m) {
                m.postRotate(90);
            }
        });
        assertRefused("setTranslate", new Mutation() {
            public void apply(Matrix m) {
                m.setTranslate(1, 1);
            }
        });
        assertRefused("setRotate", new Mutation() {
            public void apply(Matrix m) {
                m.setRotate(45);
            }
        });
        assertRefused("setValues", new Mutation() {
            public void apply(Matrix m) {
                m.setValues(new float[] {2, 0, 0, 0, 2, 0, 0, 0, 1});
            }
        });
        assertRefused("set", new Mutation() {
            public void apply(Matrix m) {
                Matrix s = new Matrix();
                s.setScale(3, 3);
                m.set(s);
            }
        });
        assertRefused("setConcat", new Mutation() {
            public void apply(Matrix m) {
                Matrix s = new Matrix();
                s.setScale(3, 3);
                m.setConcat(s, s);
            }
        });
        assertRefused("setRectToRect", new Mutation() {
            public void apply(Matrix m) {
                m.setRectToRect(new RectF(0, 0, 1, 1), new RectF(0, 0, 4, 4), Matrix.ScaleToFit.FILL);
            }
        });
        assertRefused("invert into", new Mutation() {
            public void apply(Matrix m) {
                Matrix s = new Matrix();
                s.setScale(2, 2);
                s.invert(m);
            }
        });
    }

    @Test
    public void readsAndCopiesStillWork() {
        Matrix copy = new Matrix(Matrix.IDENTITY_MATRIX);
        assertTrue(copy.postTranslate(1, 0));
        assertFalse(copy.isIdentity());
        Matrix inverse = new Matrix();
        assertTrue(Matrix.IDENTITY_MATRIX.invert(inverse));
        assertTrue(inverse.isIdentity());
        float[] pts = {3, 4};
        Matrix.IDENTITY_MATRIX.mapPoints(pts);
        assertEquals(3f, pts[0], 0f);
    }
}
