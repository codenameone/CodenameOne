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

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.Roles;
import com.codenameone.examples.bastion.api.AdminApiServer;
import com.codenameone.examples.bastion.api.AdjustmentDto;
import com.codenameone.examples.bastion.api.ApprovalDto;
import com.codenameone.examples.bastion.api.ClosePeriodDto;
import com.codenameone.examples.bastion.api.DecisionDto;
import com.codenameone.examples.bastion.api.PeriodDto;
import com.codenameone.examples.bastion.api.ReversalRequestDto;
import com.codenameone.examples.bastion.api.VerificationDto;
import com.codenameone.examples.bastion.account.SignInGuard;
import com.codenameone.examples.bastion.books.Approvals;
import com.codenameone.examples.bastion.books.Books;
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

    private final Books books;
    private final Approvals approvals;
    private final SignInGuard guard;
    private final PasswordEncoder encoder;

    public AdminEndpoint(Audit audit, Ledger ledger, DataSource db, Books books,
            Approvals approvals, SignInGuard guard, PasswordEncoder encoder) {
        this.books = books;
        this.approvals = approvals;
        this.guard = guard;
        this.encoder = encoder;
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

    @Override
    public PeriodDto closePeriod(ClosePeriodDto request) throws Exception {
        String admin = stepUp(request == null ? null : request.password());
        return books.close(admin, request.through());
    }

    @Override
    public ApprovalDto adjust(String idempotencyKey, AdjustmentDto request) throws Exception {
        String admin = stepUp(request == null ? null : request.password());
        return approvals.adjust(admin, idempotencyKey, request.accountId(), request.amount(),
                request.memo());
    }

    @Override
    public ApprovalDto reverse(String id, String idempotencyKey, ReversalRequestDto request)
            throws Exception {
        String admin = stepUp(request == null ? null : request.password());
        return approvals.reverse(admin, idempotencyKey, id, request.memo());
    }

    @Override
    public List<ApprovalDto> approvals() throws Exception {
        admin();
        return approvals.list();
    }

    @Override
    public ApprovalDto approve(String id, DecisionDto decision) throws Exception {
        String admin = stepUp(decision == null ? null : decision.password());
        return approvals.approve(admin, id);
    }

    @Override
    public ApprovalDto reject(String id, DecisionDto decision) throws Exception {
        String admin = stepUp(decision == null ? null : decision.password());
        return approvals.reject(admin, id, decision.reason());
    }

    /// The administrator, having typed their password again: every change
    /// to the books is a step-up, held to the sign-in lockout like any other
    /// password guess. Checked -- and a refusal audited -- before the
    /// change's own transaction, which would roll the record back with it.
    private String stepUp(String password) throws Exception {
        String admin = admin();
        guard.recheck(admin, password, encoder, "STAFF_STEP_UP_REFUSED",
                "enter-password-confirm", "Enter your password to confirm");
        return admin;
    }

    private static String admin() {
        String name = Caller.name();
        if (!Caller.hasRole(Roles.ADMIN)) {
            throw Refused.with(403, "administrators", "Administrators only");
        }
        return name;
    }
}
