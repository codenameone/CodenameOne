package com.codenameone.examples.fieldline.ai;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.fieldline.api.AbsenceDto;
import com.codenameone.examples.fieldline.api.AssistantReplyDto;
import com.codenameone.examples.fieldline.api.AssistantRequestDto;
import com.codenameone.examples.fieldline.api.ChatTurnDto;
import com.codenameone.examples.fieldline.api.MoveDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.domain.Job;
import com.codenameone.examples.fieldline.domain.Site;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.field.Catalog;
import com.codenameone.examples.fieldline.field.Horizon;
import com.codenameone.examples.fieldline.field.Planner;
import com.codenameone.examples.fieldline.repository.JobRepository;
import com.codenameone.examples.fieldline.repository.SiteRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The assistant in the app's chat.
///
/// For a technician it is a colleague who has read every manual: it answers
/// from the passages [Manuals] finds and from the technician's own jobs. For
/// a dispatcher it is also the scheduler's voice: "Dana is out tomorrow, cover
/// her jobs" becomes a call to [Planner#absence], and the reply carries the run
/// it made, so the app can show the same before-and-after the Optimize button
/// shows.
///
/// With a model configured this is a tool-use loop: Claude reads the request,
/// calls the tools below -- each one a method here over the same services the
/// API uses, with the caller's own rights -- and answers when it has what it
/// needs. Every tool that changes the plan is listed in the reply's `actions`,
/// so nothing changes silently. Without a model, a handful of phrasings are
/// recognized instead, enough to show each path of the feature.
@Component
public class Assistant {
    private static final int MAX_ROUNDS = 8;

    private final ClaudeClient claude;
    private final Manuals manuals;
    private final Planner planner;
    private final JobRepository jobs;
    private final SiteRepository sites;
    private final TechnicianRepository technicians;

    public Assistant(ClaudeClient claude, Manuals manuals, Planner planner, JobRepository jobs,
            SiteRepository sites, TechnicianRepository technicians) {
        this.claude = claude;
        this.manuals = manuals;
        this.planner = planner;
        this.jobs = jobs;
        this.sites = sites;
        this.technicians = technicians;
    }

    /// What one conversation turn did: the actions taken and the last run.
    static final class Turn {
        final List<String> actions = new ArrayList<String>();
        ScheduleRunDto run;
    }

    public AssistantReplyDto ask(UserDto user, AssistantRequestDto request) throws IOException {
        if (request == null || request.message() == null || request.message().trim().length() == 0) {
            throw new ResponseStatusException(400, "Ask something");
        }
        if (request.message().length() > 4000) {
            throw new ResponseStatusException(400, "That message is too long");
        }
        Turn turn = new Turn();
        if (claude.isLive()) {
            try {
                String reply = converse(user, request, turn);
                return new AssistantReplyDto(reply, turn.actions, turn.run, claude.model());
            } catch (IOException failed) {
                System.err.println("Assistant fell back to the rules: " + failed.getMessage());
                if (!turn.actions.isEmpty()) {
                    // Something already changed; say so rather than start again.
                    return new AssistantReplyDto("Done, but I lost the connection before I could "
                            + "summarize. Check the board.", turn.actions, turn.run, claude.model());
                }
            }
        }
        String reply = rules(user, request, turn);
        if (turn.run != null) {
            // The tool result lists every change for a model to summarize.
            // Without a model it was shown as is, a dozen bullets above the
            // run card that shows the same changes with their figures.
            reply = "Done. Here is what changed:";
        }
        return new AssistantReplyDto(reply, turn.actions, turn.run, "demo");
    }

    // ------------------------------------------------------------ the model

    private String system(UserDto user) {
        Horizon h = planner.horizon();
        StringBuilder s = new StringBuilder();
        s.append("You are Fieldline's assistant for a field-service company: HVAC, boilers, fire "
                + "protection, elevators, electrical, solar, controls and backflow work in "
                + "commercial buildings. Now is ").append(h.label(System.currentTimeMillis()))
                .append(" (").append(h.zone().getId()).append("). Day offsets count from today "
                + "(0) in that zone.\n");
        s.append("You are talking to ").append(user.displayName()).append(", ");
        if (user.dispatcher()) {
            s.append("a dispatcher. You can read and change the schedule with the tools. Before "
                    + "changing anything, be sure who and when is meant; if a name matches more "
                    + "than one technician, ask. After a change, report what moved, from the "
                    + "tool's result, in a few lines.\n");
        } else {
            s.append("a technician on site. Help them diagnose and fix equipment. Answer from "
                    + "the manual excerpts the search_manuals tool returns and cite their "
                    + "titles; say plainly when the manuals do not cover something. Safety "
                    + "first: lockout, gas, refrigerant, arc flash and life-safety systems.\n");
        }
        s.append("Be brief: this is read on a phone, often with gloves on. Plain text, short "
                + "lines, no tables.");
        return s.toString();
    }

