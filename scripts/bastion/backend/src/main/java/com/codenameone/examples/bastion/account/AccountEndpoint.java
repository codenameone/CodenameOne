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
package com.codenameone.examples.bastion.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.api.AccountApiServer;
import com.codenameone.examples.bastion.api.PasskeyDto;
import com.codenameone.examples.bastion.api.PasswordChangeDto;
import com.codenameone.examples.bastion.api.RecoveryCodesDto;
import com.codenameone.examples.bastion.api.RegisterDto;
import com.codenameone.examples.bastion.api.TotpCodeDto;
import com.codenameone.examples.bastion.api.TotpEnrollmentDto;
import com.codenameone.examples.bastion.api.UserDto;

import java.util.List;

/// [com.codenameone.examples.bastion.api.AccountApi], served. Every call but
/// `register` acts on the caller's own account, named by their token and
/// never by the request.
@Component
public class AccountEndpoint implements AccountApiServer {
    private final Accounts accounts;
    private final SecondFactor secondFactor;
    private final PasskeyList passkeys;

    public AccountEndpoint(Accounts accounts, SecondFactor secondFactor, PasskeyList passkeys) {
        this.accounts = accounts;
        this.secondFactor = secondFactor;
        this.passkeys = passkeys;
    }

    @Override
    public UserDto register(RegisterDto request) throws Exception {
        return accounts.register(request);
    }

    @Override
    public UserDto me() throws Exception {
        return accounts.describe(Caller.name());
    }

    @Override
    public UserDto changePassword(PasswordChangeDto change) throws Exception {
        return accounts.changePassword(Caller.name(), change == null ? null : change.current(),
                change == null ? null : change.replacement());
    }

    @Override
    public TotpEnrollmentDto startTotp() throws Exception {
        return secondFactor.start(Caller.name());
    }

    @Override
    public RecoveryCodesDto confirmTotp(TotpCodeDto code) throws Exception {
        return secondFactor.confirm(Caller.name(), code == null ? null : code.code());
    }

    @Override
    public RecoveryCodesDto newRecoveryCodes(TotpCodeDto code) throws Exception {
        return secondFactor.renew(Caller.name(), code == null ? null : code.code());
    }

    @Override
    public List<PasskeyDto> passkeys() throws Exception {
        return passkeys.of(Caller.name());
    }

    @Override
    public List<PasskeyDto> removePasskey(String id) throws Exception {
        return passkeys.remove(Caller.name(), id);
    }
}
