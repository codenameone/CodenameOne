package com.codename1.flutter;

/**
 * An opaque token that, in Flutter, holds serialized restoration data for a
 * subtree. Codename One does not persist restoration state, so this is an inert
 * marker: it exists purely so that {@code restoreState(RestorationBucket?, bool)}
 * and {@link RestorationMixin#bucket()} type-check. It carries no data.
 */
public class RestorationBucket {
}
