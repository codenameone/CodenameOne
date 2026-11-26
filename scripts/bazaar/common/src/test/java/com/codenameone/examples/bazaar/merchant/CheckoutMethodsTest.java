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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.CheckoutFieldDto;
import com.codenameone.examples.bazaar.api.CheckoutSettingsDto;
import com.codenameone.examples.bazaar.api.TipPresetDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The checkout settings' payment method switches. Nothing here opens a display.
class CheckoutMethodsTest {
    @Test
    void switchingOnAppendsAndKeepsTipsAndFields() {
        List<TipPresetDto> tips = new ArrayList<TipPresetDto>();
        List<CheckoutFieldDto> fields = new ArrayList<CheckoutFieldDto>();
        CheckoutSettingsDto s = new CheckoutSettingsDto(Arrays.asList("klarna"), true, tips,
                fields);
        CheckoutSettingsDto out = CheckoutMethods.switched(s, "cod", true);
        assertEquals(Arrays.asList("klarna", "cod"), out.methods());
        assertTrue(out.tipsEnabled());
        assertSame(tips, out.tips());
        assertSame(fields, out.fields());
        assertTrue(CheckoutMethods.on(out, "cod"));
    }

    @Test
    void switchingOffRemovesOnlyThatTypeAndNeverDuplicates() {
        CheckoutSettingsDto s = new CheckoutSettingsDto(Arrays.asList("klarna", "ideal",
                "klarna"), false, null, null);
        assertEquals(Arrays.asList("ideal"), CheckoutMethods.switched(s, "klarna", false)
                .methods());
        assertEquals(Arrays.asList("klarna", "ideal"), CheckoutMethods.switched(s, "ideal",
                true).methods());
    }

    @Test
    void aStoreThatSavedNothingStartsEmpty() {
        assertFalse(CheckoutMethods.on(null, "klarna"));
        assertEquals(Arrays.asList("ideal"), CheckoutMethods.switched(null, "ideal", true)
                .methods());
    }
}
