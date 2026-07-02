// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::biometric-authentication-java-001[]
import com.codename1.security.Biometrics;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;

Biometrics b = Biometrics.getInstance();
if (!b.canAuthenticate()) {
    // Fall back to password
    return;
}
b.authenticate("Unlock your account").onResult((success, err) -> {
    if (err != null) {
        BiometricError code = ((BiometricException) err).getError();
        switch (code) {
            case USER_CANCELED: /* user dismissed the prompt */ break;
            case LOCKED_OUT:    /* too many bad attempts */ break;
            case NOT_ENROLLED:  /* prompt the user to enrol in Settings */ break;
            default:            /* generic failure */ break;
        }
    } else {
        // Authenticated. Continue with the gated action.
    }
});
// end::biometric-authentication-java-001[]

// tag::biometric-authentication-java-002[]
import com.codename1.security.SecureStorage;

// Write (Android prompts; iOS does not unless ios.Fingerprint.addPassword.prompt=true)
SecureStorage.getInstance().set("Save your token", "user@example.com", token);

// Read (prompts on both platforms)
SecureStorage.getInstance().get("Unlock your token", "user@example.com")
    .onResult((value, err) -> {
        if (err == null) {
            // Use value
        }
    });
// end::biometric-authentication-java-002[]

// tag::biometric-authentication-java-003[]
SecureStorage.getInstance().setKeychainAccessGroup("TEAMID123.group.com.example.app");
// end::biometric-authentication-java-003[]

// tag::biometric-authentication-java-004[]
import com.codename1.security.AuthenticationOptions;

Biometrics.getInstance().authenticate(new AuthenticationOptions()
    .setReason("Authorize transfer")                // iOS localizedReason; Android title fallback
    .setTitle("Confirm payment")                    // Android only
    .setSubtitle("Stripe charge $25.00")            // Android only
    .setNegativeButtonText("Cancel")                // Android only
    .setBiometricOnly(true)                         // reject PIN / passcode fallback
    .setSensitiveTransaction(true)                  // request class-3 ("strong") biometric (Android 30+)
);
// end::biometric-authentication-java-004[]
