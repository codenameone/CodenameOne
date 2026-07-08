package com.codename1.certificatewizard;

import com.codename1.certificatewizard.api.MockSigningService;
import com.codename1.certificatewizard.api.SigningService;
import com.codename1.certificatewizard.api.SigningState;
import com.codename1.certificatewizard.api.WizardDecisions;
import com.codename1.certificatewizard.project.ProjectBinding;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CertificateWizardModelTest {
    @Test
    void profileRulesMatchAppleProfileTypes() {
        assertFalse(WizardDecisions.profileRequiresDevices("IOS_APP_STORE"));
        assertTrue(WizardDecisions.profileRequiresDevices("IOS_APP_ADHOC"));
        assertTrue(WizardDecisions.profileRequiresDevices("IOS_APP_DEVELOPMENT"));
        assertTrue(WizardDecisions.profileRequiresDevices("MAC_APP_DEVELOPMENT"));
        assertEquals("IOS_DISTRIBUTION", WizardDecisions.requiredCertificateType("IOS_APP_STORE"));
        assertEquals("IOS_DEVELOPMENT", WizardDecisions.requiredCertificateType("IOS_APP_DEVELOPMENT"));
        assertEquals("MAC_APP_DISTRIBUTION", WizardDecisions.requiredCertificateType("MAC_APP_STORE"));
        assertEquals("DEVELOPER_ID_APPLICATION", WizardDecisions.requiredCertificateType("MAC_APP_DIRECT"));
    }

    @Test
    void compatibleCertificatesMustBeExportableForAutoSetupReuse() {
        long now = System.currentTimeMillis();
        List<SigningState.Certificate> certs = new ArrayList<SigningState.Certificate>();
        certs.add(new SigningState.Certificate(1L, "APPLE_EXISTING", "IOS_DISTRIBUTION",
                "Existing App Store Certificate", "SER1", now + 300L * 86400000L, "ACTIVE", false));
        certs.add(new SigningState.Certificate(2L, "APPLE_EXPORTABLE", "IOS_DISTRIBUTION",
                "Exportable App Store Certificate", "SER2", now + 300L * 86400000L, "ACTIVE", true));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                certs, null, null, null, null);

        List<SigningState.Certificate> compatible = WizardDecisions.compatibleCertificates(state, "IOS_APP_STORE");

        assertEquals(1, compatible.size());
        assertEquals("APPLE_EXPORTABLE", compatible.get(0).appleCertId());
    }

    @Test
    void createProfileValidationRequiresDevicesOnlyWhenNeeded() {
        List<String> certs = new ArrayList<String>();
        certs.add("CERT1");
        assertTrue(WizardDecisions.canCreateProfile("IOS_APP_STORE", "BID1", certs, null, "Store"));
        assertFalse(WizardDecisions.canCreateProfile("IOS_APP_ADHOC", "BID1", certs, null, "Adhoc"));
        List<String> devices = new ArrayList<String>();
        devices.add("DEV1");
        assertTrue(WizardDecisions.canCreateProfile("IOS_APP_ADHOC", "BID1", certs, devices, "Adhoc"));
        assertFalse(WizardDecisions.canCreateProfile("IOS_APP_STORE", "BID1", certs, devices, ""));
    }

    @Test
    void projectBindingParsesDescriptor() {
        ProjectBinding b = ProjectBinding.parse("projectDir=/p\nsettings=/p/codenameone_settings.properties\n"
                + "outputDir=/tmp/certs\nuser=a@b.com\ntoken=secret\nbaseUrl=https://example.com\n");
        assertTrue(b.isValid());
        assertEquals("/p", b.projectDir());
        assertEquals("/p/codenameone_settings.properties", b.settings());
        assertEquals("/tmp/certs", b.outputDir());
        assertEquals("a@b.com", b.user());
        assertEquals("secret", b.token());
        assertEquals("https://example.com", b.baseUrl());
    }

    @Test
    void mockServiceMutationsUpdateSnapshot() {
        MockSigningService service = new MockSigningService();
        final SigningState[] before = new SigningState[1];
        service.refresh(r -> before[0] = r.value);
        int certCount = before[0].certificates.size();
        service.createCertificate("IOS_DISTRIBUTION", "New Dist", r -> assertTrue(r.ok));
        final SigningState[] after = new SigningState[1];
        service.refresh(r -> after[0] = r.value);
        assertEquals(certCount + 1, after[0].certificates.size());

        service.createBundleId("com.example.newapp", "New App", true, r -> assertTrue(r.ok));
        service.registerDevice("QA", "00008120-000A1C3E0C68201E", r -> assertTrue(r.ok));
        service.refresh(r -> after[0] = r.value);
        assertTrue(after[0].bundleIds.size() >= 3);
        assertTrue(after[0].devices.size() >= 3);

        service.clearSigningData(r -> assertTrue(r.ok));
        service.refresh(r -> after[0] = r.value);
        assertFalse(after[0].credential.configured());
        assertTrue(after[0].certificates.isEmpty());
    }

    @Test
    void noTokenRefreshDoesNotEraseCachedCredentialState() {
        assertTrue(CertificateWizard.shouldPreserveCachedCredentialState("", true, SigningState.empty()));
        assertFalse(CertificateWizard.shouldPreserveCachedCredentialState("jwt", true, SigningState.empty()));
        assertFalse(CertificateWizard.shouldPreserveCachedCredentialState("", false, SigningState.empty()));
    }

    @Test
    void blankServerCredentialMetadataKeepsCachedDisplayValues() {
        SigningState current = new SigningState(new SigningState.Credential(true, "ABC123XYZ", "issuer-1"),
                null, null, null, null, null);
        SigningState refreshed = new SigningState(new SigningState.Credential(true, "", ""),
                null, null, null, null, null);

        SigningState merged = CertificateWizard.preserveCachedCredentialDetails(current, refreshed);

        assertTrue(merged.credential.configured());
        assertEquals("ABC123XYZ", merged.credential.keyId());
        assertEquals("issuer-1", merged.credential.issuerId());
    }

    @Test
    void cloudServiceUsesGeneratedClientForClearSigningData() throws Exception {
        Path sourcePath = Paths.get("src/main/java/com/codename1/certificatewizard/api/CloudSigningService.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Paths.get("../common/src/main/java/com/codename1/certificatewizard/api/CloudSigningService.java");
        }
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        assertTrue(source.contains("credentialApi.clearSigningData(bearerToken"));
        assertFalse(source.contains("Rest.delete(baseUrl + \"/appsec/7.0/apple/signing-data\")"));
    }
}
