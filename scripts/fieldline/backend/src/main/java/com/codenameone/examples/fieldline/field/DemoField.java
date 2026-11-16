/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.fieldline.field;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.fieldline.ai.Manuals;
import com.codenameone.examples.fieldline.domain.Job;
import com.codenameone.examples.fieldline.domain.Site;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.repository.JobRepository;
import com.codenameone.examples.fieldline.repository.SiteRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;

import java.io.IOException;
import java.util.Random;

/// The demonstration company: a twelve-person crew based in Portland, the
/// customers' sites around the city, and a working week of jobs, planned by
/// the scheduler once everything is in. Seeded with a fixed random seed, so
/// every checkout starts from the same board.
///
/// The equipment manuals are seeded on every profile, production included:
/// they are what the assistant's answers cite, and a deployment replaces or
/// extends them with its own.
@Component
public class DemoField {
    /// name, skills, home latitude, home longitude, home label, first hour, last hour
    static final Object[][] CREW = {
        {"Dana Okafor", "HVAC,ELECTRICAL,CONTROLS", 45.5122, -122.6587, "Hosford-Abernethy", 7, 16},
        {"Sam Whitaker", "FIRE,ELECTRICAL", 45.5590, -122.6450, "Concordia", 8, 17},
        {"Priya Raman", "ELEVATOR,ELECTRICAL", 45.4810, -122.6260, "Woodstock", 8, 17},
        {"Luis Ortega", "PLUMBING,GAS", 45.5400, -122.7010, "Northwest", 7, 15},
        {"Jordan Kim", "HVAC,GAS", 45.4870, -122.8040, "Beaverton", 8, 17},
        {"Aisha Bello", "SOLAR,ELECTRICAL", 45.5230, -122.5560, "Montavilla", 9, 18},
        {"Tom Lindqvist", "FIRE,PLUMBING", 45.4210, -122.6650, "Milwaukie", 7, 16},
        {"Mei Chen", "CONTROLS,HVAC", 45.5450, -122.8900, "Hillsboro", 8, 17},
        {"Rafael Duarte", "ELECTRICAL,SOLAR", 45.6000, -122.6800, "St. Johns", 8, 17},
        {"Nora Fitzgerald", "ELEVATOR,CONTROLS", 45.5050, -122.4300, "Gresham", 8, 17},
        {"Kwame Mensah", "GAS,HVAC,PLUMBING", 45.4450, -122.7800, "Tigard", 7, 16},
        {"Elena Petrova", "FIRE,ELEVATOR", 45.6310, -122.6710, "Vancouver", 8, 17},
    };

