package com.codename1.social;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;
import com.codename1.test.UITestBase;
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

class GoogleConnectTest extends UITestBase {

    private Field googleInstanceField;
    private Field networkManagerInstanceField;
    private NetworkManager originalNetworkManager;
    private NetworkManager mockNetworkManager;

    @BeforeEach
    void setUpEnvironment() throws Exception {
        resetGoogleSingleton();
        mockNetworkManager();
        clearStoredCredentials();
    }

    @AfterEach
    void tearDownEnvironment() throws Exception {
        restoreNetworkManager();
        clearStoredCredentials();
    }

    @Test
    void testGetInstanceReturnsSingletonWhenNoImplClass() {
        GoogleConnect first = GoogleConnect.getInstance();
        GoogleConnect second = GoogleConnect.getInstance();
        assertSame(first, second);
        assertEquals(GoogleConnect.class, first.getClass());
    }

    @Test
    void testGetInstanceUsesImplClass() throws Exception {
        resetGoogleSingleton();
        GoogleConnect.implClass = CustomGoogleConnect.class;
        GoogleConnect instance = GoogleConnect.getInstance();
        assertTrue(instance instanceof CustomGoogleConnect);
        assertTrue(((CustomGoogleConnect) instance).constructed);
    }

    @Test
    void testGetInstanceFallsBackWhenInstantiationFails() throws Exception {
        resetGoogleSingleton();
        GoogleConnect.implClass = ThrowingGoogleConnect.class;
        GoogleConnect instance = GoogleConnect.getInstance();
        assertEquals(GoogleConnect.class, instance.getClass());
    }

    @Test
    void testIsNativeLoginSupportedAlwaysFalse() {
        assertFalse(new GoogleConnect().isNativeLoginSupported());
    }

    @Test
    void testCreateOauth2IncludesOfflineAccessParameters() throws Exception {
        GoogleConnect connect = new GoogleConnect();
        connect.setClientId("client");
        connect.setClientSecret("secret");
        connect.setRedirectURI("redirect");
        connect.setScope("profile email");

        Method createOauth = GoogleConnect.class.getDeclaredMethod("createOauth2");
        createOauth.setAccessible(true);
        Oauth2 oauth = (Oauth2) createOauth.invoke(connect);
        assertNotNull(oauth);

        Field paramsField = Oauth2.class.getDeclaredField("additionalParams");
        paramsField.setAccessible(true);
        Hashtable params = (Hashtable) paramsField.get(oauth);
        assertEquals("force", params.get("approval_prompt"));
        assertEquals("offline", params.get("access_type"));

        assertEquals("https://accounts.google.com/o/oauth2/auth", getFieldValue(oauth, "oauth2URL"));
        assertEquals("client", getFieldValue(oauth, "clientId"));
        assertEquals("redirect", getFieldValue(oauth, "redirectURI"));
        assertEquals("profile email", getFieldValue(oauth, "scope"));
        assertEquals("secret", getFieldValue(oauth, "clientSecret"));
        assertEquals("https://www.googleapis.com/oauth2/v3/token", getFieldValue(oauth, "tokenRequestURL"));
    }

    @Test
    void testValidateTokenReturnsFalseOnAuthorizationError() throws Exception {
        GoogleConnect connect = new GoogleConnect();
        doAnswer(invocation -> {
            ConnectionRequest req = (ConnectionRequest) invocation.getArgument(0);
            req.setFailSilently(true);
            assertFalse(req.isPost());
            assertEquals("https://www.googleapis.com/plus/v1/people/me", req.getUrl());
            Hashtable headers = getHeaders(req);
            assertEquals("Bearer invalid", headers.get("Authorization"));
            Method handleError = req.getClass().getDeclaredMethod("handleErrorResponseCode", int.class, String.class);
            handleError.setAccessible(true);
            handleError.invoke(req, 401, "Unauthorized");
            return null;
        }).when(mockNetworkManager).addToQueueAndWait(any(ConnectionRequest.class));

        boolean valid = connect.validateToken("invalid");
        assertFalse(valid);
        verify(mockNetworkManager).addToQueueAndWait(any(ConnectionRequest.class));
    }

    @Test
    void testValidateTokenReturnsTrueWhenNoError() throws Exception {
        GoogleConnect connect = new GoogleConnect();
        final AtomicReference<ConnectionRequest> captured = new AtomicReference<ConnectionRequest>();
        doAnswer(invocation -> {
            ConnectionRequest req = (ConnectionRequest) invocation.getArgument(0);
            captured.set(req);
            return null;
        }).when(mockNetworkManager).addToQueueAndWait(any(ConnectionRequest.class));

        boolean valid = connect.validateToken("valid");
        assertTrue(valid);
        assertEquals("https://www.googleapis.com/plus/v1/people/me", captured.get().getUrl());
        Hashtable headers = getHeaders(captured.get());
        assertEquals("Bearer valid", headers.get("Authorization"));
    }

    private Object getFieldValue(Oauth2 oauth, String fieldName) throws Exception {
        Field field = Oauth2.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(oauth);
    }

    private Hashtable getHeaders(ConnectionRequest request) throws Exception {
        Field headersField = ConnectionRequest.class.getDeclaredField("userHeaders");
        headersField.setAccessible(true);
        Hashtable headers = (Hashtable) headersField.get(request);
        return headers == null ? new Hashtable() : headers;
    }

    private void resetGoogleSingleton() throws Exception {
        if (googleInstanceField == null) {
            googleInstanceField = GoogleConnect.class.getDeclaredField("instance");
            googleInstanceField.setAccessible(true);
        }
        googleInstanceField.set(null, null);
        GoogleConnect.implClass = null;
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
        com.codename1.io.Storage.getInstance().deleteStorageFile(GoogleConnect.class.getName() + "AccessToken");
        com.codename1.io.Preferences.delete(GoogleConnect.class.getName() + "Token");
    }

    private static class CustomGoogleConnect extends GoogleConnect {
        boolean constructed;

        CustomGoogleConnect() {
            constructed = true;
        }
    }

    private static class ThrowingGoogleConnect extends GoogleConnect {
        ThrowingGoogleConnect() {
            throw new RuntimeException("failure");
        }
    }
}
