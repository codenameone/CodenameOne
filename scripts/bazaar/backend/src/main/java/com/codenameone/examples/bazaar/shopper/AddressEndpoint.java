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
package com.codenameone.examples.bazaar.shopper;

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AddressApiServer;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.SavedAddressDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;

import java.util.ArrayList;
import java.util.List;

/// A shopper's saved addresses, kept by account rather than by store so
/// every store's checkout offers them. Exactly one is the default while any
/// exist: the first saved, until another is chosen; deleting the default
/// makes the oldest remaining one the default.
@Component
public class AddressEndpoint implements AddressApiServer {
    static final int MAX_ADDRESSES = 20;
    private final CustomerAddressRepository rows;

    public AddressEndpoint(CustomerAddressRepository rows) {
        this.rows = rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedAddressDto> list() throws Exception {
        return listOf(Caller.name());
    }

    @Override
    @Transactional
    public SavedAddressDto add(SavedAddressDto address) throws Exception {
        String user = Caller.name();
        List<CustomerAddress> mine = rows.findByUsername(user);
        if (mine.size() >= MAX_ADDRESSES) {
            throw new ResponseStatusException(409, "At most " + MAX_ADDRESSES + " addresses");
        }
        CustomerAddress a = new CustomerAddress();
        a.setId(Ids.next());
        a.setUsername(user);
        fill(a, address);
        boolean wantsDefault = mine.isEmpty() || address.isDefault();
        if (wantsDefault) {
            clearDefault(mine);
        }
        a.setIsDefault(wantsDefault);
        rows.insert(a);
        return dto(a);
    }

    @Override
    @Transactional
    public SavedAddressDto update(String id, SavedAddressDto address) throws Exception {
        String user = Caller.name();
        CustomerAddress a = mine(user, id);
        fill(a, address);
        if (address.isDefault() && !a.isIsDefault()) {
            clearDefault(rows.findByUsername(user));
            a.setIsDefault(true);
        }
        rows.save(a);
        return dto(a);
    }

    @Override
    @Transactional
    public List<SavedAddressDto> remove(String id) throws Exception {
        String user = Caller.name();
        CustomerAddress a = mine(user, id);
        rows.delete(a);
        if (a.isIsDefault()) {
            List<CustomerAddress> rest = rows.findByUsername(user);
            if (!rest.isEmpty()) {
                CustomerAddress next = rest.get(0);
                next.setIsDefault(true);
                rows.save(next);
            }
        }
        return listOf(user);
    }

    @Override
    @Transactional
    public List<SavedAddressDto> makeDefault(String id) throws Exception {
        String user = Caller.name();
        CustomerAddress a = mine(user, id);
        clearDefault(rows.findByUsername(user));
        a.setIsDefault(true);
        rows.save(a);
        return listOf(user);
    }

    private CustomerAddress mine(String user, String id) {
        CustomerAddress a = id == null ? null : rows.findById(id);
        if (a == null || !user.equals(a.getUsername())) {
            throw new ResponseStatusException(404, "No such address");
        }
        return a;
    }

    private void clearDefault(List<CustomerAddress> all) {
        for (int iter = 0; iter < all.size(); iter++) {
            CustomerAddress c = all.get(iter);
            if (c.isIsDefault()) {
                c.setIsDefault(false);
                rows.save(c);
            }
        }
    }

    private static void fill(CustomerAddress a, SavedAddressDto wanted) {
        AddressDto d = wanted == null ? null : wanted.address();
        if (d == null) {
            throw new ResponseStatusException(400, "Give the address");
        }
        a.setName(Text.required(d.name(), 120, "the name"));
        a.setLine1(Text.required(d.line1(), 200, "the street"));
        a.setLine2(Text.optional(d.line2(), 200, "the second line"));
        a.setCity(Text.required(d.city(), 120, "the city"));
        a.setRegion(Text.optional(d.region(), 80, "the region"));
        a.setPostal(Text.optional(d.postal(), 20, "the postal code"));
        String country = Codes.normalize(d.country());
        if (country.length() != 2) {
            throw new ResponseStatusException(400, "The country is a two letter code");
        }
        a.setCountry(country);
        a.setPhone(Text.optional(d.phone(), 32, "the phone"));
        a.setLat(d.lat());
        a.setLng(d.lng());
    }

    private List<SavedAddressDto> listOf(String user) {
        List<CustomerAddress> all = rows.findByUsername(user);
        List<SavedAddressDto> out = new ArrayList<SavedAddressDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(dto(all.get(iter)));
        }
        return out;
    }

    static SavedAddressDto dto(CustomerAddress a) {
        return new SavedAddressDto(a.getId(), new AddressDto(a.getName(), a.getLine1(),
                a.getLine2(), a.getCity(), a.getRegion(), a.getPostal(), a.getCountry(),
                a.getPhone(), a.getLat(), a.getLng()), a.isIsDefault());
    }
}
