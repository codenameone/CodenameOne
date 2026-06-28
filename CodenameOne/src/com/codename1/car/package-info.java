/// Portable in-car UI for Apple CarPlay and Google Android Auto. Apps describe their car experience
/// once with a small catalogue of driver-safe templates (`CarListTemplate`, `CarGridTemplate`,
/// `CarPaneTemplate`, `CarMessageTemplate`, `CarNavigationTemplate`, `CarNowPlayingTemplate`) and
/// Codename One renders them as native `CPTemplate`s on CarPlay and `androidx.car.app` templates on
/// Android Auto.
///
/// Register a `CarApplication` with `Car#setApplication(CarApplication)` from your app's `init()`.
/// The car platforms are template-based, so you cannot show a regular Codename One `Form` on the
/// head unit -- use the template catalogue. On the simulator and on ports without in-car projection
/// the API is an inert no-op and `Car#isCarConnected()` returns false, so application code needs no
/// platform conditionals.
///
/// Merely referencing this package makes the build wire the native plumbing (the CarPlay scene and
/// entitlement on iOS, the `androidx.car.app` dependency and `CarAppService` on Android); apps that
/// never use it pay nothing. See the "In-Car Experiences" chapter of the developer guide.
package com.codename1.car;
