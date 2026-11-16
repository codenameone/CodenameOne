package com.codenameone.examples.fieldline.ai;

import com.codenameone.examples.fieldline.api.CheckDto;
import com.codenameone.examples.fieldline.api.DiagnosisDto;

import java.util.ArrayList;
import java.util.List;

/// Everything the assistant is told about a job: the equipment, what the
/// customer reported, what the technician has recorded so far. Built by the
/// job service, so the assistant reads no table of its own for it.
public final class JobContext {
    public String jobId = "";
    public String title = "";
    public String kind = "";
    public String site = "";
    public String customer = "";
    public String assetType = "";
    public String assetModel = "";
    public String description = "";
    public String notes = "";
    public String technician = "";
    public List<CheckDto> checklist = new ArrayList<CheckDto>();
    public List<DiagnosisDto> diagnoses = new ArrayList<DiagnosisDto>();
    public List<String> photoNotes = new ArrayList<String>();

    /// The context as the plain text the model reads.
    public String describe() {
        StringBuilder out = new StringBuilder();
        out.append("Job ").append(jobId).append(": ").append(title).append(" (").append(kind)
                .append(")\n");
        out.append("Site: ").append(site).append(", customer ").append(customer).append('\n');
        out.append("Equipment: ").append(assetType);
        if (assetModel.length() > 0) {
            out.append(", model ").append(assetModel);
        }
        out.append('\n');
        if (description.length() > 0) {
            out.append("Reported: ").append(description).append('\n');
        }
        if (!checklist.isEmpty()) {
            out.append("Checklist so far:\n");
            for (CheckDto c : checklist) {
                String r = c.result() == null || c.result().length() == 0 ? "not yet checked" : c.result();
                out.append("- ").append(c.label()).append(": ").append(r);
                if (c.note() != null && c.note().length() > 0) {
                    out.append(" (").append(c.note()).append(')');
                }
                out.append('\n');
            }
        }
        for (DiagnosisDto d : diagnoses) {
            out.append("Earlier photo finding: ").append(d.issue()).append(", ").append(d.severity())
                    .append('\n');
        }
        if (notes.length() > 0) {
            out.append("Technician's notes: ").append(notes).append('\n');
        }
        return out.toString();
    }

    /// The checklist lines that failed.
    public List<CheckDto> failures() {
        List<CheckDto> out = new ArrayList<CheckDto>();
        for (CheckDto c : checklist) {
            if ("FAIL".equals(c.result())) {
                out.add(c);
            }
        }
        return out;
    }
}
