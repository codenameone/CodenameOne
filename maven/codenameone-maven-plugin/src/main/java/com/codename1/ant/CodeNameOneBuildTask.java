package com.codename1.ant;

import com.codename1.build.client.BuildProcess;
import com.codename1.build.client.CodeNameOneBuildClient;
import com.codename1.build.client.PromptLoginDialog;
import com.codename1.build.shared.BuildRequest;
import com.codename1.ui.Component;
import com.eclipsesource.json.JsonObject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

/**
 *
 * @author Shai Almog
 */
public class CodeNameOneBuildTask extends Task {
    //private static final String SERVER_URL = "http://localhost:8888";

    static boolean verbose;
    
    static {
        try {
            setProxySettings();
        } catch(Throwable t) {
            System.out.println("Failed to setup proxy settings: " + t);
        }
    }

    private static void setProxySettings() {
        Preferences proxyPref = Preferences.userNodeForPackage(Component.class);
        int proxySel = proxyPref.getInt("proxySel", 1);
        String proxySelHttp = proxyPref.get("proxySel-http", "");
        String proxySelPort = proxyPref.get("proxySel-port", "");

        switch (proxySel) {
            case 1:
                System.getProperties().remove("java.net.useSystemProxies");
                System.getProperties().remove("http.proxyHost");
                System.getProperties().remove("http.proxyPort");
                System.getProperties().remove("https.proxyHost");
                System.getProperties().remove("https.proxyPort");
                break;
            case 2:
                System.setProperty("java.net.useSystemProxies", "true");
                System.getProperties().remove("http.proxyHost");
                System.getProperties().remove("http.proxyPort");
                System.getProperties().remove("https.proxyHost");
                System.getProperties().remove("https.proxyPort");
                break;
            case 3:
                System.setProperty("http.proxyHost", proxySelHttp);
                System.setProperty("http.proxyPort", proxySelPort);
                System.setProperty("https.proxyHost", proxySelHttp);
                System.setProperty("https.proxyPort", proxySelPort);
                break;
        }
    }    
    
    /**
     * @return the verbose
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @param aVerbose the verbose to set
     */
    public void setVerbose(boolean aVerbose) {
        verbose = aVerbose;
    }
    private String userName;
    private String password;
    private File jarFile;
    private String displayName;
    private String mainClassName;
    private String packageName;
    private float version = 1.0f;
    private File icon;
    private String targetType;
    private String vendor;
    private String subtitle;
    private File pushCertificate;
    private File certificate;
    private String certPassword;
    private String keystoreAlias;
    private File provisioningProfile;
    private boolean includeSource;
    private String appid = "";
    private boolean appStoreBuild;
    private boolean automated;
    private File resultFile;
    private boolean production;
    private String buildArgs;
    
    private File rootDir;
                
    public void setFullClassName(String s) {
        int p = s.lastIndexOf('.');
        mainClassName = s.substring(p + 1);
        packageName = s.substring(0, p);
    }
    
