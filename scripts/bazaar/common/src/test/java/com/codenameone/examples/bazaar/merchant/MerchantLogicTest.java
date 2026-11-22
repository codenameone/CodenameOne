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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The merchant app's pure logic: the variant matrix, CSV mapping, the smart
/// collection rule builder and refund arithmetic. Nothing here opens a display.
class MerchantLogicTest {
    private static OptionDto option(String name, String... values) {
        return new OptionDto(null, name, Arrays.asList(values), 0);
    }

    private static VariantDto variant(String id, String o1, String o2, String sku, long price) {
        return new VariantDto(id, "p", "", o1, o2, null, sku, "", price, 0, 0, 0, true, true,
                false, 0, 0, new ArrayList<StockDto>(), false, "", "", 0L, "", 0, "");
    }

    @Test
    void matrixMultipliesOptionsFirstOptionSlowest() {
        List<String[]> combos = VariantMatrix.combinations(Arrays.asList(
                option("Size", "S", "M"), option("Colour", "Red", "Blue", "Green")));
        assertEquals(6, combos.size());
        assertEquals("S / Red", VariantMatrix.title(combos.get(0)));
        assertEquals("S / Green", VariantMatrix.title(combos.get(2)));
        assertEquals("M / Red", VariantMatrix.title(combos.get(3)));
    }

    @Test
    void matrixWithNoOptionsIsTheDefaultVariant() {
        List<String[]> combos = VariantMatrix.combinations(new ArrayList<OptionDto>());
        assertEquals(1, combos.size());
        assertEquals("Default", VariantMatrix.title(combos.get(0)));
        // An option with no values is skipped, repeats and blanks dropped.
        assertEquals(2, VariantMatrix.combinations(Arrays.asList(option("Empty"),
                option("Size", "S", " s ", "", "M"))).size());
    }

    @Test
    void matrixKeepsWhatWasTypedIntoExistingVariants() {
        List<VariantDto> existing = Arrays.asList(variant("v1", "S", null, "TEE-S", 2500),
                variant("v2", "M", null, "TEE-M", 2600));
        List<VariantDto> out = VariantMatrix.generate("p", Arrays.asList(
                option("Size", "s", "M", "L")), existing);
        assertEquals(3, out.size());
        assertEquals("v1", out.get(0).id());
        assertEquals("TEE-S", out.get(0).sku());
        assertEquals("TEE-M", out.get(1).sku());
        assertNull(out.get(2).id());
        assertEquals("", out.get(2).sku());
        // A new combination starts from the first variant's price.
        assertEquals(2500, out.get(2).priceCents());
        assertEquals(2, out.get(2).position());
    }

    @Test
    void matrixRefusesAnExplosion() {
        String[] many = new String[11];
        for (int iter = 0; iter < many.length; iter++) {
            many[iter] = "v" + iter;
        }
        assertThrows(IllegalArgumentException.class, () -> VariantMatrix.combinations(
                Arrays.asList(option("A", many), option("B", many))));
    }

    @Test
    void splitsTypedValues() {
        assertEquals(Arrays.asList("Red", "Blue"), VariantMatrix.split(" Red, Blue ,,BLUE, blue,"));
        assertEquals("Red, Blue", VariantMatrix.join(Arrays.asList("Red", "Blue")));
    }

    private static ProductDto product(String handle, String title, List<OptionDto> options,
            List<VariantDto> variants) {
        return new ProductDto("id-" + handle, "s", handle, title, "Soft, warm \"tee\"", "Acme",
                "Shirts", Arrays.asList("cotton", "sale"), "ACTIVE", "", "", null, 0, 0, options,
                variants, new ArrayList<ProductPhotoDto>(), new ArrayList<String>(), 0);
    }

    @Test
    void variantCustomsAndUnitsAreCheckedBeforeSaving() {
        assertNull(VariantChecks.unitProblem("691200", "PT", 0, "", 1, ""));
        assertNull(VariantChecks.unitProblem("", "", 250000, "g", 100, "g"));
        assertNull(VariantChecks.unitProblem("", "", 750000, "ml", 1, "l"));
        assertNotNull(VariantChecks.unitProblem(null, "", 0, "", 1, ""));
        assertNotNull(VariantChecks.unitProblem("", null, 0, "", 1, ""));
        assertNotNull(VariantChecks.unitProblem("", "", -1, "g", 1, "g"));
        assertNotNull(VariantChecks.unitProblem("", "", 250000, "g", 1, "ml"));
        assertNotNull(VariantChecks.unitProblem("", "", 250000, "furlong", 1, "furlong"));
        assertNotNull(VariantChecks.unitProblem("", "", 0, "g", 100, "g"));
        assertNotNull(VariantChecks.unitProblem("", "", 250000, "g", 0, "g"));
    }

