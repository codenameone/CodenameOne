package com.codenameone.examples.fieldline.field;

import java.util.ArrayList;
import java.util.List;

/// The fixed vocabulary of the field: the skills a technician can hold, the
/// kinds of job, the states a job goes through, and the checklist each kind of
/// equipment is inspected with.
///
/// Skills travel as names everywhere but the scheduler, which wants a bit mask:
/// "can this technician do this job" is then one AND, which matters in a search
/// that asks it millions of times.
public final class Catalog {
    public static final String[] SKILLS = {"ELECTRICAL", "HVAC", "PLUMBING", "FIRE", "ELEVATOR",
        "GAS", "SOLAR", "CONTROLS"};

    /// Certifications a job can require. Unlike a skill each one lapses on a
    /// date, and the planner will not send anyone whose certificate expires
    /// before the shift ends.
    public static final String[] CERTIFICATIONS = {"GAS_SAFE", "HIGH_VOLTAGE", "WORK_AT_HEIGHT",
        "CONFINED_SPACE", "REFRIGERANT", "FIRE_INSPECTOR"};

    /// Service territories. A site's territory follows from where it is
    /// relative to the depot ([#territoryOf]), so no one maintains a map.
    public static final String[] TERRITORIES = {"CENTRAL", "NORTH", "EAST", "SOUTH", "WEST"};

    public static final String REPAIR = "REPAIR";
    public static final String INSPECTION = "INSPECTION";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String INSTALL = "INSTALL";
    public static final String[] KINDS = {REPAIR, INSPECTION, MAINTENANCE, INSTALL};

    public static final String UNASSIGNED = "UNASSIGNED";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String EN_ROUTE = "EN_ROUTE";
    public static final String ON_SITE = "ON_SITE";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";
    public static final String NA = "NA";

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String DECLINED = "DECLINED";

    /// The equipment the demo sites carry, each with the skill its work needs
    /// and the lines it is inspected against.
    static final String[][] ASSETS = {
        {"Rooftop HVAC unit", "HVAC",
            "Filters clean and seated|Condenser coil free of debris|Refrigerant pressures in range"
            + "|Belt tension and wear|Condensate drain clear|Supply air temperature split 15-22 F"},
        {"Commercial boiler", "GAS",
            "Flue and venting intact|Burner flame stable and blue|Low-water cutoff trips"
            + "|Relief valve tested|No combustion leaks (CO reading)|Pressure and temperature in range"},
        {"Fire alarm panel", "FIRE",
            "Panel shows normal, no troubles|Batteries load-tested|Pull stations activate"
            + "|Smoke detectors respond to test gas|Notification devices audible and visible"
            + "|Monitoring company received signal"},
        {"Sprinkler system", "FIRE",
            "Control valves open and supervised|Gauges within normal pressure|Inspector's test flow"
            + "|No corrosion or leaks on visible pipe|Spare heads and wrench present"},
        {"Passenger elevator", "ELEVATOR",
            "Door reopening device works|Leveling within 1/4 in.|Emergency phone connects"
            + "|Machine room clean, no oil leaks|Brake and governor inspected"},
        {"Electrical switchgear", "ELECTRICAL",
            "Thermal scan, no hot spots|Breakers exercised|Enclosure clean and dry"
            + "|Torque on lugs checked|Arc-flash labels current"},
        {"Solar inverter", "SOLAR",
            "Inverter online, no fault codes|DC string voltages balanced|Ventilation clear"
            + "|AC disconnect operates|Production matches irradiance"},
        {"Building controls", "CONTROLS",
            "Controller online|Schedules match occupancy|Sensors read within tolerance"
            + "|Alarms routed to on-call|Firmware current"},
        {"Backflow preventer", "PLUMBING",
            "Test cocks operable|Check valve 1 holds|Check valve 2 holds|Relief valve opens"
            + "|Test report filed with water utility"},
    };

    private static final String[] GENERAL = {"Site safety briefing and PPE",
        "Before photos taken"};

    private Catalog() {
    }

    /// The skill names as the scheduler's mask; unknown names are ignored.
    public static long mask(String names) {
        long out = 0L;
        for (String name : split(names)) {
            for (int iter = 0; iter < SKILLS.length; iter++) {
                if (SKILLS[iter].equalsIgnoreCase(name)) {
                    out |= 1L << iter;
                }
            }
        }
        return out;
    }

    /// The certification work on an asset type needs, or "" for none.
    public static String certificationFor(String assetType) {
        if (assetType == null) {
            return "";
        }
        if (assetType.indexOf("boiler") >= 0) {
            return "GAS_SAFE";
        }
        if (assetType.indexOf("switchgear") >= 0) {
            return "HIGH_VOLTAGE";
        }
        if (assetType.indexOf("elevator") >= 0 || assetType.indexOf("Solar") >= 0) {
            return "WORK_AT_HEIGHT";
        }
        if (assetType.indexOf("Fire alarm") >= 0) {
            return "FIRE_INSPECTOR";
        }
        if (assetType.indexOf("HVAC") >= 0) {
            return "REFRIGERANT";
        }
        return "";
    }

    /// The items of `wanted` that are in `table`, in the order given.
    public static List<String> known(String[] table, List<String> wanted) {
        List<String> out = new ArrayList<String>();
        for (String w : wanted) {
            for (String t : table) {
                if (t.equals(w) && !out.contains(w)) {
                    out.add(w);
                }
            }
        }
        return out;
    }

