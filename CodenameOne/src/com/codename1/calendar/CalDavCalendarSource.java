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

import com.codename1.io.Util;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Standards-based CalDAV source. Pass the account's calendar-home URL; the
/// source discovers collections below it and uses RFC 6578 sync tokens when
/// the server advertises them.
public class CalDavCalendarSource extends CalendarSource {

    private final String homeUrl;

    private final CalDavAuthentication authentication;

    private final CalendarHttpTransport transport;

    public CalDavCalendarSource(String id, String displayName, String calendarHomeUrl, CalDavAuthentication authentication) {
        this(id, displayName, calendarHomeUrl, authentication, null);
    }

    public CalDavCalendarSource(String id, String displayName, String calendarHomeUrl, CalDavAuthentication authentication, CalendarHttpTransport transport) {
        super(id, displayName);
        if (calendarHomeUrl == null || authentication == null) {
            throw new IllegalArgumentException("calendar home URL and authentication required");
        }
        this.homeUrl = calendarHomeUrl.endsWith("/") ? calendarHomeUrl : calendarHomeUrl + "/";
        this.authentication = authentication;
        this.transport = transport == null ? new DefaultCalendarHttpTransport() : transport;
    }

    @Override
    public CalendarCapabilities getCapabilities() {
        return CalendarCapabilities.of(CalendarCapability.READ_CALENDARS, CalendarCapability.READ_EVENTS, CalendarCapability.WRITE_EVENTS, CalendarCapability.DELETE_EVENTS, CalendarCapability.READ_TASKS, CalendarCapability.WRITE_TASKS, CalendarCapability.DELETE_TASKS, CalendarCapability.RECURRENCE, CalendarCapability.ATTENDEES_READ, CalendarCapability.ATTENDEES_WRITE, CalendarCapability.ALARMS, CalendarCapability.ATTACHMENTS, CalendarCapability.CONFERENCING, CalendarCapability.DELTA_SYNC, CalendarCapability.OFFLINE_MUTATIONS);
    }

    @Override
    public CalendarAuthorizationStatus getAuthorizationStatus(CalendarAccess access) {
        return CalendarAuthorizationStatus.NOT_DETERMINED;
    }

