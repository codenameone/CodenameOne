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
import com.codenameone.examples.parley.domain.Envelope;

import java.util.List;

/// Envelopes waiting for their device.
@Repository(entity = Envelope.class, key = String.class)
public interface EnvelopeRepository extends CrudRepository<Envelope, String> {
    /// The oldest envelopes waiting for a device, a page at a time.
    List<Envelope> findTop100ByRecipientDeviceOrderBySentAtAscIdAsc(String recipientDevice);

    long countByRecipientDevice(String recipientDevice);

    Envelope findByIdAndRecipientDevice(String id, String recipientDevice);

    int deleteByIdAndRecipientDevice(String id, String recipientDevice);

    int deleteByRecipientDevice(String recipientDevice);

    int deleteByRecipient(String recipient);

    /// Removes what the disappearing timer has run out on.
    @Modifying
    @Query("delete from Envelope e where e.expiresAt > 0 and e.expiresAt <= :now")
    int expire(@Param("now") long now);

    /// Removes what nobody fetched for a month: the device is gone or lost.
    int deleteBySentAtLessThan(long before);
}