    @Test
    void csvRoundTripKeepsProductsVariantsAndPrices() throws Exception {
        List<VariantDto> tees = Arrays.asList(
                variant("v1", "S", "Red", "=TEE-S-R", 2500),
                variant("v2", "S", "Blue", "00420", 2550),
                variant("v3", "M", "Red", "TEE-M-R", 2605));
        ProductDto tee = product("tee", "Tee, classic", Arrays.asList(option("Size", "S", "M"),
                option("Colour", "Red", "Blue")), tees);
        ProductDto mug = product("mug", "Mug", new ArrayList<OptionDto>(),
                Arrays.asList(variant("m", null, null, "MUG", 1200)));
        String text = ProductCsv.write(Arrays.asList(tee, mug));
        assertTrue(text.contains("'=TEE-S-R"), text);
        List<ProductDto> back = ProductCsv.read(text);
        assertEquals(2, back.size());
        ProductDto t = back.get(0);
        assertEquals("tee", t.handle());
        assertEquals("Tee, classic", t.title());
        assertEquals("Soft, warm \"tee\"", t.description());
        assertEquals(Arrays.asList("cotton", "sale"), t.tags());
        assertEquals("ACTIVE", t.status());
        assertEquals(2, t.options().size());
        assertEquals("Colour", t.options().get(1).name());
        assertEquals(Arrays.asList("Red", "Blue"), t.options().get(1).values());
        assertEquals(3, t.variants().size());
        assertEquals("=TEE-S-R", t.variants().get(0).sku());
        assertEquals("00420", t.variants().get(1).sku());
        assertEquals(2605, t.variants().get(2).priceCents());
        assertEquals("M / Red", t.variants().get(2).title());
        assertEquals(1, back.get(1).variants().size());
        assertEquals(0, back.get(1).options().size());
    }

    @Test
    void csvFindsColumnsByHeaderAndMakesHandles() {
        String[][] rows = {
            {"price", "TITLE"},
            {"9,99", "Blue Mug!"},
            {"", ""}
        };
        List<ProductDto> back = ProductCsv.fromRows(rows);
        assertEquals(1, back.size());
        assertEquals("blue-mug", back.get(0).handle());
        assertEquals("DRAFT", back.get(0).status());
        assertEquals(999, back.get(0).variants().get(0).priceCents());
        assertThrows(IllegalArgumentException.class,
                () -> ProductCsv.fromRows(new String[][] {{"Nothing"}, {"x"}}));
    }

    @Test
    void parsesPrices() {
        assertEquals(1250, ProductCsv.cents("12.5"));
        assertEquals(1250, ProductCsv.cents("$12.50"));
        assertEquals(129900, ProductCsv.cents("1,299.00"));
        assertEquals(129900, ProductCsv.cents("1,299"));
        assertEquals(1205, ProductCsv.cents("12,05"));
        assertEquals(-50, ProductCsv.cents("-0.50"));
        assertEquals(0, ProductCsv.cents("free"));
        assertEquals("-0.05", ProductCsv.major(-5));
        assertEquals("12.00", ProductCsv.major(1200));
    }

    @Test
    void minusCountsOnlyFirstAndOverflowIsInvalid() {
        assertEquals(0, ProductCsv.cents("12-5"));
        assertEquals(0, ProductCsv.cents("1-"));
        assertEquals(-1250, ProductCsv.cents("-12.50"));
        assertEquals(0, ProductCsv.cents("9999999999999"));
        assertEquals(0, ProductCsv.typedInteger("12-5"));
        assertEquals(-12, ProductCsv.typedInteger(" -12 "));
        assertEquals(125, ProductCsv.typedInteger("125"));
        assertEquals(0, ProductCsv.typedInteger("99999999999"));
        assertEquals(Integer.MAX_VALUE, ProductCsv.typedInteger(String.valueOf(Integer.MAX_VALUE)));
        assertEquals(0, ProductCsv.typedInteger(null));
        assertEquals(0, ProductCsv.typedInteger("1.5"));
        assertEquals(0, ProductCsv.typedInteger("12a"));
        assertEquals(1299, ProductCsv.typedInteger("1,299"));
    }

