/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.jdom.etl.JDomModelETLFactory;
import org.apache.maven.model.jdom.etl.ModelETL;
import org.apache.maven.model.jdom.etl.ModelETLRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import java.util.List;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * A mojo that updates Codename One.
 * @author shannah
 */
@Mojo(name = "update")
public class UpdateCodenameOneMojo extends AbstractCN1Mojo {

    /** Where releases are published. The only source for a project that declares it. */
    private static final String R2_METADATA_URL =
            "https://repo.codenameone.com/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml";

    /**
     * Where releases were published before the migration. Its {@code <latest>} is frozen
     * at the last pre-cutover release, so it is offered ONLY to a project whose pom does
     * not declare the Codename One repository and therefore could not resolve anything
     * newer anyway. It is never a fallback for a project that does declare it -- see
     * findLatestVersion.
     */
    private static final String MAVEN_CENTRAL_METADATA_URL =
            "https://repo1.maven.org/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml";

    @Parameter(property = "cn1.metadataUrl", defaultValue = R2_METADATA_URL)
    private String metadataUrl;


    @Parameter(property="newVersion", defaultValue = "")
    private String newVersion;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        updateCodenameOne(true);

        String existingCn1Version = project.getModel().getProperties().getProperty("cn1.version");
        String existingCn1PluginVersion = project.getModel().getProperties().getProperty("cn1.plugin.version");
        boolean isAutoVersion = false;
        if (newVersion == null || newVersion.isEmpty()) {
            if (!existingCn1Version.endsWith("-SNAPSHOT")) {
                // As long as the existing version is not a snapshot, we'll update to the latest in Maven
                // by default.
                newVersion = "LATEST";
                isAutoVersion = true;
            }
        }

        if ("LATEST".equals(newVersion)) {
            String resolved = null;
            try {
                resolved = findLatestVersion();
            } catch (Exception ex) {
                getLog().error("Failed to find the latest Codename One version", ex);
            }
            // "LATEST" is a request, not a version. Leaving it in newVersion wrote the
            // literal string into cn1.version and cn1.plugin.version below, so a failure
            // to look the version up corrupted the pom it was asked to update -- into
            // one that resolves nothing. Returning is also better than falling through
            // to the else branch, which would report "already up to date".
            if (resolved == null || resolved.trim().isEmpty() || "LATEST".equals(resolved)) {
                getLog().error("Could not determine the latest Codename One version. "
                        + "Leaving cn1.version and cn1.plugin.version unchanged; pass "
                        + "-DnewVersion=<version> to set one explicitly.");
                return;
            }
            // Belt and braces over the removed Central fallback: a stale or
            // misconfigured -Dcn1.metadataUrl mirror could still answer with an older
            // release, and nothing downstream compares versions for order.
            //
            // This guards every path that reaches here, not just the automatic one.
            // "LATEST" is a symbolic request in both cases -- it means "whatever is
            // newest", so resolving it to something older is self-contradictory no
            // matter who asked. A concrete -DnewVersion=7.0.x never reaches this block
            // and is left alone, because naming a version IS a request for it,
            // downgrade included.
            if (wouldDowngrade(resolved, existingCn1Version)
                    || wouldDowngrade(resolved, existingCn1PluginVersion)) {
                getLog().warn("The newest version offered (" + resolved + ") is older than "
                        + "this project's (cn1.version=" + existingCn1Version
                        + ", cn1.plugin.version=" + existingCn1PluginVersion
                        + "), so it is not an update. Leaving both unchanged; pass "
                        + "-DnewVersion=" + resolved + " if a downgrade is intended.");
                return;
            }
            newVersion = resolved;
        }


