package com.codename1.certificatewizard.project;

public interface AndroidKeystoreProvider {
    String defaultKeystore(String projectDir);
    void generate(String keystorePath, String alias, String password, String distinguishedName) throws Exception;
}
