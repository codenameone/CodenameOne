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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Days;
import com.codenameone.examples.pulse.api.ClassTypeDto;
import com.codenameone.examples.pulse.api.LocationDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.TrainerDto;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Friend;
import com.codenameone.examples.pulse.domain.Location;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.domain.Room;
import com.codenameone.examples.pulse.domain.Staff;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.FriendRepository;
import com.codenameone.examples.pulse.repository.LocationRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;
import com.codenameone.examples.pulse.repository.StaffRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The timetable as everyone sees it: the locations, the kinds of class, the
/// trainers, and the classes themselves with how full each one is.
///
/// A class as answered carries what only the caller sees beside what everyone
/// does: which of their household are booked into it or waiting for a place,
/// and how many of their friends are going. That is what makes a timetable
/// worth opening.
@Component
public class Studio {
    /// A class that will run.
    public static final String SCHEDULED = "SCHEDULED";
    /// A class the studio called off. Its places were handed back.
    public static final String CANCELLED = "CANCELLED";
    /// A class that has ended, whose bookings were settled.
    public static final String DONE = "DONE";
    /// The most days of timetable one request answers.
    static final int MOST_DAYS = 28;

    private final LocationRepository locations;
    private final RoomRepository rooms;
    private final ClassTypeRepository types;
    private final ClassSessionRepository sessions;
    private final StaffRepository staff;
    private final ProfileRepository profiles;
    private final BookingRepository bookings;
    private final FriendRepository friends;

    public Studio(LocationRepository locations, RoomRepository rooms, ClassTypeRepository types,
            ClassSessionRepository sessions, StaffRepository staff, ProfileRepository profiles,
            BookingRepository bookings, FriendRepository friends) {
        this.locations = locations;
        this.rooms = rooms;
        this.types = types;
        this.sessions = sessions;
        this.staff = staff;
        this.profiles = profiles;
        this.bookings = bookings;
        this.friends = friends;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> locations() {
        List<Location> rows = locations.findAllByOrderByNameAsc();
        List<LocationDto> out = new ArrayList<LocationDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Location row = rows.get(iter);
            out.add(new LocationDto(row.getId(), row.getName(), row.getAddress(),
                    row.getTimeZone(), row.getOpensMinute(), row.getClosesMinute()));
        }
        return out;
    }

