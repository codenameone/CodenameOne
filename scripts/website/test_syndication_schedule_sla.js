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

const assert = require('node:assert/strict');
const { expectedRunWindow } = require('./syndication_schedule_sla.js');

const beforeDeadline = expectedRunWindow(
  '2026-08-28T15:16:59Z',
  13,
  17,
  2
);
assert.equal(beforeDeadline.expectedStart.toISOString(), '2026-08-27T13:17:00.000Z');
assert.equal(beforeDeadline.deadline.toISOString(), '2026-08-27T15:17:00.000Z');

const atDeadline = expectedRunWindow(
  '2026-08-28T15:17:00Z',
  13,
  17,
  2
);
assert.equal(atDeadline.expectedStart.toISOString(), '2026-08-28T13:17:00.000Z');
assert.equal(atDeadline.deadline.toISOString(), '2026-08-28T15:17:00.000Z');

const lateEvening = expectedRunWindow(
  '2026-08-28T18:45:00Z',
  13,
  17,
  2
);
assert.equal(lateEvening.expectedStart.toISOString(), '2026-08-28T13:17:00.000Z');

assert.throws(
  () => expectedRunWindow('not-a-date', 13, 17, 2),
  /finite numbers/
);

console.log('Syndication schedule SLA tests passed.');
