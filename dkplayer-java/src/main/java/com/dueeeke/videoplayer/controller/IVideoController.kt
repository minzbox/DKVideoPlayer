package com.dueeeke.videoplayer.controller

/**
 * Create by NghiaNv
 */
interface IVideoController {
    /**
     * Start controlling the view to automatically hide the countdown
     */
    fun startFadeOut()

    /**
     * Cancel the control view to automatically hide the countdown
     */
    fun stopFadeOut()

    /**
     * Control whether the view is in the display state
     */
    val isShowing: Boolean

    /**
     * Lock screen rotation
     */
    var isLocked: Boolean

    /**
     * Start refreshing progress
     */
    fun startProgress()

    /**
     * Stop refreshing progress
     */
    fun stopProgress()

    /**
     * Hide control view
     */
    fun hide()

    /**
     * Show control view
     */
    fun show()

    /**
     * Do you need to adapt bangs for Chinese devices
     */
    fun hasCutout(): Boolean

    /**
     * Get the height of the bangs for Chinese devices
     */
    val cutoutHeight: Int
}