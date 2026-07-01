// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::analytics-java-001[]
// Codename One's first-party service (reports into the cloud console)
Analytics.addProvider(new CodenameOneAnalyticsProvider());

// or a third-party backend, side by side
Analytics.addProvider(new GoogleAnalyticsProvider("G-XXXXXXXX", "API_SECRET"));
// end::analytics-java-001[]

// tag::analytics-java-002[]
Analytics.screen("Home", null);

Analytics.event(AnalyticsEvent.create("purchase")
        .category("commerce")
        .param("sku", "abc-123")
        .param("value", 9.99)
        .build());

Analytics.setUserProperty("plan", "pro");
Analytics.crash(throwable, "checkout failed", false);
// end::analytics-java-002[]

// tag::analytics-java-003[]
// Nothing is sent until this is called (opt-in is the default).
Analytics.setConsent(AnalyticsConsent.all());
// end::analytics-java-003[]

// tag::analytics-java-004[]
Analytics.setConsent(AnalyticsConsent.builder()
        .analytics(true)
        .crashReporting(true)
        .personalization(false)
        .build());
// end::analytics-java-004[]

// tag::analytics-java-005[]
Analytics.setConsent(AnalyticsConsent.all());
// end::analytics-java-005[]

// tag::analytics-java-006[]
Analytics.setConsent(AnalyticsConsent.builder().all().adStorage(false).build());
// end::analytics-java-006[]

// tag::analytics-java-007[]
Analytics.setConsentMode(ConsentMode.OPT_OUT);
// end::analytics-java-007[]

// tag::analytics-java-008[]
Analytics.resetClientId();
// end::analytics-java-008[]

// tag::analytics-java-009[]
Analytics.addProvider(new CodenameOneAnalyticsProvider());
// end::analytics-java-009[]

// tag::analytics-java-010[]
Analytics.addProvider(new GoogleAnalyticsProvider("G-XXXXXXXX", "API_SECRET"));
// end::analytics-java-010[]

// tag::analytics-java-011[]
Analytics.addProvider(new MatomoAnalyticsProvider("https://matomo.example.com", 1));
// end::analytics-java-011[]

// tag::analytics-java-012[]
Analytics.addProvider(new FirebaseAnalyticsProvider());
// end::analytics-java-012[]

// tag::analytics-java-013[]
Analytics.addProvider(new LoggingAnalyticsProvider());
// end::analytics-java-013[]

// tag::analytics-java-014[]
public class MyProvider extends AbstractAnalyticsProvider {
    @Override
    public String getName() {
        return "my-backend";
    }

    @Override
    public void trackScreen(String name, String referrer) {
        // send to your backend
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return capability == AnalyticsCapability.SCREEN_VIEWS;
    }
}
// end::analytics-java-014[]
