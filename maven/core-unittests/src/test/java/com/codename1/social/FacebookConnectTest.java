package com.codename1.social;

import com.codename1.facebook.FaceBookAccess;
import com.codename1.io.AccessToken;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class FacebookConnectTest extends UITestBase {

    private FacebookConnect connect;

    @BeforeEach
    void setUpFacebookConnect() {
        implementation.clearConnections();
        clearStoredCredentials();
        FaceBookAccess.setToken(null);
        connect = new FacebookConnect();
    }

    @AfterEach
    void tearDownFacebookConnect() {
        clearStoredCredentials();
        implementation.clearConnections();
        FaceBookAccess.setToken(null);
    }

    @FormTest
    void doLogoutClearsTokensAndInvokesGraphLogout() {
        AccessToken stored = new AccessToken("cached-token", null);
        connect.setAccessToken(stored);
        Preferences.set(FacebookConnect.class.getName() + "Token", "cached-token");
        FaceBookAccess.setToken("server-token");

        connect.doLogout();

        assertNull(connect.getAccessToken());
        assertNull(FaceBookAccess.getToken());
        assertNull(Preferences.get(FacebookConnect.class.getName() + "Token", null));

        TestConnection logoutRequest = implementation.getConnection("https://www.facebook.com/logout.php?access_token=server-token&confirm=1&next=https://www.codenameone.com/");
        assertNotNull(logoutRequest);
        assertFalse(logoutRequest.isPostRequest());
        assertTrue(logoutRequest.getUrl().contains("logout.php"));
    }

    @FormTest
    void doLogoutWhenNativeSupportedSkipsNetworkCall() {
        FaceBookAccess.setToken("native-token");
        NativeSupportedFacebookConnect nativeConnect = new NativeSupportedFacebookConnect();

        nativeConnect.doLogout();

        assertTrue(nativeConnect.logoutInvoked);
        assertEquals("native-token", FaceBookAccess.getToken());
        assertNull(implementation.getConnection("https://www.facebook.com/logout.php?access_token=native-token&confirm=1&next=https://www.codenameone.com/"));
    }

    @FormTest
    void getAccessTokenReturnsStoredTokenWhenAvailable() {
        AccessToken token = new AccessToken("stored-token", "refresh-token");
        connect.setAccessToken(token);

        assertSame(token, connect.getAccessToken());
    }

    @FormTest
    void getAccessTokenReturnsNativeTokenWhenSupported() {
        NativeTokenFacebookConnect nativeConnect = new NativeTokenFacebookConnect();

        AccessToken token = nativeConnect.getAccessToken();

        assertNotNull(token);
        assertEquals("native-value", token.getToken());
    }

    @FormTest
    void bridgeMethodsDelegateToDeprecatedImplementations() {
        BridgeFacebookConnect bridge = new BridgeFacebookConnect();

        bridge.nativelogin();
        bridge.nativeLogout();

        assertEquals(1, bridge.loginInvocations);
        assertEquals(1, bridge.logoutInvocations);
    }

    @FormTest
    void unsupportedOperationsThrowRuntimeExceptions() {
        assertThrows(RuntimeException.class, new FacebookConnect()::login);
        assertThrows(RuntimeException.class, new FacebookConnect()::logout);
        assertThrows(RuntimeException.class, new FacebookConnect()::getToken);
        assertThrows(RuntimeException.class, new FacebookConnect()::isLoggedIn);
        assertThrows(RuntimeException.class, () -> new FacebookConnect().askPublishPermissions(null));
        assertThrows(RuntimeException.class, new FacebookConnect()::hasPublishPermissions);
    }

    private void clearStoredCredentials() {
        Storage.getInstance().deleteStorageFile(FacebookConnect.class.getName() + "AccessToken");
        Preferences.delete(FacebookConnect.class.getName() + "Token");
    }

    private static final class NativeSupportedFacebookConnect extends FacebookConnect {
        private boolean logoutInvoked;

        @Override
        public boolean isFacebookSDKSupported() {
            return true;
        }

        @Override
        public void logout() {
            logoutInvoked = true;
        }
    }

    private static final class NativeTokenFacebookConnect extends FacebookConnect {
        @Override
        public boolean isFacebookSDKSupported() {
            return true;
        }

        @Override
        public String getToken() {
            return "native-value";
        }
    }

    private static final class BridgeFacebookConnect extends FacebookConnect {
        private int loginInvocations;
        private int logoutInvocations;

        @Override
        public void login() {
            loginInvocations++;
        }

        @Override
        public void logout() {
            logoutInvocations++;
        }
    }
}
