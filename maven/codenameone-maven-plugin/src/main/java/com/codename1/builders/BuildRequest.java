/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.builders;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Shai Almog
 */
public class BuildRequest implements java.io.Externalizable {
    static final long serialVersionUID = 42L;

    private static final int VERSION = 5;
    private String userName;
    private String password;
    private String packageName;
    private String mainClass;
    private String displayName;
    private String type;
    private String version;
    private String vendor;
    private byte[] icon;
    private byte[] certificate;
    private byte[] pushCertificate;
    private byte[] provisioningProfile;
    private String certificatePassword;
    private String subTitle;
    private boolean includeSource;
    private String keystoreAlias = "";
    private String appid;
    private boolean production;
    private String certificateName;
    private Map<String, String> args = new HashMap<String, String>();
    
    private void writeArr(ObjectOutput oo, byte[] b) throws IOException {
        if(b != null) {
            oo.writeInt(b.length);
            oo.write(b);
        } else {
            oo.writeInt(0);
        }
    }
    
    public void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeInt(VERSION);
        oo.writeUTF(userName);
        oo.writeUTF(password);
        oo.writeUTF(packageName);
        oo.writeUTF(mainClass);
        oo.writeUTF(displayName);
        oo.writeUTF(type);
        oo.writeUTF(version);
        writeArr(oo, icon);
        writeArr(oo, certificate);
        if(certificate != null) {
            oo.writeUTF(certificatePassword);
        }
        writeArr(oo, provisioningProfile);
        oo.writeUTF(vendor);
        oo.writeUTF(subTitle);
        oo.writeBoolean(includeSource);
        oo.writeUTF(keystoreAlias);
        oo.writeUTF(appid);
        writeArr(oo, pushCertificate);
        oo.writeBoolean(production);
        oo.writeInt(args.size());
        for(String k : args.keySet()) {
            oo.writeUTF(k);
            oo.writeUTF(args.get(k));
        }
    }

    private byte[] readArr(ObjectInput oi) throws IOException {
        int i = oi.readInt();
        if(i == 0) {
            return null;
        } else {
            byte[] b = new byte[i];
            oi.readFully(b);
            return b;
        }
    }
    
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        int ver = oi.readInt();
        userName = oi.readUTF();
        password = oi.readUTF();
        packageName = oi.readUTF();
        mainClass = oi.readUTF();
        displayName = oi.readUTF();
        type = oi.readUTF();
        version = oi.readUTF();
        icon = readArr(oi);
        certificate = readArr(oi);
        certificatePassword = null;
        if(certificate != null) {
            certificatePassword = oi.readUTF();
        }
        provisioningProfile = readArr(oi);
        if(ver > 1) {
            vendor = oi.readUTF();
            subTitle = oi.readUTF();
            includeSource = oi.readBoolean();
            if(ver > 2) {
                keystoreAlias = oi.readUTF();
                appid = oi.readUTF();
                if(ver > 3) {
                    pushCertificate = readArr(oi);
                    production = oi.readBoolean();
                    if(ver > 4) {
                        int count = oi.readInt();
                        args.clear();
                        for(int iter = 0 ; iter < count ; iter++) {
                            args.put(oi.readUTF(), oi.readUTF());
                        }
                        
                    }
                }
            }
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
     * @return the mainClass
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * @param mainClass the mainClass to set
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
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
     * @return the icon
     */
    public byte[] getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public void setIcon(String icon) throws IOException {
        this.icon = readFile(icon);
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the certificate
     */
    public byte[] getCertificate() {
        return certificate;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    private byte[] readFile(String s) throws IOException {
        File f = new File(s);
        DataInputStream di = new DataInputStream(new FileInputStream(f));
        byte[] b = new byte[(int)f.length()];
        di.readFully(b);
        di.close();
        return b;
    }
    
    public void setCertificate(String certificate) throws IOException {
        this.certificate = readFile(certificate);
    }
    /**
     * @return the provisioningProfile
     */
    public byte[] getProvisioningProfile() {
        return provisioningProfile;
    }

    /**
     * @param provisioningProfile the provisioningProfile to set
     */
    public void setProvisioningProfile(byte[] provisioningProfile) {
        this.provisioningProfile = provisioningProfile;
    }

    public void setProvisioningProfile(String provisioningProfile) throws IOException {
        this.provisioningProfile = readFile(provisioningProfile);
    }

    /**
     * @return the certificatePassword
     */
    public String getCertificatePassword() {
        return certificatePassword;
    }

    /**
     * @param certificatePassword the certificatePassword to set
     */
    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
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
     * @return the subTitle
     */
    public String getSubTitle() {
        return subTitle;
    }

    /**
     * @param subTitle the subTitle to set
     */
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
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
     * @return the pushCertificate
     */
    public byte[] getPushCertificate() {
        return pushCertificate;
    }

    /**
     * @param pushCertificate the pushCertificate to set
     */
    public void setPushCertificate(byte[] pushCertificate) {
        this.pushCertificate = pushCertificate;
    }

    public void setPushCertificate(String certificate) throws IOException {
        this.pushCertificate = readFile(certificate);
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
    
    public void putArgument(String key, String arg) {
        args.put(key, arg);
    }
    
    public Set<String> getArgs() {
        HashSet<String> out = new HashSet<String>();
        out.addAll(args.keySet());
        return out;
    }
    
    public String getArg(String key, String defaultVal) {
        String k = args.get(key);
        if(k == null) {
            k = defaultVal;
        }
        if (k != null) {
            // Support variables in args
            StringBuffer resultString = new StringBuffer();
            Pattern regex = Pattern.compile("\\$\\{(var\\.[^}]+)\\}");
            Matcher regexMatcher = regex.matcher(k);
            while (regexMatcher.find()) {
                String varName = regexMatcher.group(1);
                String varDefault = "";
                if (varName.contains(":")) {
                    varDefault = varName.substring(varName.indexOf(":")+1);
                    varName = varName.substring(0, varName.indexOf(":"));
                } else {
                    varDefault = regexMatcher.group(0);
                }
                String replacement = replacement = getArg(varName, null);
                if (replacement == null) {
                    regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(varDefault));
                } else {
                    regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(replacement));
                }
                

            } 
            regexMatcher.appendTail(resultString);
            k = resultString.toString();
        }
        return k;
    }

    /**
     * @return the certificateName
     */
    public String getCertificateName() {
        return certificateName;
    }

    /**
     * @param certificateName the certificateName to set
     */
    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }
}
