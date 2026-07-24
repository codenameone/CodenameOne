package com.codename1.flutter.widgets;

/**
 * The connection state of an async computation feeding an
 * {@link AsyncSnapshot}, mirroring Flutter's {@code ConnectionState}.
 */
public enum ConnectionState {
    /** Not connected to any asynchronous computation. */
    none,
    /** Connected, awaiting interaction. */
    waiting,
    /** Connected and actively producing values. */
    active,
    /** Connected to a terminated asynchronous computation. */
    done
}
