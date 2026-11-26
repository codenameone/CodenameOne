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
package com.codenameone.examples.bazaar.staff;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpEnrollment;
import com.codename1.backend.security.mfa.TotpService;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.TwoFactorApiServer;
import com.codenameone.examples.bazaar.api.TwoFactorCodeDto;
import com.codenameone.examples.bazaar.api.TwoFactorDto;

import java.util.ArrayList;
import java.util.List;

/// The app's half of two-step sign-in, `/api/account/two-factor`: the same
/// authenticator app and recovery codes as the web account's security page,
/// kept in the same tables, so a factor turned on in one is on in both.
///
/// The secret is answered by `start` alone, while the factor is not yet on,
/// and recovery codes by `confirm` and `recovery` alone; `status` shows
/// neither. Making new codes and turning the factor off ask for the
/// password, as the web page does.
@Component
public class TwoFactorEndpoint implements TwoFactorApiServer {
    private final TotpService totp;
    private final RecoveryCodeService recovery;
    private final Accounts accounts;
    private final StaffSecurity staff;
    private final DataSource db;

    public TwoFactorEndpoint(TotpService totp, RecoveryCodeService recovery, Accounts accounts,
            StaffSecurity staff, DataSource db) {
        this.db = db;
        this.totp = totp;
        this.recovery = recovery;
        this.accounts = accounts;
        this.staff = staff;
    }

    private TwoFactorDto describe(String me, String secret, String uri, List<String> codes)
            throws Exception {
        boolean on = totp.isEnabled(me);
        return new TwoFactorDto(on, secret, uri, codes == null ? new ArrayList<String>() : codes,
                on ? recovery.remaining(me) : 0, staff.requiredBy(me));
    }

    @Override
    public TwoFactorDto status() throws Exception {
        return describe(Caller.name(), "", "", null);
    }

    @Override
    public TwoFactorDto start() throws Exception {
        String me = Caller.name();
        if (totp.isEnabled(me)) {
            throw new ResponseStatusException(409, "Two-step sign-in is already on");
        }
        TotpEnrollment e = totp.beginEnrollment(me);
        return describe(me, e.getSecret(), e.getOtpauthUri(), null);
    }

    @Override
    public TwoFactorDto confirm(TwoFactorCodeDto body) throws Exception {
        String me = Caller.name();
        String code = body == null || body.code() == null ? "" : body.code().trim();
        if (totp.isEnabled(me)) {
            throw new ResponseStatusException(409, "Two-step sign-in is already on");
        }
        if (!totp.confirmEnrollment(me, code)) {
            throw new ResponseStatusException(400,
                    "That code is not right. Enter the one your app shows now.");
        }
        return describe(me, "", "", recovery.generate(me));
    }

    @Override
    public TwoFactorDto recovery(TwoFactorCodeDto body) throws Exception {
        String me = Caller.name();
        if (!totp.isEnabled(me)) {
            throw new ResponseStatusException(409, "Turn on two-step sign-in first");
        }
        password(me, body);
        return describe(me, "", "", recovery.generate(me));
    }

    @Override
    public TwoFactorDto disable(TwoFactorCodeDto body) throws Exception {
        String me = Caller.name();
        password(me, body);
        List<String> stores = staff.requiredBy(me);
        if (!stores.isEmpty()) {
            throw new ResponseStatusException(409, "A store you work at requires two-step sign-in");
        }
        totp.disable(me);
        new JdbcRecoveryCodeRepository(db).replace(me, new ArrayList<String>());
        return describe(me, "", "", null);
    }

    private void password(String me, TwoFactorCodeDto body) {
        String given = body == null ? null : body.password();
        if (given == null || !accounts.checkPassword(me, given)) {
            throw new ResponseStatusException(403, "That is not your password");
        }
    }
}
