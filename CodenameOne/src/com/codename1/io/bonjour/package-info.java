/// Bonjour / mDNS-SD service discovery and publication.
///
/// `BonjourBrowser` discovers peers advertising a given service type on the
/// local network, and `BonjourPublisher` announces a service from the
/// device. The native plumbing lives behind `BonjourPlatform`, which the
/// active port implements (NSNetService on iOS, NsdManager on Android).
package com.codename1.io.bonjour;
