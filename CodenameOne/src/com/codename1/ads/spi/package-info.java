/// Service provider interface (SPI) for the Codename One advertising API. Ad
/// network libraries implement [com.codename1.ads.spi.AdProvider] and register
/// it with [com.codename1.ads.AdManager] (typically from a static `install()`
/// method). Application code never references these types; it uses the facade
/// and format classes in [com.codename1.ads].
package com.codename1.ads.spi;
