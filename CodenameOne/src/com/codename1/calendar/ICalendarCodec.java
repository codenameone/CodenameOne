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
package com.codename1.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.codename1.util.StringUtil;

/// Reads and writes the interoperable portion of RFC 5545 used by events,
/// tasks, CalDAV, and file import/export. Unknown X-properties are preserved
/// in provider data so a read/write cycle doesn't silently discard extensions.
public final class ICalendarCodec {
    private static final String CRLF = "\r\n";
    private ICalendarCodec() {}

    public static String writeEvent(CalendarEvent event) {
        StringBuilder out = beginCalendar();
        append(out, "BEGIN:VEVENT");
        writeCommon(out, event.getId(), event.getTitle(), event.getDescription(), event.getLocation(),
                event.getStart(), event.getEnd(), event.getRecurrence(), event.getProviderData());
        if (event.getUrl() != null) append(out, "URL:" + escape(event.getUrl()));
        append(out, "STATUS:" + event.getStatus().name());
        append(out, "TRANSP:" + (event.getAvailability() == CalendarEvent.Availability.FREE ? "TRANSPARENT" : "OPAQUE"));
        append(out, "CLASS:" + event.getPrivacy().name());
        for (CalendarAttendee attendee : event.getAttendees()) writeAttendee(out, attendee);
        for (CalendarAttachment attachment : event.getAttachments()) writeAttachment(out, attachment);
        if (event.getConference() != null && event.getConference().getJoinUrl() != null) {
            append(out, "CONFERENCE;VALUE=URI:" + escape(event.getConference().getJoinUrl()));
        }
        for (CalendarAlarm alarm : event.getAlarms()) writeAlarm(out, alarm);
        append(out, "END:VEVENT");
        return endCalendar(out);
    }

    public static String writeTask(CalendarTask task) {
        StringBuilder out = beginCalendar();
        append(out, "BEGIN:VTODO");
        writeCommon(out, task.getId(), task.getTitle(), task.getDescription(), task.getLocation(),
                task.getStart(), task.getDue(), task.getRecurrence(), task.getProviderData());
        append(out, "STATUS:" + (task.isCompleted() ? "COMPLETED" : "NEEDS-ACTION"));
        if (task.getCompletionTime() != null) append(out, "COMPLETED:" + utc(task.getCompletionTime().longValue()));
        if (task.getPriority() > 0) append(out, "PRIORITY:" + task.getPriority());
        for (CalendarAttachment attachment : task.getAttachments()) writeAttachment(out, attachment);
        for (CalendarAlarm alarm : task.getAlarms()) writeAlarm(out, alarm);
        append(out, "END:VTODO");
        return endCalendar(out);
    }

    public static CalendarEvent readEvent(String text) throws CalendarException {
        Component component = component(text, "VEVENT");
        CalendarEvent out = new CalendarEvent();
        readCommon(component, out, null);
        out.setUrl(value(component, "URL"));
        out.setStatus(enumValue(CalendarEvent.Status.class, value(component, "STATUS"), CalendarEvent.Status.CONFIRMED));
        if ("TRANSPARENT".equalsIgnoreCase(value(component, "TRANSP"))) out.setAvailability(CalendarEvent.Availability.FREE);
        out.setPrivacy(enumValue(CalendarEvent.Privacy.class, value(component, "CLASS"), CalendarEvent.Privacy.DEFAULT));
        for (Property property : component.all("ATTENDEE")) out.addAttendee(readAttendee(property));
        for (Property property : component.all("ATTACH")) out.addAttachment(readAttachment(property));
        Property conference = component.first("CONFERENCE");
        if (conference != null) out.setConference(new CalendarConference().setJoinUrl(unescape(conference.value)));
        for (Component alarm : component.children) if ("VALARM".equals(alarm.name)) out.addAlarm(readAlarm(alarm));
        for (Property property : component.properties) if (property.name.startsWith("X-")) out.putProviderData(property.name, property.value);
        return out;
    }

