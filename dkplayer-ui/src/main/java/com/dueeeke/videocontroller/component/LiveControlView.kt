package com.dueeeke.videocontroller.component

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IControlComponent
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity

/**
 * Control bar at the bottom of the live broadcast
 */
class LiveControlView : FrameLayout, IControlComponent, View.OnClickListener {
    private var mControlWrapper: ControlWrapper? = null
    private var mFullScreen: ImageView? = null
    private var mBottomContainer: LinearLayout? = null
    private var mPlayButton: ImageView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            if (visibility == GONE) {
                visibility = VISIBLE
                anim?.let { startAnimation(it) }
            }
        } else {
            if (visibility == VISIBLE) {
                visibility = GONE
                anim?.let { startAnimation(it) }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_IDLE, VideoView.STATE_START_ABORT, VideoView.STATE_PREPARING, VideoView.STATE_PREPARED, VideoView.STATE_ERROR, VideoView.STATE_PLAYBACK_COMPLETED -> visibility = GONE
            VideoView.STATE_PLAYING -> mPlayButton!!.isSelected = true
            VideoView.STATE_PAUSED -> mPlayButton!!.isSelected = false
            VideoView.STATE_BUFFERING, VideoView.STATE_BUFFERED -> mPlayButton!!.isSelected = mControlWrapper!!.isPlaying
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        when (playerState) {
            VideoView.PLAYER_NORMAL -> mFullScreen!!.isSelected = false
            VideoView.PLAYER_FULL_SCREEN -> mFullScreen!!.isSelected = true
        }
        val activity = scanForActivity(context)
        if (activity != null && mControlWrapper!!.hasCutout()) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper!!.cutoutHeight
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mBottomContainer!!.setPadding(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mBottomContainer!!.setPadding(cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mBottomContainer!!.setPadding(0, 0, cutoutHeight, 0)
            }
        }
    }

    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {
        onVisibilityChanged(!isLocked, null)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fullscreen -> {
                toggleFullScreen()
            }
            R.id.iv_play -> {
                mControlWrapper!!.togglePlay()
            }
            R.id.iv_refresh -> {
                mControlWrapper!!.replay(true)
            }
        }
    }

    /**
     * Toggle fullscreen
     */
    private fun toggleFullScreen() {
        val activity = scanForActivity(context)
        mControlWrapper!!.toggleFullScreen(activity)
    }

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_live_control_view, this, true)
        mFullScreen = findViewById(R.id.fullscreen)
        mFullScreen?.setOnClickListener(this)
        mBottomContainer = findViewById(R.id.bottom_container)
        mPlayButton = findViewById(R.id.iv_play)
        mPlayButton?.setOnClickListener(this)
        val refresh = findViewById<ImageView>(R.id.iv_refresh)
        refresh.setOnClickListener(this)
    }
}