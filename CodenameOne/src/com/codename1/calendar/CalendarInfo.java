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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** A calendar or task-list collection exposed by a source. */
public class CalendarInfo {
    public enum ContentType { EVENTS, TASKS }
    private String id, sourceId, accountId, name, owner, timeZoneId;
    private int color;
    private boolean primary, readOnly;
    private ContentType contentType = ContentType.EVENTS;
    private CalendarCapabilities capabilities = CalendarCapabilities.none();
    private final Map<String,String> providerData = new HashMap<String,String>();
    public String getId() { return id; }
    public CalendarInfo setId(String v) { id = v; return this; }
    public String getSourceId() { return sourceId; }
    public CalendarInfo setSourceId(String v) { sourceId = v; return this; }
    public String getAccountId() { return accountId; }
    public CalendarInfo setAccountId(String v) { accountId = v; return this; }
    public String getName() { return name; }
    public CalendarInfo setName(String v) { name = v; return this; }
    public String getOwner() { return owner; }
    public CalendarInfo setOwner(String v) { owner = v; return this; }
    public String getTimeZoneId() { return timeZoneId; }
    public CalendarInfo setTimeZoneId(String v) { timeZoneId = v; return this; }
    public int getColor() { return color; }
    public CalendarInfo setColor(int v) { color = v; return this; }
    public boolean isPrimary() { return primary; }
    public CalendarInfo setPrimary(boolean v) { primary = v; return this; }
    public boolean isReadOnly() { return readOnly; }
    public CalendarInfo setReadOnly(boolean v) { readOnly = v; return this; }
    public ContentType getContentType() { return contentType; }
    public CalendarInfo setContentType(ContentType v) { contentType = v == null ? ContentType.EVENTS : v; return this; }
    public CalendarCapabilities getCapabilities() { return capabilities; }
    public CalendarInfo setCapabilities(CalendarCapabilities v) { capabilities = v == null ? CalendarCapabilities.none() : v; return this; }
    public CalendarInfo putProviderData(String k, String v) { if (k != null) providerData.put(k, v); return this; }
    public Map<String,String> getProviderData() { return Collections.unmodifiableMap(providerData); }
}