    public static CalendarTask readTask(String text) throws CalendarException {
        Component component = component(text, "VTODO");
        CalendarTask out = new CalendarTask();
        readCommon(component, null, out);
        out.setCompleted("COMPLETED".equalsIgnoreCase(value(component, "STATUS")));
        String completed = value(component, "COMPLETED");
        if (completed != null) out.setCompletionTime(Long.valueOf(parseDateTime(completed, null).getTimestamp()));
        String priority = value(component, "PRIORITY");
        if (priority != null) try { out.setPriority(Integer.parseInt(priority)); } catch (NumberFormatException ignored) {}
        for (Property property : component.all("ATTACH")) out.addAttachment(readAttachment(property));
        for (Component alarm : component.children) if ("VALARM".equals(alarm.name)) out.addAlarm(readAlarm(alarm));
        for (Property property : component.properties) if (property.name.startsWith("X-")) out.putProviderData(property.name, property.value);
        return out;
    }


    /// Serializes a recurrence rule without the {@code RRULE:} prefix.
    public static String writeRecurrenceRule(CalendarRecurrenceRule rule) {
        if (rule == null || rule.getFrequency() == null) throw new IllegalArgumentException("frequency required");
        return recurrence(rule);
    }


    /// Parses a recurrence rule with or without an {@code RRULE:} prefix.
    public static CalendarRecurrenceRule readRecurrenceRule(String value) throws CalendarException {
        if (value != null && value.toUpperCase().startsWith("RRULE:")) value = value.substring(6);
        return parseRecurrence(value);
    }

    private static void readCommon(Component component, CalendarEvent event, CalendarTask task) throws CalendarException {
        String id = value(component, "UID"), title = value(component, "SUMMARY");
        String description = value(component, "DESCRIPTION"), location = value(component, "LOCATION");
        Property start = component.first("DTSTART");
        Property end = component.first(event == null ? "DUE" : "DTEND");
        CalendarDateTime startValue = start == null ? null : parseDateTime(start.value, start.params.get("TZID"));
        CalendarDateTime endValue = end == null ? null : parseDateTime(end.value, end.params.get("TZID"));
        CalendarRecurrenceRule recurrence = parseRecurrence(value(component, "RRULE"));
        if (event != null) event.setId(id).setTitle(title).setDescription(description).setLocation(location)
                .setStart(startValue).setEnd(endValue).setRecurrence(recurrence);
        else task.setId(id).setTitle(title).setDescription(description).setLocation(location)
                .setStart(startValue).setDue(endValue).setRecurrence(recurrence);
    }

    private static void writeCommon(StringBuilder out, String id, String title, String description, String location,
            CalendarDateTime start, CalendarDateTime end, CalendarRecurrenceRule recurrence, Map<String,String> extensions) {
        append(out, "UID:" + escape(id == null ? String.valueOf(System.currentTimeMillis()) + "@codenameone" : id));
        append(out, "DTSTAMP:" + utc(System.currentTimeMillis()));
        if (title != null) append(out, "SUMMARY:" + escape(title));
        if (description != null) append(out, "DESCRIPTION:" + escape(description));
        if (location != null) append(out, "LOCATION:" + escape(location));
        if (start != null) writeDateTime(out, "DTSTART", start);
        if (end != null) writeDateTime(out, endName(out), end);
        if (recurrence != null) append(out, "RRULE:" + recurrence(recurrence));
        for (Map.Entry<String,String> entry : extensions.entrySet()) {
            if (entry.getKey().toUpperCase().startsWith("X-")) append(out, entry.getKey().toUpperCase() + ":" + escape(entry.getValue()));
        }
    }

    private static String endName(StringBuilder out) {
        return out.toString().indexOf("BEGIN:VTODO") >= 0 ? "DUE" : "DTEND";
    }

    private static void writeDateTime(StringBuilder out, String name, CalendarDateTime value) {
        if (value.isAllDay()) {
            CalendarDate date = value.getDate();
            append(out, name + ";VALUE=DATE:" + digits(date));
        } else if ("UTC".equals(value.getTimeZoneId()) || "GMT".equals(value.getTimeZoneId())) {
            append(out, name + ":" + utc(value.getTimestamp()));
        } else {
            append(out, name + ";TZID=" + value.getTimeZoneId() + ":" + local(value.getTimestamp(), value.getTimeZoneId()));
        }
    }

