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
package com.codename1.impl.ios;

import com.codename1.calendar.*;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** EventKit-backed local calendar source for iOS and Mac Catalyst. */
final class IOSCalendarSource extends LocalCalendarSource {
    private static final int EVENTS=0,REMINDERS=1;
    private final IOSNative nativeInstance;
    private final CalendarCapabilities capabilities;
    IOSCalendarSource(IOSNative nativeInstance){
        this.nativeInstance=nativeInstance;
        capabilities=nativeInstance.calendarSupported()?CalendarCapabilities.of(CalendarCapability.READ_CALENDARS,
                CalendarCapability.READ_EVENTS,CalendarCapability.WRITE_EVENTS,CalendarCapability.DELETE_EVENTS,
                CalendarCapability.READ_TASKS,CalendarCapability.WRITE_TASKS,CalendarCapability.DELETE_TASKS,
                CalendarCapability.ALARMS,CalendarCapability.FREE_BUSY,CalendarCapability.OFFLINE_MUTATIONS):CalendarCapabilities.none();
    }
    public CalendarCapabilities getCapabilities(){return capabilities;}
    public CalendarAuthorizationStatus getAuthorizationStatus(CalendarAccess access){return status(nativeInstance.calendarAuthorizationStatus(access==CalendarAccess.TASKS_FULL?REMINDERS:EVENTS));}
    public AsyncResource<CalendarAuthorizationStatus>requestAuthorization(final CalendarAccess access){
        final AsyncResource<CalendarAuthorizationStatus>out=new AsyncResource<CalendarAuthorizationStatus>();
        Display.getInstance().scheduleBackgroundTask(new Runnable(){public void run(){
            boolean granted=nativeInstance.calendarRequestAccess(access==CalendarAccess.TASKS_FULL?REMINDERS:EVENTS,access==CalendarAccess.EVENTS_WRITE_ONLY);
            out.complete(granted?(access==CalendarAccess.EVENTS_WRITE_ONLY?CalendarAuthorizationStatus.WRITE_ONLY:CalendarAuthorizationStatus.FULL):CalendarAuthorizationStatus.DENIED);
        }});
        return out;
    }
    public AsyncResource<CalendarPage<CalendarInfo>>listCalendars(CalendarInfo.ContentType type,String token){try{int entity=type==CalendarInfo.ContentType.TASKS?REMINDERS:EVENTS;List<CalendarInfo>items=new ArrayList<CalendarInfo>();for(Map<String,Object>m:items(nativeInstance.calendarList(entity)))items.add(new CalendarInfo().setId(s(m,"id")).setSourceId(getId()).setName(s(m,"title")).setColor(i(m,"color")).setReadOnly(!b(m,"allowsModify")).setContentType(type).setCapabilities(capabilities));return value(new CalendarPage<CalendarInfo>(items,null,String.valueOf(System.currentTimeMillis())));}catch(Exception ex){return failure(ex);}}
    public AsyncResource<CalendarPage<CalendarEvent>>queryEvents(CalendarQuery query){try{String calendar=query==null?null:query.getCalendarId();long start=query==null||query.getStartTime()==null?System.currentTimeMillis()-31536000000L:query.getStartTime().toEpochMilli(),end=query==null||query.getEndTime()==null?System.currentTimeMillis()+31536000000L:query.getEndTime().toEpochMilli();List<CalendarEvent>items=new ArrayList<CalendarEvent>();for(Map<String,Object>m:items(nativeInstance.calendarEvents(calendar,start,end)))items.add(event(m));return value(new CalendarPage<CalendarEvent>(items,null,String.valueOf(System.currentTimeMillis())));}catch(Exception ex){return failure(ex);}}
    public AsyncResource<CalendarEvent> getEvent(String calendarId, String id) {
        if (id == null) {
            return failed(CalendarError.INVALID_ARGUMENT, "eventId required");
        }
        try {
            Map<String, Object> result = parse(nativeInstance.calendarEvent(calendarId, id));
            if (s(result, "id") == null) {
                return failed(CalendarError.NOT_FOUND, "Event not found");
            }
            return value(event(result));
        } catch (Exception ex) {
            return failure(ex);
        }
    }
    public AsyncResource<CalendarEvent>saveEvent(CalendarEvent event,CalendarMutationScope scope){try{Map<String,Object>m=new HashMap<String,Object>();m.put("id",event.getId());m.put("calendarId",event.getCalendarId());m.put("title",event.getTitle());m.put("notes",event.getDescription());m.put("location",event.getLocation());putDate(m,"start",event.getStart());putDate(m,"end",event.getEnd());m.put("allDay",Boolean.valueOf(event.getStart()!=null&&event.getStart().isAllDay()));List<Map<String,Object>>alarms=new ArrayList<Map<String,Object>>();for(CalendarAlarm alarm:event.getAlarms()){Map<String,Object>a=new HashMap<String,Object>();a.put("minutes",alarm.getTimeBefore()==null?null:Long.valueOf(alarm.getTimeBefore().getSeconds()/60L));a.put("absolute",alarm.getAbsoluteTime()==null?null:Long.valueOf(alarm.getAbsoluteTime().toEpochMilli()));alarms.add(a);}m.put("alarms",alarms);boolean create=event.getId()==null;Map<String,Object>saved=parseResult(nativeInstance.calendarSaveEvent(JSONParser.toJson(m),scope==null?0:scope.ordinal()));CalendarEvent result=event(saved);fireChange(new CalendarChange(getId(),result.getCalendarId(),result.getId(),CalendarChange.EntityType.EVENT,create?CalendarChange.ChangeType.CREATED:CalendarChange.ChangeType.UPDATED));return value(result);}catch(IOException ex){return failure(ex);}catch(CalendarException ex){return failure(ex);}}
    public AsyncResource<Boolean>deleteEvent(String calendarId,String eventId,CalendarMutationScope scope,String version){boolean deleted=nativeInstance.calendarDeleteEvent(eventId,scope==null?0:scope.ordinal());if(deleted)fireChange(new CalendarChange(getId(),calendarId,eventId,CalendarChange.EntityType.EVENT,CalendarChange.ChangeType.DELETED));return value(Boolean.valueOf(deleted));}
    public AsyncResource<List<FreeBusyInterval>>queryFreeBusy(List<String>ids,Instant start,Instant end){
        final AsyncResource<List<FreeBusyInterval>>out=new AsyncResource<List<FreeBusyInterval>>();
        queryEvents(new CalendarQuery().setStartTime(start).setEndTime(end)).ready(new FreeBusyReady(out)).except(new ErrorForwarder<List<FreeBusyInterval>>(out));
        return out;
    }
    public AsyncResource<CalendarPage<CalendarTask>>queryTasks(CalendarQuery query){try{List<CalendarTask>out=new ArrayList<CalendarTask>();for(Map<String,Object>m:items(nativeInstance.calendarTasks(query==null?null:query.getCalendarId())))out.add(task(m));return value(new CalendarPage<CalendarTask>(out,null,String.valueOf(System.currentTimeMillis())));}catch(Exception ex){return failure(ex);}}
    public AsyncResource<CalendarTask>getTask(String calendarId,final String id){
        if(id==null)return failed(CalendarError.INVALID_ARGUMENT,"taskId required");
        final AsyncResource<CalendarTask>out=new AsyncResource<CalendarTask>();
        queryTasks(new CalendarQuery().setCalendarId(calendarId)).ready(new TaskReady(id,out)).except(new ErrorForwarder<CalendarTask>(out));
        return out;
    }
    public AsyncResource<CalendarTask>saveTask(CalendarTask task,CalendarMutationScope scope){try{Map<String,Object>m=new HashMap<String,Object>();m.put("id",task.getId());m.put("calendarId",task.getCalendarId());m.put("title",task.getTitle());m.put("notes",task.getDescription());m.put("completed",Boolean.valueOf(task.isCompleted()));putDate(m,"due",task.getDue());boolean create=task.getId()==null;CalendarTask saved=task(parseResult(nativeInstance.calendarSaveTask(JSONParser.toJson(m))));fireChange(new CalendarChange(getId(),saved.getCalendarId(),saved.getId(),CalendarChange.EntityType.TASK,create?CalendarChange.ChangeType.CREATED:CalendarChange.ChangeType.UPDATED));return value(saved);}catch(IOException ex){return failure(ex);}catch(CalendarException ex){return failure(ex);}}
    public AsyncResource<Boolean>deleteTask(String calendarId,String id,CalendarMutationScope scope,String version){boolean deleted=nativeInstance.calendarDeleteTask(id);if(deleted)fireChange(new CalendarChange(getId(),calendarId,id,CalendarChange.EntityType.TASK,CalendarChange.ChangeType.DELETED));return value(Boolean.valueOf(deleted));}
    private CalendarEvent event(Map<String,Object>m){CalendarEvent out=new CalendarEvent().setId(s(m,"id")).setCalendarId(s(m,"calendarId")).setSourceId(getId()).setVersion(s(m,"version")).setTitle(s(m,"title")).setDescription(s(m,"notes")).setLocation(s(m,"location")).setAvailability(b(m,"available")?CalendarEvent.Availability.FREE:CalendarEvent.Availability.BUSY);out.setStart(date(m,"start",b(m,"allDay"))).setEnd(date(m,"end",b(m,"allDay")));for(Map<String,Object>a:maps(m.get("alarms"))){if(a.get("minutes")!=null)out.addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(i(a,"minutes"))));else if(a.get("absolute")!=null)out.addAlarm(new CalendarAlarm().setAbsoluteTime(Instant.ofEpochMilli(l(a,"absolute"))));}return out;}
    private CalendarTask task(Map<String,Object>m){return new CalendarTask().setId(s(m,"id")).setCalendarId(s(m,"calendarId")).setSourceId(getId()).setVersion(s(m,"version")).setTitle(s(m,"title")).setDescription(s(m,"notes")).setCompleted(b(m,"completed")).setDue(date(m,"due",b(m,"dueAllDay")));}
    private static void putDate(Map<String,Object>m,String key,CalendarDateTime d){if(d==null)return;if(d.isAllDay()){m.put(key+"Date",d.getDate().toString());m.put(key+"AllDay",Boolean.TRUE);}else{m.put(key,Long.valueOf(d.getDateTime().toInstant().toEpochMilli()));m.put(key+"Zone",d.getDateTime().getZone().getId());}}
    private static CalendarDateTime date(Map<String,Object>m,String key,boolean allDay){String value=s(m,key+"Date");if(value!=null)return CalendarDateTime.allDay(LocalDate.parse(value));String zone=s(m,key+"Zone");return m.get(key)==null?null:CalendarDateTime.instant(Instant.ofEpochMilli(l(m,key)),ZoneId.of(zone==null?"UTC":zone));}
    private static CalendarAuthorizationStatus status(int value){return value==3?CalendarAuthorizationStatus.FULL:value==4?CalendarAuthorizationStatus.WRITE_ONLY:value==1?CalendarAuthorizationStatus.RESTRICTED:value==2?CalendarAuthorizationStatus.DENIED:CalendarAuthorizationStatus.NOT_DETERMINED;}
    private static final class FreeBusyReady implements SuccessCallback<CalendarPage<CalendarEvent>>{
        private final AsyncResource<List<FreeBusyInterval>>out;
        FreeBusyReady(AsyncResource<List<FreeBusyInterval>>out){this.out=out;}
        public void onSucess(CalendarPage<CalendarEvent>page){List<FreeBusyInterval>items=new ArrayList<FreeBusyInterval>();for(CalendarEvent event:page.getItems())if(event.getAvailability()!=CalendarEvent.Availability.FREE&&event.getStart()!=null&&event.getEnd()!=null&&!event.getStart().isAllDay())items.add(new FreeBusyInterval(event.getStart().getDateTime().toInstant(),event.getEnd().getDateTime().toInstant(),event.getAvailability()));out.complete(items);}
    }
    private static final class TaskReady implements SuccessCallback<CalendarPage<CalendarTask>>{
        private final String id;private final AsyncResource<CalendarTask>out;
        TaskReady(String id,AsyncResource<CalendarTask>out){this.id=id;this.out=out;}
        public void onSucess(CalendarPage<CalendarTask>page){for(CalendarTask task:page.getItems())if(id.equals(task.getId())){out.complete(task);return;}out.error(new CalendarException(CalendarError.NOT_FOUND,"Task not found"));}
    }
    private static final class ErrorForwarder<T>implements SuccessCallback<Throwable>{
        private final AsyncResource<T>out;
        ErrorForwarder(AsyncResource<T>out){this.out=out;}
        public void onSucess(Throwable error){out.error(error);}
    }
    private static Map<String,Object>parse(String json)throws IOException{return JSONParser.parseJSON(json);}private static Map<String,Object>parseResult(String json)throws IOException,CalendarException{Map<String,Object>out=parse(json);if(out.get("error")!=null)throw new CalendarException(CalendarError.UNKNOWN,String.valueOf(out.get("error")));return out;}@SuppressWarnings("unchecked")private static List<Map<String,Object>>items(String json)throws IOException{return maps(parse(json).get("items"));}@SuppressWarnings("unchecked")private static List<Map<String,Object>>maps(Object v){List<Map<String,Object>>out=new ArrayList<Map<String,Object>>();if(v instanceof List)for(Object x:(List<Object>)v)if(x instanceof Map)out.add((Map<String,Object>)x);return out;}private static String s(Map<String,Object>m,String k){Object v=m.get(k);return v==null?null:String.valueOf(v);}private static long l(Map<String,Object>m,String k){Object v=m.get(k);return v instanceof Number?((Number)v).longValue():Long.parseLong(String.valueOf(v));}private static int i(Map<String,Object>m,String k){Object v=m.get(k);return v instanceof Number?((Number)v).intValue():Integer.parseInt(String.valueOf(v));}private static boolean b(Map<String,Object>m,String k){Object v=m.get(k);return Boolean.TRUE.equals(v)||"true".equals(String.valueOf(v));}
    private static <T>AsyncResource<T>value(T value){AsyncResource<T>out=new AsyncResource<T>();out.complete(value);return out;}private static <T>AsyncResource<T>failed(CalendarError type,String message){AsyncResource<T>out=new AsyncResource<T>();out.error(new CalendarException(type,message));return out;}private static <T>AsyncResource<T>failure(Throwable error){AsyncResource<T>out=new AsyncResource<T>();out.error(error instanceof CalendarException?error:new CalendarException(CalendarError.UNKNOWN,error.getMessage(),error));return out;}
}
