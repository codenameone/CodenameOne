package com.codename1.flutter;

/**
 * A restorable whose value is a {@code Listenable} that is restored rather than
 * re-created, mirroring Flutter's {@code RestorableListenable<T>}. User code
 * (studies/reply/app.dart, studies/shrine/app.dart) subclasses this directly,
 * overriding {@link #createDefaultValue()} / {@link #fromPrimitives(Object)} /
 * {@link #toPrimitives()}, and reads the inherited {@link #value()} getter.
 *
 * <p>The value is created lazily from {@link #createDefaultValue()} on first
 * access (Codename One does not persist restoration data).</p>
 *
 * @param <T> the held (listenable) value type
 */
public class RestorableListenable<T> extends RestorableProperty<T> {

    private T current;
    private boolean initialized;

    /** The restored value, created lazily from {@link #createDefaultValue()}. */
    public T value() {
        if (!initialized) {
            current = createDefaultValue();
            initialized = true;
        }
        return current;
    }

    @Override
    public void initWithValue(T value) {
        this.current = value;
        this.initialized = true;
    }
}
