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
package com.codenameone.examples.reel.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.reel.domain.AdCampaign;

import java.util.List;

/// The ad_campaign rows.
@Repository(entity = AdCampaign.class, key = String.class)
public interface AdCampaignRepository extends CrudRepository<AdCampaign, String> {
    List<AdCampaign> findByActiveTrue();

    @Modifying
    @Query("update AdCampaign c set c.impressions = c.impressions + 1, c.spentMillicents = c.spentMillicents + c.cpmCents where c.id = :id")
    int impression(@Param("id") String id);

    @Modifying
    @Query("update AdCampaign c set c.clicks = c.clicks + 1 where c.id = :id")
    int click(@Param("id") String id);
}
