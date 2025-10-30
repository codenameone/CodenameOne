package com.codename1.social;

import com.codename1.facebook.FaceBookAccess;
import com.codename1.io.AccessToken;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// TODO: Restore this with proper mocking
class FacebookConnectTest extends UITestBase {

    private Field facebookInstanceField;
    private Field networkManagerInstanceField;
    private NetworkManager originalNetworkManager;
    private NetworkManager mockNetworkManager;
    private String originalClientId;
    private String originalClientSecret;
    private String originalRedirectUri;
    private String[] originalPermissions;

    //@BeforeEach
    void setUpSingletons() throws Exception {
        resetFacebookInstance();
        captureFaceBookAccessDefaults();
        mockNetworkManager();
        clearStoredCredentials();
    }

    //@AfterEach
    void restoreEnvironment() throws Exception {
        restoreNetworkManager();
        restoreFaceBookAccessDefaults();
        clearStoredCredentials();
    }

    //@Test
    void testGetInstanceReturnsSingletonWhenNoImplClass() {
        FacebookConnect first = FacebookConnect.getInstance();
        FacebookConnect second = FacebookConnect.getInstance();
        assertSame(first, second);
        assertEquals(FacebookConnect.class, first.getClass());
    }

    //@Test
    void testGetInstanceUsesImplClass() throws Exception {
        resetFacebookInstance();
        FacebookConnect.implClass = CustomFacebookConnect.class;
        FacebookConnect instance = FacebookConnect.getInstance();
        assertTrue(instance instanceof CustomFacebookConnect);
        assertTrue(((CustomFacebookConnect) instance).constructed);
    }

    //@Test
    void testGetInstanceFallsBackWhenInstantiationFails() throws Exception {
        resetFacebookInstance();
        FacebookConnect.implClass = ThrowingFacebookConnect.class;
        FacebookConnect instance = FacebookConnect.getInstance();
        assertEquals(FacebookConnect.class, instance.getClass());
    }

    //@Test
    void testDoLogoutClearsTokensAndInvokesNetworkManager() throws Exception {
        FacebookConnect connect = new FacebookConnect();
        AccessToken stored = new AccessToken("stored", null);
        connect.setAccessToken(stored);
        FaceBookAccess.setToken("serverToken");

        final AtomicReference<ConnectionRequest> captured = new AtomicReference<ConnectionRequest>();
        doAnswer(invocation -> {
            ConnectionRequest req = (ConnectionRequest) invocation.getArgument(0);
            captured.set(req);
            return null;
        }).when(mockNetworkManager).addToQueueAndWait(any(ConnectionRequest.class));

        connect.doLogout();

        assertNull(connect.getAccessToken());
        assertNull(FaceBookAccess.getToken());
        assertNotNull(captured.get());
        assertFalse(captured.get().isPost());
        assertTrue(captured.get().getUrl().contains("logout.php"));
        verify(mockNetworkManager).addToQueueAndWait(any(ConnectionRequest.class));
    }

    //@Test
    void testDoLogoutSkipsFacebookAccessWhenNativeSupported() {
        FaceBookAccess.setToken("keepToken");
        NativeSupportedFacebookConnect connect = new NativeSupportedFacebookConnect();
        connect.doLogout();
        assertTrue(connect.logoutInvoked);
        assertEquals("keepToken", FaceBookAccess.getToken());
    }

    //@Test
    void testGetAccessTokenReturnsStoredTokenWhenPresent() {
        FacebookConnect connect = new FacebookConnect();
        AccessToken token = new AccessToken("value", "refresh");
        connect.setAccessToken(token);
        assertSame(token, connect.getAccessToken());
    }

    //@Test
    void testGetAccessTokenReturnsNativeTokenWhenSupported() {
        FacebookConnect connect = new FacebookConnect() {
            @Override
            public boolean isFacebookSDKSupported() {
                return true;
            }

            @Override
            public String getToken() {
                return "nativeValue";
            }
        };
        assertEquals("nativeValue", connect.getAccessToken().getToken());
    }

