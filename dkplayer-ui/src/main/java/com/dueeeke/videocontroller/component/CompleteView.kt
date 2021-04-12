package com.dueeeke.videocontroller.component

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IControlComponent
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity

/**
 * Auto play completion view
 */
class CompleteView : FrameLayout, IControlComponent {
    private var mControlWrapper: ControlWrapper? = null
    private var mStopFullscreen: ImageView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {}
    override fun onPlayStateChanged(playState: Int) {
        if (playState == VideoView.STATE_PLAYBACK_COMPLETED) {
            visibility = VISIBLE
            mStopFullscreen!!.visibility = if (mControlWrapper!!.isFullScreen) VISIBLE else GONE
            bringToFront()
        } else {
            visibility = GONE
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (playerState == VideoView.PLAYER_FULL_SCREEN) {
            mStopFullscreen!!.visibility = VISIBLE
        } else if (playerState == VideoView.PLAYER_NORMAL) {
            mStopFullscreen!!.visibility = GONE
        }
        val activity = scanForActivity(context)
        if (activity != null && mControlWrapper!!.hasCutout()) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper!!.cutoutHeight
            val sflp = mStopFullscreen!!.layoutParams as LayoutParams
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                sflp.setMargins(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                sflp.setMargins(cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                sflp.setMargins(0, 0, 0, 0)
            }
        }
    }

    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_complete_view, this, true)
        findViewById<View>(R.id.iv_replay).setOnClickListener { mControlWrapper!!.replay(true) }
        mStopFullscreen = findViewById(R.id.stop_fullscreen)
        mStopFullscreen?.setOnClickListener {
            if (mControlWrapper!!.isFullScreen) {
                val activity = scanForActivity(context)
                if (activity != null && !activity.isFinishing) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    mControlWrapper!!.stopFullScreen()
                }
            }
        }
        isClickable = true
    }
}