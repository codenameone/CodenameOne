/// Pop-navigation and deep-link routing support.
///
/// The application-facing surface is intentionally small: declare deep-linkable
/// forms with `com.codename1.annotations.Route`, intercept back navigation
/// with `com.codename1.ui.Form#setPopGuard(PopGuard)`, and let the framework
/// wire the URL plumbing through generated code under
/// `com.codename1.router.generated`.
package com.codename1.router;
