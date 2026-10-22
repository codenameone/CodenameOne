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
package com.codenameone.examples.bastion.money;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.account.Accounts;
import com.codenameone.examples.bastion.api.NewPayeeDto;
import com.codenameone.examples.bastion.api.PayeeDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Payee;
import com.codenameone.examples.bastion.repository.PayeeRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The people a customer may pay: other customers of this bank, by e-mail.
///
/// A payee is where stolen money goes, so adding one takes a signed request
/// from a trusted device, and a new payee is in **cooling-off** for
/// `bastion.payee.cooloff-hours`: in that time it can receive no more than
/// `bastion.payee.cooloff-limit` in all. Whoever takes over a session can add
/// themselves, but cannot empty the account into themselves before the owner
/// sees the notification and the audit trail.
@Component
public class Payees {
    static final int MAX_PAYEES = 200;

    private final PayeeRepository payees;
    private final JdbcUserDetailsManager users;
    private final Money money;
    private final Audit audit;

    public Payees(PayeeRepository payees, JdbcUserDetailsManager users, Money money,
            Audit audit) {
        this.payees = payees;
        this.users = users;
        this.money = money;
        this.audit = audit;
    }

    @Transactional(rollbackFor = Exception.class)
    public PayeeDto add(String owner, NewPayeeDto request) throws IOException {
        if (request == null) {
            throw Refused.with(400, "name-payee", "Name the payee");
        }
        String username;
        try {
            username = Accounts.email(request.email());
        } catch (ResponseStatusException malformed) {
            throw Refused.with(400, "not-e-mail-address", "That is not an e-mail address");
        }
        if (username.equals(owner)) {
            throw Refused.with(400, "move-money-between-own-accounts", "Move money between your own accounts instead");
        }
        // Saying "no such customer" tells the caller who banks here. The
        // request is signed by a registered device and audited, and every
        // payee is a row a person reviews, which makes this a poor directory.
        if (!users.userExists(username)) {
            throw Refused.with(404, "no-customer-has-address", "No customer has that address");
        }
        if (payees.findFirstByOwnerAndUsername(owner, username) != null) {
            throw Refused.with(409, "payee-already-list", "That payee is already on your list");
        }
        if (payees.findByOwnerOrderByCreatedAtAsc(owner).size() >= MAX_PAYEES) {
            throw Refused.with(409, "remove-payee-before-adding-another", "Remove a payee before adding another");
        }
        Payee payee = new Payee();
        payee.setId(Ids.next());
        payee.setOwner(owner);
        payee.setUsername(username);
        payee.setNickname(nickname(request.nickname(), username));
        payee.setCreatedAt(System.currentTimeMillis());
        payees.insert(payee);
        audit.append(owner, "PAYEE_ADDED", payee.getId(), username);
        return dto(payee);
    }

    @Transactional(readOnly = true)
    public List<PayeeDto> list(String owner) throws IOException {
        List<PayeeDto> out = new ArrayList<PayeeDto>();
        for (Payee p : payees.findByOwnerOrderByCreatedAtAsc(owner)) {
            out.add(dto(p));
        }
        return out;
    }

    /// `owner`'s payee `id`, or 404 whether it is missing or someone else's.
    @Transactional(readOnly = true)
    public Payee owned(String owner, String id) throws IOException {
        Payee payee = id == null ? null : payees.findById(id);
        if (payee == null || !payee.getOwner().equals(owner)) {
            throw Refused.with(404, "no-such-payee", "No such payee");
        }
        return payee;
    }

    @Transactional(rollbackFor = Exception.class)
    public PayeeDto rename(String owner, String id, NewPayeeDto request) throws IOException {
        Payee payee = owned(owner, id);
        // Only the nickname: pointing an existing payee at somebody else would
        // skip the new payee's cooling-off and signature.
        payee.setNickname(nickname(request == null ? null : request.nickname(),
                payee.getUsername()));
        payees.save(payee);
        audit.append(owner, "PAYEE_RENAMED", id, payee.getNickname());
        return dto(payee);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayeeDto remove(String owner, String id) throws IOException {
        Payee payee = owned(owner, id);
        payees.deleteById(id);
        audit.append(owner, "PAYEE_REMOVED", id, payee.getUsername());
        return dto(payee);
    }

    private PayeeDto dto(Payee p) {
        return new PayeeDto(p.getId(), p.getUsername(), p.getNickname(), p.getCreatedAt(),
                p.getCreatedAt() + money.coolOffMillis());
    }

    private static String nickname(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.length() == 0) {
            v = fallback;
        }
        return v.length() <= 80 ? v : v.substring(0, 80);
    }
}