    private List<Object> tools(UserDto user) {
        List<Object> out = new ArrayList<Object>();
        out.add(tool("search_manuals", "Searches the equipment manuals. Returns the best "
                + "passages with their titles.", props("query", str("What to look for: symptoms, "
                + "fault codes, part names"), "asset_type", str("Optional: equipment kind, e.g. "
                + "'Commercial boiler'")), "query"));
        out.add(tool("job_details", "Everything recorded about one job: site, equipment, "
                + "checklist, notes.", props("job_id", str("The job's id")), "job_id"));
        if (!user.dispatcher()) {
            out.add(tool("my_day", "The signed-in technician's jobs for a day, in driving order.",
                    props("day_offset", integer("0 for today")), "day_offset"));
            return out;
        }
        out.add(tool("list_jobs", "Jobs on one day, optionally only one status. Returns id, "
                + "title, technician, planned time, priority.", props("day_offset", integer(
                "0 for today"), "status", str("Optional: UNASSIGNED, AWAITING_PARTS, BLOCKED, "
                + "SCHEDULED, EN_ROUTE, ON_SITE, COMPLETED")), "day_offset"));
        out.add(tool("technician_schedule", "One technician's route for a day, with drive times.",
                props("technician", str("Name or id"), "day_offset", integer("0 for today")),
                "technician", "day_offset"));
        out.add(tool("find_substitutes", "Technicians qualified for a job and how busy each is "
                + "that day, nearest first.", props("job_id", str("The job's id")), "job_id"));
        out.add(tool("mark_absent", "Records that a technician is out and re-plans around it. "
                + "Changes the schedule.", props("technician", str("Name or id"), "day_offset",
                integer("First day out, 0 for today"), "days", integer("How many days, 1 or more"),
                "reason", str("Short reason")), "technician", "day_offset", "days", "reason"));
        out.add(tool("move_job", "Gives a job to a technician at a time, checking every rule "
                + "first. Changes the schedule when it holds.", props("job_id", str("The job's id"),
                "technician", str("Name or id"), "day_offset", integer("0 for today"), "time",
                str("Local start time HH:MM")), "job_id", "technician", "day_offset", "time"));
        out.add(tool("optimize", "Re-optimizes the whole plan. Changes the schedule.",
                new LinkedHashMap<String, Object>()));
        return out;
    }

    private String converse(UserDto user, AssistantRequestDto request, Turn turn)
            throws IOException {
        List<Object> messages = new ArrayList<Object>();
        if (request.history() != null) {
            int from = Math.max(0, request.history().size() - 12);
            for (int iter = from; iter < request.history().size(); iter++) {
                ChatTurnDto t = request.history().get(iter);
                if (t == null || t.text() == null || t.text().length() == 0) {
                    continue;
                }
                String role = "assistant".equals(t.role()) ? "assistant" : "user";
                // Two turns of one role in a row are merged: the API wants
                // them alternating, and the app's history may not be.
                if (!messages.isEmpty() && role.equals(((Map) messages.get(messages.size() - 1))
                        .get("role"))) {
                    Map last = (Map) messages.get(messages.size() - 1);
                    last.put("content", last.get("content") + "\n" + t.text());
                } else {
                    messages.add(Diagnostician.map("role", role, "content", t.text()));
                }
            }
        }
        String text = request.message().trim();
        if (request.jobId() != null && request.jobId().length() > 0) {
            text = "(About job " + request.jobId() + ")\n" + text;
        }
        if (!messages.isEmpty() && "user".equals(((Map) messages.get(messages.size() - 1)).get("role"))) {
            messages.remove(messages.size() - 1);
        }
        if (!messages.isEmpty() && !"user".equals(((Map) messages.get(0)).get("role"))) {
            messages.remove(0);
        }
        messages.add(Diagnostician.map("role", "user", "content", text));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("max_tokens", Integer.valueOf(16000));
        body.put("system", system(user));
        body.put("thinking", Diagnostician.map("type", "adaptive"));
        body.put("output_config", Diagnostician.map("effort", "medium"));
        body.put("tools", tools(user));
        body.put("messages", messages);
        for (int round = 0; round < MAX_ROUNDS; round++) {
            Map response = claude.message(body);
            String stop = ClaudeClient.stopReason(response);
            if ("refusal".equals(stop)) {
                return "I can't help with that one.";
            }
            if (!"tool_use".equals(stop)) {
                String reply = ClaudeClient.text(response).trim();
                return reply.length() == 0 ? "Done." : reply;
            }
            Object content = response.get("content");
            // The assistant's turn goes back exactly as it came, thinking
            // blocks included: the next request is checked against it.
            messages.add(Diagnostician.map("role", "assistant", "content", content));
            List<Object> results = new ArrayList<Object>();
            if (content instanceof List) {
                for (Object block : (List) content) {
                    if (!(block instanceof Map) || !"tool_use".equals(((Map) block).get("type"))) {
                        continue;
                    }
                    Map use = (Map) block;
                    Object input = use.get("input");
                    String output;
                    boolean error = false;
                    try {
                        output = call(user, String.valueOf(use.get("name")),
                                input instanceof Map ? (Map) input : new LinkedHashMap(), turn);
                    } catch (ResponseStatusException refused) {
                        output = refused.getMessage();
                        error = true;
                    }
                    Map<String, Object> result = Diagnostician.map("type", "tool_result",
                            "tool_use_id", use.get("id"), "content", output);
                    if (error) {
                        result.put("is_error", Boolean.TRUE);
                    }
                    results.add(result);
                }
            }
            messages.add(Diagnostician.map("role", "user", "content", results));
        }
        return "That took more steps than I allow myself. Here is what I did so far.";
    }

