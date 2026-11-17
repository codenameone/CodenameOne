package com.codenameone.examples.fieldline.ai;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.fieldline.api.CheckDto;
import com.codenameone.examples.fieldline.api.DiagnosisDto;
import com.codenameone.examples.fieldline.api.ReportDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Looks at what a technician photographed and says what is wrong, and drafts
/// the report a finished job is signed against.
///
/// With a model configured, the photo goes to Claude together with the job --
/// the equipment and its model, what the customer reported, the checklist so
/// far -- and the manual passages that the job's words find. The answer is
/// constrained to a JSON schema, so it parses every time and the app can lay
/// it out as a card rather than show a paragraph. Without a model, or when a
/// call fails, a rule per kind of equipment answers instead, and the `source`
/// of the result says which one spoke: a technician must be able to tell an
/// opinion from a lookup table.
///
/// Neither result is a verdict. The app shows the diagnosis as a suggestion the
/// technician confirms, and a safety note is never left out when there is one.
@Component
public class Diagnostician {
    static final String DEMO = "demo";

    private static final String SYSTEM = "You are a senior field-service engineer helping a "
            + "technician on site. You look at one photo of commercial building equipment "
            + "(HVAC, boilers, fire protection, elevators, electrical, solar, controls, "
            + "plumbing) and say what is visibly wrong. Be specific to what the photo shows; "
            + "if it does not show a fault, say so and give LOW severity. Use the job "
            + "context and the manual excerpts, and cite a manual by title in "
            + "recommendedAction when you rely on it. Parts are what a technician would "
            + "order, with part numbers only when a manual excerpt gives them. confidence "
            + "is 0-100 and honest: a blurry or ambiguous photo is under 50. safetyNote "
            + "names any hazard to the technician or occupants (gas, refrigerant, arc "
            + "flash, fall, life-safety impairment) or is empty. Never tell anyone to "
            + "bypass a safety device.";

    private final ClaudeClient claude;
    private final Manuals manuals;

    public Diagnostician(ClaudeClient claude, Manuals manuals) {
        this.claude = claude;
        this.manuals = manuals;
    }

    // ------------------------------------------------------------ photos

    public DiagnosisDto diagnose(JobContext job, String base64, String mime, String note) {
        if (claude.isLive()) {
            try {
                return ask(job, base64, mime, note);
            } catch (IOException failed) {
                System.err.println("Diagnosis fell back to the rules: " + failed.getMessage());
                return rule(job, note, "demo (model unavailable)");
            }
        }
        return rule(job, note, DEMO);
    }

    private DiagnosisDto ask(JobContext job, String base64, String mime, String note)
            throws IOException {
        StringBuilder text = new StringBuilder(job.describe());
        if (note != null && note.trim().length() > 0) {
            text.append("The technician says about this photo: ").append(note.trim()).append('\n');
        }
        text.append(excerpts(job.assetType + " " + job.description + " " + (note == null ? "" : note),
                job.assetType));
        List<Object> content = new ArrayList<Object>();
        content.add(map("type", "image", "source", map("type", "base64", "media_type",
                "image/png".equals(mime) ? "image/png" : "image/jpeg", "data", base64)));
        content.add(map("type", "text", "text", text.toString()));
        Map<String, Object> request = request(SYSTEM, content, diagnosisSchema(), "high");
        Map response = claude.message(request);
        if ("refusal".equals(ClaudeClient.stopReason(response))) {
            return rule(job, note, "demo (model declined)");
        }
        Map out = Json.parseObject(ClaudeClient.text(response));
        return new DiagnosisDto(str(out, "issue"), str(out, "severity"), str(out, "likelyCause"),
                str(out, "recommendedAction"), list(out, "parts"),
                (int) Math.max(0, Math.min(100, num(out, "confidence"))), str(out, "safetyNote"),
                claude.model());
    }

