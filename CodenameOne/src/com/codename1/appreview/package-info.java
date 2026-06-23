/// App review &amp; feedback engagement API.
///
/// [com.codename1.appreview.AppReview] requests the platform's native
/// "rate this app" prompt (Apple `SKStoreReviewController` / Google Play
/// In-App Review) when available and falls back to a Codename One drawn
/// rating widget elsewhere. It also offers an optional engagement scheduler
/// that decides when to ask for a review based on launch count and elapsed
/// days, and a smart feedback split that routes unhappy users to a private
/// feedback channel instead of a public store review.
package com.codename1.appreview;
