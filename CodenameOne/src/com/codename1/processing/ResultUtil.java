/**
 * 
 */
package com.codename1.processing;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Some utility methods only intended to be used by the Result class.
 * 
 * @author ecoolman
 * 
 */
class ResultUtil {
	public static Vector optJSONArray(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return null;
		}
		return (Vector) v;
	}

	public static Hashtable optJSONObject(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return null;
		}
		return (Hashtable) v;
	}

	public static boolean optBoolean(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return false;
		}
		if (((String) v).equalsIgnoreCase("true")) {
			return true;
		} else if (v.equals("1")) {
			return true;
		}
		return false;
	}

	public static int optInt(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return 0;
		}
		return Integer.parseInt((String) v);
	}

	public static long optLong(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return 0L;
		}
		return Long.parseLong((String) v);
	}

	public static double optDouble(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return 0L;
		}
		return Double.parseDouble((String) v);
	}

	public static String optString(Hashtable h, String k) {
		Object v = h.get(k);
		if (v == null) {
			return null;
		}
		return v.toString();
	}

	public static String prettyPrint(Vector obj) {
		try {
			return PrettyPrinter.print(obj);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public static String prettyPrint(Hashtable obj) {
		try {
			return PrettyPrinter.print(obj);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