    static Map<String, Object> diagnosisSchema() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("issue", map("type", "string", "description", "What is wrong, in one line"));
        props.put("severity", map("type", "string", "enum", Arrays.asList("LOW", "MEDIUM", "HIGH",
                "CRITICAL")));
        props.put("likelyCause", map("type", "string"));
        props.put("recommendedAction", map("type", "string"));
        props.put("parts", map("type", "array", "items", map("type", "string")));
        props.put("confidence", map("type", "integer"));
        props.put("safetyNote", map("type", "string"));
        return object(props);
    }

    // ------------------------------------------------------------ reports

    /// The report of a job as recorded: a summary, the findings, and what
    /// should happen next. The customer signs this text, hashed.
    public ReportDto report(JobContext job) {
        if (claude.isLive()) {
            try {
                return draft(job);
            } catch (IOException failed) {
                System.err.println("Report fell back to the template: " + failed.getMessage());
            }
        }
        return template(job);
    }

    private ReportDto draft(JobContext job) throws IOException {
        String system = "You write the service report a customer signs at the end of a visit. "
                + "Plain, factual, past tense, no marketing. Use only what the record says; "
                + "never invent a reading or a repair. findings are one line each, failures "
                + "first. followUps are actions the customer or the next visit must take, "
                + "empty when there are none.";
        List<Object> content = new ArrayList<Object>();
        content.add(map("type", "text", "text", job.describe()));
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("summary", map("type", "string"));
        props.put("findings", map("type", "array", "items", map("type", "string")));
        props.put("followUps", map("type", "array", "items", map("type", "string")));
        Map response = claude.message(request(system, content, object(props), "medium"));
        if ("refusal".equals(ClaudeClient.stopReason(response))) {
            return template(job);
        }
        Map out = Json.parseObject(ClaudeClient.text(response));
        return new ReportDto(job.jobId, str(out, "summary"), list(out, "findings"),
                list(out, "followUps"), claude.model());
    }

    static ReportDto template(JobContext job) {
        List<String> findings = new ArrayList<String>();
        List<String> follow = new ArrayList<String>();
        int passed = 0;
        int answered = 0;
        for (CheckDto c : job.checklist) {
            if (c.result() == null || c.result().length() == 0) {
                continue;
            }
            answered++;
            if ("PASS".equals(c.result())) {
                passed++;
            } else if ("FAIL".equals(c.result())) {
                findings.add(0, "Failed: " + c.label() + (c.note() == null || c.note().length() == 0
                        ? "" : " - " + c.note()));
                follow.add("Correct: " + c.label());
            }
        }
        for (DiagnosisDto d : job.diagnoses) {
            findings.add("Observed: " + d.issue() + " (" + d.severity() + ")");
            if (!"LOW".equals(d.severity())) {
                follow.add(d.recommendedAction());
            }
        }
        if (findings.isEmpty()) {
            findings.add("All checked items passed");
        }
        String summary = sentenceCase(job.kind) + " of the " + job.assetType + (job.assetModel.length() > 0 ? " (" + job.assetModel
                + ")" : "") + " at " + job.site + " by " + job.technician + ". " + passed + " of "
                + answered + " checks passed." + (job.notes.length() > 0 ? " " + job.notes : "");
        return new ReportDto(job.jobId, summary, findings, follow, DEMO);
    }

    // ------------------------------------------------------------ rules

    /// What the demo says about a photo: the most common visible fault of each
    /// kind of equipment, sharpened by a word of the technician's note.
    static DiagnosisDto rule(JobContext job, String note, String source) {
        String n = Words.fold(note);
        String asset = job.assetType;
        List<CheckDto> fails = job.failures();
        String failing = fails.isEmpty() ? "" : " The failed check \"" + fails.get(0).label()
                + "\" agrees.";
        if (Words.any(n, "leak", "leaking", "drip", "water", "puddle")) {
            return d("Active leak at the visible joint", "HIGH", "Failed gasket or loosened "
                    + "fitting; corrosion staining suggests it has been weeping for weeks." + failing,
                    "Isolate, depressurize and replace the gasket; check torque on adjacent "
                    + "fittings before restoring.", parts("Gasket kit for " + model(job),
                    "Thread sealant"), 71, "Treat as slip hazard; isolate electrical below the leak.",
                    source);
        }
        if (Words.any(n, "burn", "burnt", "scorch", "smoke", "melted", "hot")) {
            return d("Heat damage on a conductor termination", "CRITICAL", "Loose lug or "
                    + "undersized conductor running hot." + failing, "De-energize under lockout, "
                    + "replace the damaged lug and conductor end, torque to spec, re-scan under load.",
                    parts("Mechanical lug, matching rating", "Conductor end and heat shrink"), 78,
                    "Arc-flash risk: work de-energized only, in rated PPE.", source);
        }
        if (Words.any(n, "rust", "corrosion", "corroded")) {
            return d("Surface corrosion", "MEDIUM", "Moisture exposure; coating has failed."
                    + failing, "Wire-brush, treat and recoat; check for wall loss with a gauge "
                    + "before deciding to replace.", parts("Rust converter", "Enamel coating"), 66, "",
                    source);
        }
        if (asset.indexOf("HVAC") >= 0) {
            return d("Condenser coil fouled with debris", "MEDIUM", "Cottonwood and dust "
                    + "loading reduces airflow, raising head pressure." + failing, "Power off, "
                    + "clean coil from inside out with low-pressure water; verify head pressure "
                    + "and supply-air split after restart.", parts("Coil cleaner (non-acid)",
                    "Replacement filters MERV 13"), 64, "Lock out the disconnect before opening the "
                    + "unit; fan can start remotely.", source);
        }
        if (asset.indexOf("boiler") >= 0) {
            return d("Yellow, lifting burner flame", "HIGH", "Incomplete combustion: "
                    + "dirty burner ports or a blocked air intake." + failing, "Shut down, clean "
                    + "burner and intake, re-run combustion analysis; CO must be under 100 ppm "
                    + "air-free before return to service.", parts("Burner gasket", "Flame sensor"), 69,
                    "Possible carbon monoxide: ventilate and monitor CO while working.", source);
        }
        if (asset.indexOf("Fire") >= 0 || asset.indexOf("Sprinkler") >= 0) {
            return d("Trouble condition on the panel", "HIGH", "A supervisory or battery "
                    + "trouble; batteries over four years old commonly fail load test." + failing,
                    "Load-test and replace batteries, clear the trouble, confirm the monitoring "
                    + "station received restore.", parts("12V 7Ah SLA batteries (pair)"), 62,
                    "Life-safety impairment: notify the monitoring company and building manager "
                    + "before any work; start a fire watch if the system is out of service.", source);
        }
        if (asset.indexOf("elevator") >= 0) {
            return d("Oil residue under the machine", "MEDIUM", "Seal weeping on the "
                    + "gearbox or hydraulic line." + failing, "Clean, mark and re-inspect at the "
                    + "next visit; replace the seal if the residue returns.", parts("Gearbox seal"),
                    58, "Keep the car out of service while working in the machine room.", source);
        }
        if (asset.indexOf("Solar") >= 0) {
            return d("Inverter fault, strings offline", "MEDIUM", "Ground fault or an "
                    + "isolation error after rain." + failing, "Read the fault code, test string "
                    + "insulation resistance, reset once the fault clears.", parts("MC4 connector pair"),
                    60, "DC strings stay live in daylight; treat every conductor as energized.", source);
        }
        return d("No visible fault in this photo", "LOW", "The equipment looks in normal "
                + "condition." + failing, "Continue the checklist; photograph anything that fails.",
                new ArrayList<String>(), 55, "", source);
    }

    /// "INSPECTION" as "Inspection", folded by hand: `toLowerCase()` follows
    /// the locale, and the device class library has no overload taking one.
    static String sentenceCase(String word) {
        String folded = Words.fold(word);
        return folded.length() == 0 ? "" : word.substring(0, 1) + folded.substring(1);
    }

    private static String model(JobContext job) {
        return job.assetModel.length() > 0 ? job.assetModel : job.assetType;
    }

    private static DiagnosisDto d(String issue, String severity, String cause, String action,
            List<String> parts, int confidence, String safety, String source) {
        return new DiagnosisDto(issue, severity, cause, action, parts, confidence, safety, source);
    }

    private static List<String> parts(String... names) {
        return new ArrayList<String>(Arrays.asList(names));
    }

    // ------------------------------------------------------------ plumbing

    String excerpts(String query, String assetType) {
        List<Manuals.Hit> hits = manuals.search(query, assetType, 3);
        if (hits.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("Manual excerpts:\n");
        for (Manuals.Hit h : hits) {
            out.append("[").append(h.chunk.getTitle()).append("] ").append(h.chunk.getBody())
                    .append('\n');
        }
        return out.toString();
    }

    static Map<String, Object> request(String system, List<Object> content,
            Map<String, Object> schema, String effort) {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("max_tokens", Integer.valueOf(16000));
        request.put("system", system);
        request.put("thinking", map("type", "adaptive"));
        request.put("output_config", map("effort", effort, "format", map("type", "json_schema",
                "schema", schema)));
        List<Object> messages = new ArrayList<Object>();
        messages.add(map("role", "user", "content", content));
        request.put("messages", messages);
        return request;
    }

    static Map<String, Object> object(Map<String, Object> props) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", new ArrayList<String>(props.keySet()));
        schema.put("additionalProperties", Boolean.FALSE);
        return schema;
    }

    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (int iter = 0; iter + 1 < pairs.length; iter += 2) {
            out.put(String.valueOf(pairs[iter]), pairs[iter + 1]);
        }
        return out;
    }

    static String str(Map m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    static long num(Map m, String key) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    static List<String> list(Map m, String key) {
        List<String> out = new ArrayList<String>();
        Object v = m.get(key);
        if (v instanceof List) {
            for (Object o : (List) v) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }
}
