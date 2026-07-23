/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.android;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.support.v4.content.ContextCompat;
import com.codename1.calendar.*;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** Android Calendar Provider integration. */
final class AndroidCalendarSource extends LocalCalendarSource {
    private static final String[] EVENT_COLUMNS = {Events._ID, Events.CALENDAR_ID, Events.TITLE,
            Events.DESCRIPTION, Events.EVENT_LOCATION, Events.DTSTART, Events.DTEND, Events.DURATION,
            Events.EVENT_TIMEZONE, Events.ALL_DAY, Events.RRULE, Events.STATUS, Events.AVAILABILITY,
            Events.DIRTY};
    private static final String[] INSTANCE_COLUMNS = {Instances.EVENT_ID, Instances.CALENDAR_ID,
            Instances.TITLE, Instances.DESCRIPTION, Instances.EVENT_LOCATION, Instances.BEGIN,
            Instances.END, Instances.DURATION, Instances.EVENT_TIMEZONE, Instances.ALL_DAY,
            Instances.RRULE, Instances.STATUS, Instances.AVAILABILITY, Events.DIRTY};
    private final Context context;
    private final CalendarCapabilities capabilities=CalendarCapabilities.of(CalendarCapability.READ_CALENDARS,
            CalendarCapability.READ_EVENTS,CalendarCapability.WRITE_EVENTS,CalendarCapability.DELETE_EVENTS,
            CalendarCapability.RECURRENCE,CalendarCapability.ATTENDEES_READ,CalendarCapability.ATTENDEES_WRITE,
            CalendarCapability.ALARMS,CalendarCapability.FREE_BUSY,CalendarCapability.LOCAL_CHANGE_LISTENER,
            CalendarCapability.OFFLINE_MUTATIONS);

    AndroidCalendarSource(Context context){
        this.context=context.getApplicationContext();
        try{this.context.getContentResolver().registerContentObserver(Events.CONTENT_URI,true,new ContentObserver(new Handler(Looper.getMainLooper())){
            public void onChange(boolean selfChange,Uri uri){fireChange(new CalendarChange(getId(),null,null,CalendarChange.EntityType.EVENT,CalendarChange.ChangeType.RESET));}
        });}catch(Throwable ignored){}
    }
    public CalendarCapabilities getCapabilities(){return capabilities;}
    public CalendarAuthorizationStatus getAuthorizationStatus(CalendarAccess access){
        if(access==CalendarAccess.TASKS_FULL)return CalendarAuthorizationStatus.DENIED;
        boolean read=granted(Manifest.permission.READ_CALENDAR),write=granted(Manifest.permission.WRITE_CALENDAR);
        if(access==CalendarAccess.EVENTS_WRITE_ONLY)return write?CalendarAuthorizationStatus.WRITE_ONLY:CalendarAuthorizationStatus.DENIED;
        if(access==CalendarAccess.EVENTS_READ_ONLY)return read?CalendarAuthorizationStatus.FULL:CalendarAuthorizationStatus.DENIED;
        return read&&write?CalendarAuthorizationStatus.FULL:CalendarAuthorizationStatus.DENIED;
    }
    public AsyncResource<CalendarAuthorizationStatus>requestAuthorization(CalendarAccess access){
        if(access==CalendarAccess.TASKS_FULL)return failed(CalendarError.NOT_SUPPORTED,"Android Calendar Provider does not expose tasks");
        boolean read=access==CalendarAccess.EVENTS_WRITE_ONLY||AndroidImplementation.checkForPermission(Manifest.permission.READ_CALENDAR,"Calendar access is required to read your calendars");
        boolean write=access==CalendarAccess.EVENTS_READ_ONLY||AndroidImplementation.checkForPermission(Manifest.permission.WRITE_CALENDAR,"Calendar access is required to schedule events");
        if(!read||!write)return value(CalendarAuthorizationStatus.DENIED);
        return value(access==CalendarAccess.EVENTS_WRITE_ONLY?CalendarAuthorizationStatus.WRITE_ONLY:CalendarAuthorizationStatus.FULL);
    }
    public AsyncResource<CalendarPage<CalendarInfo>>listCalendars(final CalendarInfo.ContentType type,String token){
        if(type==CalendarInfo.ContentType.TASKS)return value(new CalendarPage<CalendarInfo>(new ArrayList<CalendarInfo>(),null,null));
        if(!granted(Manifest.permission.READ_CALENDAR))return failed(CalendarError.PERMISSION_DENIED,"Calendar read permission is required");
        return background(new BlockingOperation<CalendarPage<CalendarInfo>>(){
            public CalendarPage<CalendarInfo>run(){List<CalendarInfo>items=new ArrayList<CalendarInfo>();Cursor c=null;
                try{c=resolver().query(Calendars.CONTENT_URI,new String[]{Calendars._ID,Calendars.CALENDAR_DISPLAY_NAME,Calendars.ACCOUNT_NAME,Calendars.CALENDAR_COLOR,Calendars.CALENDAR_TIME_ZONE,Calendars.IS_PRIMARY,Calendars.CALENDAR_ACCESS_LEVEL},null,null,null);
                    while(c!=null&&c.moveToNext())items.add(new CalendarInfo().setId(String.valueOf(c.getLong(0))).setSourceId(getId()).setName(c.getString(1)).setAccountId(c.getString(2)).setOwner(c.getString(2)).setColor(c.getInt(3)).setTimeZone(c.getString(4)==null?null:ZoneId.of(c.getString(4))).setPrimary(c.getInt(5)!=0).setReadOnly(c.getInt(6)<Calendars.CAL_ACCESS_CONTRIBUTOR).setContentType(type).setCapabilities(capabilities));
                    return new CalendarPage<CalendarInfo>(items,null,String.valueOf(System.currentTimeMillis()));
                }finally{if(c!=null)c.close();}}
        });
    }
    public AsyncResource<CalendarPage<CalendarEvent>> queryEvents(final CalendarQuery query) {
        if (!granted(Manifest.permission.READ_CALENDAR)) {
            return failed(CalendarError.PERMISSION_DENIED, "Calendar read permission is required");
        }
        return background(new BlockingOperation<CalendarPage<CalendarEvent>>() {
            public CalendarPage<CalendarEvent> run() throws CalendarException {
                return queryEventsBlocking(query);
            }
        });
    }