    @Test
    void smallNegativeAmountsKeepTheirSign() {
        // What the plain pipeline money and the forecast's hundredths format.
        assertEquals("-0.50", ProductCsv.major(-50));
        assertEquals("-0.99", ProductCsv.major(-99));
        assertEquals("-1.01", ProductCsv.major(-101));
        assertEquals("0.07", ProductCsv.major(7));
    }

    @Test
    void ruleBuilderRoundTrips() {
        List<SmartRules.Rule> rules = SmartRules.parse("tag:Sale and TYPE: Shirts AND price<2000");
        assertEquals(3, rules.size());
        assertEquals(new SmartRules.Rule(SmartRules.TAG, "Sale"), rules.get(0));
        assertEquals(new SmartRules.Rule(SmartRules.TYPE, "Shirts"), rules.get(1));
        assertEquals(new SmartRules.Rule(SmartRules.PRICE_BELOW, "2000"), rules.get(2));
        assertEquals("tag:Sale AND type:Shirts AND price<2000", SmartRules.format(rules));
        assertEquals(rules, SmartRules.parse(SmartRules.format(rules)));
    }

    @Test
    void ruleBuilderSkipsUnfinishedRowsAndRejectsUnknownTerms() {
        List<SmartRules.Rule> rows = new ArrayList<SmartRules.Rule>();
        rows.add(new SmartRules.Rule(SmartRules.VENDOR, "Acme"));
        rows.add(new SmartRules.Rule(SmartRules.TAG, " "));
        rows.add(new SmartRules.Rule(SmartRules.PRICE_ABOVE, "12.50"));
        assertEquals("vendor:Acme", SmartRules.format(rows));
        assertNull(SmartRules.parse("colour:red"));
        assertNull(SmartRules.parse("price<abc"));
        assertEquals(0, SmartRules.parse("").size());
        assertEquals("Manual", SmartRules.describe(new ArrayList<SmartRules.Rule>()));
    }

    private static OrderLineDto line(String id, int qty, int refunded, long price, long discount,
            long tax) {
        return new OrderLineDto(id, "v", "p", "T", "", "SKU", null, qty, 0, refunded, price,
                discount, tax, null);
    }

    private static OrderDto order(List<OrderLineDto> lines, long shipping, long total,
            long refunded) {
        return new OrderDto("o", "s", 1, "ONLINE", "OPEN", "PAID", "UNFULFILLED", "u", "e", 0, 0,
                shipping, 0, total, refunded, "USD", null, null, null, null, null, null, 0, null,
                lines, null, null, null, 0, null);
    }

    @Test
    void refundSharesDiscountAndTaxByUnit() {
        // 3 x 10.00 with 3.00 off the line and 2.70 tax added on top.
        OrderLineDto l = line("a", 3, 0, 1000, 300, 270);
        assertEquals(990, RefundMath.line(l, 1, false));
        assertEquals(1980, RefundMath.line(l, 2, false));
        // Tax already in the price is not added again.
        assertEquals(900, RefundMath.line(l, 1, true));
        assertEquals(0, RefundMath.line(l, 0, false));
    }

    @Test
    void refundTotalClampsToWhatIsLeft() {
        OrderLineDto a = line("a", 2, 1, 1000, 0, 0);
        OrderLineDto b = line("b", 1, 0, 500, 0, 0);
        OrderDto o = order(Arrays.asList(a, b), 700, 3200, 1000);
        Map<String, Integer> q = new HashMap<String, Integer>();
        q.put("a", Integer.valueOf(2));
        q.put("b", Integer.valueOf(1));
        // a has one unit left, b one: 1500, plus shipping clamped to 700.
        assertEquals(1500 + 700, RefundMath.total(o, q, 9999, true));
        // Never more than total - refunded.
        OrderDto tight = order(Arrays.asList(a, b), 700, 1800, 1000);
        assertEquals(800, RefundMath.total(tight, q, 700, true));
        assertEquals(1, RefundMath.refundable(a));
    }

    @Test
    void halfEvenRounding() {
        assertEquals(2, RefundMath.divideHalfEven(5, 2));
        assertEquals(4, RefundMath.divideHalfEven(7, 2));
        assertEquals(-2, RefundMath.divideHalfEven(-5, 2));
        assertEquals(0, RefundMath.divideHalfEven(5, 0));
    }
}
