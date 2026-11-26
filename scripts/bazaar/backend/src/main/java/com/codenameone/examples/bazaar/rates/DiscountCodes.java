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
package com.codenameone.examples.bazaar.rates;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.DiscountCodesDto;
import com.codenameone.examples.bazaar.api.ExportDto;
import com.codenameone.examples.bazaar.api.ExportRowDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Unique single-use codes generated in bulk from a discount: a mailing of
/// printed cards, an influencer each, a code per row of a spreadsheet.
///
/// Each code is a discount row of its own that copies what the original
/// gives (kind, amount, conditions, dates) with a usage limit of one and the
/// original's id in `batchId`. Checkout therefore counts and gives back a
/// use the way it does for any code, and two shoppers can never both spend
/// one. The discount list leaves the generated rows out; [#codes] answers
/// them, used or not, as CSV.
@Component
public class DiscountCodes {
    /// The most codes one request makes.
    public static final int MAX_BATCH = 1000;
    /// Random characters after the prefix: 32^8, about a trillion, so a code
    /// cannot be guessed from another.
    static final int RANDOM = 8;

    private final DiscountRepository discounts;
    private final StoreAccess access;

    public DiscountCodes(DiscountRepository discounts, StoreAccess access) {
        this.discounts = discounts;
        this.access = access;
    }

    /// Makes `wanted.count()` codes from discount `discountId` and answers
    /// them as rows of text, ready to write as CSV.
    @Transactional(rollbackFor = IOException.class)
    public ExportDto generate(String username, String storeId, String discountId,
            DiscountCodesDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Discount from = source(storeId, discountId);
        if (wanted == null || wanted.count() < 1 || wanted.count() > MAX_BATCH) {
            throw new ResponseStatusException(400, "Generate between 1 and " + MAX_BATCH + " codes");
        }
        String prefix = Codes.normalize(wanted.prefix());
        if (!prefixOk(prefix)) {
            throw new ResponseStatusException(400, "A prefix is up to 12 letters and digits");
        }
        Instant now = Instant.now();
        List<ExportRowDto> rows = new ArrayList<ExportRowDto>();
        for (int iter = 0; iter < wanted.count(); iter++) {
            String code = fresh(storeId, prefix);
            Discount d = copy(from);
            d.setId(Ids.next());
            d.setCode(code);
            d.setUsageLimit(1);
            d.setUsedCount(0);
            d.setAutomatic(false);
            d.setBatchId(from.getId());
            d.setCreatedAt(now);
            discounts.insert(d);
            rows.add(row(d));
        }
        return new ExportDto(columns(), rows);
    }

    /// Every code generated from discount `discountId`, with whether it was used.
    @Transactional(readOnly = true)
    public ExportDto codes(String username, String storeId, String discountId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Discount from = source(storeId, discountId);
        List<Discount> mine = discounts.findByBatchIdOrderByCreatedAtAsc(from.getId());
        List<ExportRowDto> rows = new ArrayList<ExportRowDto>();
        for (int iter = 0; iter < mine.size(); iter++) {
            rows.add(row(mine.get(iter)));
        }
        return new ExportDto(columns(), rows);
    }

    /// Whether `prefix` (already normalised) may start a code: up to twelve
    /// ASCII letters and digits, or none.
    public static boolean prefixOk(String prefix) {
        if (prefix == null || prefix.length() > 12) {
            return false;
        }
        for (int iter = 0; iter < prefix.length(); iter++) {
            char c = prefix.charAt(iter);
            if (!(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    private Discount source(String storeId, String discountId) {
        Discount d = discountId == null ? null : discounts.findById(discountId);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such discount");
        }
        if (d.getBatchId().length() > 0 || d.isAutomatic()) {
            throw new ResponseStatusException(409,
                    "Codes are made from a discount that is applied by code");
        }
        return d;
    }

    private String fresh(String storeId, String prefix) throws IOException {
        for (int tries = 0; tries < 8; tries++) {
            String code = prefix + Tokens.code(RANDOM);
            if (discounts.findByStoreIdAndCode(storeId, code).isEmpty()) {
                return code;
            }
        }
        throw new ResponseStatusException(409, "Could not make a unique code; try again");
    }

    private static List<String> columns() {
        return Arrays.asList("code", "used", "created_at");
    }

    private static ExportRowDto row(Discount d) {
        return new ExportRowDto(Arrays.asList(d.getCode(),
                Boolean.toString(d.getUsedCount() >= d.getUsageLimit()),
                d.getCreatedAt().toString()));
    }

    /// The discount `d` field by field: what a generated code gives is
    /// exactly what its original gives.
    static Discount copy(Discount d) {
        Discount c = new Discount();
        c.setStoreId(d.getStoreId());
        c.setTitle(d.getTitle());
        c.setKind(d.getKind());
        c.setAmount(d.getAmount());
        c.setMinSubtotalCents(d.getMinSubtotalCents());
        c.setBuyQty(d.getBuyQty());
        c.setGetQty(d.getGetQty());
        c.setAppliesTo(d.getAppliesTo());
        c.setTargetId(d.getTargetId());
        c.setOncePerCustomer(d.isOncePerCustomer());
        c.setCombinable(d.isCombinable());
        c.setStartsAt(d.getStartsAt());
        c.setEndsAt(d.getEndsAt());
        c.setActive(d.isActive());
        c.setDiscountClass(d.getDiscountClass());
        c.setCombinesProduct(d.isCombinesProduct());
        c.setCombinesOrder(d.isCombinesOrder());
        c.setCombinesShipping(d.isCombinesShipping());
        c.setEligibility(d.getEligibility());
        c.setRules(d.getRules());
        return c;
    }
}
