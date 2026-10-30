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
package com.codenameone.examples.pulse.studio;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.BookRequestDto;
import com.codenameone.examples.pulse.api.BookingApiServer;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.ClassTypeDto;
import com.codenameone.examples.pulse.api.LocationDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.StudioApiServer;
import com.codenameone.examples.pulse.api.TrainerDto;

import java.util.List;

/// The server's half of `StudioApi` and `BookingApi`: the timetable, and the
/// caller's own household's places in it.
@Component
public class StudioEndpoint implements StudioApiServer, BookingApiServer {
    private final Studio studio;
    private final Bookings bookings;

    public StudioEndpoint(Studio studio, Bookings bookings) {
        this.studio = studio;
        this.bookings = bookings;
    }

    @Override
    public List<LocationDto> locations() throws Exception {
        Caller.name();
        return studio.locations();
    }

    @Override
    public List<ClassTypeDto> classTypes() throws Exception {
        Caller.name();
        return studio.types(false);
    }

    @Override
    public List<TrainerDto> trainers() throws Exception {
        Caller.name();
        return studio.trainers();
    }

    @Override
    public List<SessionDto> classes(String location, long from, int days) throws Exception {
        return studio.classes(Caller.name(), location, from, days <= 0 ? 7 : days);
    }

    @Override
    public SessionDto session(String id) throws Exception {
        return studio.get(Caller.name(), id);
    }

    @Override
    public List<BookingDto> upcoming() throws Exception {
        return bookings.upcoming(Caller.name());
    }

    @Override
    public List<BookingDto> history() throws Exception {
        return bookings.history(Caller.name());
    }

    @Override
    public BookingDto book(BookRequestDto request) throws Exception {
        return bookings.book(Caller.name(), request);
    }

    @Override
    public BookingDto cancel(String id) throws Exception {
        return bookings.cancel(Caller.name(), id);
    }
}
