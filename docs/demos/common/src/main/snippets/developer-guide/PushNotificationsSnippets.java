// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::push-notifications-java-001[]
public class MyApplication implements PushCallback {

    // ....

    /**
     * Invoked when the push notification occurs
     *
     * @param value the value of the push notification
     */
    public void push(String value) {
        System.out.println("Received push message: "+value);
    }

    /**
     * Invoked when push registration is complete to pass the device ID to the application.
     *
     * @param deviceId OS native push ID you should not use this value and instead use <code>Push.getPushKey()</code>
     * @see Push#getPushKey()
     */
    public void registeredForPush(String deviceId) {
        System.out.println("The Push ID for this device is "+Push.getPushKey());
    }

    /**
     * Invoked to indicate an error occurred during registration for push notification
     * @param error descriptive error string
     * @param errorCode an error code
     */
    public void pushRegistrationError(String error, int errorCode) {
        System.out.println("An error occurred during push registration.");
    }
}
// end::push-notifications-java-001[]

// tag::push-notifications-java-002[]
public void push(String message) {
    if (isAudioMessage(message)) {
        // Play audio asynchronously
        playAudio(message, new Runnable() {
            public void run() {
                // Audio finished playing
                Display.getInstance().notifyPushCompletion();
            }
        });
    } else {
        // For standard messages, we can notify immediately or let it timeout (safest to notify)
        Display.getInstance().notifyPushCompletion();
    }
}
// end::push-notifications-java-002[]

// tag::push-notifications-java-003[]
public void push(String value) {
    System.out.println("Received push message: "+value);
}
// end::push-notifications-java-003[]

// tag::push-notifications-java-004[]
PushBuilder builder = new PushBuilder()
        .type(1)
        .body("Hello World")
        .imageUrl("https://example.com/myimage.jpg");
String payload = builder.build();
int pushType = builder.getType(); // Will be 99 when image/category metadata is present
// end::push-notifications-java-004[]

// tag::push-notifications-java-005[]
public void push(String message) {
    PushContent content = PushContent.get();
    if (content != null) {
        String title = content.getTitle();
        String body = content.getBody();
        String hiddenMeta = content.getMetaData();
        String imageUrl = content.getImageUrl();
        String replyText = content.getTextResponse();
        // Use these values (title/body/hiddenMeta/imageUrl/replyText) as needed in your application logic.
    }
}
// end::push-notifications-java-005[]

// tag::push-notifications-java-006[]
import com.codename1.push.PushAction;
import com.codename1.push.PushActionCategory;
import com.codename1.push.PushActionsProvider;
...

public class MyApplication implements PushCallback, PushActionsProvider {

    ...

    @Override
    public PushActionCategory[] getPushActionCategories() {
        return new PushActionCategory[]{
            new PushActionCategory("invite", new PushAction[]{
                new PushAction("yes", "Yes"),
                new PushAction("no", "No"),
                new PushAction("maybe", "Maybe"),
                new PushAction("reply", "Reply", null, "Type your response...", "Send")
            })

        };
    }
}
// end::push-notifications-java-006[]

// tag::push-notifications-java-007[]
public void push(String message) {
    PushContent content = PushContent.get();
    if (content != null) {
        if ("invite".equals(content.getCategory())) {
            if (content.getActionId() != null) {
                System.out.println("The user selected the "+content.getActionId()+" action");
                if (content.getTextResponse() != null) {
                    System.out.println("They replied: "+content.getTextResponse());
                }
            } else {
                System.out.println("The user clicked on the invite notification, but didn't select an action.");
            }
        }
    }
}
// end::push-notifications-java-007[]

// tag::push-notifications-java-008[]
private static final String PUSH_TOKEN = "********-****-****-****-*************";
// end::push-notifications-java-008[]

// tag::push-notifications-java-009[]
private static final String FCM_SERVER_API_KEY = "******************-********************";
// end::push-notifications-java-009[]

// tag::push-notifications-java-010[]
private static final boolean ITUNES_PRODUCTION_PUSH = false;
// end::push-notifications-java-010[]

// tag::push-notifications-java-011[]
private static final String ITUNES_PRODUCTION_PUSH_CERT = "https://domain.com/linkToP12Prod.p12";
private static final String ITUNES_PRODUCTION_PUSH_CERT_PASSWORD = "ProdPassword";
private static final String ITUNES_DEVELOPMENT_PUSH_CERT = "https://domain.com/linkToP12Dev.p12";
private static final String ITUNES_DEVELOPMENT_PUSH_CERT_PASSWORD = "DevPassword";
// end::push-notifications-java-011[]

// tag::push-notifications-java-012[]
String cert = ITUNES_DEVELOPMENT_PUSH_CERT;
String pass = ITUNES_DEVELOPMENT_PUSH_CERT_PASSWORD;
if(ITUNES_PRODUCTION_PUSH) {
 cert = ITUNES_PRODUCTION_PUSH_CERT;
 pass = ITUNES_PRODUCTION_PUSH_CERT_PASSWORD;
}

PushBuilder builder = new PushBuilder()
.type(102)
.badge(5)
.title("Hello World")
.body("you've 5 new tasks")
.metaData("taskListId=123");

new Push(PUSH_TOKEN, builder.build(), deviceKey)
.pushType(builder.getType())
.apnsAuth(cert, pass, ITUNES_PRODUCTION_PUSH)
.gcmAuth(FCM_SERVER_API_KEY)
.wnsAuth(WNS_SID, WNS_CLIENT_SECRET)
.send();
// end::push-notifications-java-012[]

// tag::push-notifications-java-013[]
Push.sendPushMessage(PUSH_TOKEN, "Hello World",
         ITUNES_PRODUCTION_PUSH, FCM_SERVER_API_KEY, cert, pass, 1, deviceKey);
// end::push-notifications-java-013[]

// tag::push-notifications-java-014[]
HttpURLConnection connection = (HttpURLConnection)new URL("https://push.codenameone.com/push/push").openConnection();
connection.setDoOutput(true);
connection.setRequestMethod("POST");
connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
String cert = ITUNES_DEVELOPMENT_PUSH_CERT;
String pass = ITUNES_DEVELOPMENT_PUSH_CERT_PASSWORD;
if(ITUNES_PRODUCTION_PUSH) {
 cert = ITUNES_PRODUCTION_PUSH_CERT;
 pass = ITUNES_PRODUCTION_PUSH_CERT_PASSWORD;
}
String query = "token=" + PUSH_TOKEN +
 "&device=" + URLEncoder.encode(deviceId1, "UTF-8") +
 "&device=" + URLEncoder.encode(deviceId2, "UTF-8") +
 "&device=" + URLEncoder.encode(deviceId3, "UTF-8") +
 "&type=1" +
 "&auth=" + URLEncoder.encode(FCM_SERVER_API_KEY, "UTF-8") +
 "&certPassword=" + URLEncoder.encode(pass, "UTF-8") +
 "&cert=" + URLEncoder.encode(cert, "UTF-8") +
 "&body=" + URLEncoder.encode(MESSAGE_BODY, "UTF-8") +
 "&production=" + ITUNES_PRODUCTION_PUSH +
 "&sid=" + URLEncoder.encode(WNS_SID, "UTF-8") +
 "&client_secret=" + URLEncoder.encode(WNS_CLIENT_SECRET, "UTF-8");
try (OutputStream output = connection.getOutputStream()) {
 output.write(query.getBytes("UTF-8"));
}
int c = connection.getResponseCode();
// read response JSON
// end::push-notifications-java-014[]
