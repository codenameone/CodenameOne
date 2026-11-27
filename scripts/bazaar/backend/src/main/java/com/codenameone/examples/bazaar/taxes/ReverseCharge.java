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
package com.codenameone.examples.bazaar.taxes;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.domain.Company;
import com.codenameone.examples.bazaar.domain.CompanyBuyer;
import com.codenameone.examples.bazaar.repository.CompanyBuyerRepository;
import com.codenameone.examples.bazaar.repository.CompanyRepository;
import com.codenameone.examples.bazaar.trade.VatIds;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/// The EU reverse charge on a cross-border sale to a business.
///
/// A store established in a member state (its tax settings' home country)
/// that sells to a business registered for VAT in **another** member state,
/// delivering to a member state other than its own, charges no VAT: the
/// buyer accounts for it (Article 196 of Directive 2006/112/EC). Here that
/// buyer is a company of the store ([com.codenameone.examples.bazaar.trade.Trade])
/// whose VAT id VIES confirmed (`verified`); a number checked on its format
/// alone, or one VIES could not be asked about, does not qualify.
///
/// It is applied as a tax exemption ([TaxExemptions#certificateFor]): the
/// order records [#PREFIX] and the VAT id as its certificate, the tax report
/// counts it among the exempt orders, and the invoice prints [#NOTE] with
/// the buyer's VAT id.
@Component
public class ReverseCharge {
    /// How an order's exemption certificate marks a reverse charge.
    public static final String PREFIX = "REVERSE-CHARGE ";
    /// The invoice's note.
    public static final String NOTE = "Reverse charge: VAT to be accounted for by the "
            + "recipient (Article 196, Directive 2006/112/EC).";

    private final DataSource db;
    private final CompanyBuyerRepository buyers;
    private final CompanyRepository companies;

    public ReverseCharge(DataSource db, CompanyBuyerRepository buyers,
            CompanyRepository companies) {
        this.db = db;
        this.buyers = buyers;
        this.companies = companies;
    }

    /// Whether a sale from `home` to a business with VAT id `vatId`
    /// (normalized, `DE123456789`), delivered to `delivery`, is reverse
    /// charged. VAT ids write Greece as `EL`.
    public static boolean applies(String home, String vatId, String delivery) {
        if (home == null || vatId == null || delivery == null || vatId.length() < 4) {
            return false;
        }
        String h = home.trim();
        String d = delivery.trim();
        String v = vatId.substring(0, 2);
        if ("EL".equalsIgnoreCase(v)) {
            v = "GR";
        }
        return h.length() == 2 && d.length() == 2 && eu(h) && eu(d) && eu(v)
                && !v.equalsIgnoreCase(h) && !d.equalsIgnoreCase(h);
    }

    private static boolean eu(String country) {
        for (int iter = 0; iter < TaxRules.EU.size(); iter++) {
            if (TaxRules.EU.get(iter).equalsIgnoreCase(country)) {
                return true;
            }
        }
        return false;
    }

    /// The certificate (`REVERSE-CHARGE <vat id>`) for `username` buying at
    /// `storeId` for delivery to `country`, or null.
    @Transactional(readOnly = true)
    public String certificateFor(String storeId, String username, String country)
            throws IOException {
        if (username == null || country == null) {
            return null;
        }
        List<CompanyBuyer> mine = buyers.findByStoreIdAndUsername(storeId, username);
        if (mine.isEmpty()) {
            return null;
        }
        Company c = companies.findById(mine.get(0).getCompanyId());
        if (c == null || !VatIds.VERIFIED.equals(c.getVatStatus())) {
            return null;
        }
        Map r = db.queryOne("SELECT home_country AS home_country FROM tax_settings "
                + "WHERE store_id = ?", new Object[] {storeId});
        String home = r == null ? "" : Rows.text(r.get("home_country"));
        String vat = VatIds.normalize(c.getVatId());
        return applies(home, vat, country) ? PREFIX + vat : null;
    }

    /// The buyer's VAT id when `certificate` is a reverse charge, else null.
    public static String vatIdOf(String certificate) {
        return certificate != null && certificate.startsWith(PREFIX)
                ? certificate.substring(PREFIX.length()) : null;
    }
}
