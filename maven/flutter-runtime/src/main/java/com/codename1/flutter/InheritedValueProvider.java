package com.codename1.flutter;

/**
 * Implemented by widgets that publish a value to their subtree by type
 * (provider's {@code Provider}/{@code ChangeNotifierProvider} and scoped_model's
 * {@code ScopedModel}). {@link BuildContext#providerValueOfType(Class)} walks the
 * element tree and asks each ancestor provider whether it supplies the requested
 * type.
 */
public interface InheritedValueProvider {

    /**
     * The published value when it is assignable to {@code type}, otherwise null.
     */
    Object providedValueFor(Class<?> type);
}
