package com.realarqade.thenothingmod;

/** Switches for development. Nothing in a release build turns these on; the dev-only config tab is the only thing that does. */
public final class DustDebug {
    /** Log every connection the dust block works out (position and resulting state). */
    public static volatile boolean logConnections = false;

    private DustDebug() {
    }
}
