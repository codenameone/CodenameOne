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
package com.codenameone.examples.slate.sync;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.Text;
import com.codenameone.examples.slate.api.BusyUploadDto;
import com.codenameone.examples.slate.api.CalendarConfigDto;
import com.codenameone.examples.slate.api.CalendarLinkDto;
import com.codenameone.examples.slate.api.ConnectCalendarDto;
import com.codenameone.examples.slate.api.IntervalDto;
import com.codenameone.examples.slate.domain.BusyBlock;
import com.codenameone.examples.slate.domain.CalendarLink;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.live.LiveHub;
import com.codenameone.examples.slate.repository.BusyBlockRepository;
import com.codenameone.examples.slate.repository.CalendarLinkRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// A provider's connected calendars, and the busy time each one adds to the
/// schedule.
///
/// There are two ways in, and a provider can use both:
///
/// - **The device.** The app reads the phone's own calendars -- which include
///   every account the phone is signed in to -- and uploads only the busy
///   intervals, never an event. A `DEVICE` link is that upload's name. The
///   server cannot read it by itself; it is as fresh as the app's last upload.
/// - **The server.** A Google or Microsoft account, or a published feed, which
///   the server reads every [#SYNC_MILLIS] whether or not the app is open.
///
/// Either way the result is the same rows, [BusyBlock], read by the slot
/// engine as time the provider is not free. Each link's rows are replaced,
/// stretch by stretch, by its next reading.
@Component
public class CalendarLinks {
    public static final String DEVICE = "DEVICE";
    public static final String GOOGLE = "GOOGLE";
    public static final String MICROSOFT = "MICROSOFT";
    public static final String ICS = "ICS";
    static final long SYNC_MILLIS = 900000L;
    private static final long DAY = 86400000L;
    /// How far ahead a reading looks. Further than bookings open is wasted.
    private static final long AHEAD = 120L * DAY;
    private static final int MAX_LINKS = 6;
    private static final int MAX_INTERVALS = 3000;

    private final CalendarLinkRepository links;
    private final BusyBlockRepository blocks;
    private final ProviderRepository providers;
    private final CalendarSources sources;
    private final LiveHub live;
    private final String publicUrl;

    public CalendarLinks(CalendarLinkRepository links, BusyBlockRepository blocks,
            ProviderRepository providers, CalendarSources sources, LiveHub live,
            @Value("${slate.public.url:}") String publicUrl) {
        this.links = links;
        this.blocks = blocks;
        this.providers = providers;
        this.sources = sources;
        this.live = live;
        String url = publicUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.publicUrl = url;
    }

    @Transactional(readOnly = true)
    public List<CalendarLinkDto> list(String username) {
        List<CalendarLink> rows = links.findByUsernameOrderByCreatedAtAsc(username);
        List<CalendarLinkDto> out = new ArrayList<CalendarLinkDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// What the app needs to connect a calendar: the OAuth client ids, and
    /// the address of the provider's own feed of bookings, to subscribe to.
    @Transactional(readOnly = true)
    public CalendarConfigDto config(String username) {
        Provider p = providers.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such provider");
        }
        return new CalendarConfigDto(sources.googleClientId(), sources.microsoftClientId(),
                publicUrl + "/ics/p/" + p.getFeedToken() + ".ics");
    }

    // tag::slate-connect-calendar[]
    /// Connects a calendar. For a server-read one the code or address is
    /// turned into a credential first, with no transaction open, and the
    /// calendar read once at once so the schedule shows it straight away.
    public CalendarLinkDto connect(String username, ConnectCalendarDto ask) throws IOException {
        if (ask == null) {
            throw new ResponseStatusException(400, "Nothing to connect");
        }
        String kind = ask.kind() == null ? "" : ask.kind().trim();
        String credential = "";
        if (!DEVICE.equals(kind)) {
            ExternalCalendar source = sources.of(kind);
            if (source == null) {
                throw new ResponseStatusException(400, kind.length() == 0
                        ? "Say which calendar" : "This server is not set up for " + kind);
            }
            try {
                credential = source.connect(ask.credential(), ask.redirectUri());
            } catch (IOException refused) {
                throw new ResponseStatusException(400, refused.getMessage());
            }
        }
        CalendarLink link = add(username, kind, ask, credential);
        if (!DEVICE.equals(kind)) {
            return sync(username, link.getId());
        }
        return dto(link);
    }
    // end::slate-connect-calendar[]

