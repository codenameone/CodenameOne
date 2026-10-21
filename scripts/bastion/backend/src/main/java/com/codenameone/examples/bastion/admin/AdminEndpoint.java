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
package com.codenameone.examples.bastion.admin;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.Roles;
import com.codenameone.examples.bastion.api.AdminApiServer;
import com.codenameone.examples.bastion.api.VerificationDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.ledger.Ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// [com.codenameone.examples.bastion.api.AdminApi], served.
///
/// `/api/admin/**` is already refused to anyone without ADMIN by the API
/// chain's rules. Each call asks again here: a rule is one line in another
/// file, and a route moved out from under it would otherwise be open to every
/// customer.
@Component
public class AdminEndpoint implements AdminApiServer {
    private final Audit audit;
    private final Ledger ledger;
    private final DataSource db;

    public AdminEndpoint(Audit audit, Ledger ledger, DataSource db) {
        this.audit = audit;
        this.ledger = ledger;
        this.db = db;
    }

    @Override
    public VerificationDto verifyAudit() throws Exception {
        String admin = admin();
        List<String> problems = audit.verify();
        // Who looked, and what they saw, is itself on the record. Appended
        // after the walk so the check is of the chain as it was asked about.
        audit.append(admin, "AUDIT_VERIFIED", "audit", problems.isEmpty() ? "intact"
                : problems.size() + " problems");
        return new VerificationDto(problems.isEmpty(), problems);
    }

    @Override
    public VerificationDto verifyLedger() throws Exception {
        String admin = admin();
        List rows = db.query("SELECT DISTINCT owner FROM ledger_account", new Object[0]);
        List<String> owners = new ArrayList<String>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Object row = rows.get(iter);
            Object owner = row instanceof Map ? ((Map) row).get("owner") : null;
            if (owner != null) {
                owners.add(String.valueOf(owner));
            }
        }
        List<String> problems = ledger.verify(owners);
        audit.append(admin, "LEDGER_VERIFIED", "ledger", problems.isEmpty() ? "balanced"
                : problems.size() + " problems");
        return new VerificationDto(problems.isEmpty(), problems);
    }

    private static String admin() {
        String name = Caller.name();
        if (!Caller.hasRole(Roles.ADMIN)) {
            throw new ResponseStatusException(403, "Administrators only");
        }
        return name;
    }
}