    /// name, customer, address, latitude, longitude, asset row, model, phone
    static final Object[][] SITES = {
        {"Pearl Tower", "Riverside Properties", "1120 NW Couch St", 45.5240, -122.6830, 0, "Carrier 48HC", "+15035550101"},
        {"Lloyd Center Annex", "Lloyd Holdings", "950 NE Multnomah St", 45.5310, -122.6550, 4, "Otis Gen2", "+15035550102"},
        {"Eastbank Clinic", "Cascade Health", "2400 SE Hawthorne Blvd", 45.5120, -122.6420, 2, "Notifier NFS2-3030", "+15035550103"},
        {"Willamette Labs", "Willamette Labs", "3100 SW Macadam Ave", 45.4980, -122.6720, 5, "Square D QED-2", "+15035550104"},
        {"Alberta Commons", "Alberta Co-op", "1500 NE Alberta St", 45.5590, -122.6490, 1, "Lochinvar Knight XL", "+15035550105"},
        {"Sellwood School", "Portland Schools", "8300 SE 15th Ave", 45.4650, -122.6500, 3, "Viking VK102", "+15035550106"},
        {"Beaverton Data Hall", "Northwest Compute", "14500 SW Jenkins Rd", 45.4920, -122.8200, 5, "Eaton Magnum DS", "+15035550107"},
        {"Hillsboro Fab 3", "Tualatin Semis", "2200 NE Brookwood Pkwy", 45.5400, -122.9100, 7, "Siemens Desigo", "+15035550108"},
        {"Gresham Station", "Gresham Retail", "700 NW Civic Dr", 45.5050, -122.4380, 0, "Trane Voyager", "+15035550109"},
        {"St. Johns Library", "Multnomah Libraries", "7510 N Charleston Ave", 45.5900, -122.7520, 8, "Watts 909", "+15035550110"},
        {"Tigard Medical", "Cascade Health", "9600 SW Hall Blvd", 45.4310, -122.7720, 2, "Simplex 4100ES", "+15035550111"},
        {"Clackamas Plaza", "Sunnyside Retail", "12000 SE 82nd Ave", 45.4330, -122.5750, 6, "SMA Sunny Tripower", "+15035550112"},
        {"Vancouver Waterfront", "Columbia Partners", "650 W Columbia Way", 45.6200, -122.6720, 4, "KONE MonoSpace", "+13605550113"},
        {"Kenton Brewery", "Kenton Brewing", "8100 N Denver Ave", 45.5810, -122.6860, 1, "Cleaver-Brooks CBEX", "+15035550114"},
        {"Montavilla Offices", "Glisan Group", "7800 SE Stark St", 45.5190, -122.5850, 7, "Johnson Metasys", "+15035550115"},
        {"Milwaukie Depot", "Metro Transit", "2400 SE Main St", 45.4440, -122.6380, 3, "Tyco TY-FRB", "+15035550116"},
        {"Lake Oswego Center", "Oswego Realty", "300 B Ave", 45.4200, -122.6700, 0, "Lennox Energence", "+15035550117"},
        {"Swan Island Works", "Swan Logistics", "5500 N Channel Ave", 45.5650, -122.7110, 5, "ABB SafeGear", "+15035550118"},
        {"Hollywood Theater", "Hollywood Arts", "4122 NE Sandy Blvd", 45.5350, -122.6200, 2, "Edwards EST4", "+15035550119"},
        {"Orenco Apartments", "Orenco Living", "1300 NE Orenco Station", 45.5320, -122.9040, 6, "Enphase IQ8", "+15035550120"},
        {"Foster Market", "Foster Foods", "6000 SE Foster Rd", 45.4890, -122.5960, 8, "Febco 860", "+15035550121"},
        {"South Waterfront Tower", "Riverside Properties", "3550 SW Bond Ave", 45.4970, -122.6700, 4, "Schindler 5500", "+15035550122"},
        {"Airport Way Hangar", "PDX Aviation", "7000 NE Airport Way", 45.5880, -122.5930, 1, "Weil-McLain 88", "+15035550123"},
        {"Tualatin Commons", "Tualatin City", "18880 SW Martinazzi Ave", 45.3830, -122.7630, 7, "Honeywell WEBs", "+15035550124"},
    };