    @Transactional(rollbackFor = IOException.class)
    private CalendarLink add(String username, String kind, ConnectCalendarDto ask,
            String credential) throws IOException {
        if (providers.findById(username) == null) {
            throw new ResponseStatusException(404, "No such provider");
        }
        List<CalendarLink> mine = links.findByUsernameOrderByCreatedAtAsc(username);
        if (mine.size() >= MAX_LINKS) {
            throw new ResponseStatusException(409, "Disconnect a calendar first");
        }
        if (DEVICE.equals(kind)) {
            // One phone, one link: connecting again on the same phone replaces
            // which calendars it reads rather than adding a second.
            for (int iter = 0; iter < mine.size(); iter++) {
                CalendarLink old = mine.get(iter);
                if (DEVICE.equals(old.getKind()) && old.getLabel().equals(label(ask, kind))) {
                    old.setCalendars(Text.cut(ask.calendars(), 1000));
                    old.setWriteCalendar(Text.cut(ask.writeCalendar(), 190));
                    links.save(old);
                    return old;
                }
            }
        }
        CalendarLink link = new CalendarLink();
        link.setId(Ids.next());
        link.setUsername(username);
        link.setKind(kind);
        link.setLabel(label(ask, kind));
        link.setCalendars(Text.cut(ask.calendars(), 1000));
        link.setWriteCalendar(Text.cut(ask.writeCalendar(), 190));
        link.setCredential(credential);
        link.setLastSyncedAt(Instant.EPOCH);
        link.setCreatedAt(Instant.now());
        links.insert(link);
        return link;
    }

    /// Disconnects a calendar and forgets everything it said.
    public CalendarLinkDto disconnect(String username, String id) throws IOException {
        CalendarLinkDto gone = disconnectTx(username, id);
        changed(username);
        return gone;
    }

    @Transactional(rollbackFor = IOException.class)
    private CalendarLinkDto disconnectTx(String username, String id) {
        CalendarLink link = owned(username, id);
        CalendarLinkDto dto = dto(link);
        blocks.deleteByLinkId(id);
        links.deleteById(id);
        return dto;
    }

    /// Reads a server-side calendar now.
    public CalendarLinkDto sync(String username, String id) throws IOException {
        CalendarLink link = read(username, id);
        if (DEVICE.equals(link.getKind())) {
            throw new ResponseStatusException(409, "The phone sends this calendar itself");
        }
        CalendarLinkDto done = pull(link);
        changed(username);
        return done;
    }

    // tag::slate-busy-upload[]
    /// What the app read from the phone's calendars: busy intervals over
    /// `from`..`to`, which replace what the link said about that stretch.
    public CalendarLinkDto upload(String username, BusyUploadDto busy) throws IOException {
        if (busy == null || busy.busy() == null || busy.to() <= busy.from()) {
            throw new ResponseStatusException(400, "Nothing to upload");
        }
        if (busy.to() - busy.from() > AHEAD + DAY || busy.busy().size() > MAX_INTERVALS) {
            throw new ResponseStatusException(400, "Upload up to four months at a time");
        }
        List<long[]> spans = new ArrayList<long[]>();
        for (int iter = 0; iter < busy.busy().size(); iter++) {
            IntervalDto i = busy.busy().get(iter);
            if (i != null && i.end() > i.start()) {
                spans.add(new long[] {Math.max(i.start(), busy.from()),
                    Math.min(i.end(), busy.to() + DAY)});
            }
        }
        CalendarLinkDto done = store(username, busy.linkId(), true, busy.from(), busy.to(),
                spans, "");
        changed(username);
        return done;
    }
    // end::slate-busy-upload[]

