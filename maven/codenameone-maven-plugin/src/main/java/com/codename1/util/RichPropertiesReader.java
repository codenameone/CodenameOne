package com.codename1.util;

import com.codename1.maven.GenerateAppProjectMojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
/**
 * A reader that can read a rich properties file format which can have
 * section headers to define properties whose values are multiple lines.
 * The syntax for a section looks like:
 *
 * [propertyname]
 * ====
 * Property content
 * Content goes until the closing separator.
 * ====
 *
 * It also supports properties of the form:
 *
 * key=value
 *
 *
 */
public class RichPropertiesReader {

    /**
     * An exception that is thrown if a syntax error is experienced while parsing an
     * extended config file
     */
    public static class ConfigSyntaxException extends Exception {
        private int line;

        public ConfigSyntaxException(String message, int line) {
            super(message);
            this.line = line;
        }

    }

    public void load(File file, Properties properties) throws IOException, ConfigSyntaxException {
        try (FileInputStream fis = new FileInputStream(file)) {
            load(fis, properties);
        }
    }

    public void load(InputStream inputStream, Properties properties) throws IOException, ConfigSyntaxException {

        Scanner scanner = new Scanner(inputStream, "UTF-8");
        StringBuilder sb = null;
        String sectionName = null;
        String separator = null;
        int lineNum = 0;
        while (scanner.hasNextLine()) {

            String line = scanner.nextLine();
            lineNum++;

            if (separator != null && line.equals(separator)) {
                properties.put(sectionName, sb.toString().trim());
                sectionName = null;
                sb = null;
                separator = null;
                continue;
            }

            if (sectionName != null) {
                sb.append(line).append(System.lineSeparator());
                continue;
            }

            if (sectionName == null && line.indexOf("=") > 0 && line.charAt(0) != '[') {
                String key = line.substring(0, line.indexOf("="));
                String value = line.substring(line.indexOf("=")+1);
                properties.put(key, value);
                continue;
            }

            if (line.length() > 2) {
                char firstChar = line.charAt(0);
                char lastChar = line.charAt(line.length() - 1);
                if (firstChar == '[' && lastChar == ']') {
                    sectionName = line.substring(1, line.length() - 1);
                    if (sectionName.contains("[") || sectionName.contains("]")) {
                        // Section Name should not contain any brackets
                        sectionName = null;
                        continue;
                    }
                    sb = new StringBuilder();
                    if (!scanner.hasNextLine()) {
                        throw new ConfigSyntaxException("Missing line after section marker " + sectionName, lineNum);
                    }
                    String nextLine = scanner.nextLine();
                    lineNum++;
                    if (nextLine.length() > 0 && nextLine.charAt(0) == '=') {
                        if (getSeparatorLength(nextLine) > 0) {
                            separator = nextLine;
                        } else {
                            throw new ConfigSyntaxException("Section missing separator " + sectionName, lineNum);
                        }
                    } else {
                        separator = "";
                        sb.append(nextLine).append(System.lineSeparator());
                    }
                }
            }

        }

    }

    private int getSeparatorLength(String line) {
        int len = line.length();
        int sepLength = 0;
        for (int i=0; i<len; i++) {
            char ch = line.charAt(i);
            if (ch != '=') {
                return -1;
            }
            sepLength++;
        }
        return sepLength;
    }

}
