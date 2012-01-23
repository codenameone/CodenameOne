/* Created by JReleaseInfo AntTask from Open Source Competence Group */
/* Creation date Mon Jun 30 22:12:33 CEST 2008 */
package com.l2fprod.common;

import java.util.Date;

/**
 * This class provides information gathered from the build environment.
 * 
 * @author JReleaseInfo AntTask
 */
public class Version {


   /** buildDate (set during build process to 1214856753382L). */
   private static Date buildDate = new Date(1214856753382L);

   /**
    * Get buildDate (set during build process to Mon Jun 30 22:12:33 CEST 2008).
    * @return Date buildDate
    */
   public static final Date getBuildDate() { return buildDate; }


   /** buildTimestamp (set during build process to "06/30/2008 10:12 PM"). */
   private static String buildTimestamp = new String("06/30/2008 10:12 PM");

   /**
    * Get buildTimestamp (set during build process to "06/30/2008 10:12 PM").
    * @return String buildTimestamp
    */
   public static final String getBuildTimestamp() { return buildTimestamp; }


   /** year (set during build process to "2005-2007"). */
   private static String year = new String("2005-2007");

   /**
    * Get year (set during build process to "2005-2007").
    * @return String year
    */
   public static final String getYear() { return year; }


   /** version (set during build process to "7.3"). */
   private static String version = new String("7.3");

   /**
    * Get version (set during build process to "7.3").
    * @return String version
    */
   public static final String getVersion() { return version; }


   /** project (set during build process to "l2fprod-common"). */
   private static String project = new String("l2fprod-common");

   /**
    * Get project (set during build process to "l2fprod-common").
    * @return String project
    */
   public static final String getProject() { return project; }

}
