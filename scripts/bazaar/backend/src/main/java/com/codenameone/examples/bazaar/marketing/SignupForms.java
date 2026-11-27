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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.mail.Mail;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.FormResultDto;
import com.codenameone.examples.bazaar.api.FormSubmitDto;
import com.codenameone.examples.bazaar.api.SignupFormDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.SignupEntry;
import com.codenameone.examples.bazaar.domain.SignupForm;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.SignupEntryRepository;
import com.codenameone.examples.bazaar.repository.SignupFormRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Newsletter signup forms. A submission is checked against a honeypot
/// field (a bot fills it; the bot is told it worked and nothing is kept),
/// a per-form rate limit, and duplicates. With double opt-in the address
/// gets a confirmation link first; once confirmed it is subscribed and,
/// when the form offers one, sent a single-use welcome code.
@Component
public class SignupForms {
    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    /// Submissions one form takes in a minute before it answers 429.
    public static final int PER_MINUTE = 30;
    static final String[] FIELDS = {"email", "phone", "name"};

    private final SignupFormRepository forms;
    private final SignupEntryRepository entries;
    private final DiscountRepository discounts;
    private final Campaigns campaigns;
    private final Emails emails;
    private final SiteUrls urls;
    private final StoreAccess access;
    private final Shoppers shoppers;

    public SignupForms(SignupFormRepository forms, SignupEntryRepository entries,
            DiscountRepository discounts, Campaigns campaigns, Emails emails, SiteUrls urls,
            StoreAccess access, Shoppers shoppers) {
        this.forms = forms;
        this.entries = entries;
        this.discounts = discounts;
        this.campaigns = campaigns;
        this.emails = emails;
        this.urls = urls;
        this.access = access;
        this.shoppers = shoppers;
    }