    /// What customers call in, per asset row: title, description, kind, minutes.
    static final String[][][] CALLS = {
        {{"No cooling on 4th floor", "Tenants report warm air from the vents since this morning.", "REPAIR", "120"},
            {"Quarterly HVAC service", "Filters, belts and coil cleaning.", "MAINTENANCE", "90"}},
        {{"Boiler lockout", "Boiler locked out overnight; building is cold.", "REPAIR", "150"},
            {"Annual boiler inspection", "Combustion analysis and safety controls.", "INSPECTION", "120"}},
        {{"Panel in trouble", "Panel beeping with a ground fault trouble.", "REPAIR", "90"},
            {"Annual fire alarm test", "Full device test with monitoring company.", "INSPECTION", "180"}},
        {{"Sprinkler leak at riser", "Water pooling under the riser room valve.", "REPAIR", "120"},
            {"Quarterly sprinkler inspection", "Valves, gauges and inspector's test.", "INSPECTION", "90"}},
        {{"Elevator stuck between floors", "Car stopped between 2 and 3, no passengers.", "REPAIR", "120"},
            {"Monthly elevator maintenance", "Doors, leveling and machine room.", "MAINTENANCE", "90"}},
        {{"Breaker tripping", "Main feeder breaker tripped twice this week.", "REPAIR", "120"},
            {"Thermal scan of switchgear", "Annual infrared survey.", "INSPECTION", "90"}},
        {{"Inverter fault E-31", "Inverter offline since the weekend, shows a fault code.", "REPAIR", "90"},
            {"Solar array checkup", "Strings, inverter and production review.", "MAINTENANCE", "60"}},
        {{"Controls offline", "Building automation lost contact with three controllers.", "REPAIR", "90"},
            {"Controls schedule tune-up", "Align schedules with the new occupancy.", "MAINTENANCE", "60"}},
        {{"Backflow annual test", "Utility-required annual test.", "INSPECTION", "60"},
            {"Backflow relief valve dripping", "Relief port discharging continuously.", "REPAIR", "60"}},
    };

    private final Jobs jobs;
    private final Crew crew;
    private final Planner planner;
    private final Manuals manuals;
    private final TechnicianRepository technicians;
    private final SiteRepository sites;
    private final JobRepository jobRows;
    private final Timekeeping clock;

    public DemoField(Jobs jobs, Crew crew, Planner planner, Manuals manuals,
            TechnicianRepository technicians, SiteRepository sites, JobRepository jobRows,
            Timekeeping clock) {
        this.clock = clock;
        this.jobRows = jobRows;
        this.jobs = jobs;
        this.crew = crew;
        this.planner = planner;
        this.manuals = manuals;
        this.technicians = technicians;
        this.sites = sites;
    }

    /// The crew, the sites and the week, then the first plan. `signIns` are
    /// the accounts the first crew members are, in [#CREW] order.
    public void seed(String... signIns) throws IOException {
        if (sites.count() > 0) {
            return;
        }
        build(signIns);
        planner.optimize("system");
    }

