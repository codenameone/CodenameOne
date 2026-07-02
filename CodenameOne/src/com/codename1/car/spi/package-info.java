/// Internal service-provider interface for the `com.codename1.car` in-car API. The single
/// `CarBridge` interface is implemented by each platform port to render the portable template tree
/// onto the native head unit (Apple CarPlay / Google Android Auto). Application code does not use
/// this package directly -- it drives the public `com.codename1.car` API, which obtains the bridge
/// from the platform implementation.
package com.codename1.car.spi;
