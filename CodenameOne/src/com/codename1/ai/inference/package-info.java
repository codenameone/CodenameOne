/**
 * Reusable on-device inference sessions for {@code .tflite} models.
 *
 * <p>Android uses LiteRT. iOS uses TensorFlow Lite Objective-C and may select
 * the Core ML delegate. The builder links the runtime only when an application
 * references {@link com.codename1.ai.inference.InferenceSession}. Other
 * targets expose the same API with an explicit unsupported fallback.</p>
 */
package com.codename1.ai.inference;
