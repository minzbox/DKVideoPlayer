package com.dueeeke.videocontroller.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.widget.LinearLayout
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IControlComponent
import com.dueeeke.videoplayer.player.VideoView
import kotlin.math.abs

/**
 * Play error prompt interface
 * Created by dueeeke on 2017/4/13.
 */
class ErrorView : LinearLayout, IControlComponent {
    private var mDownX = 0f
    private var mDownY = 0f
    private var mControlWrapper: ControlWrapper? = null

    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet? = null) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {}
    override fun onPlayStateChanged(playState: Int) {
        if (playState == VideoView.STATE_ERROR) {
            bringToFront()
            visibility = VISIBLE
        } else if (playState == VideoView.STATE_IDLE) {
            visibility = GONE
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {}
    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = ev.x
                mDownY = ev.y
                // True if the child does not want the parent to intercept touch events.
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val absDeltaX = abs(ev.x - mDownX)
                val absDeltaY = abs(ev.y - mDownY)
                if (absDeltaX > ViewConfiguration.get(context).scaledTouchSlop ||
                        absDeltaY > ViewConfiguration.get(context).scaledTouchSlop) {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_error_view, this, true)
        findViewById<View>(R.id.status_btn).setOnClickListener {
            visibility = GONE
            mControlWrapper!!.replay(false)
        }
        isClickable = true
    }
}