    private void validateField(Object field, String name) {
        if(field == null) {
            throw new BuildException("The attribute " + name + " is required!");
        }
    }
    
    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }
    
    private void validateFile(File field, String name) {
        validateField(field, name);
        if(!field.exists()) {
            throw new BuildException("The file " + field.getAbsolutePath() + " doesn't exist");
        }
        if(field.length() > 50 * 1024 * 1024) {
            throw new BuildException("The file " + field.getAbsolutePath() + " exceeds 50MB in size! Please reduce the size of the project for faster builds and better performing apps.");
        }
    }

    private void validateString(String field, String name) {
        validateField(field, name);
        if(field.length() == 0) {
            throw new BuildException("The attribute " + field + " can't be empty");
        }
    }
    
    private File getFilePrefs(String key, Preferences p) {
        String s = p.get(key, null);
        if(s == null || s.length() == 0) {
            return null;
        }
        File f = new File(s);
        if(f.exists() && !f.isDirectory()) {
            return f;
        }
        return null;
    }
    
    private boolean showConfirm(String text, String title) {
        java.awt.Frame[] frms = java.awt.Frame.getFrames();
        java.awt.Frame f = null;
        if(frms != null && frms.length > 0) {
            f = frms[0];
        }
        return JOptionPane.showConfirmDialog(f, text, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }
    
    public void execute() {
        if(automated) {
            System.out.println("Launching automated build...");
        }
        String cn1user = getProject().getProperty("cn1user");
        String cn1password = getProject().getProperty("cn1password");
        
        if (cn1user == null || "".equals(cn1user)) cn1user = System.getenv("CN1USER");
        if (cn1password == null || "".equals(cn1password)) cn1password = System.getenv("CN1PASS");
        
        if(cn1user != null && cn1password != null && !"".equals(cn1user) && !"".equals(cn1password)) {
            System.out.println("Setting password from the environment");
            userName = cn1user;
            password = cn1password;
        } else {
            if(userName == null || password == null) {
                System.out.println("Prompting for password");
                PromptLoginDialog p = PromptLoginDialog.checkUserPass();
                if(p.isCanceled()) {
                    System.out.println("Dialog prompt canceled");
                    return;
                }
                userName = p.getUser();
                password = p.getPass();
                System.out.println("Building for username: " + userName);
            }
        }

        validateFile(jarFile, "jarFile");
        validateString(displayName, "displayName");
        validateString(mainClassName, "mainClassName");
        validateString(packageName, "packageName");
        validateFile(icon, "icon");
        validateString(targetType, "targetType");
        validateString(vendor, "vendor");
                        
        if(mainClassName.indexOf(".") > -1) {
            throw new BuildException("The main class attribute must contain the class name without the package name in which it resides. The package name must be specified in the packageName entry");
        }
        
        if(packageName.indexOf(".") < 0 || packageName.contains("..")) {
            throw new BuildException("The packageName entry must be a valid package name in which the main class resides");
        }
        
        /*targetType = targetType.toLowerCase();
        if(!targetType.equals("iphone") && !targetType.equals("iphone_crash") && 
                !targetType.equals("android") &&
                !targetType.equals("rim") &&
                !targetType.equals("me") &&
                !targetType.equals("win") &&
                !targetType.startsWith("debug")) {
            throw new BuildException("targetType must be one of: iphone, android, rim, me or win");
        }*/
        
        try {        
            BuildProcess b = new BuildProcess();
            String u = CodeNameOneBuildClient.SERVER_URL;
            if(!b.login(u, userName, password, 
                    rootDir, buildArgs != null && 
                    buildArgs.contains("codename1.arg.build.version"))) {
                throw new BuildException("Login failed");
            }
            
            try {
                int userType = getUserType(userName, password);
                //if this is a free account
                if (userType == 1000) {
                    int limitSize = getAppSizeLimit(userName);
                    if (jarFile.length() > limitSize) {
                        showUpgradeDialog((int) jarFile.length(), limitSize);
                        return;
                    }
                }
            } catch (Exception e) {
                //if for some reason this has failed, proceed with the build process.
            }
            
            BuildRequest r = new BuildRequest();
            r.setProduction(production);
            r.setDisplayName(displayName);
            r.setMainClass(mainClassName);
            r.setVendor(vendor);
            r.setPackageName(packageName);
            r.setUserName(userName);
            r.setPassword(password);
            r.setIncludeSource(includeSource);
            r.setAppid(appid);
            
            Properties props = new SortedProperties();
            File propFile = new File(rootDir, "codenameone_settings.properties");
            InputStream propsInput = new FileInputStream(propFile);
            props.load(propsInput);
            propsInput.close();
            for(Object k : props.keySet()) {
                String key = (String)k;
                if(key.startsWith("codename1.arg.")) {
                    String value = props.getProperty(key);
                    String currentKey = key.substring(14);
                    if(currentKey.indexOf(' ') > -1) {
                        throw new BuildException("The build argument contains a space in the key: '" + currentKey + "'");
                    } 
                    r.putArgument(currentKey, value);
                }
            }
            
            if(automated) {
                r.putArgument("cn1.syncBuild", "true");
            }
            
            if(buildArgs != null) {
                for(String s : buildArgs.split(",")) {
                    String[] a = s.split("=");
                    r.putArgument(a[0], a[1]);
                }
            }

            if(subtitle == null) {
                r.setSubTitle("By " + vendor);
            } else {
                r.setSubTitle(subtitle);
            }
            r.setVersion("" + version);
            try {
                BufferedImage bi = ImageIO.read(icon);
                if(bi.getWidth() != 512 || bi.getHeight() != 512) {
                    throw new BuildException("The icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices");
                }
                r.setIcon(icon.getAbsolutePath());
            } catch (IOException ex) {
                throw new BuildException("Error reading the icon: the icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices");
            }
            r.setType(targetType);
            
            Preferences prefs = Preferences.userNodeForPackage(Component.class);
            if(targetType.indexOf("iphone") > -1) {
                if(appid == null || appid.length() == 0) {
                    throw new BuildException("The App id for an iOS build MUST be defined! You can create an iOS certificate using an iOS developer account from Apple and our certificate wizard available in Codename One Settings (right click on the project)");
                }
                
                String iOSPortJar = getProject().getProperty("override.iOSPort.jar");
                String nativeiosJar = getProject().getProperty("override.nativeios.jar");
                String byteCodeTranslatorJar = getProject().getProperty("override.ByteCodeTranslator.jar");
                String javaAPIJar = getProject().getProperty("override.JavaAPI.jar");
                if (iOSPortJar != null && !"".equals(iOSPortJar) && new File(iOSPortJar).exists()) {
                    this.log("Adding iOSPort.jar to "+jarFile);
                    System.out.println("Adding iOSPort.jar to "+jarFile);
                    addFilesToJar(jarFile, "cn1.override.", new File(iOSPortJar));
                }
                if (nativeiosJar != null && !"".equals(nativeiosJar) && new File(nativeiosJar).exists()) {
                    this.log("Adding nativeios.jar to "+jarFile);
                    addFilesToJar(jarFile, "cn1.override.", new File(nativeiosJar));
                }
                if (byteCodeTranslatorJar != null && !"".equals(byteCodeTranslatorJar) && new File(byteCodeTranslatorJar).exists()) {
                    this.log("Adding ByteCodeTranslator.jar to "+jarFile);
                    addFilesToJar(jarFile, "cn1.override.", new File(byteCodeTranslatorJar));
                }
                if (javaAPIJar != null && !"".equals(javaAPIJar) && new File(javaAPIJar).exists()) {
                    this.log("Adding JavaAPI.jar to "+jarFile);
                    addFilesToJar(jarFile, "cn1.override.", new File(javaAPIJar));
                }
                
                if(appStoreBuild) {
                    if(certificate == null || !certificate.exists() || certificate.isDirectory()) {
                        certificate = getFilePrefs("codename1.ios.release.certificate", prefs);
                    }
                    if(provisioningProfile == null || !provisioningProfile.exists() || provisioningProfile.isDirectory()) {
                        provisioningProfile = getFilePrefs("codename1.ios.release.provision", prefs);
                    }
                    if(certPassword == null || certPassword.length() == 0) {
                        certPassword = prefs.get("codename1.ios.release.certificatePassword", certPassword);
                    }
                    if(pushCertificate == null || !pushCertificate.exists() || pushCertificate.isDirectory()) {
                        pushCertificate = getFilePrefs("codename1.ios.release.pushCertificate", prefs);
                    }
                    r.putArgument("ios.buildType", "release");
                } else {
                    r.putArgument("ios.buildType", "debug");
                    if(certificate == null || !certificate.exists() || certificate.isDirectory()) {
                        certificate = getFilePrefs("codename1.ios.debug.certificate", prefs);
                    }
                    if(provisioningProfile == null || !provisioningProfile.exists() || provisioningProfile.isDirectory()) {
                        provisioningProfile = getFilePrefs("codename1.ios.debug.provision", prefs);
                    }
                    if(certPassword == null || certPassword.length() == 0) {
                        certPassword = prefs.get("codename1.ios.debug.certificatePassword", certPassword);
                    }
                    if(pushCertificate == null || !pushCertificate.exists() || pushCertificate.isDirectory()) {
                        pushCertificate = getFilePrefs("codename1.ios.debug.pushCertificate", prefs);
                    }
                }
                if(provisioningProfile == null || certificate == null || certPassword == null || certPassword.length() == 0) {
                    if (!r.getArg("ios.buildForSimulator", "false").equals("true")) {
                        throw new BuildException("A certificate from Apple with the appropriate password is required for building an iOS native app!\n"
                                + "You can generate this certificate using our certificate wizard by right clicking on the project and following"
                                + "the steps. Notice you will need an Apple IO developer account.");  
                    }                     
                }
            } else {
                if(targetType.indexOf("android") > -1) {
                    if (certificate != null && certificate.exists() && !certificate.isDirectory()) {
                        System.out.println("Found certificate: "+certificate);
                    }
                    if(certificate == null || !certificate.exists() || certificate.isDirectory()) {
                        System.out.println("No certificate found for this project.  Looking for default certificate");
                        System.out.println("Project certificate: "+certificate);
                        certificate = getFilePrefs("codename1.android.keystore", prefs);
                    }
                    if(certPassword == null || certPassword.length() == 0) {
                        certPassword = prefs.get("codename1.android.keystorePassword", certPassword);
                    }
                    if(keystoreAlias == null || keystoreAlias.length() == 0) {
                        keystoreAlias = prefs.get("codename1.android.keystoreAlias", keystoreAlias);
                    }
                    if(certPassword == null || certPassword.length() == 0 || keystoreAlias == null || keystoreAlias.length() == 0 
                            || certificate == null) {
                        if(!showConfirm("Warning: Building for Android without a valid certificate will\n"
                                + "produce a temporary certificate that cannot be used for redistribution of\n"
                                + "applications. Its very easy and free to create an Android certificate,\n"
                                + " using Codename One Settings (right click on the project)", "Warning")) {
                            return;
                        }
                    }
                } else {
                    if(targetType.indexOf("rim") > -1) {
                        if(certificate == null || !certificate.exists() || certificate.isDirectory()) {
                            certificate = getFilePrefs("codename1.rim.signtoolDb", prefs);
                        }
                        if(provisioningProfile == null || !provisioningProfile.exists() || provisioningProfile.isDirectory()) {
                            provisioningProfile = getFilePrefs("codename1.rim.signtoolCsk", prefs);
                        }
                        if(certPassword == null || certPassword.length() == 0) {
                            certPassword = prefs.get("codename1.rim.certificatePassword", certPassword);
                        }
                        if(provisioningProfile == null || certificate == null || certPassword == null || certPassword.length() == 0) {
                            if(!showConfirm("<html>Warning: Building a blackberry native application without\n"
                                    + "a digital certificate is problematic due to the nature of the Blackberry\n"
                                    + "API. If you wish to continue Codename One will sign the application with\n"
                                    + "its own certificate and present a warning to users on application startup\n"
                                    + "(hence the application will be useful for debugging purposes only!). For\n"
                                    + "information on obtaining a certificate for BlackBerry development please "
                                    + "visit: http://www.codenameone.com/signing.html", "Warning")) {
                                return;
                            }
                        }
                    }                     
                }
            }
            
            if(certificate != null && certPassword != null && certificate.length() > 0 && certPassword.length() > 0) {
                validateFile(certificate, "certificate");
                validateString(certPassword, "certPassword");
                if(r.getType().indexOf("android") > -1) {
                    validateString(keystoreAlias, "keystoreAlias");
                        try {
                            r.setCertificate(certificate.getAbsolutePath());
                        } catch (IOException ex) {
                            throw new BuildException("Error reading the certificate file: " + certificate.getAbsolutePath());
                        }
                        r.setCertificatePassword(certPassword);
                        r.setKeystoreAlias(keystoreAlias);
                } else if (r.getType().indexOf("windows") > -1) {
                    try {
                        r.setCertificate(certificate.getAbsolutePath());
                    } catch (IOException ex) {
                        throw new BuildException("Error reading the certificate file: " + certificate.getAbsolutePath());
                    }
                    r.setCertificatePassword(certPassword);
                } else if (r.getType().indexOf("desktop_macosx") > -1) {
                    validateFile(certificate, "certificate");
                    validateString(certPassword, "certPassword");
                    try {
                        r.setCertificate(certificate.getAbsolutePath());
                        r.setCertificatePassword(certPassword);
                    } catch (IOException ex) {
                        throw new BuildException("Error reading the certificate file: " + certificate.getAbsolutePath());
                    }
                } else {
                    System.out.println("Checking provisioning profile for type "+r.getType());
                    validateFile(provisioningProfile, "provisioningProfile");
                    if(r.getType().indexOf("iphone") > -1 || r.getType().indexOf("rim") > -1) {
                        try {
                            r.setCertificate(certificate.getAbsolutePath());
                        } catch (IOException ex) {
                            throw new BuildException("Error reading the certificate file: " + certificate.getAbsolutePath());
                        }
                        r.setCertificatePassword(certPassword);
                        try {
                            r.setProvisioningProfile(provisioningProfile.getAbsolutePath());
                        } catch (IOException ex) {
                            throw new BuildException("Error reading the provisioning profile file: " + provisioningProfile.getAbsolutePath());
                        }
                    } else {
                        throw new BuildException("certificate, certPassword & provisioningProfile don't apply to this target: "+r.getType());
                    }
                }
            } else {
                if(r.getType().indexOf("android") > -1) {
                    System.out.println("You sent an android build without submitting a keystore. Notice that you will receive a build that is inappropriate for distribution (although it could be used for debugging purposes). For further details read http://www.codenameone.com/signing.html");
                }
                if(r.getType().indexOf("rim") > -1) {
                    System.out.println("You sent a blackberry build without submitting a certificate. "
                            + "Notice that you will receive a build signed by us with an explicit warning splash dialog!"
                            + "You must explicitly purchase and activate a certificate from RIM in order to properly"
                            + "distribute a blackberry application."
                            + "For further details read http://www.codenameone.com/signing.html");
                }
                if(r.getType().indexOf("iphone") > -1) {
                    if (!r.getArg("ios.buildForSimulator", "false").equals("true")) {
                        System.out.println("You sent an iphone build without submitting a certificate/provisioning profile. "
                                + "Notice that you will receive a build signed by us which will only work on a jailbroken phone"
                                + "and obviously can't be submitted to the appstore. We provide this as a testing option only"
                                + "and might stop providing this service if it is abused in the future.\n"
                                + "You must purchase a certificate from Apple in order to produce a working, valid build "
                                + "and define your provisioning profile using their tools.\n"
                                + "For further details read http://www.codenameone.com/signing.html");
                    }
                }
            }
                        
            if(pushCertificate != null && pushCertificate.exists()) {
                try {
                    r.setPushCertificate(pushCertificate.getAbsolutePath());
                } catch (IOException ex) {
                    throw new BuildException("Error reading the push certificate file: " + pushCertificate.getAbsolutePath());
                }                
            }

            System.out.println("Your build size is: " + (int)(jarFile.length()/1024) + "kb");
            System.out.println("Sending build request to the server, notice that the build might take a while to complete!");
            if(!automated) {
                System.out.println("Sending build to account: " + r.getUserName());
                if(r.getUserName() == null || r.getUserName().length() == 0) {
                    // fucking shit!!!
                    Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
                    p.remove("user");
                    p.remove("pass");
                    try {
                        p.flush();
                    } catch (BackingStoreException ex) {
                        ex.printStackTrace();
                    }
                    throw new BuildException("Account login failed please re-send build and enter account credentials when prompted to do so!");
                }
            }
            String id = b.sendRequestToServer(CodeNameOneBuildClient.SERVER_URL + "/build", jarFile, r, null, true);
            if(id != null) {
                if(id.equalsIgnoreCase("OLD")) {
                    System.out.println("**** WARNING!!! Your version of Codename One is out of date! Please upgrade! ****");
                } else {
                    if(id.equalsIgnoreCase("DEAD")) {
                        System.out.println("**** ERROR!!! Your version of Codename One is NO LONGER SUPPORTED! You MUST upgrade for builds to work! ****");
                    }
                }
            }
            
            System.out.println("Your build was submitted follow the status on: https://www.codenameone.com/build-server.html");

            if(automated) {
                int placeInQueue = -1;

                while(!b.pollResult(u + "/getData", userName, password, id)) {
                    if(placeInQueue != b.getPlaceInQueue()) {
                        placeInQueue = b.getPlaceInQueue();
                        //System.out.println("Your place in the build queue is: " + placeInQueue + " notice that it is subject to change depending on your subscription type/privilages");
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                    }
                }

                System.out.println("Fetching result");
                if(resultFile == null) {
                     resultFile = new File(jarFile.getParentFile(), "result.zip");
                }
                String res = b.fetchResult(u + "/getData", userName, password, id, resultFile);
                if(res != null) {
                    System.out.println("Error: " + res);
                } else {
                    System.out.println("Build Completed sucessfully!");
                }
            }
        } catch(IOException err) {
            err.printStackTrace();
            throw new BuildException("Error in server build process");
        }
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the jarFile
     */
    public File getJarFile() {
        return jarFile;
    }

    /**
     * @param jarFile the jarFile to set
     */
    public void setJarFile(File jarFile) {
        this.jarFile = jarFile;
    }
    
    
    private void addFilesToJar(File jarFile, String prefix, File... files) {
        try {
            Zip zip = (Zip)getProject().createTask("zip");
            zip.setDestFile(jarFile);
            zip.setUpdate(true);

            File tempDir = File.createTempFile("tem", "dir");
            tempDir.delete();
            tempDir.mkdir();
            try {

                for (File f : files) {
                    File copy = new File(tempDir, prefix+f.getName());
                    Copy cp = (Copy)getProject().createTask("copy");
                    cp.setFile(f);
                    cp.setTofile(copy);
                    cp.execute();
                    FileSet fs = new FileSet();
                    fs.setFile(copy);
                    zip.addFileset(fs);
                }
                zip.execute();
            } finally {
                for (File f : tempDir.listFiles()) {
                    f.delete();
                }
                tempDir.delete();
            }

            
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
        
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the mainClassName
     */
    public String getMainClassName() {
        return mainClassName;
    }

    /**
     * @param mainClassName the mainClassName to set
     */
    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @param packageName the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @return the version
     */
    public float getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(float version) {
        this.version = version;
    }

    /**
     * @return the icon
     */
    public File getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(File icon) {
        this.icon = icon;
    }

    /**
     * @return the targetType
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * @param targetType the targetType to set
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * @return the certificate
     */
    public File getCertificate() {
        return certificate;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(File certificate) {
        this.certificate = certificate;
    }

    /**
     * @return the certPassword
     */
    public String getCertPassword() {
        return certPassword;
    }
    
    public void setSigtoolDb(File s) {
        certificate = s;
    }
    
    public File getSigtoolDb() {
        return certificate;
    }

    public void setKeystore(File s) {
        certificate = s;
    }
    
    public File getKeystore() {
        return certificate;
    }

    public void setSigtoolCsk(File s) {
        provisioningProfile = s;
    }
    
    public File getSigtoolCsk() {
        return provisioningProfile;
    }

    /**
     * @param certPassword the certPassword to set
     */
    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

    /**
     * @return the provisioningProfile
     */
    public File getProvisioningProfile() {
        return provisioningProfile;
    }

    /**
     * @param provisioningProfile the provisioningProfile to set
     */
    public void setProvisioningProfile(File provisioningProfile) {
        this.provisioningProfile = provisioningProfile;
    }

    /**
     * @return the subtitle
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * @param subtitle the subtitle to set
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * @return the includeSource
     */
    public boolean isIncludeSource() {
        return includeSource;
    }

    /**
     * @param includeSource the includeSource to set
     */
    public void setIncludeSource(boolean includeSource) {
        this.includeSource = includeSource;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @param vendor the vendor to set
     */
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * @return the keystoreAlias
     */
    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    /**
     * @param keystoreAlias the keystoreAlias to set
     */
    public void setKeystoreAlias(String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
    }

    /**
     * @return the appid
     */
    public String getAppid() {
        return appid;
    }

    /**
     * @param appid the appid to set
     */
    public void setAppid(String appid) {
        this.appid = appid;
    }

    /**
     * @return the appStoreBuild
     */
    public boolean isAppStoreBuild() {
        return appStoreBuild;
    }

    /**
     * @param appStoreBuild the appStoreBuild to set
     */
    public void setAppStoreBuild(boolean appStoreBuild) {
        this.appStoreBuild = appStoreBuild;
    }

    /**
     * @return the automated
     */
    public boolean isAutomated() {
        return automated;
    }

    /**
     * @param automated the automated to set
     */
    public void setAutomated(boolean automated) {
        this.automated = automated;
    }

    /**
     * @return the resultFileName
     */
    public File getResultFile() {
        return resultFile;
    }

    /**
     * @param resultFile the resultFileName to set
     */
    public void setResultFile(File resultFile) {
        this.resultFile = resultFile;
    }

    /**
     * @return the production
     */
    public boolean isProduction() {
        return production;
    }

    /**
     * @param production the production to set
     */
    public void setProduction(boolean production) {
        this.production = production;
    }

    /**
     * @return the pushCertificate
     */
    public File getPushCertificate() {
        return pushCertificate;
    }

    /**
     * @param pushCertificate the pushCertificate to set
     */
    public void setPushCertificate(File pushCertificate) {
        this.pushCertificate = pushCertificate;
    }

    /**
     * @return the buildArgs
     */
    public String getBuildArgs() {
        return buildArgs;
    }

    /**
     * @param buildArgs the buildArgs to set
     */
    public void setBuildArgs(String buildArgs) {
        this.buildArgs = buildArgs;
    }
    
    
    
    private int getUserType(String user, String pass) throws IOException{
            URL u = new URL("https://codenameone.com/calls?m=login&email=" + URLEncoder.encode(user, "UTF-8") + 
                            "&password=" + URLEncoder.encode(pass, "UTF-8"));
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("User-Agent", CodeNameOneBuildClient.USER_AGENT);            
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuffer sb = new StringBuffer();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            String result = sb.toString();
            
            JsonObject jsonObject = JsonObject.readFrom( result );
            int type = jsonObject.get("type").asInt();
            return type;
    }

    public static int getAppSizeLimit(String user) throws IOException {
        URL u = new URL("https://cloud.codenameone.com/build/appSizeLimit?email=" + URLEncoder.encode(user, "UTF-8"));
        URLConnection urlConnection = u.openConnection();
        urlConnection.setRequestProperty("User-Agent", CodeNameOneBuildClient.USER_AGENT);
        InputStream is = urlConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);

        int numCharsRead;
        char[] charArray = new char[20];
        StringBuffer sb = new StringBuffer();
        while ((numCharsRead = isr.read(charArray)) > 0) {
            sb.append(charArray, 0, numCharsRead);
        }
        String result = sb.toString();
        return Integer.parseInt(result);
    }

    public static void showUpgradeDialog(final int appSize, final int limitSize) {
        if(!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        showUpgradeDialog(appSize, limitSize);
                    }
                });
            } catch(Exception err) {
                throw new BuildException("Quota size limit reached for free account");
            }
            return;
        }
        System.out.println("Upgrade your account at https://www.codenameone.com/pricing.html to increase the app size limit");
        throw new BuildException("Upgrade your account at https://www.codenameone.com/pricing.html to increase the app size limit\nAlternatively you can shrink your app size using these instructions https://www.codenameone.com/blog/shrinking-sizes-optimizing.html");
    }

}
