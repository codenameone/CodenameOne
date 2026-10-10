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
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// A driver's money: what came in, what is owed, and how the last two weeks
/// went. Amounts are cents, and are the driver's share -- the fare less the
/// service's commission, plus tips.
@Mapped
public class EarningsSummaryDto {
    public String currency;
    public long today;
    public long week;
    public long month;
    /// Earned and not yet paid out.
    public long balance;
    public int trips;
    public long tips;
    /// Time on line today.
    public int onlineMinutes;
    public double rating;
    /// 0 to 1.
    public double acceptanceRate;
    /// The last 14 days, oldest first, every day present.
    public List<DayAmountDto> daily;

    public EarningsSummaryDto() {
    }
}