    private static String recurrence(CalendarRecurrenceRule rule) {
        StringBuilder out = new StringBuilder("FREQ=").append(rule.getFrequency().name());
        if (rule.getInterval() != 1) out.append(";INTERVAL=").append(rule.getInterval());
        if (rule.getCount() != null) out.append(";COUNT=").append(rule.getCount());
        if (rule.getUntil() != null) out.append(";UNTIL=").append(formatRecurrenceUntil(rule.getUntil()));
        appendNumbers(out, "BYDAY", rule.getDaysOfWeek(), true);
        appendNumbers(out, "BYMONTHDAY", rule.getDaysOfMonth(), false);
        appendNumbers(out, "BYMONTH", rule.getMonths(), false);
        return out.toString();
    }

    private static CalendarRecurrenceRule parseRecurrence(String value) throws CalendarException {
        if (value == null) return null;
        CalendarRecurrenceRule out = new CalendarRecurrenceRule();
        String[] parts = CalendarDateUtil.split(value, ';');
        for (String part : parts) {
            int equals = part.indexOf('=');
            if (equals < 1) continue;
            String key = part.substring(0, equals).toUpperCase(), data = part.substring(equals + 1);
            try {
                if ("FREQ".equals(key)) out.setFrequency(CalendarRecurrenceRule.Frequency.valueOf(data.toUpperCase()));
                else if ("INTERVAL".equals(key)) out.setInterval(Integer.parseInt(data));
                else if ("COUNT".equals(key)) out.setCount(Integer.valueOf(data));
                else if ("UNTIL".equals(key)) out.setUntil(parseDateTime(data, null));
                else if ("BYDAY".equals(key)) for (String item : CalendarDateUtil.split(data, ',')) out.addDayOfWeek(day(item));
                else if ("BYMONTHDAY".equals(key)) for (String item : CalendarDateUtil.split(data, ',')) out.addDayOfMonth(Integer.parseInt(item));
                else if ("BYMONTH".equals(key)) for (String item : CalendarDateUtil.split(data, ',')) out.addMonth(Integer.parseInt(item));
            } catch (IllegalArgumentException ex) {
                throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "Invalid recurrence rule: " + value, ex);
            }
        }
        if (out.getFrequency() == null) throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "RRULE has no frequency");
        return out;
    }

    private static void writeAttendee(StringBuilder out, CalendarAttendee attendee) {
        StringBuilder line = new StringBuilder("ATTENDEE");
        if (attendee.getName() != null) line.append(";CN=").append(quote(attendee.getName()));
        line.append(";ROLE=").append(attendee.getRole() == CalendarAttendee.Role.OPTIONAL ? "OPT-PARTICIPANT" : attendee.getRole() == CalendarAttendee.Role.RESOURCE ? "NON-PARTICIPANT" : "REQ-PARTICIPANT");
        line.append(";PARTSTAT=").append(attendee.getResponse().name().replace('_', '-'));
        line.append(':');
        String uri = attendee.getUri() != null ? attendee.getUri() : attendee.getEmail();
        if (uri != null && uri.indexOf(':') < 0) uri = "mailto:" + uri;
        line.append(uri == null ? "" : uri);
        append(out, line.toString());
    }

    private static CalendarAttendee readAttendee(Property property) {
        String uri = unescape(property.value), email = uri;
        if (email != null && email.toLowerCase().startsWith("mailto:")) email = email.substring(7);
        CalendarAttendee out = new CalendarAttendee().setUri(uri).setEmail(email).setName(property.params.get("CN"));
        String role = property.params.get("ROLE");
        if ("OPT-PARTICIPANT".equals(role)) out.setRole(CalendarAttendee.Role.OPTIONAL);
        else if ("NON-PARTICIPANT".equals(role)) out.setRole(CalendarAttendee.Role.RESOURCE);
        String response = property.params.get("PARTSTAT");
        if (response != null) try { out.setResponse(CalendarAttendee.Response.valueOf(response.replace('-', '_'))); } catch (IllegalArgumentException ignored) {}
        return out;
    }

    private static void writeAlarm(StringBuilder out, CalendarAlarm alarm) {
        append(out, "BEGIN:VALARM");
        append(out, "ACTION:" + (alarm.getMethod() == CalendarAlarm.Method.EMAIL ? "EMAIL" : alarm.getMethod() == CalendarAlarm.Method.AUDIO ? "AUDIO" : "DISPLAY"));
        if (alarm.getMinutesBefore() != null) append(out, "TRIGGER:-PT" + alarm.getMinutesBefore() + "M");
        else if (alarm.getAbsoluteTime() != null) append(out, "TRIGGER;VALUE=DATE-TIME:" + utc(alarm.getAbsoluteTime().longValue()));
        append(out, "END:VALARM");
    }

    private static CalendarAlarm readAlarm(Component component) throws CalendarException {
        String action = value(component, "ACTION"), trigger = value(component, "TRIGGER");
        CalendarAlarm out = new CalendarAlarm();
        if ("EMAIL".equals(action)) out.setMethod(CalendarAlarm.Method.EMAIL);
        else if ("AUDIO".equals(action)) out.setMethod(CalendarAlarm.Method.AUDIO);
        if (trigger != null && trigger.startsWith("-PT") && trigger.endsWith("M")) {
            try { out.setMinutesBefore(Integer.valueOf(trigger.substring(3, trigger.length() - 1))); } catch (NumberFormatException ignored) {}
        } else if (trigger != null) out.setAbsoluteTime(Long.valueOf(parseDateTime(trigger, null).getTimestamp()));
        return out;
    }

    private static void writeAttachment(StringBuilder out, CalendarAttachment attachment) {
        if (attachment.getUri() == null) return;
        StringBuilder line = new StringBuilder("ATTACH");
        if (attachment.getMimeType() != null) line.append(";FMTTYPE=").append(attachment.getMimeType());
        line.append(':').append(escape(attachment.getUri()));
        append(out, line.toString());
    }

    private static CalendarAttachment readAttachment(Property property) {
        return new CalendarAttachment().setUri(unescape(property.value)).setMimeType(property.params.get("FMTTYPE"));
    }

    private static Component component(String input, String wanted) throws CalendarException {
        if (input == null) throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "Calendar data is null");
        List<String> lines = unfold(input);
        Component root = new Component("ROOT"), current = root;
        List<Component> stack = new ArrayList<Component>();
        for (String line : lines) {
            if (line.startsWith("BEGIN:")) {
                Component child = new Component(line.substring(6).toUpperCase());
                current.children.add(child); stack.add(current); current = child;
            } else if (line.startsWith("END:")) {
                if (!stack.isEmpty()) current = stack.remove(stack.size() - 1);
            } else {
                int colon = colon(line); if (colon < 0) continue;
                String left = line.substring(0, colon), data = line.substring(colon + 1);
                String[] fields = CalendarDateUtil.split(left, ';');
                Property property = new Property(fields[0].toUpperCase(), data);
                for (int i = 1; i < fields.length; i++) {
                    int equals = fields[i].indexOf('=');
                    if (equals > 0) property.params.put(fields[i].substring(0, equals).toUpperCase(), unquote(fields[i].substring(equals + 1)));
                }
                current.properties.add(property);
            }
        }
        Component found = find(root, wanted);
        if (found == null) throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "No " + wanted + " component found");
        return found;
    }

    private static Component find(Component component, String name) {
        if (name.equals(component.name)) return component;
        for (Component child : component.children) { Component result = find(child, name); if (result != null) return result; }
        return null;
    }

    private static CalendarDateTime parseDateTime(String value, String zone) throws CalendarException {
        try {
            if (value.length() == 8) return CalendarDateTime.allDay(new CalendarDate(Integer.parseInt(value.substring(0, 4)), Integer.parseInt(value.substring(4, 6)), Integer.parseInt(value.substring(6, 8))));
            boolean zulu = value.endsWith("Z");
            String timeZone = zulu || zone == null ? "UTC" : zone;
            return CalendarDateTime.instant(CalendarDateUtil.parseDateTime(value, timeZone), timeZone);
        } catch (IllegalArgumentException ex) {
            throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "Invalid calendar date: " + value, ex);
        }
    }

    private static List<String> unfold(String input) {
        String normalized = input.replace("\r\n", "\n").replace('\r', '\n');
        String[] raw = CalendarDateUtil.split(normalized, '\n');
        List<String> out = new ArrayList<String>();
        for (String line : raw) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && !out.isEmpty()) {
                int last = out.size() - 1; out.set(last, out.get(last) + line.substring(1));
            } else if (line.length() > 0) out.add(line);
        }
        return out;
    }

    private static void append(StringBuilder out, String line) {
        byte[] bytes = StringUtil.getBytes(line);
        if (bytes.length <= 75) { out.append(line).append(CRLF); return; }
        int start = 0, chars = line.length();
        while (start < chars) {
            int end = Math.min(chars, start + 72);
            while (end > start && utf8Length(line.substring(start, end)) > 74) end--;
            if (start > 0) out.append(' ');
            out.append(line.substring(start, end)).append(CRLF); start = end;
        }
    }

    private static int utf8Length(String value) {
        try { return value.getBytes("UTF-8").length; } catch (java.io.UnsupportedEncodingException ex) { return value.length(); }
    }

    private static String escape(String value) {
        if (value == null) return null;
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    private static String unescape(String value) {
        if (value == null) return null;
        StringBuilder out = new StringBuilder(); boolean slash = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (slash) { out.append(c == 'n' || c == 'N' ? '\n' : c); slash = false; }
            else if (c == '\\') slash = true; else out.append(c);
        }
        if (slash) out.append('\\');
        return out.toString();
    }

    private static String quote(String value) { return '"' + value.replace("\"", "\\\"") + '"'; }
    private static String unquote(String value) { return value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"' ? value.substring(1, value.length() - 1) : value; }
    private static int colon(String value) { boolean quoted = false; for (int i=0;i<value.length();i++){char c=value.charAt(i);if(c=='"')quoted=!quoted;else if(c==':'&&!quoted)return i;}return -1; }
    private static String value(Component component, String name) { Property property = component.first(name); return property == null ? null : unescape(property.value); }
    private static String digits(CalendarDate date) { return pad(date.getYear(), 4) + pad(date.getMonth(), 2) + pad(date.getDay(), 2); }
    private static String pad(int value, int count) { String s=String.valueOf(value);while(s.length()<count)s="0"+s;return s; }
    private static String utc(long value) { return format(value, "UTC") + "Z"; }
    private static String local(long value, String zone) { return format(value, zone); }
    private static String format(long value, String zone) { return CalendarDateUtil.formatBasic(value, zone); }
    private static String formatRecurrenceUntil(CalendarDateTime value) { return value.isAllDay() ? digits(value.getDate()) : utc(value.getTimestamp()); }
    private static int day(String value) { String v=value.length()>2?value.substring(value.length()-2):value;String[] days={"MO","TU","WE","TH","FR","SA","SU"};for(int i=0;i<days.length;i++)if(days[i].equals(v))return i+1;throw new IllegalArgumentException(value); }
    private static void appendNumbers(StringBuilder out, String name, List<Integer> values, boolean weekdays) { if(values.isEmpty())return;out.append(';').append(name).append('=');for(int i=0;i<values.size();i++){if(i>0)out.append(',');int v=values.get(i).intValue();out.append(weekdays?new String[]{"MO","TU","WE","TH","FR","SA","SU"}[v-1]:String.valueOf(v));} }
    private static StringBuilder beginCalendar() { return new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Codename One//Calendar API//EN\r\nCALSCALE:GREGORIAN\r\n"); }
    private static String endCalendar(StringBuilder out) { return out.append("END:VCALENDAR\r\n").toString(); }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) { if(value==null)return fallback;try{return Enum.valueOf(type,value.toUpperCase().replace('-','_'));}catch(IllegalArgumentException ex){return fallback;} }

    private static final class Component {
        final String name; final List<Property> properties=new ArrayList<Property>(); final List<Component> children=new ArrayList<Component>();
        Component(String name){this.name=name;}
        Property first(String propertyName){for(Property p:properties)if(propertyName.equals(p.name))return p;return null;}
        List<Property> all(String propertyName){List<Property>out=new ArrayList<Property>();for(Property p:properties)if(propertyName.equals(p.name))out.add(p);return out;}
    }
    private static final class Property {
        final String name,value; final Map<String,String>params=new HashMap<String,String>();
        Property(String name,String value){this.name=name;this.value=value;}
    }
}
