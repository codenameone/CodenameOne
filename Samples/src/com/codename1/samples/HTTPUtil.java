/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;


import com.codename1.samples.FileUtil;
import static com.codename1.samples.FileUtil.readFileToLong;
import static com.codename1.samples.IOUtil.readToString;
import static com.codename1.samples.CertificateUtil.getSHA1Fingerprint;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 *
 * @author shannah
 */
class HTTPUtil {
    public static interface ProgressListener {
        public void update(URL url, long bytesRead, long totalBytes);
    }
    
    private static ProgressListener progressListener;
    public static void setProgressListener(ProgressListener l) {
        progressListener = l;
    }
    private static void updateProgress(URL url, long bytesRead, long totalBytes) {
        if (progressListener != null) {
            progressListener.update(url, bytesRead, totalBytes);
        }
    }
    
    public static final Logger logger = Logger.getLogger(HTTPUtil.class.getName());
    
    public static class ETags {
        private static Map<String,String> etags;
        
        public static String sanitize(String etag) {
            return etag.replaceAll("[^a-zA-Z0-9]", "").trim();
        }
        
        public static void add(String url, String etag) {
            etag = sanitize(etag);
            if (etags == null) {
                etags = new HashMap<>();
            }
            etags.put(url, etag);
        }
        
        public static String get(String url) {
            if (etags != null) {
                return etags.get(url);
            }
            return null;
        }
        
        public static void clear() {
            etags = null;
        }
    }
    
    public static class Fingerprints {
        private static Map<String,String> fingerprints;
        
        public static void add(String url, String fingerprint) {
            if (fingerprints == null) {
                fingerprints = new HashMap<>();
            }
            fingerprints.put(url, fingerprint);
        }
        
        public static String get(String url) {
            if (fingerprints != null) {
                return fingerprints.get(url);
            }
            return null;
        }
        
        public static void clear() {
            fingerprints = null;
        }
    }
    
    
    public static boolean requiresUpdate(URL u, File destFile) throws IOException {
        return requiresUpdate(u, destFile, false);
    }
    
    
    private static void saveExpires(DownloadResponse resp, File destFile) throws IOException {
        if (resp != null && resp.getConnection() != null) {
            long expires = resp.getConnection().getHeaderFieldDate("Expires", 0);
            if (expires > 0) {
                FileUtil.writeStringToFile(String.valueOf(expires), new File(destFile.getParentFile(), destFile.getName()+".expires"));
            }
        }
    }
    
    public static long getExpiryDate(File destFile) throws IOException {
        File mtimeFile = new File(destFile.getParentFile(), destFile.getName()+".expires");
        if (mtimeFile.exists()) {
            return readFileToLong(mtimeFile);
        }
        return 0;
    }
    
