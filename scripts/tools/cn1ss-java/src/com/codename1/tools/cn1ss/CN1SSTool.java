package com.codename1.tools.cn1ss;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CN1SSTool {
    private CN1SSTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(2);
        }
        String command = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        int exitCode;
        switch (command) {
            case "chunks":
                exitCode = handleChunks(rest);
                break;
            case "compare":
                exitCode = handleCompare(rest);
                break;
            case "comment":
                exitCode = handleComment(rest);
                break;
            case "simctl":
                exitCode = handleSimctl(rest);
                break;
            default:
                usage();
                exitCode = 2;
                break;
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static int handleChunks(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            return 2;
        }
        String sub = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (sub) {
            case "tests": {
                if (rest.length != 1) {
                    System.err.println("Usage: chunks tests <path>");
                    return 2;
                }
                Path path = Path.of(rest[0]);
                for (String test : ChunkParser.listTests(path)) {
                    System.out.println(test);
                }
                return 0;
            }
            case "extract": {
                boolean decode = false;
                String testName = "default";
                String channel = "";
                Path path = null;
                for (int i = 0; i < rest.length; i++) {
                    String arg = rest[i];
                    switch (arg) {
                        case "--decode":
                            decode = true;
                            break;
                        case "--test":
                            if (i + 1 >= rest.length) {
                                System.err.println("Missing value for --test");
                                return 2;
                            }
                            testName = rest[++i];
                            break;
                        case "--channel":
                            if (i + 1 >= rest.length) {
                                System.err.println("Missing value for --channel");
                                return 2;
                            }
                            channel = rest[++i];
                            break;
                        default:
                            path = Path.of(arg);
                            break;
                    }
                }
                if (path == null) {
                    System.err.println("Usage: chunks extract [--decode] [--test name] [--channel name] <path>");
                    return 2;
                }
                byte[] decoded = ChunkParser.decode(path, testName, channel);
                if (decode) {
                    try (OutputStream out = System.out) {
                        out.write(decoded);
                    }
                } else {
                    String base64 = java.util.Base64.getEncoder().encodeToString(decoded);
                    System.out.print(base64);
                }
                return 0;
            }
            case "count": {
                if (rest.length != 1) {
                    System.err.println("Usage: chunks count <path>");
                    return 2;
                }
                Path path = Path.of(rest[0]);
                int count = ChunkParser.listTests(path).size();
                System.out.println(count);
                return 0;
            }
            default:
                System.err.println("Unknown chunks subcommand: " + sub);
                return 2;
        }
    }

    private static int handleCompare(String[] args) throws IOException {
        Path referenceDir = null;
        boolean emitBase64 = false;
        Path previewDir = null;
        Path previewSourceDir = null;
        Path jsonOut = null;
        Path summaryOut = null;
        Path commentOut = null;
        String platform = "iOS";
        List<ScreenshotComparator.ActualEntry> actualEntries = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--reference-dir":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    referenceDir = Path.of(args[++i]);
                    break;
                case "--emit-base64":
                    emitBase64 = true;
                    break;
                case "--preview-dir":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    previewDir = Path.of(args[++i]);
                    break;
                case "--preview-source-dir":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    previewSourceDir = Path.of(args[++i]);
                    break;
                case "--json-out":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    jsonOut = Path.of(args[++i]);
                    break;
                case "--summary-out":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    summaryOut = Path.of(args[++i]);
                    break;
                case "--comment-out":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    commentOut = Path.of(args[++i]);
                    break;
                case "--platform":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    platform = args[++i];
                    break;
                case "--actual":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    String mapping = args[++i];
                    int idx = mapping.indexOf('=');
                    if (idx <= 0) {
                        System.err.println("Invalid --actual mapping: " + mapping);
                        return 2;
                    }
                    String name = mapping.substring(0, idx);
                    Path path = Path.of(mapping.substring(idx + 1));
                    actualEntries.add(new ScreenshotComparator.ActualEntry(name, path));
                    break;
                default:
                    System.err.println("Unknown compare option: " + arg);
                    return 2;
            }
        }

        if (referenceDir == null) {
            System.err.println("--reference-dir is required");
            return 2;
        }

        ScreenshotComparator.ComparisonReport report = ScreenshotComparator.buildReport(
                referenceDir,
                actualEntries,
                emitBase64,
                previewDir,
                previewSourceDir
        );

        if (jsonOut != null) {
            Path parent = jsonOut.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(jsonOut, report.toJson(), StandardCharsets.UTF_8);
        } else {
            System.out.println(report.toJson());
        }

        CommentRenderer.RenderResult renderResult = CommentRenderer.build(report.results(), platform);

        if (summaryOut != null) {
            Path parent = summaryOut.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(summaryOut, String.join(System.lineSeparator(), renderResult.summaryLines), StandardCharsets.UTF_8);
        }
        if (commentOut != null) {
            Path parent = commentOut.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(commentOut, renderResult.commentBody == null ? "" : renderResult.commentBody, StandardCharsets.UTF_8);
        }
        return 0;
    }

    private static int handleComment(String[] args) throws Exception {
        Path bodyPath = null;
        Path previewDir = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--body":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    bodyPath = Path.of(args[++i]);
                    break;
                case "--preview-dir":
                    if (i + 1 >= args.length) {
                        return missingValue(arg);
                    }
                    previewDir = Path.of(args[++i]);
                    break;
                default:
                    System.err.println("Unknown comment option: " + arg);
                    return 2;
            }
        }
        if (bodyPath == null) {
            System.err.println("--body is required for comment command");
            return 2;
        }
        return PrCommentPublisher.publish(bodyPath, previewDir);
    }

    private static int handleSimctl(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: simctl <best-runtime|device-type|device-info> [options]");
            return 2;
        }
        String sub = args[0];
        String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        switch (sub) {
            case "best-runtime": {
                String platform = "iOS";
                for (int i = 1; i < args.length; i++) {
                    if ("--platform".equals(args[i]) && i + 1 < args.length) {
                        platform = args[++i];
                    }
                }
                String identifier = SimctlParser.bestRuntime(input, platform);
                if (!identifier.isEmpty()) {
                    System.out.println(identifier);
                    return 0;
                }
                return 1;
            }
            case "device-type": {
                String deviceName = "iPhone 15";
                for (int i = 1; i < args.length; i++) {
                    if ("--device-name".equals(args[i]) && i + 1 < args.length) {
                        deviceName = args[++i];
                    }
                }
                String identifier = SimctlParser.findDeviceType(input, deviceName);
                if (!identifier.isEmpty()) {
                    System.out.println(identifier);
                    return 0;
                }
                return 1;
            }
            case "device-info": {
                String runtime = "";
                String deviceName = "iPhone 15";
                for (int i = 1; i < args.length; i++) {
                    if ("--runtime".equals(args[i]) && i + 1 < args.length) {
                        runtime = args[++i];
                    } else if ("--device-name".equals(args[i]) && i + 1 < args.length) {
                        deviceName = args[++i];
                    }
                }
                String info = SimctlParser.findDeviceInfo(input, runtime, deviceName);
                if (!info.isEmpty()) {
                    System.out.println(info);
                    return 0;
                }
                return 0;
            }
            default:
                System.err.println("Unknown simctl subcommand: " + sub);
                return 2;
        }
    }

    private static int missingValue(String option) {
        System.err.println("Missing value for " + option);
        return 2;
    }

    private static void usage() {
        System.err.println("Usage: CN1SSTool <chunks|compare|comment> [options]");
    }
}
