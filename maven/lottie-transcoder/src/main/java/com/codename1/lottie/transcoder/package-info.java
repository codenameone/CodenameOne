/**
 * Build-time Lottie/Bodymovin JSON transcoder. Parses a Lottie animation
 * and produces a Codename One {@code GeneratedSVGImage} subclass by
 * lowering the Lottie document into the SVG model the existing transcoder
 * already knows how to render. The runtime registry, per-port wiring and
 * theme {@code url(...)} lookup are the SVG transcoder's -- nothing new
 * is wired at startup.
 */
package com.codename1.lottie.transcoder;