    @Transactional(rollbackFor = IOException.class)
    void build(String[] signIns) throws IOException {
        Random random = new Random(20261114L);
        String[] ids = new String[CREW.length];
        String[] pay = clock.seedRates();
        for (int iter = 0; iter < CREW.length; iter++) {
            Object[] row = CREW[iter];
            Technician t = new Technician();
            ids[iter] = "t" + (101 + iter);
            t.setId(ids[iter]);
            t.setUsername(iter < signIns.length ? signIns[iter] : "");
            t.setName((String) row[0]);
            t.setInitials(Crew.initials((String) row[0]));
            t.setColor(Crew.PALETTE[iter % Crew.PALETTE.length]);
            t.setSkills((String) row[1]);
            t.setHomeLat(((Double) row[2]).doubleValue());
            t.setHomeLng(((Double) row[3]).doubleValue());
            t.setHomeLabel((String) row[4]);
            t.setLat(t.getHomeLat());
            t.setLng(t.getHomeLng());
            t.setMaxDriveMinutes(iter % 3 == 0 ? 150 : 200);
            t.setRegularMinutesPerWeek(40 * 60);
            t.setMaxMinutesPerWeek(iter % 4 == 0 ? 45 * 60 : 50 * 60);
            t.setActive(true);
            t.setCertifications(certificates((String) row[1], iter));
            // Home quarter plus the core: a soft preference the planner
            // breaks only when nobody local can take the job.
            String home = Catalog.territoryOf(t.getHomeLat(), t.getHomeLng());
            t.setTerritories("CENTRAL".equals(home) ? home : "CENTRAL," + home);
            t.setMaxJobsPerDay(iter % 4 == 1 ? 6 : 8);
            t.setMaxMinutesPerDay(iter % 3 == 0 ? 9 * 60 : 10 * 60);
            // Apprentice, technician or senior: what the timesheets cost at.
            t.setRateId(pay[iter % 5 == 3 ? 0 : iter % 4 == 0 ? 2 : 1]);
            technicians.insert(t);
            int first = ((Integer) row[5]).intValue();
            int last = ((Integer) row[6]).intValue();
            clock.seedHistory(t, first, last, iter);
            for (int day = 1; day <= 5; day++) {
                crew.addWindow(t.getId(), day, first * 60, last * 60);
            }
            if (iter == 4) {
                crew.addWindow(t.getId(), 6, 9 * 60, 13 * 60);
            }
        }
        Site[] made = new Site[SITES.length];
        for (int iter = 0; iter < SITES.length; iter++) {
            Object[] row = SITES[iter];
            Site s = new Site();
            s.setId("s" + (201 + iter));
            s.setName((String) row[0]);
            s.setCustomer((String) row[1]);
            s.setAddress((String) row[2] + ", Portland");
            s.setLat(((Double) row[3]).doubleValue());
            s.setLng(((Double) row[4]).doubleValue());
            s.setAssetType(Catalog.ASSETS[((Integer) row[5]).intValue()][0]);
            s.setAssetModel((String) row[6]);
            s.setContactPhone((String) row[7]);
            sites.insert(s);
            made[iter] = s;
        }
        // Two weeks, so the staffing forecast has a second week to show; the
        // board plans the first.
        Horizon h = planner.horizon(2 * Planner.HORIZON_DAYS);
        // Working days only: a crew that keeps office hours has nobody to send
        // on a Sunday, and a job left there would only show as unplaced.
        int[] workdays = new int[h.days()];
        int n = 0;
        for (int day = 0; day < h.days(); day++) {
            if (h.weekday(day) <= 5) {
                workdays[n++] = day;
            }
        }
        long now = System.currentTimeMillis();
        int count = 0;
        for (int w = 0; w < n; w++) {
            int day = workdays[w];
            // Demand is uneven on purpose: midweek runs past what the crew
            // can do and Friday leaves people idle, which is what the
            // staffing report is for.
            int calls = DEMAND[Math.min(DEMAND.length, h.weekday(day)) - 1];
            if (day >= Planner.HORIZON_DAYS) {
                // Next week is still filling up: about half of it is booked.
                calls = calls / 2;
            }
            for (int iter = 0; iter < calls; iter++) {
                Site site = made[random.nextInt(made.length)];
                int asset = assetRow(site.getAssetType());
                String[] call = CALLS[asset][random.nextInt(2)];
                boolean urgent = "REPAIR".equals(call[2]) && random.nextInt(3) == 0;
                int shape = urgent ? 0 : random.nextInt(4);
                int startMinute = shape == 2 ? 12 * 60 : 8 * 60;
                int endMinute = shape == 1 ? 12 * 60 : 17 * 60;
                int endDay = shape == 3 ? workdays[Math.min(n - 1, w + 2)] : day;
                long from = h.millis(h.at(day, startMinute));
                long to = h.millis(h.at(endDay, endMinute));
                int minutes = Integer.parseInt(call[3]);
                if (to - Math.max(from, now) < minutes * 60000L) {
                    // Today, and the window has already closed: a call that
                    // came in too late is tomorrow's.
                    continue;
                }
                count++;
                jobs.newJob("j" + (1000 + count), call[0], call[2], urgent ? 5 : 1 + random.nextInt(3),
                        site, call[1], minutes, Catalog.ASSETS[asset][1], from, to);
            }
        }
        linkAndExclude(ids, count);
        int off = n > 1 ? workdays[1] : 0;
        int asked = n > 3 ? workdays[3] : off;
        crew.addTimeOff(ids[6], h.dayStart(off), h.dayStart(off + 1), "Medical appointment",
                Catalog.APPROVED);
        crew.addTimeOff(ids[3], h.dayStart(asked), h.dayStart(asked + 1), "Family event",
                Catalog.PENDING);
    }

