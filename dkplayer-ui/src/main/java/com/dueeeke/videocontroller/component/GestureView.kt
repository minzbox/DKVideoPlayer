package com.dueeeke.videocontroller.component

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.*
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IGestureComponent
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.stringForTime

/**
 * Gesture control view
 */
class GestureView : FrameLayout, IGestureComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mControlWrapper: ControlWrapper? = null
    private var mIcon: ImageView? = null
    private var mProgressPercent: ProgressBar? = null
    private var mTextPercent: TextView? = null
    private var mCenterContainer: LinearLayout? = null
    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {}
    override fun onPlayerStateChanged(playerState: Int) {}
    override fun onStartSlide() {
        mControlWrapper!!.hide()
        mCenterContainer!!.visibility = VISIBLE
        mCenterContainer!!.alpha = 1f
    }

    override fun onStopSlide() {
        mCenterContainer!!.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mCenterContainer!!.visibility = GONE
                    }
                })
                .start()
    }

    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
        mProgressPercent!!.visibility = GONE
        if (slidePosition > currentPosition) {
            mIcon!!.setImageResource(R.drawable.dkplayer_ic_action_fast_forward)
        } else {
            mIcon!!.setImageResource(R.drawable.dkplayer_ic_action_fast_rewind)
        }
        mTextPercent!!.text = String.format("%s/%s", stringForTime(slidePosition), stringForTime(duration))
    }

    override fun onBrightnessChange(percent: Int) {
        mProgressPercent!!.visibility = VISIBLE
        mIcon!!.setImageResource(R.drawable.dkplayer_ic_action_brightness)
        mTextPercent!!.text = "$percent%"
        mProgressPercent!!.progress = percent
    }

    override fun onVolumeChange(percent: Int) {
        mProgressPercent!!.visibility = VISIBLE
        if (percent <= 0) {
            mIcon!!.setImageResource(R.drawable.dkplayer_ic_action_volume_off)
        } else {
            mIcon!!.setImageResource(R.drawable.dkplayer_ic_action_volume_up)
        }
        mTextPercent!!.text = "$percent%"
        mProgressPercent!!.progress = percent
    }

    override fun onPlayStateChanged(playState: Int) {
        visibility = if (playState == VideoView.STATE_IDLE || playState == VideoView.STATE_START_ABORT || playState == VideoView.STATE_PREPARING || playState == VideoView.STATE_PREPARED || playState == VideoView.STATE_ERROR || playState == VideoView.STATE_PLAYBACK_COMPLETED) {
            GONE
        } else {
            VISIBLE
        }
    }

    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLock: Boolean) {}

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_gesture_control_view, this, true)
        mIcon = findViewById(R.id.iv_icon)
        mProgressPercent = findViewById(R.id.pro_percent)
        mTextPercent = findViewById(R.id.tv_percent)
        mCenterContainer = findViewById(R.id.center_container)
    }
}