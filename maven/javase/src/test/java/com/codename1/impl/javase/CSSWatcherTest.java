package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSSWatcherTest {

    @Test
    void addsLocalizationArgumentForProjectL10nDirectory(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        Path cssDir = Files.createDirectories(projectDir.resolve("css"));
        Path l10nDir = Files.createDirectories(projectDir.resolve("l10n"));
        Path cssFile = Files.createFile(cssDir.resolve("theme.css"));

        CSSWatcher watcher = new CSSWatcher();
        List<String> args = new ArrayList<String>();

        watcher.addLocalizationArgument(args, cssFile.toFile(), null);

        assertEquals(2, args.size());
        assertEquals("-l", args.get(0));
        assertEquals(l10nDir.toFile().getAbsolutePath(), args.get(1));
    }

    @Test
    void addsLocalizationArgumentForOverrideInputInCommonModule(@TempDir Path tempDir) throws Exception {
        Path javaseDir = tempDir.resolve("javase");
        Path commonDir = tempDir.resolve("common");
        Path cssDir = Files.createDirectories(commonDir.resolve("src/main/css"));
        Path l10nDir = Files.createDirectories(commonDir.resolve("src/main/l10n"));
        Path cssFile = Files.createFile(cssDir.resolve("theme.css"));
        Files.createDirectories(javaseDir);

        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", javaseDir.toFile().getAbsolutePath());
        try {
            CSSWatcher watcher = new CSSWatcher();
            List<String> args = new ArrayList<String>();

            watcher.addLocalizationArgument(args, new File("css/theme.css"), cssFile.toFile().getAbsolutePath());

            assertTrue(args.contains("-l"));
            assertEquals(l10nDir.toFile().getAbsolutePath(), args.get(args.indexOf("-l") + 1));
        } finally {
            System.setProperty("user.dir", oldUserDir);
        }
    }
}
