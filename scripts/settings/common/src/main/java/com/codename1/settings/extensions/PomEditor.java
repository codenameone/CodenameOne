package com.codename1.settings.extensions;

public final class PomEditor {
    private PomEditor() {
    }

    public static boolean containsDependency(String pom, MavenDependency dependency) {
        if (pom == null || dependency == null) {
            return false;
        }
        int searchFrom = 0;
        while (searchFrom < pom.length()) {
            int start = pom.indexOf("<dependency", searchFrom);
            if (start < 0) {
                return false;
            }
            int openEnd = pom.indexOf('>', start);
            int close = openEnd < 0 ? -1 : pom.indexOf("</dependency>", openEnd);
            if (openEnd < 0 || close < 0) {
                return false;
            }
            String block = pom.substring(start, close + "</dependency>".length());
            if (matches(block, dependency)) {
                return true;
            }
            searchFrom = close + "</dependency>".length();
        }
        return false;
    }

    public static String addDependency(String pom, MavenDependency dependency) {
        if (pom == null) {
            pom = "";
        }
        if (dependency == null || !dependency.isValid() || containsDependency(pom, dependency)) {
            return pom;
        }
        String xml = "        <dependency>\n"
                + "            <groupId>" + xml(dependency.groupId()) + "</groupId>\n"
                + "            <artifactId>" + xml(dependency.artifactId()) + "</artifactId>\n"
                + "            <version>" + xml(dependency.version()) + "</version>\n"
                + (dependency.type().length() == 0 ? "" : "            <type>" + xml(dependency.type()) + "</type>\n")
                + "        </dependency>\n";
        int depsEnd = projectDependenciesEnd(pom);
        if (depsEnd >= 0) {
            return pom.substring(0, depsEnd) + xml + pom.substring(depsEnd);
        }
        int insertion = pom.indexOf("<profiles");
        if (insertion < 0) {
            insertion = pom.indexOf("</project>");
        }
        if (insertion >= 0) {
            return pom.substring(0, insertion)
                    + "    <dependencies>\n"
                    + xml
                    + "    </dependencies>\n"
                    + pom.substring(insertion);
        }
        return pom + "\n<dependencies>\n" + xml + "</dependencies>\n";
    }

    public static String removeDependency(String pom, MavenDependency dependency) {
        if (pom == null || dependency == null) {
            return pom;
        }
        int searchFrom = 0;
        while (searchFrom < pom.length()) {
            int start = pom.indexOf("<dependency", searchFrom);
            if (start < 0) {
                return pom;
            }
            int openEnd = pom.indexOf('>', start);
            int close = openEnd < 0 ? -1 : pom.indexOf("</dependency>", openEnd);
            if (openEnd < 0 || close < 0) {
                return pom;
            }
            int end = close + "</dependency>".length();
            String block = pom.substring(start, end);
            if (matches(block, dependency)) {
                int lineStart = start;
                while (lineStart > 0 && pom.charAt(lineStart - 1) != '\n') {
                    lineStart--;
                }
                int lineEnd = end;
                while (lineEnd < pom.length() && (pom.charAt(lineEnd) == ' ' || pom.charAt(lineEnd) == '\t')) {
                    lineEnd++;
                }
                if (lineEnd < pom.length() && pom.charAt(lineEnd) == '\n') {
                    lineEnd++;
                }
                return pom.substring(0, lineStart) + pom.substring(lineEnd);
            }
            searchFrom = end;
        }
        return pom;
    }

    private static int projectDependenciesEnd(String pom) {
        int profiles = pom.indexOf("<profiles");
        int dependencyManagementStart = pom.indexOf("<dependencyManagement");
        int dependencyManagementEnd = dependencyManagementStart < 0
                ? -1 : pom.indexOf("</dependencyManagement>", dependencyManagementStart);
        int searchFrom = 0;
        while (searchFrom < pom.length()) {
            int start = pom.indexOf("<dependencies", searchFrom);
            if (start < 0 || profiles >= 0 && start > profiles) {
                return -1;
            }
            int openEnd = pom.indexOf('>', start);
            int close = openEnd < 0 ? -1 : pom.indexOf("</dependencies>", openEnd);
            if (openEnd < 0 || close < 0) {
                return -1;
            }
            boolean managed = dependencyManagementStart >= 0
                    && start > dependencyManagementStart
                    && dependencyManagementEnd >= close;
            if (!managed) {
                return close;
            }
            searchFrom = close + "</dependencies>".length();
        }
        return -1;
    }

    private static boolean matches(String block, MavenDependency dependency) {
        return block.contains("<groupId>" + dependency.groupId() + "</groupId>")
                && block.contains("<artifactId>" + dependency.artifactId() + "</artifactId>");
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
