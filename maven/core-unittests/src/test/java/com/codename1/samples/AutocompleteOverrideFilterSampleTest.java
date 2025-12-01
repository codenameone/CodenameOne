package com.codename1.samples;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.processing.Result;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import com.codename1.ui.AutoCompleteTextField;
import com.codename1.ui.TextField;
import com.codename1.ui.list.DefaultListModel;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteOverrideFilterSampleTest extends UITestBase {

    private static final String AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";

    @BeforeEach
    void clearConnections() {
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        implementation.setConnectionResponseProvider(null);
    }

    @FormTest
    void overrideFilterPopulatesModelFromNetworkResponse() {
        final byte[] payload = createPlacesResponse("Paris, France", "Parma, Italy");
        implementation.setConnectionResponseProvider(new Function<String, byte[]>() {
            public byte[] apply(String url) {
                if (url != null && url.startsWith(AUTOCOMPLETE_URL)) {
                    return payload;
                }
                return null;
            }
        });

        DefaultListModel<String> options = new DefaultListModel<String>();
        TextField apiKeyField = new TextField("unit-test-key");
        FilteringAutoComplete field = new FilteringAutoComplete(options, apiKeyField);

        assertTrue(field.applyFilter("par"));
        assertEquals(2, options.getSize());
        assertEquals("Paris, France", options.getItemAt(0));
        assertEquals("Parma, Italy", options.getItemAt(1));

        TestConnection connection = findExecutedConnection(AUTOCOMPLETE_URL);
        assertNotNull(connection, "Network call should be executed for autocomplete");
        assertTrue(connection.getUrl().contains("input=par"));
        assertTrue(connection.getUrl().contains("key=unit-test-key"));

        assertFalse(implementation.getQueuedRequests().isEmpty(), "ConnectionRequest should be enqueued during filter");
    }

    private TestConnection findExecutedConnection(String baseUrl) {
        for (TestConnection connection : implementation.getConnections()) {
            if (connection.getUrl() != null && connection.getUrl().startsWith(baseUrl)) {
                if (connection.isReadRequested() || connection.isWriteRequested()) {
                    return connection;
                }
            }
        }
        return null;
    }

    private byte[] createPlacesResponse(String first, String second) {
        StringBuilder json = new StringBuilder();
        json.append("{\"predictions\":[{");
        json.append("\"description\":\"").append(first).append("\"},{");
        json.append("\"description\":\"").append(second).append("\"}]}");
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static class FilteringAutoComplete extends AutoCompleteTextField {
        private final DefaultListModel<String> options;
        private final TextField apiKey;

        FilteringAutoComplete(DefaultListModel<String> options, TextField apiKey) {
            super(options);
            this.options = options;
            this.apiKey = apiKey;
        }

        boolean applyFilter(String text) {
            return filter(text);
        }

        @Override
        protected boolean filter(String text) {
            if (text == null || text.length() == 0) {
                return false;
            }
            String[] suggestions = searchLocations(text);
            if (suggestions == null || suggestions.length == 0) {
                return false;
            }

            options.removeAll();
            for (String suggestion : suggestions) {
                options.addItem(suggestion);
            }
            return true;
        }

        private String[] searchLocations(String text) {
            try {
                ConnectionRequest request = new ConnectionRequest();
                request.setPost(false);
                request.setUrl(AUTOCOMPLETE_URL);
                request.addArgument("key", apiKey.getText());
                request.addArgument("input", text);
                NetworkManager.getInstance().addToQueueAndWait(request);
                byte[] data = request.getResponseData();
                if (data == null) {
                    return null;
                }
                Map<String, Object> parsed = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8));
                return Result.fromContent(parsed).getAsStringArray("//description");
            } catch (Exception e) {
                return null;
            }
        }
    }
}
