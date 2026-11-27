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
package com.codenameone.examples.bazaar.club;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.ClubApiServer;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubItemDto;
import com.codenameone.examples.bazaar.api.ClubMembershipDto;
import com.codenameone.examples.bazaar.api.JoinClubDto;
import com.codenameone.examples.bazaar.cart.Codes;

import java.util.List;

/// The server's half of `ClubApi`.
@Component
public class ClubEndpoint implements ClubApiServer {
    private final Clubs clubs;

    public ClubEndpoint(Clubs clubs) {
        this.clubs = clubs;
    }

    @Override
    public List<ClubDto> list(String storeId) throws Exception {
        return clubs.list(Caller.name(), storeId);
    }

    @Override
    public ClubDto create(String storeId, ClubDto body) throws Exception {
        return clubs.save(Caller.name(), storeId, null, body);
    }

    @Override
    public ClubDto save(String storeId, String clubId, ClubDto body) throws Exception {
        return clubs.save(Caller.name(), storeId, clubId, body);
    }

    @Override
    public List<ClubMembershipDto> members(String storeId, String clubId) throws Exception {
        return clubs.members(Caller.name(), storeId, clubId);
    }

    @Override
    public List<ClubItemDto> items(String storeId, String clubId) throws Exception {
        return clubs.items(Caller.name(), storeId, clubId);
    }

    @Override
    public List<ClubItemDto> addItem(String storeId, String clubId, ClubItemDto body)
            throws Exception {
        return clubs.addItem(Caller.name(), storeId, clubId, body);
    }

    @Override
    public List<ClubItemDto> removeItem(String storeId, String clubId, String itemId)
            throws Exception {
        return clubs.removeItem(Caller.name(), storeId, clubId, itemId);
    }

    @Override
    public List<ClubDto> storeClubs(String storeId) throws Exception {
        return clubs.storeClubs(storeId);
    }

    @Override
    public List<ClubMembershipDto> mine(String storeId) throws Exception {
        return clubs.mine(Codes.callerOrNull(), storeId);
    }

    @Override
    public ClubMembershipDto join(String storeId, String clubId, JoinClubDto body)
            throws Exception {
        return clubs.join(Codes.callerOrNull(), storeId, clubId,
                body == null ? null : body.paymentMethodId());
    }

    @Override
    public ClubMembershipDto leave(String storeId, String clubId) throws Exception {
        return clubs.leave(Codes.callerOrNull(), storeId, clubId);
    }
}
