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
package com.codenameone.examples.slate.booking;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.api.WaitlistDto;
import com.codenameone.examples.slate.api.WaitlistRequestDto;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.domain.Waitlist;
import com.codenameone.examples.slate.push.Notifier;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.WaitlistRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/// Clients waiting for a time that is not free.
///
/// A wish is a service, optionally a provider, a range of days and a window
/// of the day. When a booking that fits it is cancelled the first client
/// waiting is told, and the time is theirs to book like any other: an offer
/// is a push and a mark on the wish, not a hold, so a client who never
/// answers keeps nobody else from the time.
@Component
public class Waitlists {
    private static final int MAX_OPEN = 5;

    private final WaitlistRepository waitlist;
    private final ServiceRepository services;
    private final Catalog catalog;
    private final Notifier notifier;

    public Waitlists(WaitlistRepository waitlist, ServiceRepository services, Catalog catalog,
            Notifier notifier) {
        this.waitlist = waitlist;
        this.services = services;
        this.catalog = catalog;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public List<WaitlistDto> mine(String client) {
        List<Waitlist> rows = waitlist.findByClientAndStateInOrderByCreatedAtAsc(client, open());
        List<WaitlistDto> out = new ArrayList<WaitlistDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public WaitlistDto join(String client, WaitlistRequestDto ask) throws IOException {
        if (ask == null) {
            throw new ResponseStatusException(400, "Nothing to wait for");
        }
        Service service = catalog.offered(ask.serviceId());
        LocalDate from = Availability.day(ask.fromDay());
        LocalDate to = Availability.day(ask.toDay());
        if (to.isBefore(from) || to.toEpochDay() - from.toEpochDay() > 60) {
            throw new ResponseStatusException(400, "Wait for up to two months at a time");
        }
        int fromMinute = Catalog.range(ask.fromMinute(), 0, 1440, "The window");
        int toMinute = Catalog.range(ask.toMinute(), 0, 1440, "The window");
        if (toMinute <= fromMinute) {
            throw new ResponseStatusException(400, "The window has to end after it starts");
        }
        if (waitlist.findByClientAndStateInOrderByCreatedAtAsc(client, open()).size()
                >= MAX_OPEN) {
            throw new ResponseStatusException(409, "You are already waiting for " + MAX_OPEN
                    + " times");
        }
        String provider = ask.provider() == null || Catalog.ANY.equals(ask.provider())
                ? "" : ask.provider().trim();
        Instant now = Instant.now();
        Waitlist w = new Waitlist();
        w.setId(Ids.next());
        w.setServiceId(service.getId());
        w.setProvider(provider);
        w.setClient(client);
        w.setFromDay(from.toEpochDay());
        w.setToDay(to.toEpochDay());
        w.setFromMinute(fromMinute);
        w.setToMinute(toMinute);
        w.setState(States.WAITING);
        w.setCreatedAt(now);
        w.setUpdatedAt(now);
        waitlist.insert(w);
        return dto(w);
    }

    @Transactional(rollbackFor = IOException.class)
    public WaitlistDto leave(String client, String id) {
        Waitlist w = waitlist.findFirstByIdAndClient(id, client);
        if (w == null) {
            throw new ResponseStatusException(404, "No such wait");
        }
        waitlist.move(id, w.getState(), States.CANCELLED, Instant.now());
        w.setState(States.CANCELLED);
        return dto(w);
    }

    // tag::slate-waitlist-offer[]
    /// A booking of `serviceId` with `providers` at `start` was given back.
    /// The first wish it fits is offered it.
    public void freed(String serviceId, List<String> providers, long start) throws IOException {
        Waitlist offered = offer(serviceId, providers, start);
        if (offered != null) {
            notifier.tell(offered.getClient(), Notifier.OFFER, "",
                    "A time you wanted is free",
                    "Book it before someone else does");
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private Waitlist offer(String serviceId, List<String> providers, long start) {
        if (start <= System.currentTimeMillis()) {
            return null;
        }
        ZonedDateTime at = Instant.ofEpochMilli(start).atZone(catalog.zone());
        long day = at.toLocalDate().toEpochDay();
        int minute = at.getHour() * 60 + at.getMinute();
        List<Waitlist> waiting = waitlist.findByServiceIdAndStateOrderByCreatedAtAsc(serviceId,
                States.WAITING);
        for (int iter = 0; iter < waiting.size(); iter++) {
            Waitlist w = waiting.get(iter);
            boolean fits = day >= w.getFromDay() && day <= w.getToDay()
                    && minute >= w.getFromMinute() && minute < w.getToMinute()
                    && (w.getProvider().length() == 0 || providers.contains(w.getProvider()));
            if (fits && waitlist.move(w.getId(), States.WAITING, States.OFFERED,
                    Instant.now()) == 1) {
                w.setState(States.OFFERED);
                w.setOffered(Long.toString(start));
                waitlist.save(w);
                return w;
            }
        }
        return null;
    }
    // end::slate-waitlist-offer[]

    private WaitlistDto dto(Waitlist w) {
        Service s = services.findById(w.getServiceId());
        return new WaitlistDto(w.getId(), w.getServiceId(), s == null ? "" : s.getName(),
                w.getProvider(), LocalDate.ofEpochDay(w.getFromDay()).toString(),
                LocalDate.ofEpochDay(w.getToDay()).toString(), w.getFromMinute(),
                w.getToMinute(), w.getState(), w.getOffered(),
                w.getCreatedAt().toEpochMilli());
    }

    private static List<String> open() {
        List<String> out = new ArrayList<String>();
        out.add(States.WAITING);
        out.add(States.OFFERED);
        return out;
    }
}
