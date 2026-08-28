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

function expectedRunWindow(nowValue, scheduleHourUtc, scheduleMinuteUtc, graceHours) {
  const now = new Date(nowValue);
  const values = [
    now.getTime(),
    scheduleHourUtc,
    scheduleMinuteUtc,
    graceHours,
  ];
  if (!values.every(Number.isFinite)) {
    throw new Error('Syndication schedule SLA values must be finite numbers.');
  }

  const expectedStart = new Date(now);
  expectedStart.setUTCHours(scheduleHourUtc, scheduleMinuteUtc, 0, 0);
  const deadline = new Date(expectedStart.getTime() + graceHours * 3600000);
  if (now < deadline) {
    expectedStart.setUTCDate(expectedStart.getUTCDate() - 1);
    deadline.setUTCDate(deadline.getUTCDate() - 1);
  }

  return { expectedStart, deadline };
}

module.exports = { expectedRunWindow };
