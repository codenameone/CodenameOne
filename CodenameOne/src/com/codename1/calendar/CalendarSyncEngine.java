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

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Opt-in local-first mutation queue. It deliberately does not own credentials
/// or silently schedule work; applications call `sync()` or connect it to a
/// platform background-work callback.
public final class CalendarSyncEngine {

    private static final String MUTATIONS = "mutations";

    private final CalendarSource source;

    private final CalendarCache cache;

    private Map<String, Object> state;

    private long nextId;

    public CalendarSyncEngine(CalendarSource source, CalendarCache cache) throws CalendarException {
        if (source == null || cache == null) {
            throw new IllegalArgumentException("source and cache required");
        }
        this.source = source;
        this.cache = cache;
        state = cache.load(source.getId());
        if (!(state.get(MUTATIONS) instanceof List)) {
            state.put(MUTATIONS, new ArrayList<Object>());
        }
        nextId = System.currentTimeMillis();
    }

    public synchronized String queueEventSave(CalendarEvent event, CalendarMutationScope scope) throws CalendarException {
        if (event == null) {
            throw new IllegalArgumentException("event required");
        }
        if (event.getCalendarId() == null) {
            throw new IllegalArgumentException("event calendarId required");
        }
        return queue("saveEvent", event.getCalendarId(), event.getId(), event.getVersion(), CalendarModelCodec.encodeEvent(event), scope);
    }

    public synchronized String queueEventDelete(String calendarId, String eventId, String version, CalendarMutationScope scope) throws CalendarException {
        return queue("deleteEvent", calendarId, eventId, version, null, scope);
    }

    public synchronized String queueTaskSave(CalendarTask task, CalendarMutationScope scope) throws CalendarException {
        if (task == null) {
            throw new IllegalArgumentException("task required");
        }
        if (task.getCalendarId() == null) {
            throw new IllegalArgumentException("task calendarId required");
        }
        return queue("saveTask", task.getCalendarId(), task.getId(), task.getVersion(), CalendarModelCodec.encodeTask(task), scope);
    }

    public synchronized String queueTaskDelete(String calendarId, String taskId, String version, CalendarMutationScope scope) throws CalendarException {
        return queue("deleteTask", calendarId, taskId, version, null, scope);
    }

    private String queue(String type, String calendarId, String itemId, String version, Map<String, Object> payload, CalendarMutationScope scope) throws CalendarException {
        String id = source.getId() + "-" + (++nextId);
        Map<String, Object> mutation = new HashMap<String, Object>();
        mutation.put("id", id);
        mutation.put("type", type);
        mutation.put("calendarId", calendarId);
        mutation.put("itemId", itemId);
        mutation.put("version", version);
        mutation.put("payload", payload);
        mutation.put("scope", (scope == null ? CalendarMutationScope.ALL : scope).name());
        mutations().add(mutation);
        persist();
        return id;
    }

    public synchronized int getPendingCount() {
        return mutations().size();
    }

    public AsyncResource<CalendarSyncResult> sync() {
        final AsyncResource<CalendarSyncResult> out = new AsyncResource<CalendarSyncResult>();
        final CalendarSyncResult result = new CalendarSyncResult();
        syncNext(out, result);
        return out;
    }

