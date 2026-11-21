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
package com.codenameone.examples.bazaar.trade;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.VatCheckDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.VatLookup;
import com.codenameone.examples.bazaar.repository.VatLookupRepository;
import java.io.IOException;

/// A VAT number checked as far as the server can: the format first
/// ([VatIds]), then the registry ([VatRegistry], VIES in production).
///
/// - A **verified** or **invalid** answer is cached by number for
///   `bazaar.vat.cache-days` (default 7), so saving a company twice asks once.
/// - An **outage** (VIES or the member state's service down) is never cached
///   and never refuses anything: the check answers `unverified`, valid on
///   format, and the next save asks again.
/// - With no registry the answer is `format`, as before VIES was added.
///
/// The network call is made outside any transaction; only the cache read and
/// write are in one.
@Component
public class VatVerification {
    private final VatLookupRepository lookups;
    private final VatRegistry registry;
    private final long cacheMillis;

    public VatVerification(VatLookupRepository lookups, VatRegistry registry, Config config)
            throws IOException {
        this.lookups = lookups;
        this.registry = registry;
        int days = 7;
        try {
            days = Integer.parseInt(config.get("bazaar.vat.cache-days", "7").trim());
        } catch (NumberFormatException bad) {
            days = 7;
        }
        this.cacheMillis = Math.max(0, days) * Days.MILLIS;
    }

    /// Checks `typed` with the server's registry.
    public VatCheckDto verify(String typed) throws IOException {
        return verify(typed, registry, System.currentTimeMillis());
    }

    /// Checks `typed` with `with` as of `now`.
    public VatCheckDto verify(String typed, VatRegistry with, long now) throws IOException {
        VatCheckDto format = VatIds.check(typed);
        if (!format.valid()) {
            return format;
        }
        String id = format.vatId();
        VatLookup cached = cached(id);
        if (cached != null && now - cached.getCheckedAt() < cacheMillis) {
            return answer(format, cached.getStatus(), cached.getName(), cached.getCheckedAt());
        }
        VatRegistry.Answer a;
        try {
            a = with.lookup(format.country(), id.substring(2));
        } catch (IOException | RuntimeException down) {
            return new VatCheckDto(id, true, format.country(), "The format is valid; VIES "
                    + "could not be reached, so the number is not verified yet",
                    VatIds.UNVERIFIED, "", now);
        }
        if (a == null) {
            return format;
        }
        String status = a.valid ? VatIds.VERIFIED : VatIds.INVALID;
        store(id, status, Codes.cut(a.name, 200), Codes.cut(a.reference, 64), now);
        return answer(format, status, Codes.cut(a.name, 200), now);
    }

    private static VatCheckDto answer(VatCheckDto format, String status, String name,
            long at) {
        boolean ok = VatIds.VERIFIED.equals(status);
        return new VatCheckDto(format.vatId(), ok, format.country(), ok
                ? "VIES confirms the number" + (name.length() > 0 ? " for " + name : "")
                : "VIES says " + format.vatId() + " is not an issued VAT number",
                status, name, at);
    }

    /// The consultation reference cached for `id`, or empty.
    @Transactional(readOnly = true)
    public String reference(String id) throws IOException {
        VatLookup l = lookups.findById(id);
        return l == null ? "" : l.getReference();
    }

    @Transactional(readOnly = true)
    private VatLookup cached(String id) throws IOException {
        return lookups.findById(id);
    }

    @Transactional(rollbackFor = IOException.class)
    private void store(String id, String status, String name, String reference, long now)
            throws IOException {
        VatLookup l = lookups.findById(id);
        boolean fresh = l == null;
        if (fresh) {
            l = new VatLookup();
            l.setId(id);
        }
        l.setStatus(status);
        l.setName(name);
        l.setReference(reference);
        l.setCheckedAt(now);
        if (fresh) {
            lookups.insert(l);
        } else {
            lookups.save(l);
        }
    }
}
