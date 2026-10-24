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
package com.codenameone.examples.savor.order;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.api.SavedAddressDto;
import com.codenameone.examples.savor.domain.SavedAddress;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.SavedAddressRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The places a customer has food brought to: home, work, a friend's.
@Component
public class Addresses {
    static final int MAX = 20;

    private final SavedAddressRepository addresses;
    private final ProfileRepository profiles;

    public Addresses(SavedAddressRepository addresses, ProfileRepository profiles) {
        this.addresses = addresses;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<SavedAddressDto> list(String username) {
        List<SavedAddress> rows = addresses.findByUsernameOrderByCreatedAtAsc(username);
        List<SavedAddressDto> out = new ArrayList<SavedAddressDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Saves a new address, or changes one of the account's own when it names one.
    @Transactional
    public SavedAddressDto save(String username, SavedAddressDto form) throws IOException {
        if (form == null || !Geo.isPoint(form.lat(), form.lng())) {
            throw new ResponseStatusException(400, "Choose the place on the map");
        }
        SavedAddress row;
        if (form.id() != null && form.id().length() > 0) {
            row = own(username, form.id());
        } else {
            // Under the account's lock, which closing it takes too: an address
            // written as the account closes would outlive it, under a retired
            // name, with nobody left to remove it.
            if (profiles.hold(username) == 0) {
                throw new ResponseStatusException(404, "No such account");
            }
            if (addresses.countByUsername(username) >= MAX) {
                throw new ResponseStatusException(409, "That is enough saved addresses");
            }
            row = new SavedAddress();
            row.setId(Ids.next());
            row.setUsername(username);
            row.setCreatedAt(Instant.now());
        }
        row.setLabel(Text.required(form.label(), 40, "a name for the place"));
        row.setAddress(Text.required(form.address(), 255, "the address"));
        row.setNote(Text.optional(form.note(), 255, "the note for the courier"));
        row.setLat(form.lat());
        row.setLng(form.lng());
        if (form.id() == null || form.id().length() == 0) {
            addresses.insert(row);
        }
        return toDto(row);
    }

    @Transactional
    public SavedAddressDto remove(String username, String id) {
        SavedAddress row = own(username, id);
        addresses.deleteById(id);
        return toDto(row);
    }

    private SavedAddress own(String username, String id) {
        SavedAddress row = id == null || id.length() > 32 ? null : addresses.findById(id);
        if (row == null || !username.equals(row.getUsername())) {
            throw new ResponseStatusException(404, "No such address");
        }
        return row;
    }

    private static SavedAddressDto toDto(SavedAddress row) {
        return new SavedAddressDto(row.getId(), row.getLabel(), row.getAddress(), row.getLat(),
                row.getLng(), row.getNote());
    }
}
