package com.codename1.tools.cn1ss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class SimctlParser {
    private SimctlParser() {
    }

    static String bestRuntime(String json, String platform) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) {
            return "";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parsed;
        Object runtimesObj = root.get("runtimes");
        if (!(runtimesObj instanceof List)) {
            return "";
        }
        @SuppressWarnings("unchecked")
        List<Object> runtimes = (List<Object>) runtimesObj;
        String targetPlatform = platform == null ? "iOS" : platform;
        List<Integer> bestVersion = Collections.emptyList();
        String bestIdentifier = "";
        for (Object item : runtimes) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> runtime = (Map<String, Object>) item;
            String runtimePlatform = stringValue(runtime.get("platform"));
            if (!targetPlatform.equals(runtimePlatform)) {
                continue;
            }
            if (!isAvailable(runtime.get("isAvailable"), runtime.get("availability"))) {
                continue;
            }
            List<Integer> version = parseVersion(stringValue(runtime.get("version")));
            if (compareVersions(version, bestVersion) > 0) {
                bestVersion = version;
                bestIdentifier = stringValue(runtime.get("identifier"));
            }
        }
        return bestIdentifier == null ? "" : bestIdentifier;
    }

    static String findDeviceType(String json, String deviceName) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) {
            return "";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parsed;
        Object typesObj = root.get("devicetypes");
        if (!(typesObj instanceof List)) {
            return "";
        }
        @SuppressWarnings("unchecked")
        List<Object> types = (List<Object>) typesObj;
        for (Object item : types) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> type = (Map<String, Object>) item;
            String name = stringValue(type.get("name"));
            if (deviceName.equals(name)) {
                return stringValue(type.get("identifier"));
            }
        }
        return "";
    }

    static String findDeviceInfo(String json, String runtimeId, String deviceName) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) {
            return "";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parsed;
        Object devicesObj = root.get("devices");
        if (!(devicesObj instanceof Map)) {
            return "";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> devicesMap = (Map<String, Object>) devicesObj;
        for (Map.Entry<String, Object> entry : devicesMap.entrySet()) {
            if (runtimeId != null && !runtimeId.isEmpty() && !runtimeId.equals(entry.getKey())) {
                continue;
            }
            Object listObj = entry.getValue();
            if (!(listObj instanceof List)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Object> devices = (List<Object>) listObj;
            for (Object item : devices) {
                if (!(item instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> device = (Map<String, Object>) item;
                if (!isAvailable(device.get("isAvailable"), device.get("availability"))) {
                    continue;
                }
                String name = stringValue(device.get("name"));
                if (!deviceName.equals(name)) {
                    continue;
                }
                String udid = stringValue(device.get("udid"));
                String state = stringValue(device.get("state"));
                if (state == null || state.isEmpty()) {
                    state = "Unknown";
                }
                if (udid == null) {
                    return "";
                }
                return udid + "|" + state;
            }
        }
        return "";
    }

    private static boolean isAvailable(Object flag, Object legacy) {
        if (flag instanceof Boolean) {
            return (Boolean) flag;
        }
        if (legacy instanceof String) {
            return "(available)".equals(legacy);
        }
        return false;
    }

    private static List<Integer> parseVersion(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = text.replace('-', '.');
        String[] parts = normalized.split("\\.");
        List<Integer> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            int value = 0;
            try {
                value = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                break;
            }
            result.add(value);
        }
        return result;
    }

    private static int compareVersions(List<Integer> a, List<Integer> b) {
        int length = Math.max(a.size(), b.size());
        for (int i = 0; i < length; i++) {
            int ai = i < a.size() ? a.get(i) : 0;
            int bi = i < b.size() ? b.get(i) : 0;
            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }
}
