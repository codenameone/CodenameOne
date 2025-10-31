import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class BuildAndRun {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("BuildAndRun started");
        String platform = args[0];
        Path projectDir = Paths.get("scripts/DeviceRunnerTest");

        System.out.println("Building for platform: " + platform);
        ProcessBuilder mvn = new ProcessBuilder(
                "mvn",
                "-f",
                projectDir.resolve("pom.xml").toString(),
                "com.codenameone:codenameone-maven-plugin:build",
                "-Dcodename1.platform=" + platform,
                "-Dcodename1.buildTarget=" + platform + "-device"
        );

        mvn.inheritIO();
        if (mvn.start().waitFor() != 0) {
            System.err.println("Build failed");
            System.exit(1);
        }
        System.out.println("Build successful");

        if ("android".equals(platform)) {
            runAndroid();
        } else if ("ios".equals(platform)) {
            runIOS();
        }
    }

    private static void runAndroid() throws IOException, InterruptedException {
        System.out.println("Running Android test");
        Path apk = findApk(Paths.get("scripts/DeviceRunnerTest/target/android"));
        System.out.println("Found apk: " + apk);
        ProcessBuilder adbInstall = new ProcessBuilder("adb", "install", "-r", apk.toString());
        adbInstall.inheritIO();
        if (adbInstall.start().waitFor() != 0) {
            System.err.println("adb install failed");
            System.exit(1);
        }
        System.out.println("adb install successful");

        new ProcessBuilder("adb", "logcat", "-c").start().waitFor();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                ProcessBuilder adbLogcat = new ProcessBuilder("adb", "logcat");
                Process logcat = adbLogcat.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(logcat.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                logcat.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Starting app");
        ProcessBuilder amStart = new ProcessBuilder("adb", "shell", "am", "start", "-n", "com.mycompany.app/com.mycompany.app.DeviceRunnerTestStub");
        amStart.inheritIO();
        amStart.start().waitFor();
        System.out.println("App started");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static void runIOS() throws IOException, InterruptedException {
        System.out.println("Running iOS test");
        Path xcodeProject = Paths.get("scripts/DeviceRunnerTest/target/ios/DeviceRunnerTest.xcodeproj");
        System.out.println("Found Xcode project: " + xcodeProject);
        new ProcessBuilder("xcrun", "simctl", "boot", "iPhone 15 Pro").inheritIO().start().waitFor();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                ProcessBuilder logStream = new ProcessBuilder("xcrun", "simctl", "spawn", "iPhone 15 Pro", "log", "stream");
                Process log = logStream.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(log.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                log.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Running xcodebuild");
        new ProcessBuilder("xcodebuild", "-project", xcodeProject.toString(), "-scheme", "DeviceRunnerTest", "-destination", "platform=iOS Simulator,name=iPhone 15 Pro", "test").inheritIO().start().waitFor();
        System.out.println("xcodebuild finished");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static Path findApk(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".apk")).findFirst().orElseThrow(() -> new RuntimeException("APK not found"));
        }
    }
}