    /// Reads every server-side calendar.
    @Scheduled(fixedDelay = SYNC_MILLIS, initialDelay = 180000, lock = "slate-calendars")
    public void syncAll() throws IOException {
        List<CalendarLink> all = serverLinks();
        List<String> touched = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            pull(all.get(iter));
            if (!touched.contains(all.get(iter).getUsername())) {
                touched.add(all.get(iter).getUsername());
            }
        }
        for (int iter = 0; iter < touched.size(); iter++) {
            changed(touched.get(iter));
        }
    }

    @Transactional(readOnly = true)
    private List<CalendarLink> serverLinks() {
        return links.findByKindIn(Arrays.asList(GOOGLE, MICROSOFT, ICS));
    }

    /// One reading of a server-side calendar. A failure is recorded on the
    /// link for the provider to see, and leaves the busy time it said before.
    private CalendarLinkDto pull(CalendarLink link) throws IOException {
        long from = System.currentTimeMillis() - DAY;
        long to = from + AHEAD;
        ExternalCalendar source = sources.of(link.getKind());
        if (source == null) {
            return store(link.getUsername(), link.getId(), false, from, to, null,
                    "This server is no longer set up for " + link.getKind());
        }
        List<long[]> spans;
        try {
            spans = source.busy(link.getCredential(), link.getCalendars(), from, to);
        } catch (IOException failed) {
            return store(link.getUsername(), link.getId(), false, from, to, null,
                    Text.cut(failed.getMessage(), 250));
        }
        return store(link.getUsername(), link.getId(), false, from, to, spans, "");
    }

    @Transactional(rollbackFor = IOException.class)
    private CalendarLinkDto store(String username, String id, boolean device, long from, long to,
            List<long[]> spans, String error) throws IOException {
        CalendarLink link = owned(username, id);
        if (device != DEVICE.equals(link.getKind())) {
            throw new ResponseStatusException(409, device
                    ? "Only a phone's calendar is uploaded" : "The phone sends this calendar");
        }
        Instant now = Instant.now();
        if (spans != null) {
            blocks.clear(id, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to));
            List<long[]> merged = merge(spans);
            for (int iter = 0; iter < merged.size(); iter++) {
                BusyBlock b = new BusyBlock();
                b.setId(Ids.next());
                b.setUsername(username);
                b.setLinkId(id);
                b.setStartsAt(Instant.ofEpochMilli(merged.get(iter)[0]));
                b.setEndsAt(Instant.ofEpochMilli(merged.get(iter)[1]));
                b.setUpdatedAt(now);
                blocks.insert(b);
            }
            link.setLastSyncedAt(now);
        }
        link.setLastError(error);
        links.save(link);
        return dto(link);
    }

    /// Overlapping and touching intervals as one, so a day of back-to-back
    /// meetings is one row rather than ten.
    static List<long[]> merge(List<long[]> spans) {
        List<long[]> sorted = new ArrayList<long[]>(spans);
        Collections.sort(sorted, new Comparator<long[]>() {
            @Override
            public int compare(long[] a, long[] b) {
                return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0;
            }
        });
        List<long[]> out = new ArrayList<long[]>();
        for (int iter = 0; iter < sorted.size(); iter++) {
            long[] s = sorted.get(iter);
            if (!out.isEmpty() && s[0] <= out.get(out.size() - 1)[1]) {
                long[] last = out.get(out.size() - 1);
                last[1] = Math.max(last[1], s[1]);
            } else {
                out.add(new long[] {s[0], s[1]});
            }
        }
        return out;
    }

    private void changed(String username) {
        live.scheduleChanged(username);
        live.slotsChanged("", "");
    }

    @Transactional(readOnly = true)
    private CalendarLink read(String username, String id) {
        return owned(username, id);
    }

    private CalendarLink owned(String username, String id) {
        CalendarLink link = links.findFirstByIdAndUsername(id, username);
        if (link == null) {
            throw new ResponseStatusException(404, "No such calendar");
        }
        return link;
    }

    private static String label(ConnectCalendarDto ask, String kind) {
        String label = Text.cut(ask.label() == null ? "" : ask.label().trim(), 120);
        return label.length() > 0 ? label : kind;
    }

    private static CalendarLinkDto dto(CalendarLink l) {
        return new CalendarLinkDto(l.getId(), l.getKind(), l.getLabel(), l.getCalendars(),
                l.getWriteCalendar(), !DEVICE.equals(l.getKind()),
                l.getLastSyncedAt().toEpochMilli(), l.getLastError());
    }
}
