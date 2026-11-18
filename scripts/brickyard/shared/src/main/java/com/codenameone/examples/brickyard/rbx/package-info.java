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
/// An engine API compatible with Roblox place files, written over Codename
/// One and the Luau interpreter in `com.codenameone.examples.brickyard.luau`: the datatypes
/// (`Vector3`, `CFrame`, `Color3`, `UDim2`, enums), the instance tree with
/// signals and attributes, a cooperative script scheduler, the services a
/// place uses (players, data stores, the marketplace, tweens, input,
/// pathfinding and more) and a block character with a Humanoid. Classes are
/// described as data in [ClassDb] and given behaviour by [Behavior]
/// subclasses, so nothing is looked up by reflection. It is clean room: no
/// engine source was read and nothing is fetched from any service.
package com.codenameone.examples.brickyard.rbx;
