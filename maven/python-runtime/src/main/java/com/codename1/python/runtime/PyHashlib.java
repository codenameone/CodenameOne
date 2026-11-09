/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import com.codename1.security.Hash;
import com.codename1.security.Pbkdf2;

/// `_cn1hash`: `hashlib`'s hashes, which are Codename One's own
/// ([Hash]); this only names them the way Python does. Each hash object is
/// a `Hash` held in a `PyJavaObject`, and `digest` finishes a copy, so the
/// object can still be updated afterwards, as Python's can.
final class PyHashlib {

    private PyHashlib() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1hash", new String[0]);
        m.setAttr("new", PyBuiltin.positional("new", a -> {
            String name = Py.toStr(a[0]);
            Hash h;
            if ("blake2b".equals(name)) {
                h = Hash.blake2b((int) Py.asLong(a[1]), PyBytes.like(a[2]), PyBytes.like(a[3]), PyBytes.like(a[4]));
            } else if ("blake2s".equals(name)) {
                h = Hash.blake2s((int) Py.asLong(a[1]), PyBytes.like(a[2]), PyBytes.like(a[3]), PyBytes.like(a[4]));
            } else {
                String algorithm = algorithm(name);
                if (algorithm == null) {
                    throw Py.error(Py.VALUE_ERROR, "unsupported hash type " + name);
                }
                h = Hash.create(algorithm);
            }
            return new PyJavaObject(h);
        }));
        m.setAttr("update", PyBuiltin.positional("update", a -> {
            byte[] b = PyBytes.like(a[1]);
            hash(a[0]).update(b, 0, b.length);
            return Py.NONE;
        }));
        m.setAttr("copy", PyBuiltin.positional("copy", a -> new PyJavaObject(hash(a[0]).copy())));
        m.setAttr("digest", PyBuiltin.positional("digest", a -> new PyBytes(hash(a[0]).copy().digest())));
        m.setAttr("pbkdf2", PyBuiltin.positional("pbkdf2", a -> {
            String algorithm = algorithm(Py.toStr(a[0]));
            if (algorithm == null || Hash.SHA3_256.equals(algorithm)) {
                throw Py.error(Py.VALUE_ERROR, "unsupported hash type " + Py.toStr(a[0]));
            }
            return new PyBytes(Pbkdf2.derive(algorithm, PyBytes.like(a[1]), PyBytes.like(a[2]),
                    (int) Py.asLong(a[3]), (int) Py.asLong(a[4])));
        }));
        return m;
    }

    /// The `Hash` name of a `hashlib` one, or null for one it does not have.
    private static String algorithm(String name) {
        switch (name) {
            case "md5":
                return Hash.MD5;
            case "sha1":
                return Hash.SHA1;
            case "sha224":
                return Hash.SHA224;
            case "sha256":
                return Hash.SHA256;
            case "sha384":
                return Hash.SHA384;
            case "sha512":
                return Hash.SHA512;
            case "sha3_256":
                return Hash.SHA3_256;
            default:
                return null;
        }
    }

    private static Hash hash(Object o) {
        Object v = o instanceof PyJavaObject ? ((PyJavaObject) o).value : null;
        if (v instanceof Hash) {
            return (Hash) v;
        }
        throw Py.typeError("not a hash state");
    }
}
