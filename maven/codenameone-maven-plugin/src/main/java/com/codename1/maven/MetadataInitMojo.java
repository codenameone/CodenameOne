package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Scaffolds the {@code cn1-metadata} folder consumed by {@code cn1:metadata-push}:
 * an empty template of per-locale store-listing field files plus a screenshots
 * folder, ready to fill in and commit. Idempotent -- it only creates what's
 * missing, so re-running never overwrites your text.
 *
 * <pre>
 *   mvn cn1:metadata-init                       # apple + google, en-US
 *   mvn cn1:metadata-init -Dstore=apple -Dlocale=fr-FR
 * </pre>
 *
 * Empty field files are ignored by {@code metadata-push} (an unset field leaves
 * the store's value unchanged), so you can scaffold everything and fill in only
 * the fields you want to manage.
 */
@Mojo(name = "metadata-init", requiresProject = true)
public class MetadataInitMojo extends AbstractCN1Mojo {

    @Parameter(property = "store", defaultValue = "both")
    private String store;

    @Parameter(property = "locale", defaultValue = "en-US")
    private String locale;

    @Parameter(property = "metadataDir")
    private File metadataDir;

    private static final List<String> APPLE_FIELDS = Arrays.asList(
            "name.txt", "subtitle.txt", "description.txt", "keywords.txt",
            "promotional_text.txt", "whats_new.txt", "marketing_url.txt",
            "support_url.txt", "privacy_url.txt");
    private static final List<String> GOOGLE_FIELDS = Arrays.asList(
            "name.txt", "subtitle.txt", "description.txt", "whats_new.txt", "support_url.txt");

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            getLog().warn("Not a Codename One project directory; skipping metadata-init.");
            return;
        }
        File dir = metadataDir != null ? metadataDir : new File(getCN1ProjectDir(), "cn1-metadata");
        List<String> stores = "both".equalsIgnoreCase(store)
                ? Arrays.asList("apple", "google") : Arrays.asList(store.toLowerCase());
        try {
            mkdirs(dir);
            writeIfMissing(new File(dir, "README.txt"), readmeText());
            for (String s : stores) {
                boolean apple = "apple".equals(s);
                File storeDir = new File(dir, s);
                File localeDir = new File(storeDir, locale);
                mkdirs(localeDir);
                for (String field : (apple ? APPLE_FIELDS : GOOGLE_FIELDS)) {
                    writeIfMissing(new File(localeDir, field), "");
                }
                // An example screenshots folder for the store's default device type.
                String type = apple ? "APP_IPHONE_67" : "phoneScreenshots";
                File shotsDir = new File(new File(localeDir, "screenshots"), type);
                mkdirs(shotsDir);
                writeIfMissing(new File(shotsDir, ".gitkeep"), "");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not scaffold metadata folder under " + dir, e);
        }
        getLog().info("Metadata scaffold ready under " + dir
                + " -- fill in the .txt files and run 'mvn cn1:metadata-push'.");
    }

    private void mkdirs(File d) throws IOException {
        if (!d.isDirectory() && !d.mkdirs()) {
            throw new IOException("could not create " + d);
        }
    }

    private void writeIfMissing(File f, String content) throws IOException {
        if (f.exists()) {
            getLog().debug("exists, skipping " + f);
            return;
        }
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        getLog().info("  created " + f.getName() + " (" + f.getParentFile().getName() + ")");
    }

    private static String readmeText() {
        return "cn1-metadata -- store listing metadata as code (cn1:metadata-push).\n\n"
                + "Layout: <store>/<locale>/<field>.txt and <store>/<locale>/screenshots/<type>/*.png\n"
                + "  store  : apple | google\n"
                + "  locale : e.g. en-US, fr-FR (one folder per locale)\n"
                + "  fields : name, subtitle, description, keywords (apple), promotional_text (apple),\n"
                + "           whats_new, marketing_url, support_url, privacy_url\n"
                + "  screenshots type: Apple screenshotDisplayType (e.g. APP_IPHONE_67) /\n"
                + "                    Google imageType (e.g. phoneScreenshots)\n\n"
                + "Empty files are ignored (the store keeps its current value). Optional store-level\n"
                + "files: primary_locale.txt, primary_category.txt, secondary_category.txt (apple).\n"
                + "Push with: mvn cn1:metadata-push\n";
    }
}
