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
package com.codenameone.examples.bazaar.store;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.StoreClockApiServer;
import com.codenameone.examples.bazaar.api.StoreTimeZoneDto;

import java.io.IOException;
import java.util.Map;

/// The store's time zone, which analytics, reports and their "today" count
/// days and hours in ([Days#localNumber]). A store starts in `UTC`; its
/// managers pick an IANA zone in the settings.
@Component
public class StoreClock implements StoreClockApiServer {
    private final DataSource db;
    private final StoreAccess access;

    public StoreClock(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// The zone of `storeId`, `UTC` for a store that picked none or does not
    /// exist. No access check: callers have made theirs.
    public String zoneOf(String storeId) throws IOException {
        Map r = db.queryOne("SELECT time_zone FROM store WHERE id = ?", new Object[] {storeId});
        String zone = r == null ? "" : Rows.text(r.get("time_zone"));
        return Days.isZone(zone) ? zone : Days.UTC;
    }

    @Override
    public StoreTimeZoneDto timeZone(String storeId) throws Exception {
        access.requireArea(Caller.name(), storeId, Perms.SETTINGS, false, StoreAccess.MANAGERS);
        return new StoreTimeZoneDto(zoneOf(storeId));
    }

    @Override
    public StoreTimeZoneDto saveTimeZone(String storeId, StoreTimeZoneDto zone) throws Exception {
        access.requireArea(Caller.name(), storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        String wanted = zone == null || zone.timeZone() == null ? "" : zone.timeZone().trim();
        if (!Days.isZone(wanted)) {
            throw new ResponseStatusException(400,
                    "Name the time zone as UTC or an IANA zone such as America/New_York");
        }
        db.execute("UPDATE store SET time_zone = ? WHERE id = ?", new Object[] {wanted, storeId});
        return new StoreTimeZoneDto(wanted);
    }
}
