package com.codename1.tools.cn1ss;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CommentRenderer {
    static final String MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->";

    private CommentRenderer() {
    }

    static RenderResult build(List<Map<String, Object>> results, String platformLabel) {
        List<String> summary = new ArrayList<>();
        List<Map<String, Object>> commentEntries = new ArrayList<>();

        for (Map<String, Object> result : results) {
            String test = stringValue(result.get("test"), "unknown");
            String status = stringValue(result.get("status"), "unknown");
            String actualPath = stringValue(result.get("actual_path"), "");
            String expectedPath = stringValue(result.get("expected_path"), "");
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) result.get("details");
            String message = "";
            String copyFlag = "0";

            @SuppressWarnings("unchecked")
            Map<String, Object> preview = (Map<String, Object>) result.get("preview");
            String previewName = preview != null ? stringValue(preview.get("name"), null) : null;
            String previewPath = preview != null ? stringValue(preview.get("path"), null) : null;
            String previewMime = preview != null ? stringValue(preview.get("mime"), null) : null;
            Object previewQualityObj = preview != null ? preview.get("quality") : null;
            Integer previewQuality = previewQualityObj instanceof Number ? ((Number) previewQualityObj).intValue() : null;
            String previewNote = preview != null ? stringValue(preview.get("note"), null) : null;

            String base64 = stringValue(result.get("base64"), null);
            String base64Omitted = stringValue(result.get("base64_omitted"), null);
            Integer base64Length = toInteger(result.get("base64_length"));
            String base64Mime = stringValue(result.get("base64_mime"), null);
            String base64Codec = stringValue(result.get("base64_codec"), null);
            Integer base64Quality = toInteger(result.get("base64_quality"));
            String base64Note = stringValue(result.get("base64_note"), null);

            switch (status) {
                case "equal":
                    message = "Matches stored reference.";
                    break;
                case "missing_expected":
                    message = "Reference screenshot missing at " + expectedPath + ".";
                    copyFlag = "1";
                    commentEntries.add(buildEntry(
                            test,
                            "missing reference",
                            message,
                            previewName,
                            previewPath,
                            previewMime,
                            previewQuality,
                            previewNote,
                            base64,
                            base64Omitted,
                            base64Length,
                            base64Mime,
                            base64Codec,
                            base64Quality,
                            base64Note
                    ));
                    break;
                case "different":
                    String dims = "";
                    if (details != null && details.containsKey("width") && details.containsKey("height")) {
                        dims = String.format(
                                " (%sx%s px, bit depth %s)",
                                details.get("width"),
                                details.get("height"),
                                details.get("bit_depth")
                        );
                    }
                    message = "Screenshot differs" + dims + ".";
                    copyFlag = "1";
                    commentEntries.add(buildEntry(
                            test,
                            "updated screenshot",
                            message,
                            previewName,
                            previewPath,
                            previewMime,
                            previewQuality,
                            previewNote,
                            base64,
                            base64Omitted,
                            base64Length,
                            base64Mime,
                            base64Codec,
                            base64Quality,
                            base64Note
                    ));
                    break;
                case "error":
                    message = "Comparison error: " + stringValue(result.get("message"), "unknown error");
                    copyFlag = "1";
                    commentEntries.add(buildEntry(
                            test,
                            "comparison error",
                            message,
                            previewName,
                            previewPath,
                            previewMime,
                            previewQuality,
                            previewNote,
                            null,
                            base64Omitted,
                            base64Length,
                            base64Mime,
                            base64Codec,
                            base64Quality,
                            base64Note
                    ));
                    break;
                case "missing_actual":
                    message = "Actual screenshot missing (test did not produce output).";
                    copyFlag = "1";
                    commentEntries.add(buildEntry(
                            test,
                            "missing actual screenshot",
                            message,
                            previewName,
                            previewPath,
                            previewMime,
                            previewQuality,
                            previewNote,
                            null,
                            base64Omitted,
                            base64Length,
                            base64Mime,
                            base64Codec,
                            base64Quality,
                            base64Note
                    ));
                    break;
                default:
                    message = "Status: " + status + ".";
                    break;
            }

            String noteColumn = previewNote != null ? previewNote : (base64Note != null ? base64Note : "");
            summary.add(String.join("|", new String[] {status, test, message, copyFlag, actualPath, noteColumn}));
        }

        String commentBody = buildComment(commentEntries, platformLabel);
        return new RenderResult(summary, commentBody);
    }

    private static Map<String, Object> buildEntry(
            String test,
            String status,
            String message,
            String previewName,
            String previewPath,
            String previewMime,
            Integer previewQuality,
            String previewNote,
            String base64,
            String base64Omitted,
            Integer base64Length,
            String base64Mime,
            String base64Codec,
            Integer base64Quality,
            String base64Note
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("test", test);
        entry.put("status", status);
        entry.put("message", message);
        entry.put("artifact_name", previewName != null ? previewName : test + ".png");
        entry.put("preview_name", previewName);
        entry.put("preview_path", previewPath);
        entry.put("preview_mime", previewMime);
        entry.put("preview_quality", previewQuality);
        entry.put("preview_note", previewNote);
        entry.put("base64", base64);
        entry.put("base64_omitted", base64Omitted);
        entry.put("base64_length", base64Length);
        entry.put("base64_mime", base64Mime);
        entry.put("base64_codec", base64Codec);
        entry.put("base64_quality", base64Quality);
        entry.put("base64_note", base64Note);
        return entry;
    }

    private static String buildComment(List<Map<String, Object>> entries, String platformLabel) {
        if (entries.isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        lines.add("### " + platformLabel + " screenshot updates");
        lines.add("");
        for (Map<String, Object> entry : entries) {
            StringBuilder header = new StringBuilder();
            header.append("- **").append(stringValue(entry.get("test"), "unknown"))
                    .append("** — ")
                    .append(stringValue(entry.get("status"), "update"))
                    .append(". ")
                    .append(stringValue(entry.get("message"), ""));
            lines.add(header.toString());

            String previewName = stringValue(entry.get("preview_name"), null);
            String previewMime = stringValue(entry.get("preview_mime"), null);
            Integer previewQuality = toInteger(entry.get("preview_quality"));
            String previewNote = stringValue(entry.get("preview_note"), null);
            String base64 = stringValue(entry.get("base64"), null);
            String base64Omitted = stringValue(entry.get("base64_omitted"), null);
            Integer base64Length = toInteger(entry.get("base64_length"));
            String base64Codec = stringValue(entry.get("base64_codec"), null);
            Integer base64Quality = toInteger(entry.get("base64_quality"));
            String base64Note = stringValue(entry.get("base64_note"), null);

            List<String> notes = new ArrayList<>();
            if ("image/jpeg".equals(previewMime) && previewQuality != null) {
                notes.add("JPEG preview quality " + previewQuality);
            }
            if (previewNote != null && !previewNote.isEmpty()) {
                notes.add(previewNote);
            }
            if (base64Note != null && !base64Note.equals(previewNote)) {
                notes.add(base64Note);
            }

            if (previewName != null) {
                lines.add("");
                lines.add("  ![" + stringValue(entry.get("test"), "preview") + "](attachment:" + previewName + ")");
                if (!notes.isEmpty()) {
                    lines.add("  _Preview info: " + String.join("; ", notes) + "._");
                }
            } else if (base64 != null) {
                lines.add("");
                lines.add("  _Preview generated but could not be published; see workflow artifacts for JPEG preview._");
                if (!notes.isEmpty()) {
                    lines.add("  _Preview info: " + String.join("; ", notes) + "._");
                }
            } else if ("too_large".equals(base64Omitted)) {
                StringBuilder extra = new StringBuilder();
                if (base64Length != null) {
                    extra.append(" (base64 length ≈ ").append(String.format("%,d", base64Length)).append(" chars)");
                }
                if ("jpeg".equals(base64Codec) && base64Quality != null) {
                    notes.add("attempted JPEG quality " + base64Quality);
                }
                if (base64Note != null && !base64Note.isEmpty()) {
                    notes.add(base64Note);
                }
                lines.add("");
                lines.add("  _Preview omitted" + extra + ". " + String.join("; ", notes) + "._");
            }
        }

        lines.add("");
        lines.add(MARKER);
        lines.add("");
        return String.join("\n", lines).trim();
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    static final class RenderResult {
        final List<String> summaryLines;
        final String commentBody;

        RenderResult(List<String> summaryLines, String commentBody) {
            this.summaryLines = summaryLines;
            this.commentBody = commentBody;
        }
    }
}