    private CalendarPage<CalendarEvent> queryEventsBlocking(CalendarQuery query)
            throws CalendarException {
        List<CalendarEvent> items = new ArrayList<CalendarEvent>();
        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<String>();
        boolean expandInstances = query != null
                && (query.getStartTime() != null || query.getEndTime() != null);
        String calendarColumn = expandInstances ? Instances.CALENDAR_ID : Events.CALENDAR_ID;
        if (query != null && query.getCalendarId() != null) {
            where.append(" AND ").append(calendarColumn).append("=?");
            args.add(query.getCalendarId());
        }
        where.append(" AND ").append(Events.DELETED).append("=0");
        if (!expandInstances && query != null && query.getStartTime() != null) {
            where.append(" AND (").append(Events.DTEND).append(" IS NULL OR ")
                    .append(Events.DTEND).append(">=?)");
            args.add(String.valueOf(query.getStartTime().toEpochMilli()));
        }
        if (!expandInstances && query != null && query.getEndTime() != null) {
            where.append(" AND ").append(Events.DTSTART).append("<=?");
            args.add(String.valueOf(query.getEndTime().toEpochMilli()));
        }
        Cursor c = null;
        try {
            Uri uri = Events.CONTENT_URI;
            String[] columns = EVENT_COLUMNS;
            String sort = Events.DTSTART + " ASC";
            if (expandInstances) {
                long begin = query.getStartTime() == null ? 0L : query.getStartTime().toEpochMilli();
                long end = query.getEndTime() == null ? Long.MAX_VALUE : query.getEndTime().toEpochMilli();
                Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, begin);
                ContentUris.appendId(builder, end);
                uri = builder.build();
                columns = INSTANCE_COLUMNS;
                sort = Instances.BEGIN + " ASC";
            }
            c = resolver().query(uri, columns, where.toString(),
                    args.toArray(new String[args.size()]), sort);
            while (c != null && c.moveToNext()) {
                long startMillis = c.getLong(5);
                long endMillis = eventEndMillis(startMillis,
                        c.isNull(6) ? null : Long.valueOf(c.getLong(6)), c.getString(7));
                if (query != null && query.getStartTime() != null
                        && endMillis < query.getStartTime().toEpochMilli()) {
                    continue;
                }
                items.add(event(c, startMillis, endMillis));
            }
            return new CalendarPage<CalendarEvent>(items, null,
                    String.valueOf(System.currentTimeMillis()));
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    public AsyncResource<CalendarEvent> getEvent(final String calendarId, final String eventId) {
        if (!granted(Manifest.permission.READ_CALENDAR)) {
            return failed(CalendarError.PERMISSION_DENIED, "Calendar read permission is required");
        }
        if (eventId == null) {
            return failed(CalendarError.INVALID_ARGUMENT, "eventId required");
        }
        return background(new BlockingOperation<CalendarEvent>() {
            public CalendarEvent run() throws CalendarException {
                String selection = Events._ID + "=?";
                List<String> args = new ArrayList<String>();
                args.add(eventId);
                if (calendarId != null) {
                    selection += " AND " + Events.CALENDAR_ID + "=?";
                    args.add(calendarId);
                }
                Cursor c = null;
                try {
                    c = resolver().query(Events.CONTENT_URI, EVENT_COLUMNS, selection,
                            args.toArray(new String[args.size()]), null);
                    if (c != null && c.moveToFirst()) {
                        return event(c, c.getLong(5), eventEndMillis(c.getLong(5),
                                c.isNull(6) ? null : Long.valueOf(c.getLong(6)), c.getString(7)));
                    }
                    throw new CalendarException(CalendarError.NOT_FOUND, "Event not found");
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        });
    }
    public AsyncResource<CalendarEvent>saveEvent(final CalendarEvent event,final CalendarMutationScope scope){
        if(!granted(Manifest.permission.WRITE_CALENDAR))return failed(CalendarError.PERMISSION_DENIED,"Calendar write permission is required");if(event==null||event.getCalendarId()==null)return failed(CalendarError.INVALID_ARGUMENT,"event and calendarId required");
        if(event.getRecurrence()!=null&&(event.getStart()==null||event.getEnd()==null))return failed(CalendarError.INVALID_ARGUMENT,"Recurring events require start and end");
        if(event.getRecurrence()!=null&&millis(event.getEnd())<millis(event.getStart()))return failed(CalendarError.INVALID_ARGUMENT,"Event end must not precede start");
        return background(new BlockingOperation<CalendarEvent>(){public CalendarEvent run()throws Exception{
            if(instanceScoped(scope)&&event.getId()!=null&&(event.getRecurrence()!=null||hasRecurrence(event.getId()))){
                throw new CalendarException(CalendarError.NOT_SUPPORTED,
                        "Android Calendar Provider cannot mutate a single occurrence; use CalendarMutationScope.ALL");
            }
            ContentValues v=new ContentValues();v.put(Events.CALENDAR_ID,Long.valueOf(event.getCalendarId()));v.put(Events.TITLE,event.getTitle());v.put(Events.DESCRIPTION,event.getDescription());v.put(Events.EVENT_LOCATION,event.getLocation());if(event.getStart()!=null){v.put(Events.ALL_DAY,Integer.valueOf(event.getStart().isAllDay()?1:0));v.put(Events.DTSTART,Long.valueOf(millis(event.getStart())));v.put(Events.EVENT_TIMEZONE,event.getStart().isAllDay()?"UTC":event.getStart().getDateTime().getZone().getId());}
            if(event.getRecurrence()!=null){long durationMillis=millis(event.getEnd())-millis(event.getStart());v.putNull(Events.DTEND);v.put(Events.DURATION,providerDuration(durationMillis));v.put(Events.RRULE,ICalendarCodec.writeRecurrenceRule(event.getRecurrence()));}else{v.putNull(Events.DURATION);v.putNull(Events.RRULE);if(event.getEnd()!=null)v.put(Events.DTEND,Long.valueOf(millis(event.getEnd())));}
            boolean create=event.getId()==null;if(create){Uri uri=resolver().insert(Events.CONTENT_URI,v);event.setId(String.valueOf(ContentUris.parseId(uri)));}else resolver().update(ContentUris.withAppendedId(Events.CONTENT_URI,Long.parseLong(event.getId())),v,null,null);replaceDetails(event);event.setSourceId(getId()).setVersion(String.valueOf(System.currentTimeMillis()));fireChange(new CalendarChange(getId(),event.getCalendarId(),event.getId(),CalendarChange.EntityType.EVENT,create?CalendarChange.ChangeType.CREATED:CalendarChange.ChangeType.UPDATED));return event;}});
    }
    public AsyncResource<Boolean>deleteEvent(final String calendarId,final String eventId,final CalendarMutationScope scope,String version){
        if(!granted(Manifest.permission.WRITE_CALENDAR))return failed(CalendarError.PERMISSION_DENIED,"Calendar write permission is required");if(eventId==null)return failed(CalendarError.INVALID_ARGUMENT,"eventId required");return background(new BlockingOperation<Boolean>(){public Boolean run()throws Exception{
            if(instanceScoped(scope)&&hasRecurrence(eventId)){
                throw new CalendarException(CalendarError.NOT_SUPPORTED,
                        "Android Calendar Provider cannot delete a single occurrence; use CalendarMutationScope.ALL");
            }
            boolean deleted=resolver().delete(ContentUris.withAppendedId(Events.CONTENT_URI,Long.parseLong(eventId)),null,null)>0;if(deleted)fireChange(new CalendarChange(getId(),calendarId,eventId,CalendarChange.EntityType.EVENT,CalendarChange.ChangeType.DELETED));return Boolean.valueOf(deleted);}});
    }
    public AsyncResource<List<FreeBusyInterval>>queryFreeBusy(List<String>ids,Instant start,Instant end){
        final AsyncResource<List<FreeBusyInterval>>result=new AsyncResource<List<FreeBusyInterval>>();
        queryEvents(new CalendarQuery().setStartTime(start).setEndTime(end)).ready(new com.codename1.util.SuccessCallback<CalendarPage<CalendarEvent>>(){
            public void onSucess(CalendarPage<CalendarEvent>page){List<FreeBusyInterval>out=new ArrayList<FreeBusyInterval>();for(CalendarEvent event:page.getItems())if(event.getAvailability()!=CalendarEvent.Availability.FREE&&event.getStart()!=null&&event.getEnd()!=null&&!event.getStart().isAllDay()&&!event.getEnd().isAllDay())out.add(new FreeBusyInterval(event.getStart().getDateTime().toInstant(),event.getEnd().getDateTime().toInstant(),event.getAvailability()));result.complete(out);}
        }).except(new com.codename1.util.SuccessCallback<Throwable>(){public void onSucess(Throwable error){result.error(error);}});
        return result;
    }

    private void readDetails(CalendarEvent event){Cursor c=null;try{c=resolver().query(Attendees.CONTENT_URI,new String[]{Attendees.ATTENDEE_NAME,Attendees.ATTENDEE_EMAIL,Attendees.ATTENDEE_TYPE,Attendees.ATTENDEE_STATUS},Attendees.EVENT_ID+"=?",new String[]{event.getId()},null);while(c!=null&&c.moveToNext()){CalendarAttendee a=new CalendarAttendee().setName(c.getString(0)).setEmail(c.getString(1));if(c.getInt(2)==Attendees.TYPE_OPTIONAL)a.setRole(CalendarAttendee.Role.OPTIONAL);else if(c.getInt(2)==Attendees.TYPE_RESOURCE)a.setRole(CalendarAttendee.Role.RESOURCE);int s=c.getInt(3);a.setResponse(s==Attendees.ATTENDEE_STATUS_ACCEPTED?CalendarAttendee.Response.ACCEPTED:s==Attendees.ATTENDEE_STATUS_DECLINED?CalendarAttendee.Response.DECLINED:s==Attendees.ATTENDEE_STATUS_TENTATIVE?CalendarAttendee.Response.TENTATIVE:CalendarAttendee.Response.NEEDS_ACTION);event.addAttendee(a);}}finally{if(c!=null)c.close();}try{c=resolver().query(Reminders.CONTENT_URI,new String[]{Reminders.MINUTES,Reminders.METHOD},Reminders.EVENT_ID+"=?",new String[]{event.getId()},null);while(c!=null&&c.moveToNext())event.addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(c.getInt(0))).setMethod(c.getInt(1)==Reminders.METHOD_EMAIL?CalendarAlarm.Method.EMAIL:CalendarAlarm.Method.ALERT));}finally{if(c!=null)c.close();}}
    private void replaceDetails(CalendarEvent event){resolver().delete(Reminders.CONTENT_URI,Reminders.EVENT_ID+"=?",new String[]{event.getId()});for(CalendarAlarm alarm:event.getAlarms())if(alarm.getTimeBefore()!=null){ContentValues v=new ContentValues();v.put(Reminders.EVENT_ID,Long.valueOf(event.getId()));v.put(Reminders.MINUTES,Long.valueOf(alarm.getTimeBefore().getSeconds()/60L));v.put(Reminders.METHOD,Integer.valueOf(alarm.getMethod()==CalendarAlarm.Method.EMAIL?Reminders.METHOD_EMAIL:Reminders.METHOD_ALERT));resolver().insert(Reminders.CONTENT_URI,v);}resolver().delete(Attendees.CONTENT_URI,Attendees.EVENT_ID+"=?",new String[]{event.getId()});for(CalendarAttendee a:event.getAttendees()){ContentValues v=new ContentValues();v.put(Attendees.EVENT_ID,Long.valueOf(event.getId()));v.put(Attendees.ATTENDEE_NAME,a.getName());v.put(Attendees.ATTENDEE_EMAIL,a.getEmail());v.put(Attendees.ATTENDEE_TYPE,Integer.valueOf(a.getRole()==CalendarAttendee.Role.OPTIONAL?Attendees.TYPE_OPTIONAL:a.getRole()==CalendarAttendee.Role.RESOURCE?Attendees.TYPE_RESOURCE:Attendees.TYPE_REQUIRED));resolver().insert(Attendees.CONTENT_URI,v);}}
    private static boolean instanceScoped(CalendarMutationScope scope){
        return scope==CalendarMutationScope.THIS_INSTANCE||scope==CalendarMutationScope.THIS_AND_FUTURE;
    }
    private boolean hasRecurrence(String eventId){
        Cursor c=null;
        try{
            c=resolver().query(ContentUris.withAppendedId(Events.CONTENT_URI,Long.parseLong(eventId)),
                    new String[]{Events.RRULE,Events.RDATE},null,null,null);
            if(c!=null&&c.moveToFirst()){
                String rrule=c.isNull(0)?null:c.getString(0);
                String rdate=c.isNull(1)?null:c.getString(1);
                return (rrule!=null&&rrule.length()>0)||(rdate!=null&&rdate.length()>0);
            }
            return false;
        }finally{if(c!=null)c.close();}
    }
    private ContentResolver resolver(){return context.getContentResolver();}private boolean granted(String permission){return ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED;}private static CalendarDateTime allDay(long time){LocalDate date=ZonedDateTime.ofInstant(Instant.ofEpochMilli(time),ZoneOffset.UTC).toLocalDateTime().toLocalDate();return CalendarDateTime.allDay(date);}private static long millis(CalendarDateTime value){if(!value.isAllDay())return value.getDateTime().toInstant().toEpochMilli();return ZonedDateTime.of(value.getDate().atTime(0,0),ZoneOffset.UTC).toInstant().toEpochMilli();}
    private CalendarEvent event(Cursor c, long startMillis, long endMillis) throws CalendarException {
        CalendarEvent event = new CalendarEvent().setId(String.valueOf(c.getLong(0)))
                .setCalendarId(String.valueOf(c.getLong(1))).setSourceId(getId())
                .setTitle(c.getString(2)).setDescription(c.getString(3))
                .setLocation(c.getString(4)).setVersion(String.valueOf(c.getLong(13)));
        if (c.getInt(9) != 0) {
            event.setStart(allDay(startMillis)).setEnd(allDay(endMillis));
        } else {
            String zone = c.getString(8);
            if (zone == null) {
                zone = TimeZone.getDefault().getID();
            }
            event.setStart(CalendarDateTime.instant(Instant.ofEpochMilli(startMillis), ZoneId.of(zone)))
                    .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(endMillis), ZoneId.of(zone)));
        }
        if (c.getString(10) != null) {
            event.setRecurrence(ICalendarCodec.readRecurrenceRule(c.getString(10)));
        }
        if (c.getInt(11) == Events.STATUS_CANCELED) {
            event.setStatus(CalendarEvent.Status.CANCELED);
        } else if (c.getInt(11) == Events.STATUS_TENTATIVE) {
            event.setStatus(CalendarEvent.Status.TENTATIVE);
        }
        event.setAvailability(c.getInt(12) == Events.AVAILABILITY_FREE
                ? CalendarEvent.Availability.FREE : c.getInt(12) == Events.AVAILABILITY_TENTATIVE
                ? CalendarEvent.Availability.TENTATIVE : CalendarEvent.Availability.BUSY);
        readDetails(event);
        return event;
    }

    private static long eventEndMillis(long startMillis, Long endMillis, String duration) {
        if (endMillis != null) {
            return endMillis.longValue();
        }
        if (duration == null || duration.length() == 0) {
            return startMillis;
        }
        try {
            return startMillis + parseDurationMillis(duration);
        } catch (IllegalArgumentException ex) {
            return startMillis;
        }
    }

    private static long parseDurationMillis(String duration) {
        int length = duration.length();
        int position = 0;
        int sign = 1;
        if (position < length && (duration.charAt(position) == '+' || duration.charAt(position) == '-')) {
            if (duration.charAt(position++) == '-') {
                sign = -1;
            }
        }
        if (position >= length || (duration.charAt(position) != 'P' && duration.charAt(position) != 'p')) {
            throw new IllegalArgumentException("Invalid event duration");
        }
        position++;
        long millis = 0L;
        long amount = 0L;
        boolean digits = false;
        boolean found = false;
        while (position < length) {
            char ch = duration.charAt(position++);
            if (ch >= '0' && ch <= '9') {
                amount = amount * 10L + ch - '0';
                digits = true;
                continue;
            }
            if ((ch == 'T' || ch == 't') && !digits) {
                continue;
            }
            if (ch >= 'a' && ch <= 'z') {
                ch = (char) (ch - ('a' - 'A'));
            }
            long factor;
            if (ch == 'W') {
                factor = 7L * 24L * 60L * 60L * 1000L;
            } else if (ch == 'D') {
                factor = 24L * 60L * 60L * 1000L;
            } else if (ch == 'H') {
                factor = 60L * 60L * 1000L;
            } else if (ch == 'M') {
                factor = 60L * 1000L;
            } else if (ch == 'S') {
                factor = 1000L;
            } else {
                throw new IllegalArgumentException("Invalid event duration");
            }
            if (!digits) {
                throw new IllegalArgumentException("Invalid event duration");
            }
            millis += amount * factor;
            amount = 0L;
            digits = false;
            found = true;
        }
        if (!found || digits) {
            throw new IllegalArgumentException("Invalid event duration");
        }
        return sign * millis;
    }
    private static String providerDuration(long durationMillis) {
        long seconds = durationMillis / 1000L;
        if (durationMillis % 1000L != 0L) {
            seconds++;
        }
        return "P" + seconds + "S";
    }
    private interface BlockingOperation<T>{T run()throws Exception;}
    private static <T>AsyncResource<T>background(final BlockingOperation<T>operation){final AsyncResource<T>out=new AsyncResource<T>();Display.getInstance().scheduleBackgroundTask(new Runnable(){public void run(){try{out.complete(operation.run());}catch(Throwable error){out.error(calendarFailure(error));}}});return out;}
    private static Throwable calendarFailure(Throwable error){return error instanceof CalendarException?error:new CalendarException(CalendarError.UNKNOWN,error.getMessage(),error);}
    private static <T>AsyncResource<T>value(T value){AsyncResource<T>out=new AsyncResource<T>();out.complete(value);return out;}private static <T>AsyncResource<T>failed(CalendarError type,String message){AsyncResource<T>out=new AsyncResource<T>();out.error(new CalendarException(type,message));return out;}private static <T>AsyncResource<T>failure(Throwable error){AsyncResource<T>out=new AsyncResource<T>();out.error(calendarFailure(error));return out;}
}
