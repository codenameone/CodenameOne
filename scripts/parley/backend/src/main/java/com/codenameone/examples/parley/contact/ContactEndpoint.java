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
package com.codenameone.examples.parley.contact;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.ContactApiServer;
import com.codenameone.examples.parley.api.ContactDto;
import com.codenameone.examples.parley.api.ReportDto;
import com.codenameone.examples.parley.api.ReportViewDto;

import java.util.List;

/// The server's half of `ContactApi`.
@Component
public class ContactEndpoint implements ContactApiServer {
    private final Contacts contacts;

    public ContactEndpoint(Contacts contacts) {
        this.contacts = contacts;
    }

    @Override
    public List<ContactDto> search(String query) throws Exception {
        return contacts.search(Caller.name(), query);
    }

    @Override
    public ContactDto contact(String username) throws Exception {
        return contacts.contact(Caller.name(), username);
    }

    @Override
    public List<ContactDto> blocked() throws Exception {
        return contacts.blocked(Caller.name());
    }

    @Override
    public ContactDto block(String username) throws Exception {
        return contacts.block(Caller.name(), username);
    }

    @Override
    public ContactDto unblock(String username) throws Exception {
        return contacts.unblock(Caller.name(), username);
    }

    @Override
    public ReportViewDto report(ReportDto report) throws Exception {
        return contacts.report(Caller.name(), report);
    }
}
