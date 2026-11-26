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
package com.codenameone.examples.bazaar.platform;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AppChargeDecisionDto;
import com.codenameone.examples.bazaar.api.AppChargeDto;
import com.codenameone.examples.bazaar.api.AppListingDecisionDto;
import com.codenameone.examples.bazaar.api.AppListingDto;
import com.codenameone.examples.bazaar.api.AppReviewDto;
import com.codenameone.examples.bazaar.api.AppStoreApiServer;

import java.util.List;

/// The server's half of `AppStoreApi`. `/api/admin` is the admin role's
/// alone by the security rules; [AppDirectory] and [AppBilling] check the
/// rest.
@Component
public class AppStoreEndpoint implements AppStoreApiServer {
    private final AppDirectory directory;
    private final AppBilling billing;

    public AppStoreEndpoint(AppDirectory directory, AppBilling billing) {
        this.directory = directory;
        this.billing = billing;
    }

    @Override
    public List<AppListingDto> directory(String q, String category, int page) throws Exception {
        return directory.search(q, category, page);
    }

    @Override
    public AppListingDto listing(String handle) throws Exception {
        return directory.listing(handle);
    }

    @Override
    public List<AppReviewDto> reviews(String handle) throws Exception {
        return directory.reviews(handle);
    }

    @Override
    public AppListingDto myListing(String appId) throws Exception {
        return directory.mine(Caller.name(), appId);
    }

    @Override
    public AppListingDto submitListing(String appId, AppListingDto listing) throws Exception {
        return directory.submit(Caller.name(), appId, listing);
    }

    @Override
    public List<AppListingDto> listings(String status) throws Exception {
        return directory.queue(status);
    }

    @Override
    public AppListingDto decide(String listingId, AppListingDecisionDto decision)
            throws Exception {
        return directory.decide(Caller.name(), listingId, decision);
    }

    @Override
    public List<AppReviewDto> allReviews(String listingId) throws Exception {
        return directory.allReviews(listingId);
    }

    @Override
    public AppReviewDto hideReview(String reviewId) throws Exception {
        return directory.hide(Caller.name(), reviewId);
    }

    @Override
    public AppReviewDto review(String storeId, AppReviewDto review) throws Exception {
        return directory.review(Caller.name(), storeId, review);
    }

    @Override
    public List<AppChargeDto> charges(String storeId) throws Exception {
        return billing.charges(Caller.name(), storeId);
    }

    @Override
    public AppChargeDto decideCharge(String storeId, String chargeId,
            AppChargeDecisionDto decision) throws Exception {
        return billing.decide(Caller.name(), storeId, chargeId,
                decision != null && decision.approve());
    }
}