    // ------------------------------------------------------------ tools

    String call(UserDto user, String name, Map input, Turn turn) throws IOException {
        if ("search_manuals".equals(name)) {
            return searchManuals(Diagnostician.str(input, "query"), Diagnostician.str(input,
                    "asset_type"));
        }
        if ("job_details".equals(name)) {
            return jobDetails(user, Diagnostician.str(input, "job_id"));
        }
        if ("my_day".equals(name)) {
            return mySchedule(user.username(), (int) Diagnostician.num(input, "day_offset"));
        }
        if (!user.dispatcher()) {
            throw new ResponseStatusException(403, "Only a dispatcher can do that");
        }
        if ("list_jobs".equals(name)) {
            return listJobs((int) Diagnostician.num(input, "day_offset"), Diagnostician.str(input,
                    "status"));
        }
        if ("technician_schedule".equals(name)) {
            return schedule(technician(Diagnostician.str(input, "technician")),
                    (int) Diagnostician.num(input, "day_offset"));
        }
        if ("find_substitutes".equals(name)) {
            return substitutes(Diagnostician.str(input, "job_id"));
        }
        if ("mark_absent".equals(name)) {
            Technician tech = technician(Diagnostician.str(input, "technician"));
            Horizon h = planner.horizon();
            int from = (int) Math.max(0, Diagnostician.num(input, "day_offset"));
            int days = (int) Math.max(1, Math.min(14, Diagnostician.num(input, "days")));
            long start = Math.max(System.currentTimeMillis(), h.dayStart(from));
            ScheduleRunDto run = planner.absence(new AbsenceDto(tech.getId(), start,
                    h.dayStart(from + days), Diagnostician.str(input, "reason")), user.username());
            turn.run = run;
            turn.actions.add(run.summary());
            return describe(run);
        }
        if ("move_job".equals(name)) {
            Technician tech = technician(Diagnostician.str(input, "technician"));
            long start = at((int) Diagnostician.num(input, "day_offset"), Diagnostician.str(input,
                    "time"));
            Planner.Validation v = planner.move(new MoveDto(Diagnostician.str(input, "job_id"),
                    tech.getId(), start, true), user.username());
            if (!v.ok) {
                return "Refused: " + v.message;
            }
            turn.run = v.run;
            turn.actions.add(v.run == null ? v.message : v.run.summary());
            return v.message + (v.warnings.isEmpty() ? "" : "\n" + String.join("\n", v.warnings));
        }
        if ("optimize".equals(name)) {
            ScheduleRunDto run = planner.optimize(user.username());
            turn.run = run;
            turn.actions.add(run.summary());
            return describe(run);
        }
        throw new ResponseStatusException(400, "No tool called " + name);
    }