    private void syncNext(final AsyncResource<CalendarSyncResult> out, final CalendarSyncResult result) {
        final Map<String, Object> mutation;
        synchronized (this) {
            mutation = firstRunnableMutation();
            if (mutation == null) {
                result.setRemaining(mutations().size());
                out.complete(result);
                return;
            }
        }
        AsyncResource operation = execute(mutation);
        operation.ready(new SuccessCallback<Object>() {

            @Override
            public void onSucess(Object value) {
                synchronized (CalendarSyncEngine.this) {
                    mutations().remove(mutation);
                    result.applied();
                    try {
                        persist();
                    } catch (CalendarException ex) {
                        out.error(ex);
                        return;
                    }
                }
                syncNext(out, result);
            }
        }).except(new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                if (error instanceof CalendarException && ((CalendarException) error).getError() == CalendarError.CONFLICT) {
                    loadConflict(mutation, out, result);
                    return;
                }
                result.setRemaining(getPendingCount());
                out.error(error);
            }
        });
    }

    private AsyncResource execute(Map<String, Object> m) {
        String type = string(m, "type");
        String cal = string(m, "calendarId");
        String item = string(m, "itemId");
        String version = string(m, "version");
        CalendarMutationScope scope;
        try {
            scope = CalendarMutationScope.valueOf(string(m, "scope"));
        } catch (Exception e) {
            scope = CalendarMutationScope.ALL;
        }
        if ("saveEvent".equals(type)) {
            return source.saveEvent(CalendarModelCodec.decodeEvent((Map) m.get("payload")), scope);
        }
        if ("deleteEvent".equals(type)) {
            return source.deleteEvent(cal, item, scope, version);
        }
        if ("saveTask".equals(type)) {
            return source.saveTask(CalendarModelCodec.decodeTask((Map) m.get("payload")), scope);
        }
        if ("deleteTask".equals(type)) {
            return source.deleteTask(cal, item, scope, version);
        }
        return CalendarSource.unsupported("Unknown offline mutation");
    }

    private void loadConflict(final Map<String, Object> mutation, final AsyncResource<CalendarSyncResult> out, final CalendarSyncResult result) {
        String type = string(mutation, "type");
        AsyncResource remote = type.indexOf("Event") >= 0 ? source.getEvent(string(mutation, "calendarId"), string(mutation, "itemId")) : source.getTask(string(mutation, "calendarId"), string(mutation, "itemId"));
        remote.ready(new SuccessCallback<Object>() {

            @Override
            public void onSucess(Object value) {
                Map<String, Object> remoteMap = value instanceof CalendarEvent ? CalendarModelCodec.encodeEvent((CalendarEvent) value) : value instanceof CalendarTask ? CalendarModelCodec.encodeTask((CalendarTask) value) : null;
                synchronized (CalendarSyncEngine.this) {
                    mutation.put("paused", Boolean.TRUE);
                    mutation.put("remote", remoteMap);
                    try {
                        persist();
                    } catch (CalendarException ex) {
                        out.error(ex);
                        return;
                    }
                }
                result.addConflict(new CalendarConflict(string(mutation, "id"), (Map) mutation.get("payload"), remoteMap));
                syncNext(out, result);
            }
        }).except(new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                out.error(error);
            }
        });
    }

    public synchronized void resolveConflict(String mutationId, CalendarConflict.Resolution resolution, Map<String, Object> merged) throws CalendarException {
        Map<String, Object> found = find(mutationId);
        if (found == null) {
            throw new CalendarException(CalendarError.NOT_FOUND, "Conflict not found");
        }
        if (resolution == CalendarConflict.Resolution.KEEP_REMOTE) {
            mutations().remove(found);
        } else {
            Map remote = (Map) found.get("remote");
            if (resolution == CalendarConflict.Resolution.MERGED) {
                if (merged == null) {
                    throw new IllegalArgumentException("merged value required");
                }
                found.put("payload", merged);
            }
            if (remote != null) {
                found.put("version", remote.get("version"));
            }
            found.remove("paused");
            found.remove("remote");
        }
        persist();
    }

    private Map<String, Object> firstRunnableMutation() {
        for (Object value : mutations()) {
            Map<String, Object> m = (Map<String, Object>) value;
            if (!Boolean.TRUE.equals(m.get("paused"))) {
                return m;
            }
        }
        return null;
    }

    private Map<String, Object> find(String id) {
        for (Object value : mutations()) {
            Map<String, Object> m = (Map<String, Object>) value;
            if (id.equals(m.get("id"))) {
                return m;
            }
        }
        return null;
    }

    private List<Object> mutations() {
        return (List<Object>) state.get(MUTATIONS);
    }

    private void persist() throws CalendarException {
        cache.store(source.getId(), state);
    }

    private static String string(Map m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
