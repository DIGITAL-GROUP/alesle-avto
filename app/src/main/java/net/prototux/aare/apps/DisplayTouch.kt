package net.prototux.aare.apps

/**
 * From the car content area's pixels (where the app is drawn) to the app display's pixels.
 *
 * The display is created at the content area's size, so this is usually 1:1; while a resize is on
 * its way (rail moved, head unit reconfigured) the two differ and touches are scaled. Points
 * outside the area are clamped to its edge, so a drag that slides off doesn't jump.
 */
data class DisplayTouch(val viewWidth: Int, val viewHeight: Int, val displayWidth: Int, val displayHeight: Int) {
    val scaleX: Float = displayWidth.toFloat() / viewWidth.coerceAtLeast(1)
    val scaleY: Float = displayHeight.toFloat() / viewHeight.coerceAtLeast(1)
    val identity: Boolean get() = viewWidth == displayWidth && viewHeight == displayHeight

    fun x(viewX: Float): Float = (viewX * scaleX).coerceIn(0f, (displayWidth - 1).coerceAtLeast(0).toFloat())
    fun y(viewY: Float): Float = (viewY * scaleY).coerceIn(0f, (displayHeight - 1).coerceAtLeast(0).toFloat())
}
