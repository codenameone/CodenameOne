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
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.reel.domain.Playback;

import java.util.List;

/// The playback rows.
@Repository(entity = Playback.class, key = String.class)
public interface PlaybackRepository extends CrudRepository<Playback, String> {
    int deleteByUsername(String username);

    List<Playback> findByUsernameAndEndedAt(String username, long endedAt);

    List<Playback> findByEndedAtOrderByStartedAtDesc(long endedAt);

    List<Playback> findByEndedAtAndLastHeartbeatAtLessThan(long endedAt, long before);

    List<Playback> findByUsernameAndStartedAtGreaterThan(String username, long since);

    List<Playback> findByStartedAtGreaterThan(long since);

    List<Playback> findByTitleIdAndStartedAtGreaterThan(String titleId, long since);

    long countByEndedAt(long endedAt);
}
