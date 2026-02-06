/* 
    Document   : package
    Created on : Oct 11, 2007, 10:38:26 AM
    Author     : Shai Almog
*/

/// Look of the application can be fully customized via this package, it represents
/// a rendering layer that can be plugged in separately in runtime and themed to
/// provide any custom look. Unlike the Swing PLAF this layer does not support any
/// aspect of "feel" as in event handling etc. since these aspects would require a
/// much bigger and more elaborate layer unfit for small device OTA delivery.
///
/// Sizes of components are also calculated by the `com.codename1.ui.plaf.LookAndFeel`
/// since the size is very much affected by the look of the application e.g. the thickness
/// of the border and the font sizes.
package com.codename1.ui.plaf;
