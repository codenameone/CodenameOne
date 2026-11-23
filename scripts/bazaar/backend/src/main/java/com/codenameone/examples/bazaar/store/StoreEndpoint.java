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
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.StaffDto;
import com.codenameone.examples.bazaar.api.StoreApiServer;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.api.StoreRoleDto;

import java.util.List;

/// The server's half of `StoreApi`. The `/api/shop` calls need no sign-in and
/// never read the caller; the merchant ones are checked against the store's
/// staff list by [Stores].
@Component
public class StoreEndpoint implements StoreApiServer {
    private final Stores stores;

    public StoreEndpoint(Stores stores) {
        this.stores = stores;
    }

    @Override
    public List<StoreDto> stores() throws Exception {
        return stores.publicStores();
    }

    @Override
    public StoreDto store(String handle) throws Exception {
        return stores.publicStore(handle);
    }

    @Override
    public List<LocationDto> pickupLocations(String handle) throws Exception {
        return stores.pickupLocations(handle);
    }

    @Override
    public List<StoreRoleDto> myStores() throws Exception {
        return stores.myStores(Caller.name());
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.admin.Platform platform;

    @Override
    public List<com.codenameone.examples.bazaar.api.StoreStatsDto> organization()
            throws Exception {
        return platform.organization(Caller.name());
    }

    @Override
    public StoreDto storeSettings(String storeId) throws Exception {
        return stores.settings(Caller.name(), storeId);
    }

    @Override
    public StoreDto saveSettings(String storeId, StoreDto settings) throws Exception {
        return stores.saveSettings(Caller.name(), storeId, settings);
    }

    @Override
    public List<LocationDto> locations(String storeId) throws Exception {
        return stores.locations(Caller.name(), storeId);
    }

    @Override
    public LocationDto addLocation(String storeId, LocationDto location) throws Exception {
        return stores.addLocation(Caller.name(), storeId, location);
    }

    @Override
    public LocationDto saveLocation(String storeId, String locationId, LocationDto location)
            throws Exception {
        return stores.saveLocation(Caller.name(), storeId, locationId, location);
    }

    @Override
    public LocationDto removeLocation(String storeId, String locationId) throws Exception {
        return stores.removeLocation(Caller.name(), storeId, locationId);
    }

    @Override
    public List<StaffDto> staff(String storeId) throws Exception {
        return stores.staff(Caller.name(), storeId);
    }

    @Override
    public StaffDto saveStaff(String storeId, StaffDto staff) throws Exception {
        return stores.saveStaff(Caller.name(), storeId, staff);
    }

    @Override
    public StaffDto removeStaff(String storeId, String staffId) throws Exception {
        return stores.removeStaff(Caller.name(), storeId, staffId);
    }
}