    static String describe(ScheduleRunDto run) {
        StringBuilder out = new StringBuilder(run.summary());
        if (run.before() != null && run.after() != null) {
            out.append("\nUnassigned ").append(run.before().unassigned()).append(" -> ")
                    .append(run.after().unassigned()).append("; driving ")
                    .append(run.before().driveMinutes()).append(" -> ")
                    .append(run.after().driveMinutes()).append(" min; overtime ")
                    .append(run.before().overtimeMinutes()).append(" -> ")
                    .append(run.after().overtimeMinutes()).append(" min");
        }
        int shown = 0;
        for (int iter = 0; run.changes() != null && iter < run.changes().size() && shown < 12; iter++) {
            out.append("\n- ").append(run.changes().get(iter).jobTitle()).append(": ")
                    .append(run.changes().get(iter).reason());
            shown++;
        }
        return out.toString();
    }

    String searchManuals(String query, String assetType) {
        List<Manuals.Hit> hits = manuals.search(query, assetType.length() == 0 ? null : assetType, 4);
        if (hits.isEmpty()) {
            return "No manual passage matches.";
        }
        StringBuilder out = new StringBuilder();
        for (Manuals.Hit h : hits) {
            out.append("[").append(h.chunk.getTitle()).append("]\n").append(h.chunk.getBody())
                    .append("\n\n");
        }
        return out.toString().trim();
    }

    @Transactional(readOnly = true)
    String jobDetails(UserDto user, String jobId) {
        Job job = jobs.findById(jobId);
        if (job == null) {
            throw new ResponseStatusException(404, "No job " + jobId);
        }
        if (!user.dispatcher() && !job.getTechId().equals(user.techId())) {
            throw new ResponseStatusException(403, "That job is not yours");
        }
        Site site = sites.findById(job.getSiteId());
        Horizon h = planner.horizon();
        return job.getId() + ": " + job.getTitle() + " [" + job.getKind() + ", " + job.getStatus()
                + ", priority " + job.getPriority() + "]\n"
                + (site == null ? "" : site.getName() + ", " + site.getAddress() + "; "
                + site.getAssetType() + " " + site.getAssetModel() + "\n")
                + "Reported: " + job.getDescription() + "\n"
                + (job.getPlannedStart() > 0 ? "Planned " + h.label(job.getPlannedStart()) + "\n" : "")
                + (job.getNotes().length() > 0 ? "Notes: " + job.getNotes() : "");
    }

    @Transactional(readOnly = true)
    String listJobs(int dayOffset, String status) {
        Horizon h = planner.horizon();
        long from = h.dayStart(dayOffset);
        long to = h.dayStart(dayOffset + 1);
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (Job job : jobs.inSpan(from, to)) {
            if (status.length() > 0 && !status.equals(job.getStatus())) {
                continue;
            }
            Technician t = job.getTechId().length() == 0 ? null : technicians.findById(job.getTechId());
            out.append(job.getId()).append(" | ").append(job.getTitle()).append(" | ")
                    .append(t == null ? "unassigned" : t.getName()).append(" | ")
                    .append(job.getPlannedStart() > 0 ? h.label(job.getPlannedStart()) : "-")
                    .append(" | P").append(job.getPriority()).append(" | ").append(job.getStatus())
                    .append('\n');
            n++;
        }
        return n == 0 ? "No jobs." : out.toString();
    }

    @Transactional(readOnly = true)
    String mySchedule(String username, int dayOffset) {
        Technician me = technicians.findByUsername(username);
        if (me == null) {
            throw new ResponseStatusException(403, "You are not a technician");
        }
        return schedule(me, dayOffset);
    }

    @Transactional(readOnly = true)
    String schedule(Technician tech, int dayOffset) {
        Horizon h = planner.horizon();
        List<Job> day = jobs.plannedFor(tech.getId(), h.dayStart(dayOffset), h.dayStart(dayOffset + 1));
        if (day.isEmpty()) {
            return tech.getName() + " has nothing planned that day.";
        }
        StringBuilder out = new StringBuilder(tech.getName()).append(":\n");
        for (Job job : day) {
            out.append(h.time(job.getPlannedStart())).append("-").append(h.time(job.getPlannedEnd()))
                    .append(" ").append(job.getId()).append(" ").append(job.getTitle())
                    .append(" (").append(job.getTravelBefore()).append(" min drive)\n");
        }
        return out.toString();
    }

