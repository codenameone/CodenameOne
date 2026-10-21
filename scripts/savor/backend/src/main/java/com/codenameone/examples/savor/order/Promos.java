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
package com.codenameone.examples.savor.order;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.savor.api.PromoDto;
import com.codenameone.examples.savor.domain.Promo;
import com.codenameone.examples.savor.repository.PromoRepository;
import java.util.ArrayList;
import java.util.List;

/// Promotion codes, which the admins create and switch off. A code takes a
/// percentage off, an amount off, or both, from orders over a minimum, once
/// per account.
@Component
public class Promos {
    private final PromoRepository promos;

    public Promos(PromoRepository promos) {
        this.promos = promos;
    }

    @Transactional(readOnly = true)
    public List<PromoDto> all() {
        List<Promo> rows = promos.newest(new PageRequest(0, 200));
        List<PromoDto> out = new ArrayList<PromoDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    @Transactional
    public PromoDto save(PromoDto form) {
        if (form == null || form.code() == null) {
            throw new ResponseStatusException(400, "Name the code");
        }
        String code = Quotes.promoCode(form.code());
        if (code.length() < 3 || code.length() > 20) {
            throw new ResponseStatusException(400, "A code is 3 to 20 letters and digits");
        }
        for (int iter = 0; iter < code.length(); iter++) {
            char c = code.charAt(iter);
            if (!(c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) {
                throw new ResponseStatusException(400, "A code is 3 to 20 letters and digits");
            }
        }
        if (form.percentOff() < 0 || form.percentOff() > 100 || form.amountOffCents() < 0
                || form.amountOffCents() > 100000 || form.minimumCents() < 0
                || form.minimumCents() > 100000) {
            throw new ResponseStatusException(400, "Check the discount and the minimum");
        }
        if (form.percentOff() == 0 && form.amountOffCents() == 0L) {
            throw new ResponseStatusException(400, "A code has to take something off");
        }
        Promo row = promos.findById(code);
        boolean unsaved = row == null;
        if (unsaved) {
            row = new Promo();
            row.setCode(code);
            row.setCreatedAt(System.currentTimeMillis());
        }
        row.setPercentOff(form.percentOff());
        row.setAmountOffCents(form.amountOffCents());
        row.setMinimumCents(form.minimumCents());
        row.setActive(form.active());
        if (unsaved) {
            promos.insert(row);
        }
        return toDto(row);
    }

    private static PromoDto toDto(Promo row) {
        return new PromoDto(row.getCode(), row.getPercentOff(), row.getAmountOffCents(),
                row.getMinimumCents(), row.isActive());
    }
}
