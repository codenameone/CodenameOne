package com.codename1.flutter.material;

import dart.async.Future;

/**
 * The handle returned by {@link ScaffoldState#showBottomSheet}
 * ({@code PersistentBottomSheetController} in Flutter). Its {@code closed}
 * future completes with the sheet's result when the sheet is dismissed; at this
 * milestone the sheet is not mounted, so the future is already complete.
 */
public class PersistentBottomSheetController {

    /**
     * A future that resolves when the sheet is dismissed. Not persisted here, so
     * it resolves immediately with a null result.
     */
    public Future<Object> closed() {
        return Future.value(null);
    }

    public void close() {
    }
}
