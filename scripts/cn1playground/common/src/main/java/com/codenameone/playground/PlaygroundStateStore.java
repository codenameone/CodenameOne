package com.codenameone.playground;

import com.codename1.io.Storage;
import com.codename1.l10n.L10NManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

final class PlaygroundStateStore {
    static final class HistoryEntry {
        final String script;
        final long timestamp;

        HistoryEntry(String script, long timestamp) {
            this.script = script;
            this.timestamp = timestamp;
        }

        String title() {
            if (timestamp <= 0) {
                return "Earlier save";
            }
            return L10NManager.getInstance().formatDateTimeShort(new Date(timestamp));
        }

        String detail(List<HistoryEntry> allEntries) {
            String detail = summarize(script, allEntries);
            return detail.length() > 0 ? detail : "Saved at " + title();
        }
    }

    private static final String CURRENT_SCRIPT_KEY = "cn1playground.currentScript";
    private static final String CURRENT_OUTPUT_KEY = "cn1playground.currentOutput";
    private static final String HISTORY_KEY = "cn1playground.history";
    private static final int MAX_HISTORY = 10;

    private PlaygroundStateStore() {
    }

    static String loadCurrentScript() {
        Object value = Storage.getInstance().readObject(CURRENT_SCRIPT_KEY);
        return value instanceof String && ((String) value).length() > 0
                ? (String) value
                : PlaygroundExamples.DEFAULT_SCRIPT;
    }

    static String loadCurrentOutput() {
        Object value = Storage.getInstance().readObject(CURRENT_OUTPUT_KEY);
        return value instanceof String ? (String) value : "";
    }

    static void saveCurrentState(String script, String output) {
        Storage.getInstance().writeObject(CURRENT_SCRIPT_KEY, script == null ? "" : script);
        Storage.getInstance().writeObject(CURRENT_OUTPUT_KEY, output == null ? "" : output);
    }

    static List<HistoryEntry> loadHistory() {
        Object value = Storage.getInstance().readObject(HISTORY_KEY);
        ArrayList<HistoryEntry> out = new ArrayList<HistoryEntry>();
        if (value instanceof Vector) {
            Vector raw = (Vector) value;
            for (int i = 0; i < raw.size(); i++) {
                Object entry = raw.elementAt(i);
                HistoryEntry parsed = parseHistoryEntry(entry);
                if (parsed != null) {
                    out.add(parsed);
                }
            }
            return out;
        }
        if (value instanceof List) {
            List<?> raw = (List<?>) value;
            for (Object entry : raw) {
                HistoryEntry parsed = parseHistoryEntry(entry);
                if (parsed != null) {
                    out.add(parsed);
                }
            }
        }
        return out;
    }

    static List<HistoryEntry> pushHistory(String script) {
        String normalized = script == null ? "" : script.trim();
        if (normalized.length() == 0) {
            return loadHistory();
        }
        ArrayList<HistoryEntry> history = new ArrayList<HistoryEntry>(loadHistory());
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry existing = history.get(i);
            if (normalized.equals(normalizeScript(existing.script))) {
                return history;
            }
        }
        history.add(0, new HistoryEntry(script, System.currentTimeMillis()));
        while (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
        Storage.getInstance().writeObject(HISTORY_KEY, serializeHistory(history));
        return history;
    }

    private static Vector serializeHistory(List<HistoryEntry> history) {
        Vector out = new Vector();
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);
            Hashtable data = new Hashtable();
            data.put("script", entry.script);
            data.put("timestamp", Long.valueOf(entry.timestamp));
            out.addElement(data);
        }
        return out;
    }

    private static HistoryEntry parseHistoryEntry(Object value) {
        if (value instanceof Hashtable) {
            Hashtable data = (Hashtable) value;
            Object script = data.get("script");
            if (!(script instanceof String) || ((String) script).length() == 0) {
                return null;
            }
            Object timestamp = data.get("timestamp");
            long time = timestamp instanceof Long ? ((Long) timestamp).longValue() : 0L;
            return new HistoryEntry((String) script, time);
        }
        if (value instanceof String && ((String) value).length() > 0) {
            return new HistoryEntry((String) value, 0L);
        }
        return null;
    }

    private static String normalizeScript(String script) {
        return script == null ? "" : script.trim();
    }

    private static String summarize(String script, List<HistoryEntry> allEntries) {
        if (script == null) {
            return "Untitled";
        }
        ArrayList<String> candidateLines = new ArrayList<String>();
        int start = 0;
        while (start < script.length()) {
            int end = script.indexOf('\n', start);
            if (end < 0) {
                end = script.length();
            }
            String trimmed = script.substring(start, end).trim();
            if (isMeaningfulSummaryLine(trimmed)) {
                candidateLines.add(trimmed);
            }
            start = end + 1;
        }
        for (int i = 0; i < candidateLines.size(); i++) {
            String line = candidateLines.get(i);
            if (isDistinctive(line, allEntries)) {
                return ellipsize(line);
            }
        }
        if (!candidateLines.isEmpty()) {
            return ellipsize(candidateLines.get(0));
        }
        return "";
    }

    private static boolean isDistinctive(String line, List<HistoryEntry> allEntries) {
        int matches = 0;
        for (int i = 0; i < allEntries.size(); i++) {
            HistoryEntry entry = allEntries.get(i);
            if (entry == null || entry.script == null) {
                continue;
            }
            if (entry.script.indexOf(line) >= 0) {
                matches++;
                if (matches > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isMeaningfulSummaryLine(String line) {
        if (line == null || line.length() == 0) {
            return false;
        }
        return !line.startsWith("import ")
                && !line.startsWith("package ")
                && !line.startsWith("//")
                && !line.startsWith("*")
                && !line.startsWith("/*")
                && !"{".equals(line)
                && !"}".equals(line);
    }

    private static String ellipsize(String value) {
        if (value.length() > 48) {
            return value.substring(0, 45) + "...";
        }
        return value;
    }
}
