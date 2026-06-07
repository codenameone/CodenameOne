/// JavaScript-style `Promise` for asynchronous Codename One code.
///
/// `Promise` mirrors the ECMAScript Promise contract (`then`, `catch`,
/// `finally`, `all`, `race`, `resolve`, `reject`) on top of Codename One's
/// EDT and `invokeAndBlock` model. `Functor` and `ExecutorFunction` are
/// the small functional interfaces the API hands callbacks to so it can
/// remain Java 5 source-compatible.
package com.codename1.util.promise;
