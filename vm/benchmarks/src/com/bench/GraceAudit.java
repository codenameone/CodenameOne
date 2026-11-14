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
package com.bench;

/**
 * Grace-completeness gate born from issue 5425: a bursty small-object size
 * class allocates fresh objects WHILE the concurrent mark runs (System.gc is
 * asynchronous), each holding the only reference to an older object, then
 * goes quiet across the next GC cycle. Any grace scheme that tracks "pages
 * with fresh slots" incrementally must still trace those objects; the
 * fresh-page-stack scheme this driver was written against dropped them and
 * the sweep freed their children while still referenced. Run with
 * -DCN1_GRACE_AUDIT: every reported doomedChildren value must be zero.
 */
public class GraceAudit {
    public static void main(String[] args) throws Exception {
        GraceAuditWorkload.run(true);
    }
}
