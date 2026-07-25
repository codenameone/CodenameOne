/**
 * Vendor-neutral on-device language identification, translation, and smart
 * reply.
 *
 * <p>Android and iOS builds use feature-scoped ML Kit components. Each
 * public entry point is scanned independently, so language identification
 * does not bundle translation or Smart Reply. Translation model payloads are
 * installed lazily by ML Kit. Other targets expose the same API with an
 * explicit unsupported fallback.</p>
 */
package com.codename1.ai.language;
