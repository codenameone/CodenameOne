/// The maps service-provider interface (SPI).
///
/// This package defines the contract that native map providers (Apple MapKit,
/// Google Maps, Bing, Huawei, ...) implement so that
/// [com.codename1.maps.NativeMap] can drive a native map peer without the core
/// framework depending on any provider SDK. Provider implementations are not
/// part of the core: they are injected into the app's
/// `com.codename1.maps` package by the build tooling when a `maps.provider`
/// build hint selects one, and they register themselves with
/// [com.codename1.maps.spi.MapProviderRegistry]. When no provider is present
/// `NativeMap` falls back to the pure-vector [com.codename1.maps.MapView].
package com.codename1.maps.spi;