    @Override
    public AsyncResource<CalendarAuthorizationStatus> requestAuthorization(CalendarAccess access) {
        final AsyncResource<CalendarAuthorizationStatus> out = new AsyncResource<CalendarAuthorizationStatus>();
        request("PROPFIND", homeUrl, "<?xml version=\"1.0\"?><propfind xmlns=\"DAV:\"><prop><current-user-principal/></prop></propfind>", "application/xml", headers("Depth", "0")).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                out.complete(CalendarAuthorizationStatus.FULL);
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarPage<CalendarInfo>> listCalendars(final CalendarInfo.ContentType type, String pageToken) {
        final CalendarInfo.ContentType requestedType = type == null ? CalendarInfo.ContentType.EVENTS : type;
        final AsyncResource<CalendarPage<CalendarInfo>> out = new AsyncResource<CalendarPage<CalendarInfo>>();
        String body = "<?xml version=\"1.0\"?><propfind xmlns=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\" " + "xmlns:cs=\"http://calendarserver.org/ns/\"><prop><displayname/><resourcetype/" + "><c:supported-calendar-component-set/><cs:getctag/><sync-token/></prop></propfind>";
        request("PROPFIND", homeUrl, body, "application/xml", headers("Depth", "1")).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                List<CalendarInfo> items = new ArrayList<CalendarInfo>();
                for (String response : elements(r.getBody(), "response")) {
                    String href = xmlValue(response, "href");
                    String components = xmlElement(response, "supported-calendar-component-set");
                    if (href == null || xmlElement(response, "calendar") == null) {
                        continue;
                    }
                    boolean events = components == null || containsIgnoreCase(components, "VEVENT");
                    boolean tasks = containsIgnoreCase(components, "VTODO");
                    if ((requestedType == CalendarInfo.ContentType.EVENTS && !events) || (requestedType == CalendarInfo.ContentType.TASKS && !tasks)) {
                        continue;
                    }
                    items.add(new CalendarInfo().setId(resolve(href)).setSourceId(getId()).setName(xmlValue(response, "displayname")).setContentType(requestedType).setCapabilities(getCapabilities()).putProviderData("ctag", xmlValue(response, "getctag")).putProviderData("syncToken", xmlValue(response, "sync-token")));
                }
                out.complete(new CalendarPage<CalendarInfo>(items, null, null));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarPage<CalendarEvent>> queryEvents(final CalendarQuery query) {
        final AsyncResource<CalendarPage<CalendarEvent>> out = new AsyncResource<CalendarPage<CalendarEvent>>();
        if (query == null || query.getCalendarId() == null) {
            return fail(out, "calendarId required");
        }
        String body = query.getSyncToken() == null ? calendarQuery("VEVENT", query) : syncQuery(query.getSyncToken());
        request("REPORT", query.getCalendarId(), body, "application/xml", headers("Depth", "1")).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                List<CalendarEvent> items = new ArrayList<CalendarEvent>();
                try {
                    for (String response : elements(r.getBody(), "response")) {
                        String data = xmlValue(response, "calendar-data");
                        String href = xmlValue(response, "href");
                        if (data == null || href == null) {
                            continue;
                        }
                        CalendarEvent event = ICalendarCodec.readEvent(data).setCalendarId(query.getCalendarId()).setSourceId(getId()).setVersion(xmlValue(response, "getetag"));
                        String uid = event.getId();
                        if (uid != null) {
                            event.putProviderData("caldav.uid", uid);
                        }
                        event.setId(resolve(href));
                        items.add(event);
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarEvent>(items, null, xmlValue(r.getBody(), "sync-token")));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> getEvent(final String calendarId, final String eventId) {
        final AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
        request("GET", resource(calendarId, eventId), null, null, null).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                try {
                    CalendarEvent event = ICalendarCodec.readEvent(r.getBody()).setCalendarId(calendarId).setSourceId(getId()).setVersion(r.getHeader("ETag"));
                    if (event.getId() != null) {
                        event.putProviderData("caldav.uid", event.getId());
                    }
                    event.setId(resource(calendarId, eventId));
                    out.complete(event);
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> saveEvent(final CalendarEvent event, CalendarMutationScope scope) {
        final AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
        if (event == null || event.getCalendarId() == null) {
            return fail(out, "event and calendarId required");
        }
        final boolean create = event.getId() == null;
        final String uid = createUid(event.getProviderData());
        final String resourceUrl = create ? resource(event.getCalendarId(), uid) : resource(event.getCalendarId(), event.getId());
        Map<String, String> h = version(event.getVersion(), create);
        String resourceId = event.getId();
        event.setId(uid);
        String data = ICalendarCodec.writeEvent(event);
        event.setId(resourceId);
        request("PUT", resourceUrl, data, "text/calendar; charset=utf-8", h).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                event.setId(resourceUrl).setVersion(r.getHeader("ETag")).putProviderData("caldav.uid", uid);
                out.complete(event);
                fireChange(new CalendarChange(getId(), event.getCalendarId(), event.getId(), CalendarChange.EntityType.EVENT, create ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteEvent(final String calendarId, final String eventId, CalendarMutationScope scope, String version) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        request("DELETE", resource(calendarId, eventId), null, null, version(version, false)).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                out.complete(Boolean.TRUE);
                fireChange(new CalendarChange(getId(), calendarId, eventId, CalendarChange.EntityType.EVENT, CalendarChange.ChangeType.DELETED));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarPage<CalendarTask>> queryTasks(final CalendarQuery query) {
        final AsyncResource<CalendarPage<CalendarTask>> out = new AsyncResource<CalendarPage<CalendarTask>>();
        if (query == null || query.getCalendarId() == null) {
            return fail(out, "calendarId required");
        }
        String body = query.getSyncToken() == null ? calendarQuery("VTODO", query) : syncQuery(query.getSyncToken());
        request("REPORT", query.getCalendarId(), body, "application/xml", headers("Depth", "1")).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                List<CalendarTask> items = new ArrayList<CalendarTask>();
                try {
                    for (String response : elements(r.getBody(), "response")) {
                        String data = xmlValue(response, "calendar-data");
                        String href = xmlValue(response, "href");
                        if (data == null || href == null) {
                            continue;
                        }
                        CalendarTask task = ICalendarCodec.readTask(data).setCalendarId(query.getCalendarId()).setSourceId(getId()).setVersion(xmlValue(response, "getetag"));
                        String uid = task.getId();
                        if (uid != null) {
                            task.putProviderData("caldav.uid", uid);
                        }
                        task.setId(resolve(href));
                        items.add(task);
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarTask>(items, null, xmlValue(r.getBody(), "sync-token")));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarTask> getTask(final String calendarId, final String taskId) {
        final AsyncResource<CalendarTask> out = new AsyncResource<CalendarTask>();
        request("GET", resource(calendarId, taskId), null, null, null).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                try {
                    CalendarTask task = ICalendarCodec.readTask(r.getBody()).setCalendarId(calendarId).setSourceId(getId()).setVersion(r.getHeader("ETag"));
                    if (task.getId() != null) {
                        task.putProviderData("caldav.uid", task.getId());
                    }
                    out.complete(task.setId(resource(calendarId, taskId)));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarTask> saveTask(final CalendarTask task, CalendarMutationScope scope) {
        final AsyncResource<CalendarTask> out = new AsyncResource<CalendarTask>();
        if (task == null || task.getCalendarId() == null) {
            return fail(out, "task and calendarId required");
        }
        final boolean create = task.getId() == null;
        final String uid = createUid(task.getProviderData());
        final String resourceUrl = create ? resource(task.getCalendarId(), uid) : resource(task.getCalendarId(), task.getId());
        String resourceId = task.getId();
        task.setId(uid);
        String data = ICalendarCodec.writeTask(task);
        task.setId(resourceId);
        request("PUT", resourceUrl, data, "text/calendar; charset=utf-8", version(task.getVersion(), create)).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                task.setId(resourceUrl).setVersion(r.getHeader("ETag")).putProviderData("caldav.uid", uid);
                out.complete(task);
                fireChange(new CalendarChange(getId(), task.getCalendarId(), task.getId(), CalendarChange.EntityType.TASK, create ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteTask(final String calendarId, final String taskId, CalendarMutationScope scope, String version) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        request("DELETE", resource(calendarId, taskId), null, null, version(version, false)).ready(new SuccessCallback<CalendarHttpResponse>() {

            @Override
            public void onSucess(CalendarHttpResponse r) {
                out.complete(Boolean.TRUE);
                fireChange(new CalendarChange(getId(), calendarId, taskId, CalendarChange.EntityType.TASK, CalendarChange.ChangeType.DELETED));
            }
        }).except(error(out));
        return out;
    }

    private AsyncResource<CalendarHttpResponse> request(final String method, final String url, final String body, final String contentType, final Map<String, String> headers) {
        final AsyncResource<CalendarHttpResponse> out = new AsyncResource<CalendarHttpResponse>();
        authorizeAndSend(method, url, body, contentType, headers, null, false, out);
        return out;
    }

    private void authorizeAndSend(final String method, final String url, final String body, final String contentType, final Map<String, String> headers, final String challenge, final boolean retried, final AsyncResource<CalendarHttpResponse> out) {
        authentication.authorization(method, url, challenge, retried).ready(new SuccessCallback<String>() {

            @Override
            public void onSucess(String authorization) {
                CalendarHttpRequest request = new CalendarHttpRequest(method, url).setBody(body).header("Accept", "application/xml, text/calendar");
                if (authorization != null) {
                    request.header("Authorization", authorization);
                }
                if (contentType != null) {
                    request.header("Content-Type", contentType);
                }
                if (headers != null) {
                    for (Map.Entry<String, String> h : headers.entrySet()) {
                        request.header(h.getKey(), h.getValue());
                    }
                }
                transport.execute(request).ready(new SuccessCallback<CalendarHttpResponse>() {

                    @Override
                    public void onSucess(CalendarHttpResponse response) {
                        if (response.getStatusCode() == 401 && !retried) {
                            authorizeAndSend(method, url, body, contentType, headers, response.getHeader("WWW-Authenticate"), true, out);
                            return;
                        }
                        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                            out.complete(response);
                        } else {
                            out.error(httpError(response));
                        }
                    }
                }).except(error(out));
            }
        }).except(error(out));
    }

    private static CalendarException httpError(CalendarHttpResponse r) {
        int code = r.getStatusCode();
        boolean invalidSyncToken = code == 410 || code == 403
                && containsIgnoreCase(r.getBody(), "valid-sync-token");
        CalendarError type = code == 401 ? CalendarError.AUTHENTICATION_REQUIRED
                : invalidSyncToken ? CalendarError.SYNC_TOKEN_EXPIRED
                : code == 403 ? CalendarError.PERMISSION_DENIED
                : code == 404 ? CalendarError.NOT_FOUND
                : code == 409 || code == 412 ? CalendarError.CONFLICT
                : code == 429 ? CalendarError.RATE_LIMITED
                : code >= 500 ? CalendarError.NETWORK : CalendarError.INVALID_ARGUMENT;
        return new CalendarException(type, "CalDAV server returned HTTP " + code,
                code, r.getBody(), null);
    }

    private static String createUid(Map<String, String> providerData) {
        String uid = providerData.get("caldav.uid");
        if (uid != null) {
            return uid;
        }
        String mutationId = providerData.get("cn1.mutationId");
        return mutationId == null ? String.valueOf(System.currentTimeMillis()) + "@codenameone"
                : mutationId + "@codenameone";
    }

    private static boolean containsIgnoreCase(String value, String target) {
        if (value == null) {
            return false;
        }
        int limit = value.length() - target.length();
        for (int i = 0; i <= limit; i++) {
            if (value.regionMatches(true, i, target, 0, target.length())) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        return value != null && value.length() >= suffix.length()
                && value.regionMatches(true, value.length() - suffix.length(),
                suffix, 0, suffix.length());
    }

    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return out.toString();
    }

    private String resolve(String href) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        int scheme = homeUrl.indexOf("://");
        int slash = homeUrl.indexOf('/', scheme + 3);
        String origin = slash < 0 ? homeUrl : homeUrl.substring(0, slash);
        return href.startsWith("/") ? origin + href : homeUrl + href;
    }

    private static String resource(String calendarId, String id) {
        if (id.startsWith("http://") || id.startsWith("https://")) {
            return id;
        }
        String base = calendarId.endsWith("/") ? calendarId : calendarId + "/";
        return base + safe(id) + (endsWithIgnoreCase(id, ".ics") ? "" : ".ics");
    }

    private static String safe(String value) {
        // Percent-encode the id as a single path segment so ids containing
        // '?', '#', '%', '/' or non-ASCII cannot address a different resource.
        return Util.encodeUrl(value);
    }

    private static String calendarQuery(String component, CalendarQuery q) {
        StringBuilder filter = new StringBuilder("<c:comp-filter name=\"VCALENDAR\"><c:comp-filter name=\"").append(component).append("\"");
        if (q.getStartTime() == null && q.getEndTime() == null) {
            filter.append("/>");
        } else {
            filter.append('>');
            filter.append("<c:time-range");
            if (q.getStartTime() != null) {
                filter.append(" start=\"").append(icalTime(q.getStartTime())).append("\"");
            }
            if (q.getEndTime() != null) {
                filter.append(" end=\"").append(icalTime(q.getEndTime())).append("\"");
            }
            filter.append("/></c:comp-filter>");
        }
        filter.append("</c:comp-filter>");
        return "<?xml version=\"1.0\"?><c:calendar-query xmlns:d=\"DAV:\" " + "xmlns:c=\"urn:ietf:params:xml:ns:caldav\"><d:prop><d:getetag/><c:calendar-data/></d:prop><c:filter>" + filter + "</c:filter></c:calendar-query>";
    }

    private static String syncQuery(String token) {
        return "<?xml version=\"1.0\"?><d:sync-collection xmlns:d=\"DAV:\" " + "xmlns:c=\"urn:ietf:params:xml:ns:caldav\"><d:sync-token>" + xml(token) + ("</d:sync-token><d:sync-level>1</d:sync-level><d:prop><d:getetag/><c:calendar-data/></d:prop></" + "d:sync-collection>");
    }

    private static String icalTime(Instant value) {
        return CalendarDateUtil.formatBasic(value, ZoneId.of("UTC")) + "Z";
    }

    private static Map<String, String> headers(String name, String value) {
        Map<String, String> out = new HashMap<String, String>();
        out.put(name, value);
        return out;
    }

    private static Map<String, String> version(String value, boolean create) {
        Map<String, String> out = new HashMap<String, String>();
        if (create) {
            out.put("If-None-Match", "*");
        } else if (value != null) {
            out.put("If-Match", value);
        }
        return out;
    }

    private static String xmlValue(String source, String local) {
        String element = xmlElement(source, local);
        return element == null ? null : unxml(stripTags(element).trim());
    }

    private static String xmlElement(String source, String local) {
        if (source == null) {
            return null;
        }
        String lower = asciiLower(source);
        int open = findElement(lower, asciiLower(local), 0);
        if (open < 0) {
            return null;
        }
        int gt = source.indexOf('>', open);
        if (gt < 0) {
            return null;
        }
        String tag = source.substring(open + 1, gt).trim();
        if (tag.endsWith("/")) {
            return "";
        }
        int space = tag.indexOf(' ');
        if (space >= 0) {
            tag = tag.substring(0, space);
        }
        String close = "</" + tag + ">";
        int end = lower.indexOf(asciiLower(close), gt + 1);
        return end < 0 ? null : source.substring(gt + 1, end);
    }

    private static List<String> elements(String source, String local) {
        List<String> out = new ArrayList<String>();
        if (source == null) {
            return out;
        }
        String lower = asciiLower(source);
        int from = 0;
        while (true) {
            int open = findElement(lower, asciiLower(local), from);
            if (open < 0) {
                break;
            }
            int gt = source.indexOf('>', open);
            if (gt < 0) {
                break;
            }
            String tag = source.substring(open + 1, gt).trim();
            if (tag.endsWith("/")) {
                out.add("");
                from = gt + 1;
                continue;
            }
            int space = tag.indexOf(' ');
            if (space >= 0) {
                tag = tag.substring(0, space);
            }
            String close = "</" + tag + ">";
            int end = lower.indexOf(asciiLower(close), gt + 1);
            if (end < 0) {
                break;
            }
            out.add(source.substring(gt + 1, end));
            from = end + close.length();
        }
        return out;
    }

    private static int findElement(String source, String local, int from) {
        int position = from;
        while (position < source.length()) {
            int open = source.indexOf('<', position);
            if (open < 0 || open + 1 >= source.length()) {
                return -1;
            }
            int start = open + 1;
            char first = source.charAt(start);
            if (first == '/' || first == '!' || first == '?') {
                position = start + 1;
                continue;
            }
            int end = start;
            while (end < source.length()) {
                char c = source.charAt(end);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n'
                        || c == '>' || c == '/') {
                    break;
                }
                end++;
            }
            String qualified = source.substring(start, end);
            int colon = qualified.lastIndexOf(':');
            String name = colon < 0 ? qualified : qualified.substring(colon + 1);
            if (local.equals(name)) {
                return open;
            }
            position = end + 1;
        }
        return -1;
    }

    private static String stripTags(String value) {
        StringBuilder out = new StringBuilder();
        boolean tag = false;
        for (int i = 0; i < value.length(); i++) {
            if (!tag && value.startsWith("<![CDATA[", i)) {
                int end = value.indexOf("]]>", i + 9);
                if (end < 0) {
                    out.append(value.substring(i + 9));
                    break;
                }
                out.append(value.substring(i + 9, end));
                i = end + 2;
                continue;
            }
            char c = value.charAt(i);
            if (c == '<') {
                tag = true;
            } else if (c == '>') {
                tag = false;
            } else if (!tag) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String xml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String unxml(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
    }

    private static <T> SuccessCallback<Throwable> error(final AsyncResource<T> out) {
        return new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                out.error(error);
            }
        };
    }

    private static <T> AsyncResource<T> fail(AsyncResource<T> out, String message) {
        out.error(new CalendarException(CalendarError.INVALID_ARGUMENT, message));
        return out;
    }
}
