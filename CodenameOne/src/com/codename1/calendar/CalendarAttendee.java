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

/// A person, group, or resource invited to an event.
public class CalendarAttendee {
    public enum Role { REQUIRED, OPTIONAL, RESOURCE }
    public enum Response { NONE, NEEDS_ACTION, ACCEPTED, DECLINED, TENTATIVE, DELEGATED }
    private String name, email, uri;
    private Role role = Role.REQUIRED;
    private Response response = Response.NONE;
    private boolean organizer, self;
    public String getName() { return name; }
    public CalendarAttendee setName(String v) { name = v; return this; }
    public String getEmail() { return email; }
    public CalendarAttendee setEmail(String v) { email = v; return this; }
    public String getUri() { return uri; }
    public CalendarAttendee setUri(String v) { uri = v; return this; }
    public Role getRole() { return role; }
    public CalendarAttendee setRole(Role v) { role = v == null ? Role.REQUIRED : v; return this; }
    public Response getResponse() { return response; }
    public CalendarAttendee setResponse(Response v) { response = v == null ? Response.NONE : v; return this; }
    public boolean isOrganizer() { return organizer; }
    public CalendarAttendee setOrganizer(boolean v) { organizer = v; return this; }
    public boolean isSelf() { return self; }
    public CalendarAttendee setSelf(boolean v) { self = v; return this; }
}
