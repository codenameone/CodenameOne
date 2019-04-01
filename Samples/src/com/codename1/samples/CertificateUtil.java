/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shannah
 */
class CertificateUtil {

    public static String getSHA1Fingerprint(X509Certificate cert) throws CertificateEncodingException {
        return getFingerprint(cert, "SHA1");
    }

    public static String getFingerprint(X509Certificate server, String type) throws CertificateEncodingException {
        try {
            byte[] encoded = server.getEncoded();
            MessageDigest sha1 = MessageDigest.getInstance(type);
            System.out.println("  Subject " + server.getSubjectDN());
            System.out.println("   Issuer  " + server.getIssuerDN());
            sha1.update(encoded);
            return bytesToHex(sha1.digest());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertificateUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}