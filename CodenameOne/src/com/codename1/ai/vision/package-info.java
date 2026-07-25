/**
 * Vendor-neutral on-device vision APIs for still images and live camera frames.
 *
 * <p>The automatic backend uses Apple Vision/Core Image on iOS and Mac
 * Catalyst and ML Kit on Android. Unsupported ports report that through
 * {@code isSupported()}. Optional backends are selected with
 * {@link com.codename1.ai.vision.VisionBackends}.</p>
 *
 * <p>Each analyzer is a separate build-time feature. Referencing one analyzer
 * causes the builder to retain only its platform adapter and native
 * dependency. {@link com.codename1.ai.vision.VisionPipeline} safely copies
 * callback-owned camera frames and drops stale pending frames under load.</p>
 */
package com.codename1.ai.vision;