    /// A code as words: `GAS_SAFE` reads "Gas safe".
    public static String label(String code) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < code.length(); iter++) {
            char c = code.charAt(iter);
            if (c == '_') {
                out.append(' ');
            } else if (iter == 0) {
                out.append(c);
            } else {
                out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
        }
        return out.toString();
    }

    /// The certification names in `names` as a mask over [#CERTIFICATIONS].
    public static long certMask(String names) {
        return maskOf(CERTIFICATIONS, names);
    }

    /// The territory names in `names` as a mask over [#TERRITORIES].
    public static long territoryMask(String names) {
        return maskOf(TERRITORIES, names);
    }

    private static long maskOf(String[] table, String names) {
        long out = 0L;
        for (String name : split(names)) {
            int at = name.indexOf('=');
            String code = at < 0 ? name : name.substring(0, at);
            for (int iter = 0; iter < table.length; iter++) {
                if (table[iter].equals(code)) {
                    out |= 1L << iter;
                }
            }
        }
        return out;
    }

    /// The territory a site lies in: CENTRAL within 4 km of the depot, else
    /// the compass quarter it falls in.
    public static String territoryOf(double lat, double lng) {
        double dy = (lat - Crew.DEPOT_LAT) * 111.0;
        double dx = (lng - Crew.DEPOT_LNG) * 111.0 * Math.cos(Math.toRadians(Crew.DEPOT_LAT));
        if (dx * dx + dy * dy < 16.0) {
            return "CENTRAL";
        }
        if (Math.abs(dy) >= Math.abs(dx)) {
            return dy > 0 ? "NORTH" : "SOUTH";
        }
        return dx > 0 ? "EAST" : "WEST";
    }

    /// `CODE=yyyy-mm-dd` items as `{code, date}` pairs, malformed ones dropped.
    public static List<String[]> certifications(String csv) {
        List<String[]> out = new ArrayList<String[]>();
        for (String item : split(csv)) {
            int at = item.indexOf('=');
            String code = at < 0 ? item : item.substring(0, at);
            String date = at < 0 ? "" : item.substring(at + 1);
            for (String known : CERTIFICATIONS) {
                if (known.equals(code)) {
                    out.add(new String[] {code, date});
                }
            }
        }
        return out;
    }

    /// The skill names in `mask`, in catalog order.
    public static List<String> names(long mask) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < SKILLS.length; iter++) {
            if ((mask & (1L << iter)) != 0) {
                out.add(SKILLS[iter]);
            }
        }
        return out;
    }

    /// A comma-separated list as its items, trimmed, empties dropped.
    public static List<String> split(String csv) {
        List<String> out = new ArrayList<String>();
        if (csv == null) {
            return out;
        }
        int at = 0;
        while (at <= csv.length()) {
            int end = csv.indexOf(',', at);
            if (end < 0) {
                end = csv.length();
            }
            String item = csv.substring(at, end).trim();
            if (item.length() > 0) {
                out.add(item);
            }
            at = end + 1;
        }
        return out;
    }

    /// The skills a list names, as the canonical comma-separated text.
    public static String join(List<String> skills) {
        StringBuilder out = new StringBuilder();
        if (skills != null) {
            for (String skill : names(mask(String.join(",", skills)))) {
                if (out.length() > 0) {
                    out.append(',');
                }
                out.append(skill);
            }
        }
        return out.toString();
    }

    /// The skill work on an asset needs, ELECTRICAL for one not in the catalog.
    public static String skillFor(String assetType) {
        for (String[] asset : ASSETS) {
            if (asset[0].equals(assetType)) {
                return asset[1];
            }
        }
        return "ELECTRICAL";
    }

    /// The checklist a job starts with: the general lines, then the asset's.
    /// A repair carries only the general lines and a "fault fixed and tested"
    /// one; an inspection or maintenance visit the asset's full list.
    public static List<String> checklist(String kind, String assetType) {
        List<String> out = new ArrayList<String>();
        for (String line : GENERAL) {
            out.add(line);
        }
        if (REPAIR.equals(kind) || INSTALL.equals(kind)) {
            out.add(REPAIR.equals(kind) ? "Fault found and repaired" : "Installed to manufacturer spec");
            out.add("System tested in operation");
            out.add("Customer walked through the work");
            return out;
        }
        for (String[] asset : ASSETS) {
            if (asset[0].equals(assetType)) {
                int at = 0;
                String lines = asset[2];
                while (at < lines.length()) {
                    int end = lines.indexOf('|', at);
                    if (end < 0) {
                        end = lines.length();
                    }
                    out.add(lines.substring(at, end));
                    at = end + 1;
                }
            }
        }
        return out;
    }

    public static boolean isKind(String kind) {
        for (String k : KINDS) {
            if (k.equals(kind)) {
                return true;
            }
        }
        return false;
    }

    /// Whether a job in this state is still the scheduler's to plan.
    public static boolean isOpen(String status) {
        return !COMPLETED.equals(status) && !CANCELLED.equals(status);
    }

    /// Whether a job in this state has started: its technician and start are
    /// facts now, not a plan.
    public static boolean isUnderWay(String status) {
        return EN_ROUTE.equals(status) || ON_SITE.equals(status);
    }
}