    /// The kinds of class; the retired ones only when `all`, for the owner.
    @Transactional(readOnly = true)
    public List<ClassTypeDto> types(boolean all) {
        List<ClassType> rows = types.findAllByOrderByNameAsc();
        List<ClassTypeDto> out = new ArrayList<ClassTypeDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ClassType row = rows.get(iter);
            if (all || row.isActive()) {
                out.add(type(row));
            }
        }
        return out;
    }

    public static ClassTypeDto type(ClassType row) {
        return new ClassTypeDto(row.getId(), row.getName(), row.getDescription(),
                row.getCategory(), row.getIntensity(), row.getMinAge(), row.getMaxAge(),
                row.getCapacity(), row.getMinutes(), row.getColor(), row.isActive());
    }

    @Transactional(readOnly = true)
    public List<TrainerDto> trainers() {
        List<Staff> rows = staff.findByActiveTrueOrderByUsernameAsc();
        List<TrainerDto> out = new ArrayList<TrainerDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Staff row = rows.get(iter);
            out.add(new TrainerDto(row.getUsername(), name(row.getUsername(), null),
                    row.getTitle(), row.getBio(), row.getLocationId()));
        }
        return out;
    }

    /// The classes at a location -- or every location, for an empty one --
    /// from the start of the day `from` falls in, for `days` days.
    @Transactional(readOnly = true)
    public List<SessionDto> classes(String viewer, String locationId, long from, int days)
            throws IOException {
        if (days < 1 || days > MOST_DAYS) {
            throw new ResponseStatusException(400, "Ask for 1 to " + MOST_DAYS + " days");
        }
        Instant start = Instant.ofEpochMilli(Days.start(from <= 0L ? System.currentTimeMillis()
                : from));
        Instant end = start.plusMillis(days * Days.MILLIS);
        List<ClassSession> rows = locationId == null || locationId.length() == 0
                ? sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        start, end)
                : sessions.findByLocationIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        locationId, start, end);
        return describe(viewer, rows);
    }

    /// One class. Answers 404 for an id that is none.
    @Transactional(readOnly = true)
    public SessionDto get(String viewer, String id) throws IOException {
        ClassSession row = session(id);
        List<ClassSession> one = new ArrayList<ClassSession>();
        one.add(row);
        return describe(viewer, one).get(0);
    }

    /// A class, or null.
    public ClassSession sessionOrNull(String id) {
        return id == null || id.length() == 0 || id.length() > 32 ? null : sessions.findById(id);
    }

    /// A class, or 404.
    public ClassSession session(String id) {
        ClassSession row = sessionOrNull(id);
        if (row == null) {
            throw new ResponseStatusException(404, "No such class");
        }
        return row;
    }

    /// Classes as `viewer` sees them. Everything the rows name -- types,
    /// rooms, trainers, the viewer's bookings and their friends' -- is read
    /// once for the whole list, not once a class.
    public List<SessionDto> describe(String viewer, List<ClassSession> rows) throws IOException {
        List<SessionDto> out = new ArrayList<SessionDto>();
        if (rows.isEmpty()) {
            return out;
        }
        Map<String, ClassType> typeById = new HashMap<String, ClassType>();
        List<ClassType> allTypes = types.findAllByOrderByNameAsc();
        for (int iter = 0; iter < allTypes.size(); iter++) {
            typeById.put(allTypes.get(iter).getId(), allTypes.get(iter));
        }
        Map<String, String> roomNames = new HashMap<String, String>();
        List<Room> allRooms = rooms.findAllByOrderByLocationIdAscNameAsc();
        for (int iter = 0; iter < allRooms.size(); iter++) {
            roomNames.put(allRooms.get(iter).getId(), allRooms.get(iter).getName());
        }
        Map<String, String> locationNames = new HashMap<String, String>();
        List<Location> allLocations = locations.findAllByOrderByNameAsc();
        for (int iter = 0; iter < allLocations.size(); iter++) {
            locationNames.put(allLocations.get(iter).getId(), allLocations.get(iter).getName());
        }
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ids.add(rows.get(iter).getId());
        }
        Set<String> circle = viewer == null ? new HashSet<String>() : circle(viewer);
        List<Booking> taken = viewer == null ? new ArrayList<Booking>()
                : bookings.findBySessionIdIn(ids);
        Map<String, String> trainerNames = new HashMap<String, String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ClassSession row = rows.get(iter);
            ClassType type = typeById.get(row.getClassTypeId());
            List<String> mine = new ArrayList<String>();
            List<String> queued = new ArrayList<String>();
            Set<String> going = new HashSet<String>();
            for (int b = 0; b < taken.size(); b++) {
                Booking booking = taken.get(b);
                if (!booking.getSessionId().equals(row.getId())) {
                    continue;
                }
                boolean held = Bookings.BOOKED.equals(booking.getState())
                        || Bookings.CHECKED_IN.equals(booking.getState());
                if (booking.getAccount().equals(viewer)) {
                    if (held) {
                        mine.add(booking.getMemberId());
                    } else if (Bookings.WAITLISTED.equals(booking.getState())) {
                        queued.add(booking.getMemberId());
                    }
                } else if (held && circle.contains(booking.getAccount())) {
                    going.add(booking.getAccount());
                }
            }
            out.add(new SessionDto(row.getId(), row.getClassTypeId(),
                    type == null ? "" : type.getName(), type == null ? "" : type.getCategory(),
                    type == null ? 0 : type.getIntensity(), type == null ? 0 : type.getMinAge(),
                    type == null ? 0 : type.getMaxAge(), type == null ? "" : type.getColor(),
                    row.getLocationId(), text(locationNames.get(row.getLocationId())),
                    text(roomNames.get(row.getRoomId())), row.getTrainer(),
                    name(row.getTrainer(), trainerNames), row.getStartsAt().toEpochMilli(),
                    row.getEndsAt().toEpochMilli(), row.getCapacity(), row.getBooked(),
                    row.getWaitlisted(), row.getState(), row.getNote(), mine, queued,
                    going.size()));
        }
        return out;
    }

    /// The accounts `viewer` is friends with: both sides accepted.
    Set<String> circle(String viewer) {
        Set<String> out = new HashSet<String>();
        List<Friend> rows = friends.findByAccountAndState(viewer, "ACCEPTED");
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rows.get(iter).getFriend());
        }
        return out;
    }

    /// An account's display name, through `cache` when there is one.
    String name(String username, Map<String, String> cache) {
        if (username == null || username.length() == 0) {
            return "";
        }
        String known = cache == null ? null : cache.get(username);
        if (known != null) {
            return known;
        }
        Profile profile = profiles.findById(username);
        String name = profile == null ? "" : profile.getDisplayName();
        if (cache != null) {
            cache.put(username, name);
        }
        return name;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