    @Transactional(readOnly = true)
    String substitutes(String jobId) {
        Job job = jobs.findById(jobId);
        if (job == null) {
            throw new ResponseStatusException(404, "No job " + jobId);
        }
        Site site = sites.findById(job.getSiteId());
        long need = Catalog.mask(job.getSkills());
        Horizon h = planner.horizon();
        long dayStart = job.getPlannedStart() > 0 ? job.getPlannedStart() : job.getWindowStart();
        Horizon day = Horizon.of(dayStart, 1, h.zone());
        List<Object[]> rows = new ArrayList<Object[]>();
        for (Technician t : technicians.active()) {
            if ((Catalog.mask(t.getSkills()) & need) != need || t.getId().equals(job.getTechId())) {
                continue;
            }
            int booked = 0;
            for (Job j : jobs.plannedFor(t.getId(), day.origin(), day.end())) {
                booked += j.getDurationMinutes() + j.getTravelBefore();
            }
            double km = site == null ? 0 : distanceKm(t.getLat(), t.getLng(), site.getLat(), site.getLng());
            rows.add(new Object[] {t, Double.valueOf(km), Integer.valueOf(booked)});
        }
        for (int i = 1; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            int j = i - 1;
            while (j >= 0 && ((Double) rows.get(j)[1]).doubleValue() > ((Double) r[1]).doubleValue()) {
                rows.set(j + 1, rows.get(j));
                j--;
            }
            rows.set(j + 1, r);
        }
        if (rows.isEmpty()) {
            return "Nobody else holds " + Catalog.names(need) + ".";
        }
        StringBuilder out = new StringBuilder();
        for (Object[] r : rows) {
            Technician t = (Technician) r[0];
            out.append(t.getName()).append(" (").append(t.getId()).append("): ")
                    .append(Math.round(((Double) r[1]).doubleValue() * 10) / 10.0).append(" km away, ")
                    .append(r[2]).append(" min booked that day\n");
        }
        return out.toString();
    }

