# Evidence map

Source: `docs/website/content/blog/automated-store-submissions.md`
Canonical: https://www.codenameone.com/blog/automated-store-submissions/

## Thesis

How versioned metadata turns mobile store submission into a repeatable build step

## Supported beats

- **After The Build Turns Green:** A release isn't done when the build turns green. Someone still uploads the .ipa to App Store Connect, pastes the what's-new text into two or three web consoles, re-uploads screenshots because the store flagged one, and repeats the ritual per locale.
- **Your Listing Is Code Now:** The part I like most is cn1:metadata-push. Your store listing, the description, subtitle, keywords, what's-new text and screenshots, becomes a folder of plain files in your repo.
- **Beyond Google Play:** Here's the narrative part, and we might as well say it directly. Google ships an app store and an app framework, and its tooling naturally treats Play as the finish line. We don't have a store, so we have no stake in which one wins.
- **Organization Accounts:** Build cloud accounts can now belong to an organization. Members share visibility into the apps the team builds, the analytics and crash reports around them, and common data like the submission credentials above, while builds and quotas remain individual.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5353

## Independent problem evidence

- Add a new app: https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app/ — The app record must exist before a build is uploaded, and listing assets follow their own workflow.
- Google Play Developer API: https://developers.google.com/android-publisher/api-ref/rest — The Android Publisher API manages bundles, listings, images, tracks, and releases as separate resources.

## Product proof

- `docs/website/static/blog/automated-store-submissions/submission-store-credentials.png`
- `docs/website/static/blog/automated-store-submissions/org.png`
- `docs/website/static/blog/automated-store-submissions/delete.png`