        getLog().info("Existing cn1.version="+existingCn1Version);
        getLog().info("Existing cn1.plugin.version="+existingCn1PluginVersion);
        if (newVersion != null && !newVersion.isEmpty() && (!newVersion.equals(existingCn1Version) || !newVersion.equals(existingCn1PluginVersion))) {

            getLog().info("Attempting to update project to version " + newVersion);
            //MavenXpp3Reader pomReader = new MavenXpp3Reader();

            Model model = null;
            ModelETL modelETL;
            File pomFile = new File(project.getParent().getBasedir(), "pom.xml");
            /*
            try (FileInputStream fis = new FileInputStream(pomFile)) {
                model = pomReader.read(new InputStreamReader(fis, "UTF-8"), false);
            } catch (Exception ex) {
                getLog().error("Failed to load pom.xml file from parent project", ex);
                throw new MojoExecutionException("Failed to read pom.xml file", ex);
            }

             */
            try {
                ModelETLRequest modelETLRequest = new ModelETLRequest();
                modelETL = new JDomModelETLFactory().newInstance(modelETLRequest);
                modelETL.extract(pomFile);
                model = modelETL.getModel();
            } catch (Exception ex) {
                getLog().error("Failed to load pom.xml file from parent project", ex);
                throw new MojoExecutionException("Failed to read pom.xml file", ex);
            }

            boolean changed = false;
            if (!isAutoVersion || !existingCn1Version.endsWith("-SNAPSHOT")) {
                if (!existingCn1Version.equals(newVersion)) {
                    getLog().info("Setting cn1.version=" + newVersion);
                    model.getProperties().setProperty("cn1.version", newVersion);
                    changed = true;
                } else {
                    getLog().info("cn1.version already up to date.  Not changing");
                }
            } else {
                getLog().warn("Not updating cn1.version because current version is a snapshot.  To update cn1.version property run mvn cn1:update -DnewVersion=XXXX");
            }
            if (!isAutoVersion || !existingCn1PluginVersion.endsWith("-SNAPSHOT")) {
                if (!existingCn1PluginVersion.equals(newVersion)) {
                    getLog().info("Setting cn1.plugin.version=" + newVersion);
                    model.getProperties().setProperty("cn1.plugin.version", newVersion);
                    changed = true;
                } else {
                    getLog().info("cn1.plugin.version already up to date. Not changing.");
                }
            } else {
                getLog().warn("Not updating cn1.plugin.version because current version is a snapshot.  To update cn1.plugin.version property, run mvn cn1:update -DnewVersion=XXX");
            }

            if (changed) {
                try {
                    FileUtils.copyFile(pomFile, new File(pomFile.getParentFile(), "pom.xml.bak"));
                } catch (Exception ex) {
                    throw new MojoExecutionException("Failed to back up pom.xml file", ex);
                }
                /*
                try (FileOutputStream fos = new FileOutputStream(pomFile)) {
                    MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                    getLog().info("Updating "+pomFile+" with new cn1.version and cn1.plugin.version properties");

                    pomWriter.write(fos, model);


                } catch (IOException e) {
                    getLog().error("Failed to write changes to the pom file", e);
                    throw new MojoExecutionException("Failed to write canges to the pom file.", e);
                }

                 */
                try {
                    modelETL.load(pomFile);

                } catch (IOException e) {
                    getLog().error("Failed to write changes to the pom file", e);
                    throw new MojoExecutionException("Failed to write canges to the pom file.", e);
                }

            }



        } else {
            if (newVersion == null || newVersion.isEmpty()) {
                getLog().warn("Not updating pom.xml file because it is currently set to use a SNAPSHOT version of Codename One.");
                getLog().info("To update to a newer version of CN1 in maven use the -DnewVersion property.");
                getLog().info("e.g. -DnewVersion=LATEST to update to the latest version in Maven central");
                getLog().info("or -DnewVersion=7.0.12, for example");
            } else {
                getLog().info("Maven version already up to date.  Not updating pom.xml file");
            }
        }






 
    }

    /**
     * Finds the newest version this project could actually build against.
     *
     * Codename One publishes to its own repository, and during the migration also to
     * Maven Central. Asking the Codename One repository first is what lets cn1:update
     * see releases made after Central stops receiving them. But discovery must not run
     * ahead of resolution: a project whose pom does not declare that repository cannot
     * resolve a version that exists only there, so offering it would write an
     * unbuildable version into cn1.version and cn1.plugin.version. Such a project is
     * therefore only offered what Central can serve.
     *
     * This corrects itself: generated projects gain the repository as part of the
     * migration, and from then on they discover from it too.
     *
     * Override the primary source with -Dcn1.metadataUrl=... to point at a mirror.
     */
    private String findLatestVersion() throws IOException, XmlPullParserException {
        if (!projectCanResolveFrom(metadataUrl)) {
            getLog().info("This project does not declare " + repositoryHost(metadataUrl)
                    + " in <repositories>/<pluginRepositories>, so only versions available "
                    + "from Maven Central are offered. Add that repository to see newer "
                    + "releases.");
            return readLatestVersion(MAVEN_CENTRAL_METADATA_URL);
        }
        // Deliberately NO fallback to Maven Central here. Central no longer receives
        // releases, so its <latest> is frozen at the last version published before the
        // cutover and can never be newer than this repository's. Falling back to it
        // during an outage of this repository would hand back that frozen version, and
        // the caller compares versions only for inequality -- so cn1:update would
        // silently DOWNGRADE a post-cutover project to the last Central release. That
        // fallback was harmless only while both repositories carried the same releases.
        //
        // Failing is the honest outcome: the caller leaves the pom untouched and says
        // it could not determine the latest version.
        return readLatestVersion(metadataUrl);
    }

    /**
     * @return true if moving to {@code candidate} would take a project currently on
     * {@code current} backwards.
     *
     * A -SNAPSHOT current version is never treated as downgraded by a release. Moving
     * off a development build onto a published one is an ordinary thing to ask for, and
     * ComparableVersion ranks 8.0-SNAPSHOT above 7.0.267 purely on the numbering -- so
     * without this a contributor running -DnewVersion=LATEST on a snapshot project
     * would be refused the very thing the command means.
     */
    private static boolean wouldDowngrade(String candidate, String current) {
        if (current == null || current.trim().isEmpty() || current.endsWith("-SNAPSHOT")) {
            return false;
        }
        return new ComparableVersion(candidate).compareTo(new ComparableVersion(current)) < 0;
    }

    /** Host of a metadata URL, for both the containment test and the message. */
    private static String repositoryHost(String url) {
        try {
            return new URL(url).getHost();
        } catch (IOException ex) {
            return url;
        }
    }

    /**
     * Whether the project declares a repository on the same host as the given metadata
     * URL, for either dependencies or plugins. Both matter: the framework artifacts come
     * from &lt;repositories&gt; and codenameone-maven-plugin from &lt;pluginRepositories&gt;,
     * so a version is only usable when both can be reached.
     */
    private boolean projectCanResolveFrom(String url) {
        return canResolveFrom(url,
                project.getRemoteArtifactRepositories(),
                project.getPluginArtifactRepositories());
    }

    /** Pure form of {@link #projectCanResolveFrom(String)}, so it can be tested directly. */
    static boolean canResolveFrom(String url,
                                  List<ArtifactRepository> dependencyRepositories,
                                  List<ArtifactRepository> pluginRepositories) {
        String host = repositoryHost(url);
        return declaresHost(dependencyRepositories, host)
                && declaresHost(pluginRepositories, host);
    }

    private static boolean declaresHost(List<ArtifactRepository> repositories, String host) {
        if (repositories == null) {
            return false;
        }
        for (ArtifactRepository repository : repositories) {
            if (repository != null && repository.getUrl() != null
                    && repository.getUrl().contains(host)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identifies this plugin to the repository. NOT cosmetic: the Codename One
     * repository is behind Cloudflare, whose Browser Integrity Check rejects the
     * JDK's default "Java/1.8.0_x" agent with a 403 (error 1010). It accepts
     * "Java/11" and later, so a bare openStream() works on a modern JDK and fails
     * on JDK 8 -- and the failure is silent, because findLatestVersion falls back
     * to Maven Central, which no longer receives new releases. A JDK 8 user would
     * be told the last version published before the cutover is the latest one,
     * indefinitely. ToolingHelpClient hit the same trap against a different
     * Cloudflare-fronted endpoint.
     */
    private static final String USER_AGENT = "codenameone-maven-plugin";

    private String readLatestVersion(String url) throws IOException, XmlPullParserException {
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        // URLConnection, not HttpURLConnection. cn1.metadataUrl is a user-settable
        // parameter, so it can legitimately be a file: URL pointing at an offline
        // mirror; casting would throw ClassCastException, which is not an IOException
        // and so escapes every IOException handler between here and executeImpl.
        // setRequestProperty and the timeouts are declared on URLConnection, so the
        // cast bought nothing here in the first place.
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        // Follows redirects by default, which the repository's custom domain uses.
        try (Reader input = new InputStreamReader(connection.getInputStream(), "UTF-8")) {
            Metadata metadata = reader.read(input, false);
            String latest = metadata.getVersioning().getLatest();
            if (latest == null || latest.trim().isEmpty()) {
                throw new IOException("No <latest> version in " + url);
            }
            return latest;
        } finally {
            // Only HTTP connections have one, and only when the connection was
            // actually made -- a file: URL has nothing to release.
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }
    
}