    @Transactional(readOnly = true)
    public List<SignupFormDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<SignupForm> rows = forms.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<SignupFormDto> out = new ArrayList<SignupFormDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), true));
        }
        return out;
    }

    SignupFormDto dto(SignupForm f, boolean merchant) {
        List<String> fields = new ArrayList<String>();
        String all = f.getFields();
        int start = 0;
        while (start <= all.length() && all.length() > 0) {
            int comma = all.indexOf(',', start);
            int end = comma < 0 ? all.length() : comma;
            String one = all.substring(start, end).trim();
            if (one.length() > 0) {
                fields.add(one);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return new SignupFormDto(f.getId(), f.getName(), f.getHeadline(), fields, f.getRewardBps(),
                f.isDoubleOptIn(), f.isActive(), f.getHoneypot(),
                merchant ? entries.countByFormId(f.getId()) : 0);
    }

    SignupForm form(String storeId, String formId) {
        SignupForm f = formId == null ? null : forms.findById(formId);
        if (f == null || !storeId.equals(f.getStoreId())) {
            throw new ResponseStatusException(404, "No such form");
        }
        return f;
    }

    @Transactional(rollbackFor = IOException.class)
    public SignupFormDto save(String username, String storeId, String formId, SignupFormDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return write(storeId, formId, in);
    }

    public SignupFormDto write(String storeId, String formId, SignupFormDto in) throws IOException {
        if (in == null || Codes.cut(in.name(), 80).length() == 0) {
            throw new ResponseStatusException(400, "Name the form");
        }
        if (in.rewardBps() < 0 || in.rewardBps() > 5000) {
            throw new ResponseStatusException(400, "A welcome discount is 0 to 50 percent");
        }
        StringBuilder fields = new StringBuilder("email");
        List<String> wanted = in.fields() == null ? new ArrayList<String>() : in.fields();
        for (int iter = 0; iter < wanted.size(); iter++) {
            String w = wanted.get(iter);
            boolean known = false;
            for (int k = 0; k < FIELDS.length; k++) {
                known = known || FIELDS[k].equals(w);
            }
            if (!known) {
                throw new ResponseStatusException(400, "Fields are email, phone and name");
            }
            if (!"email".equals(w) && fields.indexOf(w) < 0) {
                fields.append(',').append(w);
            }
        }
        SignupForm f = formId == null ? null : form(storeId, formId);
        boolean fresh = f == null;
        if (fresh) {
            f = new SignupForm();
            f.setId(Ids.next());
            f.setStoreId(storeId);
            f.setCreatedAt(Instant.now());
            // The trap field's name: a bot fills what looks like a field.
            f.setHoneypot("website");
        }
        f.setName(Codes.cut(in.name(), 80));
        f.setHeadline(Codes.cut(in.headline(), 200));
        f.setFields(fields.toString());
        f.setRewardBps(in.rewardBps());
        f.setDoubleOptIn(in.doubleOptIn());
        f.setActive(in.active());
        if (fresh) {
            forms.insert(f);
        } else {
            forms.save(f);
        }
        return dto(f, true);
    }

    @Transactional(readOnly = true)
    public SignupFormDto publicForm(String storeId, String formId) {
        shoppers.store(storeId);
        SignupForm f = form(storeId, formId);
        if (!f.isActive()) {
            throw new ResponseStatusException(404, "No such form");
        }
        return dto(f, false);
    }

    /// A submission. A filled trap is answered like a success and dropped.
    public FormResultDto submit(String storeId, String formId, FormSubmitDto in, Instant now)
            throws IOException {
        shoppers.store(storeId);
        SignupForm f = form(storeId, formId);
        if (!f.isActive()) {
            throw new ResponseStatusException(404, "No such form");
        }
        if (in == null) {
            throw new ResponseStatusException(400, "Give an e-mail address");
        }
        if (in.trap() != null && in.trap().length() > 0) {
            return new FormResultDto(f.isDoubleOptIn() ? PENDING : CONFIRMED, "",
                    f.isDoubleOptIn() ? "Check your inbox" : "Thanks for subscribing");
        }
        String email = Tokens.fold(in.email());
        if (!Mail.isAddress(email) || email.length() > 190) {
            throw new ResponseStatusException(400, "Give an e-mail address");
        }
        allow(f.getId(), now);
        SignupEntry e = record(f, email, in, now);
        if (e == null) {
            return new FormResultDto(CONFIRMED, "", "You are already subscribed");
        }
        if (f.isDoubleOptIn()) {
            emails.confirmSignup(storeId, email, urls.absolute(null, "/api/shop/m/confirm/"
                    + e.getConfirmToken()));
            return new FormResultDto(PENDING, "", "Check your inbox to confirm");
        }
        String code = confirm(e.getConfirmToken(), now);
        return new FormResultDto(CONFIRMED, code == null ? "" : code, "Thanks for subscribing");
    }

    /// The rate-limit hook: [#PER_MINUTE] submissions a minute per form.
    /// Counted from the stored entries, so it holds across every server.
    @Transactional(readOnly = true)
    void allow(String formId, Instant now) {
        List<SignupEntry> recent = entries.findByFormIdOrderByCreatedAtDesc(formId);
        int n = 0;
        long since = now.toEpochMilli() - 60000L;
        for (int iter = 0; iter < recent.size() && iter <= PER_MINUTE; iter++) {
            if (recent.get(iter).getCreatedAt().toEpochMilli() >= since) {
                n++;
            }
        }
        if (n >= PER_MINUTE) {
            throw new ResponseStatusException(429, "Too many signups just now; try again in a minute");
        }
    }

    /// The entry for the address, or null when it is already confirmed. A
    /// pending one is kept, so a second submission sends its link again.
    @Transactional(rollbackFor = IOException.class)
    SignupEntry record(SignupForm f, String email, FormSubmitDto in, Instant now) throws IOException {
        List<SignupEntry> same = entries.findByFormIdAndEmail(f.getId(), email);
        for (int iter = 0; iter < same.size(); iter++) {
            if (CONFIRMED.equals(same.get(iter).getStatus())) {
                return null;
            }
            return same.get(iter);
        }
        SignupEntry e = new SignupEntry();
        e.setId(Ids.next());
        e.setStoreId(f.getStoreId());
        e.setFormId(f.getId());
        e.setEmail(email);
        e.setPhone(Codes.cut(Tokens.digits(in.phone()), 32));
        e.setData(Codes.cut(in.name(), 120));
        e.setConfirmToken(Tokens.token());
        e.setStatus(PENDING);
        e.setCreatedAt(now);
        entries.insert(e);
        return e;
    }

    /// Confirms an entry by its token: subscribes the address and issues the
    /// welcome code. Answers the code (empty when the form has none), or null
    /// for a token that is unknown or already used.
    @Transactional(rollbackFor = IOException.class)
    public String confirm(String token, Instant now) throws IOException {
        SignupEntry e = token == null ? null : entries.findByConfirmToken(token);
        if (e == null || !PENDING.equals(e.getStatus())) {
            return null;
        }
        SignupForm f = forms.findById(e.getFormId());
        e.setStatus(CONFIRMED);
        e.setConfirmedAt(now);
        String code = "";
        if (f != null && f.getRewardBps() > 0) {
            Discount d = new Discount();
            d.setId(Ids.next());
            d.setStoreId(e.getStoreId());
            code = "HELLO" + Tokens.code(7);
            d.setCode(code);
            d.setTitle("Welcome " + (f.getRewardBps() / 100) + "% off");
            d.setKind(DiscountEngine.PERCENT);
            d.setAmount(f.getRewardBps());
            d.setAppliesTo(DiscountEngine.ALL);
            d.setUsageLimit(1);
            d.setCombinable(false);
            d.setStartsAt(now);
            d.setEndsAt(now.plusMillis(30L * 24 * 3600 * 1000));
            d.setActive(true);
            d.setCreatedAt(now);
            discounts.insert(d);
            e.setDiscountCode(code);
        }
        entries.save(e);
        campaigns.setConsent(e.getStoreId(), e.getEmail(), Campaigns.EMAIL, Campaigns.SUBSCRIBED,
                "form:" + e.getFormId());
        if (code.length() > 0) {
            try {
                emails.signupReward(e.getStoreId(), e.getEmail(), code);
            } catch (IOException | RuntimeException failed) {
                // The code is on the entry and was answered to the form.
                System.err.println("signup reward mail: " + failed);
            }
        }
        return code;
    }
}
