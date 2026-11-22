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
package com.codenameone.examples.bazaar.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PayLabelsTest {
    @Test
    void genericLabelsTranslateAndNamesDoNot() {
        assertTrue(PayLabels.translatable("Card"));
        assertTrue(PayLabels.translatable("SEPA Direct Debit"));
        assertFalse(PayLabels.translatable("Klarna"));
        assertFalse(PayLabels.translatable("Visa 4242"));
        assertFalse(PayLabels.translatable(null));
    }

    @Test
    void everyGenericLabelIsInEveryBundle() throws Exception {
        String[] langs = {"", "_en", "_es", "_fr", "_de", "_he"};
        for (int l = 0; l < langs.length; l++) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(
                    "src/main/l10n/Bundle" + langs[l] + ".properties")) {
                p.load(in);
            }
            for (int iter = 0; iter < PayLabels.GENERIC.length; iter++) {
                assertTrue(p.containsKey(PayLabels.GENERIC[iter]),
                        PayLabels.GENERIC[iter] + " in Bundle" + langs[l]);
            }
        }
    }
}