    //@Test
    void testCreateOauth2ConfiguresFaceBookAccess() throws Exception {
        FacebookConnect connect = new FacebookConnect();
        connect.setClientId("client");
        connect.setClientSecret("secret");
        connect.setRedirectURI("redirect");
        connect.setScope("scope");

        Method createOauth = FacebookConnect.class.getDeclaredMethod("createOauth2");
        createOauth.setAccessible(true);
        Oauth2 oauth = (Oauth2) createOauth.invoke(connect);
        assertNotNull(oauth);

        Field additionalParamsField = Oauth2.class.getDeclaredField("additionalParams");
        additionalParamsField.setAccessible(true);
        Hashtable params = (Hashtable) additionalParamsField.get(oauth);
        assertNotNull(params);
        assertTrue(params.containsKey("display"));

        assertEquals("client", getStaticFieldValue("clientId"));
        assertEquals("secret", getStaticFieldValue("clientSecret"));
        assertEquals("redirect", getStaticFieldValue("redirectURI"));
        assertArrayEquals(new String[]{"public_profile", "email", "user_friends"}, (String[]) getStaticFieldValue("permissions"));
    }

    //@Test
    void testBridgeMethodsDelegateToDeprecatedImplementations() {
        TestableFacebookConnect connect = new TestableFacebookConnect();
        connect.nativelogin();
        connect.nativeLogout();
        assertEquals(1, connect.loginCalls);
        assertEquals(1, connect.logoutCalls);
    }

    //@Test
    void testUnsupportedOperationsThrow() {
        FacebookConnect connect = new FacebookConnect();
        assertThrows(RuntimeException.class, connect::login);
        assertThrows(RuntimeException.class, connect::logout);
        assertThrows(RuntimeException.class, connect::getToken);
        assertThrows(RuntimeException.class, connect::isLoggedIn);
        assertThrows(RuntimeException.class, () -> connect.askPublishPermissions(null));
        assertThrows(RuntimeException.class, connect::hasPublishPermissions);
    }

    private void resetFacebookInstance() throws Exception {
        if (facebookInstanceField == null) {
            facebookInstanceField = FacebookConnect.class.getDeclaredField("instance");
            facebookInstanceField.setAccessible(true);
        }
        facebookInstanceField.set(null, null);
        FacebookConnect.implClass = null;
    }

    private void captureFaceBookAccessDefaults() throws Exception {
        originalClientId = (String) getStaticFieldValue("clientId");
        originalClientSecret = (String) getStaticFieldValue("clientSecret");
        originalRedirectUri = (String) getStaticFieldValue("redirectURI");
        originalPermissions = ((String[]) getStaticFieldValue("permissions")).clone();
    }

    private Object getStaticFieldValue(String name) throws Exception {
        Field field = FaceBookAccess.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private void restoreFaceBookAccessDefaults() throws Exception {
        setStaticField("clientId", originalClientId);
        setStaticField("clientSecret", originalClientSecret);
        setStaticField("redirectURI", originalRedirectUri);
        setStaticField("permissions", originalPermissions.clone());
        FaceBookAccess.setToken(null);
    }

    private void setStaticField(String name, Object value) throws Exception {
        Field field = FaceBookAccess.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private void mockNetworkManager() throws Exception {
        if (networkManagerInstanceField == null) {
            networkManagerInstanceField = NetworkManager.class.getDeclaredField("INSTANCE");
            networkManagerInstanceField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(networkManagerInstanceField, networkManagerInstanceField.getModifiers() & ~Modifier.FINAL);
        }
        originalNetworkManager = (NetworkManager) networkManagerInstanceField.get(null);
        mockNetworkManager = mock(NetworkManager.class);
        networkManagerInstanceField.set(null, mockNetworkManager);
    }

    private void restoreNetworkManager() throws Exception {
        if (networkManagerInstanceField != null) {
            networkManagerInstanceField.set(null, originalNetworkManager);
        }
    }

    private void clearStoredCredentials() {
        Storage.getInstance().deleteStorageFile(FacebookConnect.class.getName() + "AccessToken");
        Preferences.delete(FacebookConnect.class.getName() + "Token");
        FaceBookAccess.setToken(null);
    }

    private static class CustomFacebookConnect extends FacebookConnect {
        boolean constructed;

        CustomFacebookConnect() {
            constructed = true;
        }
    }

    private static class ThrowingFacebookConnect extends FacebookConnect {
        ThrowingFacebookConnect() {
            throw new RuntimeException("failure");
        }
    }

    private static class NativeSupportedFacebookConnect extends FacebookConnect {
        boolean logoutInvoked;

        @Override
        public boolean isFacebookSDKSupported() {
            return true;
        }

        @Override
        public void logout() {
            logoutInvoked = true;
        }
    }

    private static class TestableFacebookConnect extends FacebookConnect {
        int loginCalls;
        int logoutCalls;

        @Override
        public void login() {
            loginCalls++;
        }

        @Override
        public void logout() {
            logoutCalls++;
        }
    }
}