    /// Calls a day, Monday to Friday.
    static final int[] DEMAND = {40, 46, 54, 44, 28};

    /// The certificates a trade needs, valid for a year; one technician's gas
    /// certificate lapses in three days, so the planner stops sending them to
    /// boilers then and the crew screen says so.
    static String certificates(String skills, int iter) {
        String year = java.time.LocalDate.now().plusYears(1).toString();
        String soon = java.time.LocalDate.now().plusDays(3).toString();
        StringBuilder out = new StringBuilder();
        String[][] map = {{"GAS", "GAS_SAFE"}, {"ELECTRICAL", "HIGH_VOLTAGE"},
            {"ELEVATOR", "WORK_AT_HEIGHT"}, {"SOLAR", "WORK_AT_HEIGHT"},
            {"FIRE", "FIRE_INSPECTOR"}, {"HVAC", "REFRIGERANT"}};
        for (String[] m : map) {
            if (("," + skills + ",").indexOf("," + m[0] + ",") >= 0
                    && out.indexOf(m[1] + "=") < 0) {
                if (out.length() > 0) {
                    out.append(',');
                }
                out.append(m[1]).append('=').append(iter == 3 && "GAS_SAFE".equals(m[1]) ? soon : year);
            }
        }
        return out.toString();
    }

    /// A survey that has to come before an install, and a customer who asked
    /// not to have one technician on site again.
    private void linkAndExclude(String[] ids, int count) throws IOException {
        if (count < 12) {
            return;
        }
        Job survey = jobRows.findById("j1003");
        Job install = jobRows.findById("j1009");
        if (survey != null && install != null) {
            survey.setTitle("Site survey: " + survey.getTitle());
            install.setAfterJobId(survey.getId());
            jobRows.save(survey);
            jobRows.save(install);
        }
        Job picky = jobRows.findById("j1005");
        if (picky != null) {
            picky.setExcludedTechs(ids[0]);
            jobRows.save(picky);
        }
    }

    private static int assetRow(String assetType) {
        for (int iter = 0; iter < Catalog.ASSETS.length; iter++) {
            if (Catalog.ASSETS[iter][0].equals(assetType)) {
                return iter;
            }
        }
        return 0;
    }

