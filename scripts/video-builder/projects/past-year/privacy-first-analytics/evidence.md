# Evidence map

Source: `docs/website/content/blog/privacy-first-analytics.md`
Canonical: https://www.codenameone.com/blog/privacy-first-analytics/

## Thesis

A provider-neutral analytics facade where consent runs before transport

## Supported beats

- **The problem with the old service:** Codename One shipped a single AnalyticsService wired to Google Analytics v1. You called AnalyticsService.init with your tracking id, and that was the whole story. One vendor, one protocol, baked into the framework.
- **One facade, a provider SPI, a consent gate that runs first:** Analytics is the facade your app calls. You register one or more AnalyticsProvider implementations against it, and it fans every screen, event, setUserProperty and crash call out to all of them.
- **A tradeoff worth saying out loud:** A consent gate that is off by default means you will measure less than an always-on tracker does. Some users will decline, and their sessions will not show up in your reports.
- **The first-party backend: events, goals, and a console:** CodenameOneAnalyticsProvider is the one provider that needs no third-party account. It batches events to the Build Cloud, where they show up in an analytics console next to your builds.
- **The five providers, and mixing them:** Writing your own backend is small. Extend AbstractAnalyticsProvider, which gives every SPI method an empty body, and override only what you support.
- **Migrating from the old service:** The old AnalyticsService still works. It is deprecated, and it now delegates to the new API rather than carrying its own Google Analytics code, so existing apps keep running without a change. The UIBuilder analytics hook is unchanged too.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5266
- https://stats.example.com

## Independent problem evidence

- Google Consent Mode: https://developers.google.com/tag-platform/security/guides/consent — Google's consent mode changes storage and data-collection behavior according to the user's consent choices.
- Matomo Tracking Consent: https://developer.matomo.org/guides/tracking-consent — Matomo documents consent requirements and APIs for tracking and cookie consent rather than assuming collection is always allowed.

## Product proof

- `docs/website/static/blog/privacy-first-analytics/analytics-console-empty.png`
- `docs/website/static/blog/privacy-first-analytics/analytics-console-overview.png`
