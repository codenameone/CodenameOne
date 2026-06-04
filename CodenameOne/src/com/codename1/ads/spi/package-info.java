/// Service provider interface (SPI) for the modern Codename One advertising
/// API. Ad network plugins (cn1libs) implement [com.codename1.ads.spi.AdProvider]
/// and ship an [com.codename1.ads.spi.AdProviderInstaller] so they are
/// discovered automatically. Application code never references these types; it
/// uses the facade and format classes in [com.codename1.ads].
package com.codename1.ads.spi;