    public static InputStream openStream(URL u) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)u.openConnection();
        conn.setInstanceFollowRedirects(true);
        return conn.getInputStream();
    }
    
    public static boolean requiresUpdate(URL u, File destFile, boolean forceCheck) throws IOException {
        //https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download
        if (destFile.exists() && "github.com".equals(u.getHost()) && u.getPath().contains("/releases/download/")) {
            logger.fine("Github release assets should not require updating.");
            return false;
        }
        if (!forceCheck && getExpiryDate(destFile) >= System.currentTimeMillis()) {
            return false;
        }
        File etagFile = new File(destFile.getParentFile(), destFile.getName()+".etag");
        if (destFile.exists() && etagFile.exists()) {
            String etag;
            try (InputStream is = new FileInputStream(etagFile)) {
                etag = readToString(is).trim();
            }
            DownloadResponse resp = new DownloadResponse();
            if (doesETagMatch(resp, u, etag)) {
                saveExpires(resp, destFile);
                return false;
            }
            saveExpires(resp, destFile);
        }
        return true;
    }
    
    public static boolean update(URL u, File destFile, File tempDir, boolean requireHttps, boolean requireFingerprintMatch) throws IOException, HttpsRequiredException, FingerprintChangedException {
        return update(u, destFile, tempDir, requireHttps, requireFingerprintMatch, false);
    }
    
    public static boolean update(URL u, File destFile, File tempDir, boolean requireHttps, boolean requireFingerprintMatch, boolean forceCheck) throws IOException, HttpsRequiredException, FingerprintChangedException {
        if (destFile.exists() && "github.com".equals(u.getHost()) && u.getPath().contains("/releases/download/")) {
            logger.fine("Github release assets should not require updating.");
            return false;
        }
        if (!forceCheck && getExpiryDate(destFile) > System.currentTimeMillis()) {
            return false;
        }
        File etagFile = new File(destFile.getParentFile(), destFile.getName()+".etag");
        if (destFile.exists() && etagFile.exists()) {
            String etag;
            try (InputStream is = new FileInputStream(etagFile)) {
                etag = readToString(is).trim();
            }
            DownloadResponse resp = new DownloadResponse();
            if (doesETagMatch(resp, u, etag)) {
                saveExpires(resp, destFile);
                return false;
            }
            saveExpires(resp, destFile);
        }
        File sha1File = new File(destFile.getParentFile(), destFile.getName()+".sha1");
        
        File tempFile = File.createTempFile(destFile.getName(), "progress", tempDir);
        Fingerprints.clear();
        DownloadResponse resp = new DownloadResponse();
        download(resp, u, tempFile, requireHttps);
        if (u.getProtocol() == "https") {
            String newFingerprint = Fingerprints.get(u.getHost());
            if (newFingerprint != null) {
                if (requireFingerprintMatch && sha1File.exists()) {
                    String lastFingerprint = FileUtil.readFileToString(sha1File).trim();
                    if (!lastFingerprint.equals(newFingerprint)) {
                        throw new FingerprintChangedException(u, destFile);
                    }
                } else {
                    if (sha1File.exists()) {
                        String existingFingerprint = FileUtil.readFileToString(sha1File).trim();
                        if (!existingFingerprint.equals(newFingerprint)) {
                            FileUtil.writeStringToFile(newFingerprint, sha1File);
                        }
                    } else {
                        FileUtil.writeStringToFile(newFingerprint, sha1File);
                    }
                }
            } else {
                if (requireFingerprintMatch) {
                    throw new FingerprintChangedException(u, destFile);
                }
            }
        }
        
        String etag0 = ETags.get(u.toString());
        if (etag0 != null) {
            FileUtil.writeStringToFile(etag0, new File(destFile.getParentFile(), destFile.getName()+".etag"));
        }
        saveExpires(resp, destFile);
        FileUtil.writeStringToFile(u.toString(), new File(destFile.getParentFile(), destFile.getName()+".src"));
        Files.move(tempFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
    
    public static class DownloadResponse {
        private HttpURLConnection connection;

        /**
         * @return the connection
         */
        public HttpURLConnection getConnection() {
            return connection;
        }

        /**
         * @param connection the connection to set
         */
        public void setConnection(HttpURLConnection connection) {
            this.connection = connection;
        }
    }
    
    
    
    public static File download(URL u, File f, boolean requireHttps) throws IOException, HttpsRequiredException {
        return download(null, u, f, requireHttps);
    }
    public static File download(DownloadResponse resp, URL u, File f, boolean requireHttps) throws IOException, HttpsRequiredException {
        URLConnection conn = u.openConnection();
        
        if (Boolean.getBoolean("client4j.disableHttpCache")) {
            conn.setUseCaches(false);
        }
        if (conn instanceof HttpURLConnection) {
            
            HttpURLConnection http = (HttpURLConnection)conn;
            http.setInstanceFollowRedirects(true);
            if (resp != null) {
                resp.setConnection(http);
            }
            
            int responseCode = http.getResponseCode();
            if (responseCode < 200 && responseCode >= 300) {
                
                throw new IOException("Failed to downlod url "+u+" to file "+f+".  HTTP response code was "+responseCode+" and response message was "+http.getResponseMessage());
            }
            
            String etag = http.getHeaderField("ETag");
            if (etag != null) {
                ETags.add(u.toString(), etag);
            }
        }
        
        if (conn instanceof HttpsURLConnection) {
            try {
                Certificate cert = ((HttpsURLConnection)conn).getServerCertificates()[0];
                if (cert instanceof X509Certificate) {
                    try {
                        String newFingerprint = getSHA1Fingerprint((X509Certificate)cert).trim();
                        Fingerprints.add(u.getHost(), newFingerprint);
                        
                    } catch (CertificateEncodingException ex) {
                        Logger.getLogger(HTTPUtil.class.getName()).log(Level.SEVERE, null, ex);
                        throw new IOException(ex);
                    }
                } else {
                    throw new IOException("Unsupported certificate type: "+cert.getClass().getName());
                }
            } catch (SSLPeerUnverifiedException ex) {
                throw new IOException(ex);
            }
        } else if (requireHttps) {
            throw new HttpsRequiredException(u, f);
        }
        
        try (InputStream input = conn.getInputStream()) {
            try (FileOutputStream output = new FileOutputStream(f)) {
                byte[] buf = new byte[128 * 1024];
                long total = conn.getContentLengthLong();
                long read = 0l;
                int len;
                while ((len = input.read(buf)) >= 0) {
                    read += len;
                    output.write(buf, 0, len);
                    updateProgress(u, read, total);
                    
                }
            }
        }
        
        return f;
    }
    
    
    public static boolean doesETagMatch(DownloadResponse resp, URL url, String etag) throws IOException {
        return doesETagMatch(resp, url, etag, true);
    }
    
    //public static boolean doesETagMatch(URL url, String etag) throws IOException {
    //    return doesETagMatch(url, etag, true);
    //}
    
    //public static boolean doesETagMatch(URL url, String etag, boolean followRedirects) throws IOException {
    //    return doesETagMatch(null, url, etag, followRedirects);
    //}
    
    public static boolean doesETagMatch(DownloadResponse resp, URL url, String etag, boolean followRedirects) throws IOException {
        if (etag == null) {
            return false;
        }
        //log("Checking etag for "+url);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        if (resp != null) {
            resp.setConnection(conn);
        }
        if (Boolean.getBoolean("client4j.disableHttpCache")) {
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
        }
        conn.setRequestMethod("HEAD");
        //https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download
        conn.setInstanceFollowRedirects(followRedirects);
        
        int response = conn.getResponseCode();
        logger.fine(""+conn.getHeaderFields());
        String newETag = conn.getHeaderField("ETag");
        //log("New etag is "+newETag+", old="+etag);
        
        if (newETag != null) {
            return etag.equals(ETags.sanitize(newETag));
        } else {
            return false;
        }
    }
    
    
}