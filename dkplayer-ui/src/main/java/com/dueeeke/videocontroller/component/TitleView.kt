package com.dueeeke.videocontroller.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IControlComponent
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.currentSystemTime
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity

/**
 * Title bar at the top of the player when in fullscreen state
 */
class TitleView : FrameLayout, IControlComponent {
    private var mControlWrapper: ControlWrapper? = null
    private var mTitleContainer: LinearLayout? = null
    private var mBack: ImageView? = null
    private var mTitle: TextView? = null
    private var mSysTime: TextView? = null  //System current time
    private var mBatteryReceiver: BatteryReceiver? = null
    private var mIsRegister //Whether to register BatteryReceiver
            = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setTitle(title: String?) {
        mTitle!!.text = title
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mIsRegister) {
            context.unregisterReceiver(mBatteryReceiver)
            mIsRegister = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mIsRegister) {
            context.registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            mIsRegister = true
        }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        //Only works in full screen
//        if (!mControlWrapper!!.isFullScreen) return
        if (isVisible) {
            if (visibility == GONE) {
                mSysTime!!.text = currentSystemTime
                visibility = VISIBLE
                if (mControlWrapper?.isFullScreen == true) {
                    mBack?.visibility = VISIBLE
                    mSysTime?.visibility = VISIBLE
                } else {
                    mBack?.visibility = GONE
                    mSysTime?.visibility = GONE
                }
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
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (playerState == VideoView.PLAYER_FULL_SCREEN) {
            if (mControlWrapper!!.isShowing && !mControlWrapper!!.isLocked) {
                visibility = VISIBLE
                mBack?.visibility = VISIBLE
                mSysTime!!.text = currentSystemTime
            }
            mTitle!!.isSelected = true
        } else {
            visibility = GONE
            mTitle!!.isSelected = false
        }
        val activity = scanForActivity(context)
        if (activity != null && mControlWrapper!!.hasCutout()) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper!!.cutoutHeight
            when (orientation) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                    mTitleContainer!!.setPadding(0, 0, 0, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                    mTitleContainer!!.setPadding(cutoutHeight, 0, 0, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                    mTitleContainer!!.setPadding(0, 0, cutoutHeight, 0)
                }
            }
        }
    }

    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {
        if (isLocked) {
            visibility = GONE
        } else {
            visibility = VISIBLE
            mSysTime!!.text = currentSystemTime
        }
    }

    private class BatteryReceiver(private val pow: ImageView) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            val current = extras.getInt("level") // Get current power
            val total = extras.getInt("scale") // Get total power
            val percent = current * 100 / total
            pow.drawable.level = percent
        }
    }

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_title_view, this, true)
        mTitleContainer = findViewById(R.id.title_container)
        mBack = findViewById<ImageView>(R.id.back)
        mBack?.setOnClickListener {
            val activity = scanForActivity(context)
            if (activity != null && mControlWrapper!!.isFullScreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                mControlWrapper!!.stopFullScreen()
            }
        }
        mTitle = findViewById(R.id.title)
        mSysTime = findViewById(R.id.sys_time)
        val batteryLevel = findViewById<ImageView>(R.id.iv_battery)
        mBatteryReceiver = BatteryReceiver(batteryLevel)
    }
}