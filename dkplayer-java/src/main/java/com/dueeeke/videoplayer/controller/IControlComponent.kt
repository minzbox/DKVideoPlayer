package com.dueeeke.videoplayer.controller

import android.view.View
import android.view.animation.Animation

interface IControlComponent {
    fun attach(controlWrapper: ControlWrapper)
    val view: View?
    fun onVisibilityChanged(isVisible: Boolean, anim: Animation?)
    fun onPlayStateChanged(playState: Int)
    fun onPlayerStateChanged(playerState: Int)
    fun setProgress(duration: Int, position: Int)
    fun onLockStateChanged(isLocked: Boolean)
}