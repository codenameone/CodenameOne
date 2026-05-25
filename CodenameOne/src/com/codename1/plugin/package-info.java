/// Plugin SPI for extending core framework behaviour without subclassing.
///
/// A `Plugin` is an `ActionListener<PluginEvent>` registered through
/// `PluginSupport`; core code fires typed `PluginEvent` instances (see
/// [com.codename1.plugin.event]) at well-defined extension points and the
/// first plugin to consume the event takes over the default behaviour --
/// for example, swapping the built-in image picker for a custom gallery.
package com.codename1.plugin;
