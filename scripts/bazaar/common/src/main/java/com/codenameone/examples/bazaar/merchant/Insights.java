/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.codenameone.examples.bazaar.merchant;

import com.codenameone.examples.bazaar.api.BreakdownDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import java.util.ArrayList;
import java.util.List;

/// What the analytics charts take from the database-counted insights.
final class Insights {
    private Insights() {
    }

    /// The channels that sold something, for the donut: a slice of nothing
    /// only crowds the legend.
    static List<BreakdownDto> withSales(List<BreakdownDto> channels) {
        List<BreakdownDto> out = new ArrayList<BreakdownDto>();
        for (int iter = 0; channels != null && iter < channels.size(); iter++) {
            if (channels.get(iter).orders() > 0) {
                out.add(channels.get(iter));
            }
        }
        return out;
    }

    /// Each funnel step as a share of the first, in basis points (the first
    /// is 10000; a funnel starting at nothing is all zeros).
    static int[] shares(List<FunnelStepDto> funnel) {
        int n = funnel == null ? 0 : funnel.size();
        int[] out = new int[n];
        long first = n == 0 ? 0 : funnel.get(0).count();
        for (int iter = 0; iter < n; iter++) {
            out[iter] = first <= 0 ? 0 : (int) (funnel.get(iter).count() * 10000L / first);
        }
        return out;
    }
}
