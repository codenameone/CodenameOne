/// Runtime support for build-time-transcoded SVG images.
///
/// The Codename One SVG transcoder (in `maven/svg-transcoder`) parses SVG
/// files at build time and emits an [Image] subclass per file that renders
/// the SVG via the [Graphics] shape API. Those generated classes extend
/// [GeneratedSVGImage], which lives in this package together with the small
/// runtime helpers needed for SMIL animation interpolation.
///
/// User code never references these classes directly -- generated SVGs appear
/// under their source filename in any [com.codename1.ui.util.Resources] that
/// has been wired up through the transcoder's generated registry.
package com.codename1.svg;
