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
/// Reading XML on the server: a streaming pull parser, a small element tree and
/// an Apple property list reader.
///
/// [XmlPullParser] is the parser everything else is built on; [XmlElement] is a
/// tree for documents small enough to hold in memory; [PropertyLists] reads XML
/// property lists, including the one inside a signed provisioning profile.
///
/// All three are safe for untrusted input by construction. A document type
/// declaration is skipped and never interpreted, so no external entity is ever
/// resolved and no entity is ever expanded beyond the five XML predefines and
/// numeric character references; nesting depth is capped. There is no switch
/// that turns either protection off.
package com.codename1.backend.xml;
