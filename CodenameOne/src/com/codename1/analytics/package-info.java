/// The analytics API tracks how your application is used in the field through a
/// generic provider SPI. Register one or more {@link com.codename1.analytics.AnalyticsProvider}
/// implementations with the {@link com.codename1.analytics.Analytics} facade and
/// report screen views, events, user properties and crashes; the facade fans
/// each call out to every provider once the relevant consent has been granted.
///
/// Built-in providers include the first-party
/// {@link com.codename1.analytics.CodenameOneAnalyticsProvider} (reporting into
/// the Codename One cloud, with capabilities gated by your subscription tier),
/// {@link com.codename1.analytics.GoogleAnalyticsProvider} (Google Analytics 4),
/// {@link com.codename1.analytics.MatomoAnalyticsProvider} (privacy-first, self
/// hostable), {@link com.codename1.analytics.FirebaseAnalyticsProvider} and the
/// {@link com.codename1.analytics.LoggingAnalyticsProvider} used in the
/// simulator and tests.
///
/// Consent is configurable and defaults to opt-in
/// ({@link com.codename1.analytics.ConsentMode#OPT_IN}): nothing is collected or
/// transmitted until the application records a choice via
/// {@link com.codename1.analytics.Analytics#setConsent(com.codename1.analytics.AnalyticsConsent)},
/// helping you comply with GDPR / CCPA. The pseudonymous client id is not
/// derived from any hardware identifier and can be cleared with
/// {@link com.codename1.analytics.Analytics#resetClientId()} to honour erasure
/// requests.
///
/// The previous {@link com.codename1.analytics.AnalyticsService} entry point is
/// retained, deprecated, and now delegates to this API.
package com.codename1.analytics;
