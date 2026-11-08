package net.prototux.aare.apps;

import android.view.MotionEvent;
import android.view.Surface;

/**
 * Displays for phone apps on the car screen, run by Shizuku with adb (shell) rights
 * (ShellDisplayService): only the shell may put other apps' activities on a display it creates
 * and inject input into them. This is what scrcpy does with --new-display and --start-app.
 */
interface IShellDisplay {
    /** Creates a virtual display drawing into [surface] (may be null). Returns its id, or -1. */
    int create(in Surface surface, int width, int height, int densityDpi) = 1;

    void resize(int displayId, int width, int height, int densityDpi) = 2;

    /** Where the display draws; null pauses drawing (the apps on it keep running). */
    void setSurface(int displayId, in Surface surface) = 3;

    /**
     * Starts (or brings to front) [activity] of [packageName] full screen on the display, moving
     * its task there first if it is on another display. Returns null, or why it failed.
     */
    String launch(int displayId, String packageName, String activity) = 4;

    /** A touch event in the display's pixels, pointer ids kept (multi-touch). */
    oneway void injectMotion(int displayId, in MotionEvent event) = 5;

    /** A key press (down and up), e.g. KEYCODE_BACK. */
    oneway void injectKey(int displayId, int keyCode) = 6;

    /** The package in front on the display, "" if nothing is on it, null if that can't be told. */
    String topPackage(int displayId) = 7;

    /** Removes the display; the activities on it are destroyed. */
    void release(int displayId) = 8;

    /** Shizuku's user-service teardown call. */
    void destroy() = 16777114;
}