    /// asset row, title, body
    static final Object[][] MANUAL = {
        {0, "Rooftop unit: no cooling", "Check thermostat call for cooling (Y1 to C, 24 VAC). If the compressor contactor is pulled in but the compressor is silent, check the run capacitor (replace if more than 6% below rating) and the compressor overload. A frozen evaporator coil means low airflow or low charge: replace filters, confirm blower RPM, then check superheat. Fault code 3 flashes on the control board: high-pressure lockout, usually a dirty condenser coil or failed condenser fan motor."},
        {0, "Rooftop unit: resetting a tripped unit", "Before resetting a tripped rooftop unit, read the fault code on the control board: resetting clears it. To reset, set the thermostat to off, open the unit disconnect for five minutes, close it and call for cooling again. A high-pressure lockout that trips again within an hour is not a reset problem: clean the condenser coil and check the condenser fan motor. Never jumper a pressure switch to keep a unit running."},
        {0, "Rooftop unit: belt and blower service", "Belt deflection should be 1/64 in. per inch of span. Glazed or cracked belts are replaced in matched sets. Align sheaves with a straightedge; misalignment shows as wear on one belt edge. Lubricate blower bearings only where fittings are present."},
        {0, "Rooftop unit: refrigerant pressures", "R-410A at 95 F outdoor: suction 125-135 psig, discharge 340-380 psig. Superheat target on a fixed orifice comes from the chart on the access panel; on a TXV system subcooling should be 8-12 F. Oil spots on fittings indicate a leak; confirm with an electronic detector before adding charge."},
        {1, "Boiler lockout and ignition faults", "Lockout after three trials for ignition: inspect the flame sensor (clean with fine emery cloth, minimum 0.8 microamp signal), check the igniter for cracks, and confirm gas pressure at the valve (inlet 7 in. WC natural gas). Error E-07 is flame loss during run, often from a dirty sensor or flue recirculation. Never bypass the low-water cutoff."},
        {1, "Boiler combustion analysis", "Target CO2 8.5-9.5% on natural gas and CO under 100 ppm air-free. Rising CO with falling CO2 points to a dirty burner or blocked air intake. Record stack temperature; a rise of more than 50 F from the last service suggests scaling on the heat exchanger."},
        {2, "Fire alarm panel: ground fault trouble", "A ground fault trouble means a field wire is touching earth. Disconnect circuits one at a time at the panel until the trouble clears, then meter the circuit to ground (should read open). Common causes are water in an outdoor device box or a pinched cable at a junction box. Notify the monitoring company before disabling any circuit."},
        {2, "Fire alarm panel: battery load test", "Standby batteries are replaced every 4 years or when terminal voltage drops below 85% under load. Test with the AC disconnected for at least 5 minutes. A panel showing 'BATTERY LOW' with good batteries usually has a failed charger fuse."},
        {3, "Sprinkler riser leaks and valves", "Drips at the main drain usually mean the drain valve is not fully seated; cycle it fully open and closed. Leaks at a grooved coupling require draining the system and replacing the gasket. Every control valve must be open and supervised; a valve found closed is an impairment and the fire department and owner must be told."},
        {4, "Elevator: car stopped between floors", "Confirm no passengers are trapped first. A car stopped between floors with the door zone lost usually means a door lock circuit opened during travel; inspect hoistway door interlocks for misalignment. Fault 0405 on Otis Gen2 is a brake monitoring fault. Never move the car on inspection with a hoistway door open."},
        {4, "Elevator: leveling and door protection", "Leveling accuracy must be within 1/4 in. under load. Re-leveling continuously suggests worn rope or a failing encoder. The door reopening device must reverse the doors without contact; test the full height of the light curtain."},
        {5, "Switchgear: nuisance tripping and thermal scans", "A breaker tripping repeatedly under normal load: check for a loose or overheated lug (a 10 C rise over a matching phase is a deficiency, 40 C is critical), measure phase currents, and look for harmonic heating on the neutral. Thermal scans must be done under at least 40% load. Arc-flash PPE per the label is required to open energized gear."},
        {6, "Solar inverter fault codes", "E-31 is a ground fault / insulation resistance fault: measure insulation resistance of each DC string to ground (minimum 1 megohm). Wet connectors are the usual cause in spring. E-12 is grid overvoltage, often a utility issue; record the voltage. Strings that differ by more than 5% in voltage point to a failed module or bypass diode."},
        {7, "Building controls: controllers offline", "Controllers dropping off the BACnet MS/TP trunk: check end-of-line termination at both ends only, verify shield grounded at one point, and look for duplicate MAC addresses after a replacement. Baud rate must match on every device. Offline controllers keep their last schedule, so a space may be heated or cooled after hours."},
        {8, "Backflow preventer testing", "Reduced pressure assembly: check valve 1 must hold at least 5.0 psid, relief valve must open at no less than 2.0 psid, check valve 2 must hold 1.0 psid. A continuously discharging relief valve means check valve 1 is fouled; flush or rebuild with the manufacturer's kit. Tests are reported to the water utility within 10 days."},
    };

    /// Adds the manual passages that are not there yet.
    public void manuals() throws IOException {
        if (manuals.count() >= MANUAL.length) {
            return;
        }
        for (int iter = 0; iter < MANUAL.length; iter++) {
            Object[] row = MANUAL[iter];
            manuals.add("m" + (301 + iter), Catalog.ASSETS[((Integer) row[0]).intValue()][0],
                    (String) row[1], (String) row[2]);
        }
    }
}
