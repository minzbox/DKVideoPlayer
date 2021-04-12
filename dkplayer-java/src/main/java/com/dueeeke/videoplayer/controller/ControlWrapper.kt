package com.dueeeke.videoplayer.controller

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap

/**
 * The purpose of this class is to call both the api of VideoView and the api of BaseVideoController in ControlComponent.
 * Some apis are encapsulated for easy use
 */
class ControlWrapper(private val mIMediaPlayerControl: IMediaPlayerControl, private val iVideoController: IVideoController): IMediaPlayerControl, IVideoController {

    override fun start() {
        mIMediaPlayerControl.start()
    }

    override fun pause() {
        mIMediaPlayerControl.pause()
    }

    override val duration: Long
        get() = mIMediaPlayerControl.duration

    override val currentPosition: Long
        get() = mIMediaPlayerControl.currentPosition

    override fun seekTo(pos: Long) {
        mIMediaPlayerControl.seekTo(pos)
    }

    override val isPlaying: Boolean
        get() = mIMediaPlayerControl.isPlaying

    override val bufferedPercentage: Int
        get() = mIMediaPlayerControl.bufferedPercentage


    override fun startFullScreen() {
        mIMediaPlayerControl.startFullScreen()
    }

    override fun stopFullScreen() {
        mIMediaPlayerControl.stopFullScreen()
    }

    override val isFullScreen: Boolean
        get() = mIMediaPlayerControl.isFullScreen

    override var isMute: Boolean
        get() = mIMediaPlayerControl.isMute
        set(value) {
            mIMediaPlayerControl.isMute = value
        }

    override fun setScreenScaleType(screenScaleType: Int) {
        mIMediaPlayerControl.setScreenScaleType(screenScaleType)
    }

    override var speed: Float
        get() = mIMediaPlayerControl.speed
        set(value) {
            mIMediaPlayerControl.speed = value
        }

    override val tcpSpeed: Long
        get() = mIMediaPlayerControl.tcpSpeed

    override fun replay(resetPosition: Boolean) {
        mIMediaPlayerControl.replay(resetPosition)
    }

    override fun setMirrorRotation(enable: Boolean) {
        mIMediaPlayerControl.setMirrorRotation(enable)
    }

    override fun doScreenShot(): Bitmap? {
        return mIMediaPlayerControl.doScreenShot()
    }

    override val videoSize: IntArray?
        get() = mIMediaPlayerControl.videoSize

    override fun setRotation(rotation: Float) {
        mIMediaPlayerControl.setRotation(rotation)
    }

    override fun startTinyScreen() {
        mIMediaPlayerControl.startTinyScreen()
    }

    override fun stopTinyScreen() {
        mIMediaPlayerControl.stopTinyScreen()
    }

    override val isTinyScreen: Boolean
        get() = mIMediaPlayerControl.isTinyScreen

    override fun startFadeOut() {
        iVideoController.startFadeOut()
    }

    override fun stopFadeOut() {
        iVideoController.stopFadeOut()
    }

    override val isShowing: Boolean
        get() = iVideoController.isShowing
    override var isLocked: Boolean
        get() = iVideoController.isLocked
        set(value) {
            iVideoController.isLocked = value
        }

    override fun startProgress() {
        iVideoController.startProgress()
    }

    override fun stopProgress() {
        iVideoController.stopProgress()
    }

    override fun show() {
        iVideoController.show()
    }

    override fun hide() {
        iVideoController.hide()
    }

    override fun hasCutout(): Boolean {
        return iVideoController.hasCutout()
    }

    override val cutoutHeight: Int
        get() = iVideoController.cutoutHeight

    fun togglePlay() {
        if (isPlaying) {
            pause()
        } else {
            start()
        }
    }

    fun toggleFullScreen(activity: Activity?) {
        if (activity == null || activity.isFinishing) return
        if (isFullScreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            stopFullScreen()
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            startFullScreen()
        }
    }

    fun toggleFullScreen() {
        if (isFullScreen) {
            stopFullScreen()
        } else {
            startFullScreen()
        }
    }

    /**
     * Switch between horizontal and vertical screens, decide whether to rotate the screen according to the adaptation width and height
     */
    fun toggleFullScreenByVideoSize(activity: Activity?) {
        if (activity == null || activity.isFinishing) return
        val size = videoSize
        val width = size!![0]
        val height = size[1]
        if (isFullScreen) {
            stopFullScreen()
            if (width > height) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } else {
            startFullScreen()
            if (width > height) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    fun toggleLockState() {
        isLocked = !isLocked
    }

    /**
     * Toggle show/hide state
     */
    fun toggleShowState() {
        if (isShowing) {
            hide()
        } else {
            show()
        }
    }
}