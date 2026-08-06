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

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_calendar {
    private GeneratedAccess_com_codename1_calendar() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("CalDavAuthentication".equals(simpleName)) {
            return com.codename1.calendar.CalDavAuthentication.class;
        }
        if ("CalDavCalendarSource".equals(simpleName)) {
            return com.codename1.calendar.CalDavCalendarSource.class;
        }
        if ("CalendarAccess".equals(simpleName)) {
            return com.codename1.calendar.CalendarAccess.class;
        }
        if ("CalendarAlarm".equals(simpleName)) {
            return com.codename1.calendar.CalendarAlarm.class;
        }
        if ("Method".equals(simpleName)) {
            return com.codename1.calendar.CalendarAlarm.Method.class;
        }
        if ("CalendarAttachment".equals(simpleName)) {
            return com.codename1.calendar.CalendarAttachment.class;
        }
        if ("CalendarAttendee".equals(simpleName)) {
            return com.codename1.calendar.CalendarAttendee.class;
        }
        if ("Response".equals(simpleName)) {
            return com.codename1.calendar.CalendarAttendee.Response.class;
        }
        if ("Role".equals(simpleName)) {
            return com.codename1.calendar.CalendarAttendee.Role.class;
        }
        if ("CalendarAuthToken".equals(simpleName)) {
            return com.codename1.calendar.CalendarAuthToken.class;
        }
        if ("CalendarAuthorizationStatus".equals(simpleName)) {
            return com.codename1.calendar.CalendarAuthorizationStatus.class;
        }
        if ("CalendarCache".equals(simpleName)) {
            return com.codename1.calendar.CalendarCache.class;
        }
        if ("CalendarCapabilities".equals(simpleName)) {
            return com.codename1.calendar.CalendarCapabilities.class;
        }
        if ("CalendarCapability".equals(simpleName)) {
            return com.codename1.calendar.CalendarCapability.class;
        }
        if ("CalendarChange".equals(simpleName)) {
            return com.codename1.calendar.CalendarChange.class;
        }
        if ("ChangeType".equals(simpleName)) {
            return com.codename1.calendar.CalendarChange.ChangeType.class;
        }
        if ("EntityType".equals(simpleName)) {
            return com.codename1.calendar.CalendarChange.EntityType.class;
        }
        if ("CalendarChangeListener".equals(simpleName)) {
            return com.codename1.calendar.CalendarChangeListener.class;
        }
        if ("CalendarConference".equals(simpleName)) {
            return com.codename1.calendar.CalendarConference.class;
        }
        if ("CalendarConflict".equals(simpleName)) {
            return com.codename1.calendar.CalendarConflict.class;
        }
        if ("Resolution".equals(simpleName)) {
            return com.codename1.calendar.CalendarConflict.Resolution.class;
        }
        if ("CalendarDateTime".equals(simpleName)) {
            return com.codename1.calendar.CalendarDateTime.class;
        }
        if ("CalendarError".equals(simpleName)) {
            return com.codename1.calendar.CalendarError.class;
        }
        if ("CalendarEvent".equals(simpleName)) {
            return com.codename1.calendar.CalendarEvent.class;
        }
        if ("Availability".equals(simpleName)) {
            return com.codename1.calendar.CalendarEvent.Availability.class;
        }
        if ("Privacy".equals(simpleName)) {
            return com.codename1.calendar.CalendarEvent.Privacy.class;
        }
        if ("Status".equals(simpleName)) {
            return com.codename1.calendar.CalendarEvent.Status.class;
        }
        if ("CalendarException".equals(simpleName)) {
            return com.codename1.calendar.CalendarException.class;
        }
        if ("CalendarHttpRequest".equals(simpleName)) {
            return com.codename1.calendar.CalendarHttpRequest.class;
        }
        if ("CalendarHttpResponse".equals(simpleName)) {
            return com.codename1.calendar.CalendarHttpResponse.class;
        }
        if ("CalendarHttpTransport".equals(simpleName)) {
            return com.codename1.calendar.CalendarHttpTransport.class;
        }
        if ("CalendarInfo".equals(simpleName)) {
            return com.codename1.calendar.CalendarInfo.class;
        }
        if ("ContentType".equals(simpleName)) {
            return com.codename1.calendar.CalendarInfo.ContentType.class;
        }
        if ("CalendarManager".equals(simpleName)) {
            return com.codename1.calendar.CalendarManager.class;
        }
        if ("CalendarModelCodec".equals(simpleName)) {
            return com.codename1.calendar.CalendarModelCodec.class;
        }
        if ("CalendarMutationScope".equals(simpleName)) {
            return com.codename1.calendar.CalendarMutationScope.class;
        }
        if ("CalendarPage".equals(simpleName)) {
            return com.codename1.calendar.CalendarPage.class;
        }
        if ("CalendarQuery".equals(simpleName)) {
            return com.codename1.calendar.CalendarQuery.class;
        }
        if ("CalendarRecurrenceRule".equals(simpleName)) {
            return com.codename1.calendar.CalendarRecurrenceRule.class;
        }
        if ("Frequency".equals(simpleName)) {
            return com.codename1.calendar.CalendarRecurrenceRule.Frequency.class;
        }
        if ("CalendarSource".equals(simpleName)) {
            return com.codename1.calendar.CalendarSource.class;
        }
        if ("CalendarSyncEngine".equals(simpleName)) {
            return com.codename1.calendar.CalendarSyncEngine.class;
        }
        if ("CalendarSyncResult".equals(simpleName)) {
            return com.codename1.calendar.CalendarSyncResult.class;
        }
        if ("CalendarTask".equals(simpleName)) {
            return com.codename1.calendar.CalendarTask.class;
        }
        if ("CalendarTokenProvider".equals(simpleName)) {
            return com.codename1.calendar.CalendarTokenProvider.class;
        }
        if ("DefaultCalendarHttpTransport".equals(simpleName)) {
            return com.codename1.calendar.DefaultCalendarHttpTransport.class;
        }
        if ("FreeBusyInterval".equals(simpleName)) {
            return com.codename1.calendar.FreeBusyInterval.class;
        }
        if ("GoogleCalendarSource".equals(simpleName)) {
            return com.codename1.calendar.GoogleCalendarSource.class;
        }
        if ("ICalendarCodec".equals(simpleName)) {
            return com.codename1.calendar.ICalendarCodec.class;
        }
        if ("LocalCalendarSource".equals(simpleName)) {
            return com.codename1.calendar.LocalCalendarSource.class;
        }
        if ("MemoryCalendarCache".equals(simpleName)) {
            return com.codename1.calendar.MemoryCalendarCache.class;
        }
        if ("MicrosoftCalendarSource".equals(simpleName)) {
            return com.codename1.calendar.MicrosoftCalendarSource.class;
        }
        if ("OAuthCalendarSource".equals(simpleName)) {
            return com.codename1.calendar.OAuthCalendarSource.class;
        }
        if ("OidcCalendarTokenProvider".equals(simpleName)) {
            return com.codename1.calendar.OidcCalendarTokenProvider.class;
        }
        if ("TokenListener".equals(simpleName)) {
            return com.codename1.calendar.OidcCalendarTokenProvider.TokenListener.class;
        }
        if ("StorageCalendarCache".equals(simpleName)) {
            return com.codename1.calendar.StorageCalendarCache.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.calendar.CalDavCalendarSource.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalDavAuthentication.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalDavAuthentication.class}, false);
                return new com.codename1.calendar.CalDavCalendarSource((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.calendar.CalDavAuthentication) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalDavAuthentication.class, com.codename1.calendar.CalendarHttpTransport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalDavAuthentication.class, com.codename1.calendar.CalendarHttpTransport.class}, false);
                return new com.codename1.calendar.CalDavCalendarSource((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.calendar.CalDavAuthentication) adaptedArgs[3], (com.codename1.calendar.CalendarHttpTransport) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.calendar.CalendarAlarm.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarAlarm();
            }
        }
        if (type == com.codename1.calendar.CalendarAttachment.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarAttachment();
            }
        }
        if (type == com.codename1.calendar.CalendarAttendee.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarAttendee();
            }
        }
        if (type == com.codename1.calendar.CalendarAuthToken.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.time.Instant.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.time.Instant.class, java.lang.String.class}, false);
                return new com.codename1.calendar.CalendarAuthToken((java.lang.String) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.calendar.CalendarChange.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarChange.EntityType.class, com.codename1.calendar.CalendarChange.ChangeType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarChange.EntityType.class, com.codename1.calendar.CalendarChange.ChangeType.class}, false);
                return new com.codename1.calendar.CalendarChange((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.calendar.CalendarChange.EntityType) adaptedArgs[3], (com.codename1.calendar.CalendarChange.ChangeType) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.calendar.CalendarConference.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarConference();
            }
        }
        if (type == com.codename1.calendar.CalendarEvent.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarEvent();
            }
        }
        if (type == com.codename1.calendar.CalendarException.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class}, false);
                return new com.codename1.calendar.CalendarException((com.codename1.calendar.CalendarError) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.calendar.CalendarException((com.codename1.calendar.CalendarError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class, java.lang.Integer.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class, java.lang.Integer.class, java.lang.Throwable.class}, false);
                return new com.codename1.calendar.CalendarException((com.codename1.calendar.CalendarError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Throwable) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarError.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.calendar.CalendarException((com.codename1.calendar.CalendarError) adaptedArgs[0], (java.lang.String) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.String) adaptedArgs[3], (java.lang.Throwable) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.calendar.CalendarHttpRequest.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.calendar.CalendarHttpRequest((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.calendar.CalendarHttpResponse.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.util.Map.class}, false);
                return new com.codename1.calendar.CalendarHttpResponse(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], (java.util.Map) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.calendar.CalendarInfo.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarInfo();
            }
        }
        if (type == com.codename1.calendar.CalendarPage.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.calendar.CalendarPage((java.util.List) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.calendar.CalendarQuery.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarQuery();
            }
        }
        if (type == com.codename1.calendar.CalendarRecurrenceRule.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarRecurrenceRule();
            }
        }
        if (type == com.codename1.calendar.CalendarSyncEngine.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarSource.class, com.codename1.calendar.CalendarCache.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarSource.class, com.codename1.calendar.CalendarCache.class}, false);
                return new com.codename1.calendar.CalendarSyncEngine((com.codename1.calendar.CalendarSource) adaptedArgs[0], (com.codename1.calendar.CalendarCache) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.calendar.CalendarSyncResult.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarSyncResult();
            }
        }
        if (type == com.codename1.calendar.CalendarTask.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.CalendarTask();
            }
        }
        if (type == com.codename1.calendar.DefaultCalendarHttpTransport.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.DefaultCalendarHttpTransport();
            }
        }
        if (type == com.codename1.calendar.FreeBusyInterval.class) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.Instant.class, com.codename1.calendar.CalendarEvent.Availability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.Instant.class, com.codename1.calendar.CalendarEvent.Availability.class}, false);
                return new com.codename1.calendar.FreeBusyInterval((java.time.Instant) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (com.codename1.calendar.CalendarEvent.Availability) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.calendar.GoogleCalendarSource.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class}, false);
                return new com.codename1.calendar.GoogleCalendarSource((com.codename1.calendar.CalendarTokenProvider) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class, com.codename1.calendar.CalendarHttpTransport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class, com.codename1.calendar.CalendarHttpTransport.class}, false);
                return new com.codename1.calendar.GoogleCalendarSource((com.codename1.calendar.CalendarTokenProvider) adaptedArgs[0], (com.codename1.calendar.CalendarHttpTransport) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.calendar.MemoryCalendarCache.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.calendar.MemoryCalendarCache();
            }
        }
        if (type == com.codename1.calendar.MicrosoftCalendarSource.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class}, false);
                return new com.codename1.calendar.MicrosoftCalendarSource((com.codename1.calendar.CalendarTokenProvider) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class, com.codename1.calendar.CalendarHttpTransport.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class, com.codename1.calendar.CalendarHttpTransport.class}, false);
                return new com.codename1.calendar.MicrosoftCalendarSource((com.codename1.calendar.CalendarTokenProvider) adaptedArgs[0], (com.codename1.calendar.CalendarHttpTransport) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.calendar.OidcCalendarTokenProvider.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcClient.class, com.codename1.io.oidc.OidcTokens.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcClient.class, com.codename1.io.oidc.OidcTokens.class}, false);
                return new com.codename1.calendar.OidcCalendarTokenProvider((com.codename1.io.oidc.OidcClient) adaptedArgs[0], (com.codename1.io.oidc.OidcTokens) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.calendar.StorageCalendarCache.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.calendar.StorageCalendarCache((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.calendar.CalDavAuthentication.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.calendar.CalendarCapabilities.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.calendar.CalendarDateTime.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.calendar.CalendarManager.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.calendar.CalendarModelCodec.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.calendar.ICalendarCodec.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.calendar.LocalCalendarSource.class) return invokeStatic6(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("basic".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.calendar.CalDavAuthentication.basic((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("bearer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTokenProvider.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                return com.codename1.calendar.CalDavAuthentication.bearer((com.codename1.calendar.CalendarTokenProvider) adaptedArgs[0], varArgs);
            }
        }
        if ("digest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.calendar.CalDavAuthentication.digest((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.calendar.CalDavAuthentication.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("none".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.calendar.CalendarCapabilities.none();
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarCapability[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarCapability[].class}, true);
                com.codename1.calendar.CalendarCapability[] varArgs = new com.codename1.calendar.CalendarCapability[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.calendar.CalendarCapability) adaptedArgs[i];
                }
                return com.codename1.calendar.CalendarCapabilities.of(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.calendar.CalendarCapabilities.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("allDay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.LocalDate.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.LocalDate.class}, false);
                return com.codename1.calendar.CalendarDateTime.allDay((java.time.LocalDate) adaptedArgs[0]);
            }
        }
        if ("instant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class, java.time.ZoneId.class}, false);
                return com.codename1.calendar.CalendarDateTime.instant((java.time.Instant) adaptedArgs[0], (java.time.ZoneId) adaptedArgs[1]);
            }
        }
        if ("timed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.ZonedDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.ZonedDateTime.class}, false);
                return com.codename1.calendar.CalendarDateTime.timed((java.time.ZonedDateTime) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.calendar.CalendarDateTime.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.calendar.CalendarManager.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.calendar.CalendarManager.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("decodeDateTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.calendar.CalendarModelCodec.decodeDateTime((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("decodeEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.calendar.CalendarModelCodec.decodeEvent((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("decodeTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return com.codename1.calendar.CalendarModelCodec.decodeTask((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("encodeDateTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false);
                return com.codename1.calendar.CalendarModelCodec.encodeDateTime((com.codename1.calendar.CalendarDateTime) adaptedArgs[0]);
            }
        }
        if ("encodeEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class}, false);
                return com.codename1.calendar.CalendarModelCodec.encodeEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0]);
            }
        }
        if ("encodeTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class}, false);
                return com.codename1.calendar.CalendarModelCodec.encodeTask((com.codename1.calendar.CalendarTask) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.calendar.CalendarModelCodec.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("readEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.calendar.ICalendarCodec.readEvent((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("readRecurrenceRule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.calendar.ICalendarCodec.readRecurrenceRule((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("readTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.calendar.ICalendarCodec.readTask((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("writeEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class}, false);
                return com.codename1.calendar.ICalendarCodec.writeEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0]);
            }
        }
        if ("writeRecurrenceRule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.class}, false);
                return com.codename1.calendar.ICalendarCodec.writeRecurrenceRule((com.codename1.calendar.CalendarRecurrenceRule) adaptedArgs[0]);
            }
        }
        if ("writeTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class}, false);
                return com.codename1.calendar.ICalendarCodec.writeTask((com.codename1.calendar.CalendarTask) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.calendar.ICalendarCodec.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.calendar.LocalCalendarSource.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.calendar.LocalCalendarSource.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.calendar.GoogleCalendarSource) {
            try {
                return invoke0((com.codename1.calendar.GoogleCalendarSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.MicrosoftCalendarSource) {
            try {
                return invoke1((com.codename1.calendar.MicrosoftCalendarSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalDavCalendarSource) {
            try {
                return invoke2((com.codename1.calendar.CalDavCalendarSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.LocalCalendarSource) {
            try {
                return invoke3((com.codename1.calendar.LocalCalendarSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.OAuthCalendarSource) {
            try {
                return invoke4((com.codename1.calendar.OAuthCalendarSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalDavAuthentication) {
            try {
                return invoke5((com.codename1.calendar.CalDavAuthentication) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarAlarm) {
            try {
                return invoke6((com.codename1.calendar.CalendarAlarm) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarAttachment) {
            try {
                return invoke7((com.codename1.calendar.CalendarAttachment) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarAttendee) {
            try {
                return invoke8((com.codename1.calendar.CalendarAttendee) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarAuthToken) {
            try {
                return invoke9((com.codename1.calendar.CalendarAuthToken) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarCapabilities) {
            try {
                return invoke10((com.codename1.calendar.CalendarCapabilities) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarChange) {
            try {
                return invoke11((com.codename1.calendar.CalendarChange) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarConference) {
            try {
                return invoke12((com.codename1.calendar.CalendarConference) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarConflict) {
            try {
                return invoke13((com.codename1.calendar.CalendarConflict) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarDateTime) {
            try {
                return invoke14((com.codename1.calendar.CalendarDateTime) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarEvent) {
            try {
                return invoke15((com.codename1.calendar.CalendarEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarException) {
            try {
                return invoke16((com.codename1.calendar.CalendarException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarHttpRequest) {
            try {
                return invoke17((com.codename1.calendar.CalendarHttpRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarHttpResponse) {
            try {
                return invoke18((com.codename1.calendar.CalendarHttpResponse) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarInfo) {
            try {
                return invoke19((com.codename1.calendar.CalendarInfo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarManager) {
            try {
                return invoke20((com.codename1.calendar.CalendarManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarPage) {
            try {
                return invoke21((com.codename1.calendar.CalendarPage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarQuery) {
            try {
                return invoke22((com.codename1.calendar.CalendarQuery) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarRecurrenceRule) {
            try {
                return invoke23((com.codename1.calendar.CalendarRecurrenceRule) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarSource) {
            try {
                return invoke24((com.codename1.calendar.CalendarSource) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarSyncEngine) {
            try {
                return invoke25((com.codename1.calendar.CalendarSyncEngine) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarSyncResult) {
            try {
                return invoke26((com.codename1.calendar.CalendarSyncResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarTask) {
            try {
                return invoke27((com.codename1.calendar.CalendarTask) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.DefaultCalendarHttpTransport) {
            try {
                return invoke28((com.codename1.calendar.DefaultCalendarHttpTransport) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.FreeBusyInterval) {
            try {
                return invoke29((com.codename1.calendar.FreeBusyInterval) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.MemoryCalendarCache) {
            try {
                return invoke30((com.codename1.calendar.MemoryCalendarCache) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.OidcCalendarTokenProvider) {
            try {
                return invoke31((com.codename1.calendar.OidcCalendarTokenProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.StorageCalendarCache) {
            try {
                return invoke32((com.codename1.calendar.StorageCalendarCache) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarCache) {
            try {
                return invoke33((com.codename1.calendar.CalendarCache) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarChangeListener) {
            try {
                return invoke34((com.codename1.calendar.CalendarChangeListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarHttpTransport) {
            try {
                return invoke35((com.codename1.calendar.CalendarHttpTransport) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.CalendarTokenProvider) {
            try {
                return invoke36((com.codename1.calendar.CalendarTokenProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.calendar.OidcCalendarTokenProvider.TokenListener) {
            try {
                return invoke37((com.codename1.calendar.OidcCalendarTokenProvider.TokenListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.calendar.GoogleCalendarSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("deleteCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.deleteCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteCalendar((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("deleteTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.getAuthorizationStatus((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("listCalendars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false);
                return typedTarget.listCalendars((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("queryEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryEvents((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("queryFreeBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false);
                return typedTarget.queryFreeBusy((java.util.List) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.time.Instant) adaptedArgs[2]);
            }
        }
        if ("queryTasks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryTasks((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.requestAuthorization((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("respondToEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false);
                return typedTarget.respondToEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("saveCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.saveCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
        }
        if ("saveEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("saveTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveTask((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.calendar.MicrosoftCalendarSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("deleteCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.deleteCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteCalendar((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("deleteTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.getAuthorizationStatus((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("listCalendars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false);
                return typedTarget.listCalendars((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("queryEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryEvents((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("queryFreeBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false);
                return typedTarget.queryFreeBusy((java.util.List) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.time.Instant) adaptedArgs[2]);
            }
        }
        if ("queryTasks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryTasks((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.requestAuthorization((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("respondToEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false);
                return typedTarget.respondToEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("saveCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.saveCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
        }
        if ("saveEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("saveTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveTask((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.calendar.CalDavCalendarSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("deleteCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.deleteCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteCalendar((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("deleteTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.getAuthorizationStatus((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("listCalendars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false);
                return typedTarget.listCalendars((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("queryEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryEvents((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("queryFreeBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false);
                return typedTarget.queryFreeBusy((java.util.List) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.time.Instant) adaptedArgs[2]);
            }
        }
        if ("queryTasks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryTasks((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.requestAuthorization((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("respondToEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false);
                return typedTarget.respondToEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("saveCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.saveCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
        }
        if ("saveEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("saveTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveTask((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.calendar.LocalCalendarSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("deleteCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.deleteCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteCalendar((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("deleteTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.getAuthorizationStatus((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("listCalendars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false);
                return typedTarget.listCalendars((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("queryEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryEvents((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("queryFreeBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false);
                return typedTarget.queryFreeBusy((java.util.List) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.time.Instant) adaptedArgs[2]);
            }
        }
        if ("queryTasks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryTasks((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.requestAuthorization((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("respondToEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false);
                return typedTarget.respondToEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("saveCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.saveCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
        }
        if ("saveEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("saveTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveTask((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.calendar.OAuthCalendarSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("deleteCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.deleteCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteCalendar((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("deleteTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.getAuthorizationStatus((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("listCalendars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false);
                return typedTarget.listCalendars((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("queryEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryEvents((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("queryFreeBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false);
                return typedTarget.queryFreeBusy((java.util.List) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.time.Instant) adaptedArgs[2]);
            }
        }
        if ("queryTasks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryTasks((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.requestAuthorization((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("respondToEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false);
                return typedTarget.respondToEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("saveCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.saveCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
        }
        if ("saveEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("saveTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveTask((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.calendar.CalDavAuthentication typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("authorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.authorization((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.calendar.CalendarAlarm typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAbsoluteTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteTime();
            }
        }
        if ("getMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMethod();
            }
        }
        if ("getTimeBefore".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeBefore();
            }
        }
        if ("setAbsoluteTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return typedTarget.setAbsoluteTime((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("setMethod".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.Method.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.Method.class}, false);
                return typedTarget.setMethod((com.codename1.calendar.CalendarAlarm.Method) adaptedArgs[0]);
            }
        }
        if ("setTimeBefore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.setTimeBefore((java.time.Duration) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.calendar.CalendarAttachment typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getContent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContent();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMimeType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMimeType();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("getUri".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUri();
            }
        }
        if ("setContent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return typedTarget.setContent((byte[]) adaptedArgs[0]);
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setMimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setMimeType((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.setSize(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("setUri".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setUri((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.calendar.CalendarAttendee typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getEmail".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEmail();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getResponse".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponse();
            }
        }
        if ("getRole".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRole();
            }
        }
        if ("getUri".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUri();
            }
        }
        if ("isOrganizer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOrganizer();
            }
        }
        if ("isSelf".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSelf();
            }
        }
        if ("setEmail".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setEmail((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setOrganizer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setOrganizer(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setResponse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.Response.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.Response.class}, false);
                return typedTarget.setResponse((com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[0]);
            }
        }
        if ("setRole".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.Role.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.Role.class}, false);
                return typedTarget.setRole((com.codename1.calendar.CalendarAttendee.Role) adaptedArgs[0]);
            }
        }
        if ("setSelf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setSelf(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setUri".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setUri((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.calendar.CalendarAuthToken typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccessToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessToken();
            }
        }
        if ("getExpiresAt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpiresAt();
            }
        }
        if ("getScopes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScopes();
            }
        }
        if ("isExpiringWithin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Duration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Duration.class}, false);
                return typedTarget.isExpiringWithin((java.time.Duration) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.calendar.CalendarCapabilities typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asSet();
            }
        }
        if ("supports".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarCapability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarCapability.class}, false);
                return typedTarget.supports((com.codename1.calendar.CalendarCapability) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.calendar.CalendarChange typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCalendarId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCalendarId();
            }
        }
        if ("getChangeType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChangeType();
            }
        }
        if ("getEntityType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEntityType();
            }
        }
        if ("getItemId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItemId();
            }
        }
        if ("getSourceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSourceId();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.calendar.CalendarConference typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addPhoneNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.addPhoneNumber((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getJoinUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getJoinUrl();
            }
        }
        if ("getPhoneNumbers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPhoneNumbers();
            }
        }
        if ("getProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProvider();
            }
        }
        if ("isCreateRequested".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCreateRequested();
            }
        }
        if ("setCreateRequested".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setCreateRequested(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setJoinUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setJoinUrl((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setProvider".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setProvider((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.calendar.CalendarConflict typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLocal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocal();
            }
        }
        if ("getMutationId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMutationId();
            }
        }
        if ("getRemote".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRemote();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.calendar.CalendarDateTime typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDate();
            }
        }
        if ("getDateTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDateTime();
            }
        }
        if ("isAllDay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAllDay();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.calendar.CalendarEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAlarm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false);
                return typedTarget.addAlarm((com.codename1.calendar.CalendarAlarm) adaptedArgs[0]);
            }
        }
        if ("addAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false);
                return typedTarget.addAttachment((com.codename1.calendar.CalendarAttachment) adaptedArgs[0]);
            }
        }
        if ("addAttendee".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.class}, false);
                return typedTarget.addAttendee((com.codename1.calendar.CalendarAttendee) adaptedArgs[0]);
            }
        }
        if ("clearAlarms".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearAlarms();
            }
        }
        if ("clearAttachments".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearAttachments();
            }
        }
        if ("clearAttendees".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearAttendees();
            }
        }
        if ("clearProviderData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearProviderData();
            }
        }
        if ("getAlarms".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlarms();
            }
        }
        if ("getAttachments".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttachments();
            }
        }
        if ("getAttendees".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttendees();
            }
        }
        if ("getAvailability".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailability();
            }
        }
        if ("getCalendarId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCalendarId();
            }
        }
        if ("getConference".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConference();
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getEnd".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEnd();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocation();
            }
        }
        if ("getPrivacy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrivacy();
            }
        }
        if ("getProviderData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProviderData();
            }
        }
        if ("getRecurrence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecurrence();
            }
        }
        if ("getRecurringEventId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecurringEventId();
            }
        }
        if ("getSourceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSourceId();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("putProviderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.putProviderData((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("removeAlarm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false);
                return typedTarget.removeAlarm((com.codename1.calendar.CalendarAlarm) adaptedArgs[0]);
            }
        }
        if ("removeAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false);
                return typedTarget.removeAttachment((com.codename1.calendar.CalendarAttachment) adaptedArgs[0]);
            }
        }
        if ("removeAttendee".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttendee.class}, false);
                return typedTarget.removeAttendee((com.codename1.calendar.CalendarAttendee) adaptedArgs[0]);
            }
        }
        if ("removeProviderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.removeProviderData((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setAvailability".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.Availability.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.Availability.class}, false);
                return typedTarget.setAvailability((com.codename1.calendar.CalendarEvent.Availability) adaptedArgs[0]);
            }
        }
        if ("setCalendarId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setCalendarId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setConference".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarConference.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarConference.class}, false);
                return typedTarget.setConference((com.codename1.calendar.CalendarConference) adaptedArgs[0]);
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDescription((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setEnd".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false);
                return typedTarget.setEnd((com.codename1.calendar.CalendarDateTime) adaptedArgs[0]);
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLocation((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPrivacy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.Privacy.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.Privacy.class}, false);
                return typedTarget.setPrivacy((com.codename1.calendar.CalendarEvent.Privacy) adaptedArgs[0]);
            }
        }
        if ("setRecurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.class}, false);
                return typedTarget.setRecurrence((com.codename1.calendar.CalendarRecurrenceRule) adaptedArgs[0]);
            }
        }
        if ("setRecurringEventId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setRecurringEventId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSourceId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSourceId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false);
                return typedTarget.setStart((com.codename1.calendar.CalendarDateTime) adaptedArgs[0]);
            }
        }
        if ("setStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.Status.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.Status.class}, false);
                return typedTarget.setStatus((com.codename1.calendar.CalendarEvent.Status) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setUrl((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setVersion((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.calendar.CalendarException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getError".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getError();
            }
        }
        if ("getResponseBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseBody();
            }
        }
        if ("getResponseCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseCode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.calendar.CalendarHttpRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBody();
            }
        }
        if ("getHeaders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaders();
            }
        }
        if ("getMethod".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMethod();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("header".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.header((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setBody((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.calendar.CalendarHttpResponse typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBody();
            }
        }
        if ("getHeader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getHeader((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getHeaders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeaders();
            }
        }
        if ("getStatusCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatusCode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.calendar.CalendarInfo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAccountId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccountId();
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getContentType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContentType();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getProviderData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProviderData();
            }
        }
        if ("getSourceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSourceId();
            }
        }
        if ("getTimeZone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeZone();
            }
        }
        if ("isPrimary".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPrimary();
            }
        }
        if ("isReadOnly".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isReadOnly();
            }
        }
        if ("putProviderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.putProviderData((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setAccountId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setAccountId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setCapabilities".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarCapabilities.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarCapabilities.class}, false);
                return typedTarget.setCapabilities((com.codename1.calendar.CalendarCapabilities) adaptedArgs[0]);
            }
        }
        if ("setColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setContentType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class}, false);
                return typedTarget.setContentType((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0]);
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setName((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setOwner((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPrimary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setPrimary(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setReadOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setReadOnly(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setSourceId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSourceId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTimeZone".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.ZoneId.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.ZoneId.class}, false);
                return typedTarget.setTimeZone((java.time.ZoneId) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.calendar.CalendarManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLocalSource".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalSource();
            }
        }
        if ("getSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getSource((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getSources".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSources();
            }
        }
        if ("registerSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarSource.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarSource.class}, false);
                return typedTarget.registerSource((com.codename1.calendar.CalendarSource) adaptedArgs[0]);
            }
        }
        if ("removeSource".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.removeSource((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.calendar.CalendarPage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getItems".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getItems();
            }
        }
        if ("getNextPageToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextPageToken();
            }
        }
        if ("getSyncToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSyncToken();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.calendar.CalendarQuery typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCalendarId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCalendarId();
            }
        }
        if ("getEndTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndTime();
            }
        }
        if ("getPageSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPageSize();
            }
        }
        if ("getPageToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPageToken();
            }
        }
        if ("getStartTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartTime();
            }
        }
        if ("getSyncToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSyncToken();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("isExpandRecurrences".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isExpandRecurrences();
            }
        }
        if ("isIncludeDeleted".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIncludeDeleted();
            }
        }
        if ("setCalendarId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setCalendarId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setEndTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return typedTarget.setEndTime((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("setExpandRecurrences".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setExpandRecurrences(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setIncludeDeleted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setIncludeDeleted(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setPageSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPageSize(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPageToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setPageToken((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setStartTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return typedTarget.setStartTime((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("setSyncToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSyncToken((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setText((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.calendar.CalendarRecurrenceRule typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addDayOfMonth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.addDayOfMonth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("addDayOfWeek".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.addDayOfWeek(toIntValue(adaptedArgs[0]));
            }
        }
        if ("addMonth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.addMonth(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCount();
            }
        }
        if ("getDaysOfMonth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDaysOfMonth();
            }
        }
        if ("getDaysOfWeek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDaysOfWeek();
            }
        }
        if ("getFrequency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrequency();
            }
        }
        if ("getInterval".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInterval();
            }
        }
        if ("getMonths".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMonths();
            }
        }
        if ("getUntil".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUntil();
            }
        }
        if ("setCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCount(Integer.valueOf(toIntValue(adaptedArgs[0])));
            }
        }
        if ("setFrequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.Frequency.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.Frequency.class}, false);
                return typedTarget.setFrequency((com.codename1.calendar.CalendarRecurrenceRule.Frequency) adaptedArgs[0]);
            }
        }
        if ("setInterval".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setInterval(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setUntil".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false);
                return typedTarget.setUntil((com.codename1.calendar.CalendarDateTime) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.calendar.CalendarSource typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.addChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("deleteCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.deleteCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.deleteCalendar((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("deleteEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("deleteTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class, java.lang.String.class}, false);
                return typedTarget.deleteTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("getAuthorizationStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.getAuthorizationStatus((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCapabilities();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.getTask((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("isAvailable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAvailable();
            }
        }
        if ("listCalendars".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.ContentType.class, java.lang.String.class}, false);
                return typedTarget.listCalendars((com.codename1.calendar.CalendarInfo.ContentType) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("queryEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryEvents((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("queryFreeBusy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.time.Instant.class, java.time.Instant.class}, false);
                return typedTarget.queryFreeBusy((java.util.List) adaptedArgs[0], (java.time.Instant) adaptedArgs[1], (java.time.Instant) adaptedArgs[2]);
            }
        }
        if ("queryTasks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarQuery.class}, false);
                return typedTarget.queryTasks((com.codename1.calendar.CalendarQuery) adaptedArgs[0]);
            }
        }
        if ("removeChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChangeListener.class}, false);
                typedTarget.removeChangeListener((com.codename1.calendar.CalendarChangeListener) adaptedArgs[0]); return null;
            }
        }
        if ("requestAuthorization".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAccess.class}, false);
                return typedTarget.requestAuthorization((com.codename1.calendar.CalendarAccess) adaptedArgs[0]);
            }
        }
        if ("respondToEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarAttendee.Response.class, java.lang.String.class}, false);
                return typedTarget.respondToEvent((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.calendar.CalendarAttendee.Response) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("saveCalendar".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarInfo.class}, false);
                return typedTarget.saveCalendar((com.codename1.calendar.CalendarInfo) adaptedArgs[0]);
            }
        }
        if ("saveEvent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveEvent((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("saveTask".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.saveTask((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(com.codename1.calendar.CalendarSyncEngine typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPendingCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPendingCount();
            }
        }
        if ("queueEventDelete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.queueEventDelete((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[3]);
            }
        }
        if ("queueEventSave".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarEvent.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.queueEventSave((com.codename1.calendar.CalendarEvent) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("queueTaskDelete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.queueTaskDelete((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[3]);
            }
        }
        if ("queueTaskSave".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarTask.class, com.codename1.calendar.CalendarMutationScope.class}, false);
                return typedTarget.queueTaskSave((com.codename1.calendar.CalendarTask) adaptedArgs[0], (com.codename1.calendar.CalendarMutationScope) adaptedArgs[1]);
            }
        }
        if ("resolveConflict".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.calendar.CalendarConflict.Resolution.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.calendar.CalendarConflict.Resolution.class, java.util.Map.class}, false);
                typedTarget.resolveConflict((java.lang.String) adaptedArgs[0], (com.codename1.calendar.CalendarConflict.Resolution) adaptedArgs[1], (java.util.Map) adaptedArgs[2]); return null;
            }
        }
        if ("sync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.sync();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(com.codename1.calendar.CalendarSyncResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAppliedCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAppliedCount();
            }
        }
        if ("getConflicts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConflicts();
            }
        }
        if ("getRemainingCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRemainingCount();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke27(com.codename1.calendar.CalendarTask typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAlarm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false);
                return typedTarget.addAlarm((com.codename1.calendar.CalendarAlarm) adaptedArgs[0]);
            }
        }
        if ("addAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false);
                return typedTarget.addAttachment((com.codename1.calendar.CalendarAttachment) adaptedArgs[0]);
            }
        }
        if ("clearAlarms".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearAlarms();
            }
        }
        if ("clearAttachments".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearAttachments();
            }
        }
        if ("clearProviderData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.clearProviderData();
            }
        }
        if ("getAlarms".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlarms();
            }
        }
        if ("getAttachments".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttachments();
            }
        }
        if ("getCalendarId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCalendarId();
            }
        }
        if ("getCompletionTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompletionTime();
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getDue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDue();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocation();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        if ("getProviderData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProviderData();
            }
        }
        if ("getRecurrence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRecurrence();
            }
        }
        if ("getSourceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSourceId();
            }
        }
        if ("getStart".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStart();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("isCompleted".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCompleted();
            }
        }
        if ("putProviderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.putProviderData((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("removeAlarm".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAlarm.class}, false);
                return typedTarget.removeAlarm((com.codename1.calendar.CalendarAlarm) adaptedArgs[0]);
            }
        }
        if ("removeAttachment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarAttachment.class}, false);
                return typedTarget.removeAttachment((com.codename1.calendar.CalendarAttachment) adaptedArgs[0]);
            }
        }
        if ("removeProviderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.removeProviderData((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setCalendarId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setCalendarId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setCompleted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setCompleted(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setCompletionTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.time.Instant.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.time.Instant.class}, false);
                return typedTarget.setCompletionTime((java.time.Instant) adaptedArgs[0]);
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setDescription((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setDue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false);
                return typedTarget.setDue((com.codename1.calendar.CalendarDateTime) adaptedArgs[0]);
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setLocation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLocation((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPriority".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setPriority(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setRecurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarRecurrenceRule.class}, false);
                return typedTarget.setRecurrence((com.codename1.calendar.CalendarRecurrenceRule) adaptedArgs[0]);
            }
        }
        if ("setSourceId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSourceId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setStart".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarDateTime.class}, false);
                return typedTarget.setStart((com.codename1.calendar.CalendarDateTime) adaptedArgs[0]);
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setVersion((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke28(com.codename1.calendar.DefaultCalendarHttpTransport typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarHttpRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarHttpRequest.class}, false);
                return typedTarget.execute((com.codename1.calendar.CalendarHttpRequest) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke29(com.codename1.calendar.FreeBusyInterval typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAvailability".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailability();
            }
        }
        if ("getEndTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndTime();
            }
        }
        if ("getStartTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartTime();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke30(com.codename1.calendar.MemoryCalendarCache typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.clear((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("load".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.load((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("store".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                typedTarget.store((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke31(com.codename1.calendar.OidcCalendarTokenProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class}, false);
                return typedTarget.getToken((java.lang.String[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("setTokenListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.OidcCalendarTokenProvider.TokenListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.OidcCalendarTokenProvider.TokenListener.class}, false);
                return typedTarget.setTokenListener((com.codename1.calendar.OidcCalendarTokenProvider.TokenListener) adaptedArgs[0]);
            }
        }
        if ("setTokens".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcTokens.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcTokens.class}, false);
                typedTarget.setTokens((com.codename1.io.oidc.OidcTokens) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke32(com.codename1.calendar.StorageCalendarCache typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.clear((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("load".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.load((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("store".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                typedTarget.store((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke33(com.codename1.calendar.CalendarCache typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.clear((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("load".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.load((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("store".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                typedTarget.store((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke34(com.codename1.calendar.CalendarChangeListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("calendarChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChange.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarChange.class}, false);
                typedTarget.calendarChanged((com.codename1.calendar.CalendarChange) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke35(com.codename1.calendar.CalendarHttpTransport typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarHttpRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.calendar.CalendarHttpRequest.class}, false);
                return typedTarget.execute((com.codename1.calendar.CalendarHttpRequest) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke36(com.codename1.calendar.CalendarTokenProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.Boolean.class}, false);
                return typedTarget.getToken((java.lang.String[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke37(com.codename1.calendar.OidcCalendarTokenProvider.TokenListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("tokensUpdated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcTokens.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.oidc.OidcTokens.class}, false);
                typedTarget.tokensUpdated((com.codename1.io.oidc.OidcTokens) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.calendar.CalendarAccess.class) return getStaticField0(name);
        if (type == com.codename1.calendar.CalendarAlarm.Method.class) return getStaticField1(name);
        if (type == com.codename1.calendar.CalendarAttendee.Response.class) return getStaticField2(name);
        if (type == com.codename1.calendar.CalendarAttendee.Role.class) return getStaticField3(name);
        if (type == com.codename1.calendar.CalendarAuthorizationStatus.class) return getStaticField4(name);
        if (type == com.codename1.calendar.CalendarCapability.class) return getStaticField5(name);
        if (type == com.codename1.calendar.CalendarChange.ChangeType.class) return getStaticField6(name);
        if (type == com.codename1.calendar.CalendarChange.EntityType.class) return getStaticField7(name);
        if (type == com.codename1.calendar.CalendarConflict.Resolution.class) return getStaticField8(name);
        if (type == com.codename1.calendar.CalendarError.class) return getStaticField9(name);
        if (type == com.codename1.calendar.CalendarEvent.Availability.class) return getStaticField10(name);
        if (type == com.codename1.calendar.CalendarEvent.Privacy.class) return getStaticField11(name);
        if (type == com.codename1.calendar.CalendarEvent.Status.class) return getStaticField12(name);
        if (type == com.codename1.calendar.CalendarInfo.ContentType.class) return getStaticField13(name);
        if (type == com.codename1.calendar.CalendarMutationScope.class) return getStaticField14(name);
        if (type == com.codename1.calendar.CalendarRecurrenceRule.Frequency.class) return getStaticField15(name);
        if (type == com.codename1.calendar.GoogleCalendarSource.class) return getStaticField16(name);
        if (type == com.codename1.calendar.MicrosoftCalendarSource.class) return getStaticField17(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("EVENTS_FULL".equals(name)) return com.codename1.calendar.CalendarAccess.EVENTS_FULL;
        if ("EVENTS_READ_ONLY".equals(name)) return com.codename1.calendar.CalendarAccess.EVENTS_READ_ONLY;
        if ("EVENTS_WRITE_ONLY".equals(name)) return com.codename1.calendar.CalendarAccess.EVENTS_WRITE_ONLY;
        if ("TASKS_FULL".equals(name)) return com.codename1.calendar.CalendarAccess.TASKS_FULL;
        throw unsupportedStaticField(com.codename1.calendar.CalendarAccess.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("ALERT".equals(name)) return com.codename1.calendar.CalendarAlarm.Method.ALERT;
        if ("AUDIO".equals(name)) return com.codename1.calendar.CalendarAlarm.Method.AUDIO;
        if ("DEFAULT".equals(name)) return com.codename1.calendar.CalendarAlarm.Method.DEFAULT;
        if ("EMAIL".equals(name)) return com.codename1.calendar.CalendarAlarm.Method.EMAIL;
        throw unsupportedStaticField(com.codename1.calendar.CalendarAlarm.Method.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("ACCEPTED".equals(name)) return com.codename1.calendar.CalendarAttendee.Response.ACCEPTED;
        if ("DECLINED".equals(name)) return com.codename1.calendar.CalendarAttendee.Response.DECLINED;
        if ("DELEGATED".equals(name)) return com.codename1.calendar.CalendarAttendee.Response.DELEGATED;
        if ("NEEDS_ACTION".equals(name)) return com.codename1.calendar.CalendarAttendee.Response.NEEDS_ACTION;
        if ("NONE".equals(name)) return com.codename1.calendar.CalendarAttendee.Response.NONE;
        if ("TENTATIVE".equals(name)) return com.codename1.calendar.CalendarAttendee.Response.TENTATIVE;
        throw unsupportedStaticField(com.codename1.calendar.CalendarAttendee.Response.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("OPTIONAL".equals(name)) return com.codename1.calendar.CalendarAttendee.Role.OPTIONAL;
        if ("REQUIRED".equals(name)) return com.codename1.calendar.CalendarAttendee.Role.REQUIRED;
        if ("RESOURCE".equals(name)) return com.codename1.calendar.CalendarAttendee.Role.RESOURCE;
        throw unsupportedStaticField(com.codename1.calendar.CalendarAttendee.Role.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("DENIED".equals(name)) return com.codename1.calendar.CalendarAuthorizationStatus.DENIED;
        if ("FULL".equals(name)) return com.codename1.calendar.CalendarAuthorizationStatus.FULL;
        if ("NOT_DETERMINED".equals(name)) return com.codename1.calendar.CalendarAuthorizationStatus.NOT_DETERMINED;
        if ("RESTRICTED".equals(name)) return com.codename1.calendar.CalendarAuthorizationStatus.RESTRICTED;
        if ("WRITE_ONLY".equals(name)) return com.codename1.calendar.CalendarAuthorizationStatus.WRITE_ONLY;
        throw unsupportedStaticField(com.codename1.calendar.CalendarAuthorizationStatus.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("ALARMS".equals(name)) return com.codename1.calendar.CalendarCapability.ALARMS;
        if ("ATTACHMENTS".equals(name)) return com.codename1.calendar.CalendarCapability.ATTACHMENTS;
        if ("ATTENDEES_READ".equals(name)) return com.codename1.calendar.CalendarCapability.ATTENDEES_READ;
        if ("ATTENDEES_WRITE".equals(name)) return com.codename1.calendar.CalendarCapability.ATTENDEES_WRITE;
        if ("CONFERENCING".equals(name)) return com.codename1.calendar.CalendarCapability.CONFERENCING;
        if ("DELETE_EVENTS".equals(name)) return com.codename1.calendar.CalendarCapability.DELETE_EVENTS;
        if ("DELETE_TASKS".equals(name)) return com.codename1.calendar.CalendarCapability.DELETE_TASKS;
        if ("DELTA_SYNC".equals(name)) return com.codename1.calendar.CalendarCapability.DELTA_SYNC;
        if ("FREE_BUSY".equals(name)) return com.codename1.calendar.CalendarCapability.FREE_BUSY;
        if ("LOCAL_CHANGE_LISTENER".equals(name)) return com.codename1.calendar.CalendarCapability.LOCAL_CHANGE_LISTENER;
        if ("MANAGE_CALENDARS".equals(name)) return com.codename1.calendar.CalendarCapability.MANAGE_CALENDARS;
        if ("OFFLINE_MUTATIONS".equals(name)) return com.codename1.calendar.CalendarCapability.OFFLINE_MUTATIONS;
        if ("READ_CALENDARS".equals(name)) return com.codename1.calendar.CalendarCapability.READ_CALENDARS;
        if ("READ_EVENTS".equals(name)) return com.codename1.calendar.CalendarCapability.READ_EVENTS;
        if ("READ_TASKS".equals(name)) return com.codename1.calendar.CalendarCapability.READ_TASKS;
        if ("RECURRENCE".equals(name)) return com.codename1.calendar.CalendarCapability.RECURRENCE;
        if ("RESPOND_TO_INVITATIONS".equals(name)) return com.codename1.calendar.CalendarCapability.RESPOND_TO_INVITATIONS;
        if ("WRITE_EVENTS".equals(name)) return com.codename1.calendar.CalendarCapability.WRITE_EVENTS;
        if ("WRITE_TASKS".equals(name)) return com.codename1.calendar.CalendarCapability.WRITE_TASKS;
        throw unsupportedStaticField(com.codename1.calendar.CalendarCapability.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("CREATED".equals(name)) return com.codename1.calendar.CalendarChange.ChangeType.CREATED;
        if ("DELETED".equals(name)) return com.codename1.calendar.CalendarChange.ChangeType.DELETED;
        if ("RESET".equals(name)) return com.codename1.calendar.CalendarChange.ChangeType.RESET;
        if ("UPDATED".equals(name)) return com.codename1.calendar.CalendarChange.ChangeType.UPDATED;
        throw unsupportedStaticField(com.codename1.calendar.CalendarChange.ChangeType.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("CALENDAR".equals(name)) return com.codename1.calendar.CalendarChange.EntityType.CALENDAR;
        if ("EVENT".equals(name)) return com.codename1.calendar.CalendarChange.EntityType.EVENT;
        if ("TASK".equals(name)) return com.codename1.calendar.CalendarChange.EntityType.TASK;
        throw unsupportedStaticField(com.codename1.calendar.CalendarChange.EntityType.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("KEEP_LOCAL".equals(name)) return com.codename1.calendar.CalendarConflict.Resolution.KEEP_LOCAL;
        if ("KEEP_REMOTE".equals(name)) return com.codename1.calendar.CalendarConflict.Resolution.KEEP_REMOTE;
        if ("MERGED".equals(name)) return com.codename1.calendar.CalendarConflict.Resolution.MERGED;
        throw unsupportedStaticField(com.codename1.calendar.CalendarConflict.Resolution.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("AUTHENTICATION_REQUIRED".equals(name)) return com.codename1.calendar.CalendarError.AUTHENTICATION_REQUIRED;
        if ("CANCELED".equals(name)) return com.codename1.calendar.CalendarError.CANCELED;
        if ("CONFLICT".equals(name)) return com.codename1.calendar.CalendarError.CONFLICT;
        if ("INVALID_ARGUMENT".equals(name)) return com.codename1.calendar.CalendarError.INVALID_ARGUMENT;
        if ("MALFORMED_RESPONSE".equals(name)) return com.codename1.calendar.CalendarError.MALFORMED_RESPONSE;
        if ("NETWORK".equals(name)) return com.codename1.calendar.CalendarError.NETWORK;
        if ("NOT_AVAILABLE".equals(name)) return com.codename1.calendar.CalendarError.NOT_AVAILABLE;
        if ("NOT_FOUND".equals(name)) return com.codename1.calendar.CalendarError.NOT_FOUND;
        if ("NOT_SUPPORTED".equals(name)) return com.codename1.calendar.CalendarError.NOT_SUPPORTED;
        if ("PERMISSION_DENIED".equals(name)) return com.codename1.calendar.CalendarError.PERMISSION_DENIED;
        if ("RATE_LIMITED".equals(name)) return com.codename1.calendar.CalendarError.RATE_LIMITED;
        if ("READ_ONLY".equals(name)) return com.codename1.calendar.CalendarError.READ_ONLY;
        if ("STORAGE".equals(name)) return com.codename1.calendar.CalendarError.STORAGE;
        if ("SYNC_TOKEN_EXPIRED".equals(name)) return com.codename1.calendar.CalendarError.SYNC_TOKEN_EXPIRED;
        if ("UNKNOWN".equals(name)) return com.codename1.calendar.CalendarError.UNKNOWN;
        throw unsupportedStaticField(com.codename1.calendar.CalendarError.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
        if ("BUSY".equals(name)) return com.codename1.calendar.CalendarEvent.Availability.BUSY;
        if ("FREE".equals(name)) return com.codename1.calendar.CalendarEvent.Availability.FREE;
        if ("OUT_OF_OFFICE".equals(name)) return com.codename1.calendar.CalendarEvent.Availability.OUT_OF_OFFICE;
        if ("TENTATIVE".equals(name)) return com.codename1.calendar.CalendarEvent.Availability.TENTATIVE;
        if ("WORKING_ELSEWHERE".equals(name)) return com.codename1.calendar.CalendarEvent.Availability.WORKING_ELSEWHERE;
        throw unsupportedStaticField(com.codename1.calendar.CalendarEvent.Availability.class, name);
    }

    private static Object getStaticField11(String name) throws Exception {
        if ("CONFIDENTIAL".equals(name)) return com.codename1.calendar.CalendarEvent.Privacy.CONFIDENTIAL;
        if ("DEFAULT".equals(name)) return com.codename1.calendar.CalendarEvent.Privacy.DEFAULT;
        if ("PRIVATE".equals(name)) return com.codename1.calendar.CalendarEvent.Privacy.PRIVATE;
        if ("PUBLIC".equals(name)) return com.codename1.calendar.CalendarEvent.Privacy.PUBLIC;
        throw unsupportedStaticField(com.codename1.calendar.CalendarEvent.Privacy.class, name);
    }

    private static Object getStaticField12(String name) throws Exception {
        if ("CANCELED".equals(name)) return com.codename1.calendar.CalendarEvent.Status.CANCELED;
        if ("CONFIRMED".equals(name)) return com.codename1.calendar.CalendarEvent.Status.CONFIRMED;
        if ("TENTATIVE".equals(name)) return com.codename1.calendar.CalendarEvent.Status.TENTATIVE;
        throw unsupportedStaticField(com.codename1.calendar.CalendarEvent.Status.class, name);
    }

    private static Object getStaticField13(String name) throws Exception {
        if ("EVENTS".equals(name)) return com.codename1.calendar.CalendarInfo.ContentType.EVENTS;
        if ("TASKS".equals(name)) return com.codename1.calendar.CalendarInfo.ContentType.TASKS;
        throw unsupportedStaticField(com.codename1.calendar.CalendarInfo.ContentType.class, name);
    }

    private static Object getStaticField14(String name) throws Exception {
        if ("ALL".equals(name)) return com.codename1.calendar.CalendarMutationScope.ALL;
        if ("THIS_AND_FUTURE".equals(name)) return com.codename1.calendar.CalendarMutationScope.THIS_AND_FUTURE;
        if ("THIS_INSTANCE".equals(name)) return com.codename1.calendar.CalendarMutationScope.THIS_INSTANCE;
        throw unsupportedStaticField(com.codename1.calendar.CalendarMutationScope.class, name);
    }

    private static Object getStaticField15(String name) throws Exception {
        if ("DAILY".equals(name)) return com.codename1.calendar.CalendarRecurrenceRule.Frequency.DAILY;
        if ("MONTHLY".equals(name)) return com.codename1.calendar.CalendarRecurrenceRule.Frequency.MONTHLY;
        if ("WEEKLY".equals(name)) return com.codename1.calendar.CalendarRecurrenceRule.Frequency.WEEKLY;
        if ("YEARLY".equals(name)) return com.codename1.calendar.CalendarRecurrenceRule.Frequency.YEARLY;
        throw unsupportedStaticField(com.codename1.calendar.CalendarRecurrenceRule.Frequency.class, name);
    }

    private static Object getStaticField16(String name) throws Exception {
        if ("SCOPE_CALENDAR".equals(name)) return com.codename1.calendar.GoogleCalendarSource.SCOPE_CALENDAR;
        if ("SCOPE_TASKS".equals(name)) return com.codename1.calendar.GoogleCalendarSource.SCOPE_TASKS;
        throw unsupportedStaticField(com.codename1.calendar.GoogleCalendarSource.class, name);
    }

    private static Object getStaticField17(String name) throws Exception {
        if ("SCOPE_CALENDARS".equals(name)) return com.codename1.calendar.MicrosoftCalendarSource.SCOPE_CALENDARS;
        if ("SCOPE_TASKS".equals(name)) return com.codename1.calendar.MicrosoftCalendarSource.SCOPE_TASKS;
        throw unsupportedStaticField(com.codename1.calendar.MicrosoftCalendarSource.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
