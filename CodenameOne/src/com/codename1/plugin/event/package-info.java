/// Typed events dispatched to plugins.
///
/// `PluginEvent<T>` is the common base; concrete events such as
/// `OpenGalleryEvent` and `IsGalleryTypeSupportedEvent` describe specific
/// extension points and carry the data a plugin needs to handle (or
/// decline) them.
package com.codename1.plugin.event;
