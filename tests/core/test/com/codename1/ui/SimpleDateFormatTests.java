/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.l10n.DateFormat;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.system.NativeLookup;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.tests.NativeTimeZoneUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author shannah
 */
public class SimpleDateFormatTests extends AbstractTest {

    private static class Res {

        boolean complete;
        Throwable error;
    }

    @Override
    public boolean runTest() throws Exception {
        
        if (Display.getInstance().isSimulator()) {
            
            NativeTimeZoneUtil tzUtil = NativeLookup.create(NativeTimeZoneUtil.class);
            TimeZone defaultTz = TimeZone.getDefault();
            try {
                for (String tzName : new String[]{"America/Vancouver", "America/Chicago", "UTC"}) {

                    tzUtil.setDefaultTimeZone(tzName);
                    
                    String[] data = new String[]{
                        "yyyy-MM-dd kk:mm:ss.SSS", "2018-04-26 08:04:30.511", "h:mm aa", "8:04 AM",
                        "yyyy-MM-dd hh:mm aaa, z", "2018-04-26 08:04 PM, PDT", "h:mm aa", "8:04 PM",
                        "yyyy-MM-dd hh:mm aaa, z", "2018-04-26 08:04 PM, GMT-08:00", "h:mm aa", "5:04 PM",
                        "yyyy-MM-dd hh:mm aaa, z", "2018-04-26 08:04 PM, GMT", "h:mm aa", "5:04 PM",
                        "dd-MMM-yy HH:mm:ss z", "26-aug-20 18:02:09 gmt"//,
                        //"EEE, dd-MMM-yy HH:mm:ss z", "wed, 26-aug-20 18:02:09 gmt"

                    };
                    for (int i=0; i<data.length; i+=4) {
                        java.text.DateFormat messageDateFormat0 = new java.text.SimpleDateFormat(data[i], java.util.Locale.US);
                        SimpleDateFormat messageDateFormat = new SimpleDateFormat(data[i]);
                        messageDateFormat.getDateFormatSymbols().addZoneMapping("America/Vancouver", "Pacific Standard Time", "Pacific Daylight Time", "PST", "PDT");
                        messageDateFormat.getDateFormatSymbols().addZoneMapping("America/New_York", "Eastern Standard Time", "Eastern Daylight Time", "EST", "EDT");
                        messageDateFormat.getDateFormatSymbols().addZoneMapping("America/Chicago", "Central Standard Time", "Central Daylight Time", "CST", "CDT");
                        Date when0 = messageDateFormat0.parse(data[i+1]);
                        Date when = messageDateFormat.parse(data[i+1]);
                        assertEqual(when0, when, "In timezone "+tzName+", SimpleDateFormat parse deviated from java.text version.  Parsing "+data[i+1]+" with format "+data[i]);
                        //if (!when0.equals(when)) {
                        //    throw new RuntimeException("Test "+(i/4)+" FAILED.  Expected "+when0+" but found "+when);
                        //}
                        // What is date that is associated with when?
                        DateFormat displayTimeFormat = messageDateFormat;
                        java.text.DateFormat displayTimeFormat0 = new java.text.SimpleDateFormat(data[i], java.util.Locale.US);
                        String output0 = displayTimeFormat0.format(when);
                        String output = displayTimeFormat.format(when);
                        assertEqual(output0, output, "In timezone "+tzName+" SimpleDateFormat format deviated from java.text version.  Formatting "+data[i+1]+" with format "+data[i]);

                    }
                }
            } finally {
                tzUtil.setDefaultTimeZone(defaultTz.getID());
            }
        
        }
        /*
        if (true) {
            long[] timestamps = new long[]{
                1524755070511L, 1524798240000L, 1524801840000L, 1524773040000L
            };
            String[] data = new String[]{
                "yyyy-MM-dd kk:mm:ss.SSS", "2018-04-26 08:04:30.511 PDT", "h:mm aa", "8:04 AM",
                "yyyy-MM-dd hh:mm aaa, z", "2018-04-26 08:04 PM, PDT", "h:mm aa", "8:04 PM",
                "yyyy-MM-dd hh:mm aaa, z", "2018-04-26 08:04 PM, GMT-08:00", "h:mm aa", "5:04 PM",
                "yyyy-MM-dd hh:mm aaa, z", "2018-04-26 08:04 PM, GMT", "h:mm aa", "5:04 PM"

            };
            for (int i=0; i<data.length; i+=4) {
                //java.text.DateFormat messageDateFormat0 = new java.text.SimpleDateFormat(data[i]);
                SimpleDateFormat messageDateFormat = new SimpleDateFormat(data[i]);
                messageDateFormat.getDateFormatSymbols().addZoneMapping("America/Vancouver", "Pacific Standard Time", "Pacific Daylight Time", "PST", "PDT");
                messageDateFormat.getDateFormatSymbols().addZoneMapping("America/New_York", "Eastern Standard Time", "Eastern Daylight Time", "EST", "EDT");
                //Date when0 = messageDateFormat0.parse(data[i+1]);
                Date when = messageDateFormat.parse(data[i+1]);
                assertEqual(timestamps[i/4], when.getTime(), "SimpleDateFormat parse deviated from java.text version.  Parsing "+data[i+1]+" with format "+data[i]);
                
                // Formatting depends on the timezone, so we won't run these tests as they will turn out
                // differently depending on the timezone of the device.
                //if (!when0.equals(when)) {
                //    throw new RuntimeException("Test "+(i/4)+" FAILED.  Expected "+when0+" but found "+when);
                //}
                // What is date that is associated with when?
                //DateFormat displayTimeFormat = new SimpleDateFormat(data[i+2]);
                //java.text.DateFormat displayTimeFormat0 = new java.text.SimpleDateFormat(data[i+2]);
                //String output0 = displayTimeFormat0.format(when);
                //String output = displayTimeFormat.format(when);
                //assertEqual(output0, output, "SimpleDateFormat format deviated from java.text version.  Formatting "+data[i+1]+" with format "+data[i+2]);

            }
        }
                */
        return true;

    }

}
