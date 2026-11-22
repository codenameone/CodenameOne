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
package com.codenameone.examples.bazaar.content;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.MetafieldDefDto;
import com.codenameone.examples.bazaar.api.MetafieldValueDto;
import com.codenameone.examples.bazaar.api.MetaobjectEntryDto;
import com.codenameone.examples.bazaar.api.MetaobjectTypeDto;
import com.codenameone.examples.bazaar.domain.MetafieldDef;
import com.codenameone.examples.bazaar.domain.MetafieldValue;
import com.codenameone.examples.bazaar.domain.MetaobjectEntry;
import com.codenameone.examples.bazaar.domain.MetaobjectType;
import com.codenameone.examples.bazaar.repository.MetafieldDefRepository;
import com.codenameone.examples.bazaar.repository.MetafieldValueRepository;
import com.codenameone.examples.bazaar.repository.MetaobjectEntryRepository;
import com.codenameone.examples.bazaar.repository.MetaobjectTypeRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Merchant-defined fields and objects.
///
/// A **metafield** is a typed field a store adds to its products, variants,
/// collections, customers or orders: text, a number, money (cents), a date
/// (`yyyy-mm-dd`), yes/no, or a reference to an entry of one of the store's
/// own **metaobject** types (a "Material" type with entries "Wool",
/// "Linen"). Values are checked against the type when they are written and
/// stored as text plus a number, so they sort and compare without parsing
/// again. A field marked `storefront` is shown in the product page's specs.
///
/// Every row carries its store, and every lookup checks it: an id from
/// another store is 404 whatever kind of row it names. The owner a value is
/// written against is checked by kind through [MetafieldOwners].
@Component
public class Metafields {
    public static final String[] OWNERS = {"PRODUCT", "VARIANT", "COLLECTION", "CUSTOMER", "ORDER"};
    public static final String[] TYPES = {"text", "number", "money", "date", "boolean", "reference"};
    static final int MAX_FIELDS = 100;

    private final MetafieldDefRepository defs;
    private final MetafieldValueRepository values;
    private final MetaobjectTypeRepository types;
    private final MetaobjectEntryRepository entries;
    private final StoreAccess access;
    private final MetafieldOwners owners;
    private final com.codenameone.examples.bazaar.market.Translations translations;

    public Metafields(MetafieldDefRepository defs, MetafieldValueRepository values,
            MetaobjectTypeRepository types, MetaobjectEntryRepository entries,
            StoreAccess access, MetafieldOwners owners, com.codenameone.examples.bazaar.market.Translations translations) {
        this.translations = translations;
        this.defs = defs;
        this.values = values;
        this.types = types;
        this.entries = entries;
        this.access = access;
        this.owners = owners;
    }

    // ============================================================ definitions

    @Transactional(readOnly = true)
    public List<MetafieldDefDto> definitions(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<MetafieldDef> rows = defs.findByStoreIdOrderByPositionAsc(storeId);
        List<MetafieldDefDto> out = new ArrayList<MetafieldDefDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public MetafieldDefDto define(String username, String storeId, MetafieldDefDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what the field is");
        }
        String owner = choose(in.ownerKind(), OWNERS, "owner");
        String type = choose(in.type(), TYPES, "type");
        String name = Lists.cut(in.name(), 120);
        if (name.length() == 0) {
            throw new ResponseStatusException(400, "Give the field a name");
        }
        String key = Lists.handle(in.key(), name, 60);
        if (defs.findByStoreIdAndOwnerKindAndFieldKey(storeId, owner, key) != null) {
            throw new ResponseStatusException(409, "That field exists already");
        }
        List<MetafieldDef> all = defs.findByStoreIdOrderByPositionAsc(storeId);
        if (all.size() >= MAX_FIELDS) {
            throw new ResponseStatusException(400, "A store has at most " + MAX_FIELDS + " fields");
        }
        String refKind = "";
        if ("reference".equals(type)) {
            MetaobjectType t = type(storeId, in.refKind());
            refKind = t.getId();
        }
        MetafieldDef row = new MetafieldDef();
        row.setId(Ids.next());
        row.setStoreId(storeId);
        row.setOwnerKind(owner);
        row.setFieldKey(key);
        row.setName(name);
        row.setFieldType(type);
        row.setRefKind(refKind);
        row.setStorefront(in.storefront());
        row.setPosition(all.size());
        row.setCreatedAt(Instant.now());
        defs.insert(row);
        return dto(row);
    }

