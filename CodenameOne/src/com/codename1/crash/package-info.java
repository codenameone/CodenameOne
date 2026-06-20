/// On device crash protection client for Codename One applications.
///
/// `CrashProtection` installs handlers that capture unexpected failures and
/// assembles a `CrashReportPayload` describing the crash. A `PiiScrubber`
/// removes personally identifiable information from the payload before it is
/// delivered, so reports can be collected without leaking user data.
package com.codename1.crash;