    static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1) * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        return 6371.0 * Math.sqrt(dLat * dLat + dLng * dLng);
    }

    /// A technician by id, or by a name or part of one. Ambiguity is refused
    /// with the candidates, which the model then asks the dispatcher about.
    @Transactional(readOnly = true)
    Technician technician(String who) {
        Technician byId = technicians.findById(who);
        if (byId != null) {
            return byId;
        }
        String q = Words.fold(who).trim();
        List<Technician> found = new ArrayList<Technician>();
        for (Technician t : technicians.active()) {
            String name = Words.fold(t.getName());
            if (q.length() > 0 && (name.equals(q) || Words.has(name, q) || name.startsWith(q))) {
                found.add(t);
            }
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new ResponseStatusException(404, "No technician called " + who);
        }
        StringBuilder names = new StringBuilder();
        for (Technician t : found) {
            names.append(names.length() == 0 ? "" : ", ").append(t.getName());
        }
        throw new ResponseStatusException(409, who + " could be " + names);
    }

    long at(int dayOffset, String hhmm) {
        Horizon h = planner.horizon();
        int colon = hhmm == null ? -1 : hhmm.indexOf(':');
        if (colon < 1) {
            throw new ResponseStatusException(400, "A time is HH:MM");
        }
        try {
            int hours = Integer.parseInt(hhmm.substring(0, colon).trim());
            int minutes = Integer.parseInt(hhmm.substring(colon + 1).trim());
            return h.millis(h.at(Math.max(0, dayOffset), hours * 60 + minutes));
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "A time is HH:MM");
        }
    }

    // ------------------------------------------------------------ the rules

    /// The assistant without a model: a few phrasings, each reaching the same
    /// tools the model would call.
    String rules(UserDto user, AssistantRequestDto request, Turn turn) throws IOException {
        String q = Words.fold(request.message());
        if (user.dispatcher()) {
            if (Words.any(q, "optimize", "optimise", "reoptimize", "replan", "rebalance")) {
                return call(user, "optimize", new LinkedHashMap(), turn);
            }
            if (Words.any(q, "out", "off", "sick", "absent", "away")) {
                Technician who = mentioned(q);
                if (who != null) {
                    int day = Words.has(q, "tomorrow") ? 1 : weekday(q);
                    Map<String, Object> in = Diagnostician.map("technician", who.getId(),
                            "day_offset", Integer.valueOf(day), "days", Integer.valueOf(1),
                            "reason", Words.has(q, "sick") ? "Sick" : "Out");
                    return call(user, "mark_absent", in, turn);
                }
                return "Who is out? Name the technician, for example \"Dana is out tomorrow\".";
            }
            if (Words.any(q, "unassigned", "uncovered", "open")) {
                String list = listJobs(0, Catalog.UNASSIGNED);
                return "No jobs.".equals(list) ? "Every job today is covered." : "Unassigned today:\n" + list;
            }
            if (Words.any(q, "cover", "substitute", "replace", "instead")) {
                Job job = mentionedJob(request.message());
                if (job != null) {
                    return "Qualified for " + job.getTitle() + ":\n" + substitutes(job.getId());
                }
            }
            Technician who = mentioned(q);
            if (who != null) {
                return schedule(who, Words.has(q, "tomorrow") ? 1 : 0);
            }
        }
        if (request.jobId() != null && request.jobId().length() > 0 || Words.any(q, "how", "why",
                "fault", "error", "code", "noise", "leak", "reset", "replace", "pressure", "test")) {
            // From a job, "what could cause this?" means this job: its title
            // and reported problem are part of the question. Asked alone,
            // the one word worth matching was "cause", and the manual that
            // said it first -- a solar inverter's -- answered an HVAC call.
            List<Manuals.Hit> hits = manuals.search(request.message() + " " + aboutJob(request.jobId()),
                    assetOf(request.jobId()), 2);
            if (!hits.isEmpty()) {
                Manuals.Hit best = hits.get(0);
                return "From \"" + best.chunk.getTitle() + "\":\n" + best.chunk.getBody()
                        + (hits.size() > 1 ? "\n\nSee also \"" + hits.get(1).chunk.getTitle() + "\"." : "");
            }
        }
        if (user.dispatcher()) {
            return "I can re-plan for you. Try:\n- \"Dana is out tomorrow\"\n- \"Optimize the "
                    + "schedule\"\n- \"What is unassigned?\"\n- \"Who can cover <job>?\"\n"
                    + "With a Claude key configured I understand anything you'd say to a "
                    + "colleague.";
        }
        return "Ask me about the equipment in front of you: a fault code, a symptom, a test "
                + "procedure. I'll answer from the manuals.";
    }

    /// The kind of equipment a job is on, or null.
    @Transactional(readOnly = true)
    /// The words a job adds to a question asked from it: its title and the
    /// problem reported, or nothing outside a job.
    String aboutJob(String jobId) {
        Job job = jobId == null || jobId.length() == 0 ? null : jobs.findById(jobId);
        if (job == null) {
            return "";
        }
        return job.getTitle() + " " + (job.getDescription() == null ? "" : job.getDescription());
    }

    String assetOf(String jobId) {
        Job job = jobId == null || jobId.length() == 0 ? null : jobs.findById(jobId);
        Site site = job == null ? null : sites.findById(job.getSiteId());
        return site == null ? null : site.getAssetType();
    }

    @Transactional(readOnly = true)
    Technician mentioned(String folded) {
        List<String> words = Words.words(folded);
        for (Technician t : technicians.active()) {
            List<String> name = Words.words(Words.fold(t.getName()));
            if (!name.isEmpty() && words.contains(name.get(0))) {
                return t;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    Job mentionedJob(String message) {
        String folded = Words.fold(message);
        Horizon h = planner.horizon();
        for (Job job : jobs.inSpan(h.origin(), h.end())) {
            if (Words.has(folded, Words.fold(job.getId()).trim())) {
                return job;
            }
        }
        for (Job job : jobs.inSpan(h.origin(), h.end())) {
            List<String> title = Words.words(Words.fold(job.getTitle()));
            int hit = 0;
            for (String w : title) {
                if (w.length() > 3 && Words.has(folded, w)) {
                    hit++;
                }
            }
            if (hit >= 2) {
                return job;
            }
        }
        return null;
    }

    private int weekday(String folded) {
        List<String> names = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday",
                "saturday", "sunday");
        Horizon h = planner.horizon();
        for (int d = 0; d < 7; d++) {
            if (Words.has(folded, names.get(h.weekday(d) - 1))) {
                return d;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------ schemas

    private static Map<String, Object> tool(String name, String description,
            Map<String, Object> props, String... required) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", Arrays.asList(required));
        schema.put("additionalProperties", Boolean.FALSE);
        return Diagnostician.map("name", name, "description", description, "input_schema", schema);
    }

    private static Map<String, Object> props(Object... pairs) {
        return Diagnostician.map(pairs);
    }

    private static Map<String, Object> str(String description) {
        return Diagnostician.map("type", "string", "description", description);
    }

    private static Map<String, Object> integer(String description) {
        return Diagnostician.map("type", "integer", "description", description);
    }
}
