package com.codename1.flutter;

/**
 * A {@link RestorableListenable} specialised for {@code ChangeNotifier} values,
 * mirroring Flutter's {@code RestorableChangeNotifier<T>}. In Flutter this also
 * disposes the held notifier; Codename One folds that into
 * {@link RestorableProperty#dispose()}.
 *
 * @param <T> the held ChangeNotifier value type
 */
public class RestorableChangeNotifier<T> extends RestorableListenable<T> {
}