    /// Removes a field and every value of it.
    @Transactional(rollbackFor = IOException.class)
    public void undefine(String username, String storeId, String fieldId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        MetafieldDef d = def(storeId, fieldId);
        List<MetafieldValue> vs = values.findByDefId(d.getId());
        for (int iter = 0; iter < vs.size(); iter++) {
            values.delete(vs.get(iter));
        }
        defs.delete(d);
        translations.forget(storeId, com.codenameone.examples.bazaar.market.Translations.METAFIELD, d.getId());
    }

    // ================================================================= values

    /// Every field of `ownerKind` with its value on `ownerId` (empty when unset).
    @Transactional(readOnly = true)
    public List<MetafieldValueDto> valuesOf(String username, String storeId, String ownerKind,
            String ownerId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String owner = choose(ownerKind, OWNERS, "owner");
        requireOwner(storeId, owner, ownerId);
        return fields(storeId, owner, ownerId, false);
    }

    /// Sets (or, with an empty value, clears) one field on one owner.
    @Transactional(rollbackFor = IOException.class)
    public List<MetafieldValueDto> put(String username, String storeId, MetafieldValueDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say which field to set");
        }
        MetafieldDef d = def(storeId, in.defId());
        requireOwner(storeId, d.getOwnerKind(), in.ownerId());
        MetafieldValue row = values.findByDefIdAndOwnerId(d.getId(), in.ownerId());
        String raw = in.value() == null ? "" : in.value().trim();
        if (raw.length() == 0) {
            if (row != null) {
                values.delete(row);
            }
            return fields(storeId, d.getOwnerKind(), in.ownerId(), false);
        }
        boolean fresh = row == null;
        if (fresh) {
            row = new MetafieldValue();
            row.setId(Ids.next());
            row.setStoreId(storeId);
            row.setDefId(d.getId());
            row.setOwnerId(in.ownerId());
        }
        coerce(storeId, d, raw, row);
        row.setUpdatedAt(Instant.now());
        if (fresh) {
            values.insert(row);
        } else {
            values.save(row);
        }
        return fields(storeId, d.getOwnerKind(), in.ownerId(), false);
    }

    /// The storefront fields set on a product, for its specs section.
    @Transactional(readOnly = true)
    public List<MetafieldValueDto> storefront(String storeId, String ownerKind, String ownerId)
            throws IOException {
        List<MetafieldValueDto> all = fields(storeId, ownerKind, ownerId, true);
        List<MetafieldValueDto> out = new ArrayList<MetafieldValueDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).value().length() > 0) {
                out.add(all.get(iter));
            }
        }
        return out;
    }

    /// [#storefront] in `locale`: each field's name and each reference's
    /// entry title translated where the store has a translation (the exact
    /// locale, then its language), else as written. A null locale is the
    /// original.
    @Transactional(readOnly = true)
    public List<MetafieldValueDto> storefront(String storeId, String ownerKind, String ownerId,
            String locale) throws IOException {
        List<MetafieldValueDto> plain = storefront(storeId, ownerKind, ownerId);
        if (locale == null || plain.isEmpty()) {
            return plain;
        }
        java.util.Map<String, java.util.Map<String, String>> names = translations.allOf(storeId,
                com.codenameone.examples.bazaar.market.Translations.METAFIELD, locale);
        java.util.Map<String, java.util.Map<String, String>> titles = translations.allOf(storeId,
                com.codenameone.examples.bazaar.market.Translations.METAOBJECT_ENTRY, locale);
        java.util.Map<String, java.util.Map<String, String>> texts = translations.allOf(storeId,
                com.codenameone.examples.bazaar.market.Translations.METAFIELD_VALUE, locale);
        List<MetafieldValue> vs = values.findByStoreIdAndOwnerId(storeId, ownerId);
        List<MetafieldValueDto> out = new ArrayList<MetafieldValueDto>();
        for (int iter = 0; iter < plain.size(); iter++) {
            MetafieldValueDto f = plain.get(iter);
            String name = pickText(names.get(f.defId()), "name", f.name());
            String value = f.value();
            if ("text".equals(f.type())) {
                // The words of a text field translate as the field's own row.
                for (int v = 0; v < vs.size(); v++) {
                    if (vs.get(v).getDefId().equals(f.defId())) {
                        value = pickText(texts.get(vs.get(v).getId()), "value", value);
                    }
                }
            } else if ("reference".equals(f.type())) {
                for (int v = 0; v < vs.size(); v++) {
                    if (vs.get(v).getDefId().equals(f.defId())) {
                        value = pickText(titles.get(vs.get(v).getTextValue()), "title", value);
                    }
                }
            }
            out.add(new MetafieldValueDto(f.defId(), f.ownerKind(), f.ownerId(), f.key(),
                    name, f.type(), value));
        }
        return out;
    }

    private static String pickText(java.util.Map<String, String> tr, String field,
            String original) {
        String t = tr == null ? null : tr.get(field);
        return t == null || t.length() == 0 ? original : t;
    }

    private List<MetafieldValueDto> fields(String storeId, String owner, String ownerId,
            boolean storefrontOnly) throws IOException {
        List<MetafieldDef> ds = defs.findByStoreIdAndOwnerKindOrderByPositionAsc(storeId, owner);
        List<MetafieldValue> vs = values.findByStoreIdAndOwnerId(storeId, ownerId);
        List<MetafieldValueDto> out = new ArrayList<MetafieldValueDto>();
        for (int iter = 0; iter < ds.size(); iter++) {
            MetafieldDef d = ds.get(iter);
            if (storefrontOnly && !d.isStorefront()) {
                continue;
            }
            String shown = "";
            for (int v = 0; v < vs.size(); v++) {
                if (vs.get(v).getDefId().equals(d.getId())) {
                    shown = display(d, vs.get(v));
                }
            }
            out.add(new MetafieldValueDto(d.getId(), owner, ownerId, d.getFieldKey(), d.getName(),
                    d.getFieldType(), shown));
        }
        return out;
    }

    /// Checks `raw` against the field's type and stores it on `row`; 400 when it does not fit.
    void coerce(String storeId, MetafieldDef d, String raw, MetafieldValue row)
            throws IOException {
        String type = d.getFieldType();
        long number = 0;
        String text = raw;
        if ("number".equals(type)) {
            number = whole(raw, "a whole number");
            text = Long.toString(number);
        } else if ("money".equals(type)) {
            number = cents(raw);
            text = Long.toString(number);
        } else if ("date".equals(type)) {
            number = date(raw);
            text = raw;
        } else if ("boolean".equals(type)) {
            if ("true".equals(raw) || "yes".equals(raw) || "on".equals(raw) || "1".equals(raw)) {
                number = 1;
                text = "true";
            } else if ("false".equals(raw) || "no".equals(raw) || "0".equals(raw)) {
                number = 0;
                text = "false";
            } else {
                throw new ResponseStatusException(400, d.getName() + " is yes or no");
            }
        } else if ("reference".equals(type)) {
            MetaobjectEntry e = raw.length() > 32 ? null : entries.findById(raw);
            if (e == null || !storeId.equals(e.getStoreId()) || !e.getTypeId().equals(d.getRefKind())) {
                throw new ResponseStatusException(400, d.getName() + " names an entry it cannot");
            }
            text = e.getId();
        } else {
            text = Lists.cut(RichText.collapse(raw), 1000);
        }
        row.setTextValue(text);
        row.setNumberValue(number);
    }

    private String display(MetafieldDef d, MetafieldValue v) throws IOException {
        String type = d.getFieldType();
        if ("money".equals(type)) {
            long c = v.getNumberValue();
            long abs = Math.abs(c);
            long frac = abs % 100;
            return (c < 0 ? "-" : "") + abs / 100 + "." + (frac < 10 ? "0" : "") + frac;
        }
        if ("boolean".equals(type)) {
            return v.getNumberValue() == 1 ? "Yes" : "No";
        }
        if ("reference".equals(type)) {
            MetaobjectEntry e = entries.findById(v.getTextValue());
            return e == null ? "" : e.getTitle();
        }
        return v.getTextValue() == null ? "" : v.getTextValue();
    }

    static long whole(String raw, String what) {
        int i = raw.startsWith("-") ? 1 : 0;
        if (raw.length() == i || raw.length() > 15) {
            throw new ResponseStatusException(400, "That is not " + what);
        }
        long out = 0;
        for (; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < '0' || c > '9') {
                throw new ResponseStatusException(400, "That is not " + what);
            }
            out = out * 10 + (c - '0');
        }
        return raw.startsWith("-") ? -out : out;
    }

    /// `12`, `12.5` or `12.50` as cents.
    static long cents(String raw) {
        int dot = raw.indexOf('.');
        if (dot < 0) {
            return Math.multiplyExact(whole(raw, "an amount"), 100L);
        }
        String frac = raw.substring(dot + 1);
        if (frac.length() == 0 || frac.length() > 2 || frac.startsWith("-")) {
            throw new ResponseStatusException(400, "That is not an amount");
        }
        String head = raw.substring(0, dot);
        boolean negative = head.startsWith("-");
        long units = head.length() == 0 || "-".equals(head) ? 0 : whole(head, "an amount");
        long f = whole(frac, "an amount") * (frac.length() == 1 ? 10 : 1);
        long abs = Math.addExact(Math.multiplyExact(Math.abs(units), 100L), f);
        return negative ? -abs : abs;
    }

    /// `yyyy-mm-dd` as the number yyyymmdd.
    static long date(String raw) {
        if (raw.length() != 10 || raw.charAt(4) != '-' || raw.charAt(7) != '-') {
            throw new ResponseStatusException(400, "A date is written like 2026-05-31");
        }
        long y = whole(raw.substring(0, 4), "a date");
        long m = whole(raw.substring(5, 7), "a date");
        long day = whole(raw.substring(8, 10), "a date");
        if (y < 1 || m < 1 || m > 12 || day < 1 || day > 31) {
            throw new ResponseStatusException(400, "A date is written like 2026-05-31");
        }
        return y * 10000 + m * 100 + day;
    }

    private void requireOwner(String storeId, String owner, String ownerId) throws IOException {
        if (ownerId == null || ownerId.length() == 0 || ownerId.length() > 32
                || !owners.owns(storeId, owner, ownerId)) {
            throw new ResponseStatusException(404, "No such record");
        }
    }

    private MetafieldDef def(String storeId, String fieldId) throws IOException {
        MetafieldDef d = fieldId == null || fieldId.length() > 32 ? null : defs.findById(fieldId);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such field");
        }
        return d;
    }

    static String choose(String in, String[] allowed, String what) {
        for (int iter = 0; iter < allowed.length; iter++) {
            if (allowed[iter].equals(in)) {
                return in;
            }
        }
        throw new ResponseStatusException(400, "That " + what + " is not one of the choices");
    }

    static MetafieldDefDto dto(MetafieldDef d) {
        return new MetafieldDefDto(d.getId(), d.getOwnerKind(), d.getFieldKey(), d.getName(),
                d.getFieldType(), d.getRefKind(), d.isStorefront());
    }

    // ============================================================ metaobjects

    @Transactional(readOnly = true)
    public List<MetaobjectTypeDto> types(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<MetaobjectType> rows = types.findByStoreIdOrderByNameAsc(storeId);
        List<MetaobjectTypeDto> out = new ArrayList<MetaobjectTypeDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            MetaobjectType t = rows.get(iter);
            out.add(new MetaobjectTypeDto(t.getId(), t.getHandle(), t.getName()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public MetaobjectTypeDto addType(String username, String storeId, MetaobjectTypeDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String name = in == null ? "" : Lists.cut(in.name(), 120);
        if (name.length() == 0) {
            throw new ResponseStatusException(400, "Give the type a name");
        }
        String handle = Lists.handle(in.handle(), name, 60);
        if (types.findByStoreIdAndHandle(storeId, handle) != null) {
            throw new ResponseStatusException(409, "That type exists already");
        }
        MetaobjectType t = new MetaobjectType();
        t.setId(Ids.next());
        t.setStoreId(storeId);
        t.setHandle(handle);
        t.setName(name);
        t.setCreatedAt(Instant.now());
        types.insert(t);
        return new MetaobjectTypeDto(t.getId(), t.getHandle(), t.getName());
    }

    @Transactional(readOnly = true)
    public List<MetaobjectEntryDto> entries(String username, String storeId, String typeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return entryDtos(type(storeId, typeId));
    }

    @Transactional(rollbackFor = IOException.class)
    public List<MetaobjectEntryDto> addEntry(String username, String storeId, String typeId,
            MetaobjectEntryDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        MetaobjectType t = type(storeId, typeId);
        String title = in == null ? "" : Lists.cut(in.title(), 200);
        if (title.length() == 0) {
            throw new ResponseStatusException(400, "Give the entry a title");
        }
        MetaobjectEntry e = new MetaobjectEntry();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setTypeId(t.getId());
        e.setHandle(Lists.handle(in.handle(), title, 80));
        e.setTitle(title);
        e.setCreatedAt(Instant.now());
        entries.insert(e);
        return entryDtos(t);
    }

    /// Removes an entry, and every reference to it; answers its type.
    @Transactional(rollbackFor = IOException.class)
    public String removeEntry(String username, String storeId, String entryId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        MetaobjectEntry e = entryId == null || entryId.length() > 32 ? null
                : entries.findById(entryId);
        if (e == null || !storeId.equals(e.getStoreId())) {
            throw new ResponseStatusException(404, "No such entry");
        }
        List<MetafieldDef> ds = defs.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < ds.size(); iter++) {
            if (!e.getTypeId().equals(ds.get(iter).getRefKind())) {
                continue;
            }
            List<MetafieldValue> vs = values.findByDefId(ds.get(iter).getId());
            for (int v = 0; v < vs.size(); v++) {
                if (e.getId().equals(vs.get(v).getTextValue())) {
                    values.delete(vs.get(v));
                }
            }
        }
        entries.delete(e);
        translations.forget(storeId, com.codenameone.examples.bazaar.market.Translations.METAOBJECT_ENTRY, e.getId());
        return e.getTypeId();
    }

    private MetaobjectType type(String storeId, String typeId) throws IOException {
        MetaobjectType t = typeId == null || typeId.length() > 32 ? null : types.findById(typeId);
        if (t == null || !storeId.equals(t.getStoreId())) {
            throw new ResponseStatusException(404, "No such type");
        }
        return t;
    }

    private List<MetaobjectEntryDto> entryDtos(MetaobjectType t) throws IOException {
        List<MetaobjectEntry> rows = entries.findByTypeIdOrderByTitleAsc(t.getId());
        List<MetaobjectEntryDto> out = new ArrayList<MetaobjectEntryDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            MetaobjectEntry e = rows.get(iter);
            out.add(new MetaobjectEntryDto(e.getId(), e.getTypeId(), e.getHandle(), e.getTitle()));
        }
        return out;
    }
}
