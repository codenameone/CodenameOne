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
package com.codenameone.examples.bastion.exchange;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.CryptoAddressDto;
import com.codenameone.examples.bastion.api.NewCryptoAddressDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.CryptoAddress;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.repository.CryptoAddressRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The addresses outside the bank a customer's coins may be withdrawn to.
///
/// An allow-list, not a field on the withdrawal: whoever has stolen a token
/// and a device key can type their own address into a withdrawal in a
/// second, but adding it here starts a cooling-off period
/// (`bastion.address.cooloff-hours`, a day by default) before it can be used,
/// and the customer is told of every address added (the audit, and the app's
/// security centre). Removing one is immediate and needs no signature:
/// taking a permission away is never the dangerous direction.
///
/// The format check is deliberately loose -- letters and digits, 20 to 100 of
/// them -- because the chain is simulated. A real exchange validates each
/// chain's checksum (base58check, bech32, EIP-55) so a typo is refused before
/// coins go to an address nobody holds.
@Component
public class CryptoWallets {
    static final int MAX_ADDRESSES = 20;

    private final CryptoAddressRepository addresses;
    private final Ledger ledger;
    private final Audit audit;
    private final long coolOffMillis;

    public CryptoWallets(CryptoAddressRepository addresses, Ledger ledger, Audit audit,
            @Value("${bastion.address.cooloff-hours:24}") String coolOffHours) {
        this.addresses = addresses;
        this.ledger = ledger;
        this.audit = audit;
        this.coolOffMillis = Long.parseLong(coolOffHours.trim()) * 3600L * 1000L;
    }

    @Transactional(rollbackFor = Exception.class)
    public CryptoAddressDto add(String owner, NewCryptoAddressDto request) throws IOException {
        if (request == null || request.address() == null) {
            throw Refused.with(400, "send-address", "Send the address");
        }
        Asset asset = ledger.asset(request.asset());
        if ("FIAT".equals(asset.getKind())) {
            throw Refused.with(400, "coin-withdrawn-address", "Only a coin is withdrawn to an address");
        }
        String address = request.address().trim();
        if (!wellFormed(address)) {
            throw Refused.with(400, "not-address", "That is not an address");
        }
        int live = 0;
        for (CryptoAddress a : addresses.findByOwnerOrderByCreatedAtAsc(owner)) {
            if (!a.isRemoved()) {
                live++;
                if (a.getAsset().equals(asset.getCode()) && a.getAddress().equals(address)) {
                    throw Refused.with(409, "address-already-list", "That address is already on your list");
                }
            }
        }
        if (live >= MAX_ADDRESSES) {
            throw Refused.with(409, "remove-address-before-adding-another", "Remove an address before adding another");
        }
        long now = System.currentTimeMillis();
        CryptoAddress a = new CryptoAddress();
        a.setId(Ids.next());
        a.setOwner(owner);
        a.setAsset(asset.getCode());
        a.setAddress(address);
        String label = request.label() == null ? "" : request.label();
        a.setLabel(label.length() > 80 ? label.substring(0, 80) : label);
        a.setUsableAt(now + coolOffMillis);
        a.setCreatedAt(now);
        addresses.insert(a);
        audit.append(owner, "ADDRESS_ADDED", a.getId(), asset.getCode() + " " + address
                + " usable at " + a.getUsableAt());
        return dto(a);
    }

    @Transactional(rollbackFor = Exception.class)
    public CryptoAddressDto remove(String owner, String id) throws IOException {
        CryptoAddress a = owned(owner, id);
        a.setRemoved(true);
        addresses.save(a);
        audit.append(owner, "ADDRESS_REMOVED", a.getId(), a.getAsset() + " " + a.getAddress());
        return dto(a);
    }

    @Transactional(readOnly = true)
    public List<CryptoAddressDto> list(String owner) throws IOException {
        List<CryptoAddressDto> out = new ArrayList<CryptoAddressDto>();
        for (CryptoAddress a : addresses.findByOwnerOrderByCreatedAtAsc(owner)) {
            if (!a.isRemoved()) {
                out.add(dto(a));
            }
        }
        return out;
    }

    /// `owner`'s live address `id`, or 404 -- whether it does not exist, was
    /// removed, or is someone else's.
    @Transactional(readOnly = true)
    public CryptoAddress owned(String owner, String id) throws IOException {
        CryptoAddress a = id == null ? null : addresses.findById(id);
        if (a == null || a.isRemoved() || !a.getOwner().equals(owner)) {
            throw Refused.with(404, "no-such-address", "No such address");
        }
        return a;
    }

    static boolean wellFormed(String address) {
        if (address.length() < 20 || address.length() > 100) {
            return false;
        }
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    static CryptoAddressDto dto(CryptoAddress a) {
        return new CryptoAddressDto(a.getId(), a.getAsset(), a.getAddress(), a.getLabel(),
                a.getUsableAt(), a.getCreatedAt());
    }
}
