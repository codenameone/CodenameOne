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
package com.codenameone.examples.parley.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.parley.domain.Device;

import java.time.Instant;
import java.util.List;

/// The devices of each account, with their published public keys.
@Repository(entity = Device.class, key = String.class)
public interface DeviceRepository extends CrudRepository<Device, String> {
    List<Device> findByUsername(String username);

    List<Device> findByUsernameIn(List<String> usernames);

    Device findByIdAndUsername(String id, String username);

    long countByUsername(String username);

    int deleteByUsername(String username);

    @Query("select d.platform, count(d) from Device d group by d.platform")
    List<Object[]> byPlatform();

    @Modifying
    @Query("update Device d set d.lastSeenAt = :at where d.id = :id")
    int seen(@Param("id") String id, @Param("at") Instant at);
}
