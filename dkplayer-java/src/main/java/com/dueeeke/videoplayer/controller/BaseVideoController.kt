package com.dueeeke.videoplayer.controller

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import com.dueeeke.videoplayer.controller.OrientationHelper.OnOrientationChangeListener
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.player.VideoViewManager.Companion.config
import com.dueeeke.videoplayer.player.VideoViewManager.Companion.instance
import com.dueeeke.videoplayer.util.CutoutUtil.adaptCutoutAboveAndroidP
import com.dueeeke.videoplayer.util.CutoutUtil.allowDisplayToCutout
import com.dueeeke.videoplayer.util.L
import com.dueeeke.videoplayer.util.PlayerUtils
import com.dueeeke.videoplayer.util.PlayerUtils.getNetworkType
import com.dueeeke.videoplayer.util.PlayerUtils.getStatusBarHeightPortrait
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity
import java.util.*

/**
 * Controller base class
 * This type of integrated processing logic for various events, including
 * 1. Player state change: {@link #handlePlayerStateChanged(int)}
 * 2. Play state change: {@link #handlePlayStateChanged(int)}
 * 3. Control the display and hide of the view: {@link #handleVisibilityChanged(boolean, Animation)}
 * 4. Play progress change: {@link #handleSetProgress(int, int)}
 * 5. Lock state change: {@link #handleLockStateChanged(boolean)}
 * 6. Device orientation monitoring: {@link #onOrientationChanged(int)}
 */
abstract class BaseVideoController @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), IVideoController, OnOrientationChangeListener {
    //Player wrapper class, which integrates the api of IMediaPlayerControl and the api of IVideoController
    @JvmField
    protected var mControlWrapper: ControlWrapper? = null
    @JvmField
    protected var mActivity: Activity? = null

    //Whether the controller is in display state
    private var mShowing = false

    private var mIsLocked = false

    //Play view hide timeout
    private var mDefaultTimeout = 4000

    //Whether to enable to enter/exit the full screen according to the screen orientation
    private var mEnableOrientation = false

    //Screen orientation monitor auxiliary class
    @JvmField
    protected var mOrientationHelper: OrientationHelper? = null

    //Use for Chinese phone
    private var mAdaptCutout = false

    //Chinese phone
    private var mHasCutout: Boolean = false

    //Bangs height
    private var mCutoutHeight = 0

    //Whether to start refreshing progress
    private var mIsStartProgress = false

    //Saved all control components
    @JvmField
    protected var mControlComponents = LinkedHashMap<IControlComponent, Boolean>()
    private var mShowAnim: Animation? = null
    private var mHideAnim: Animation? = null
    private val inv by lazy { initView()}
    protected open fun initView() {
        if (layoutId != 0) {
            LayoutInflater.from(context).inflate(layoutId, this, true)
        }
        mOrientationHelper = OrientationHelper(context.applicationContext)
        mEnableOrientation = config!!.mEnableOrientation
        mAdaptCutout = config!!.mAdaptCutout
        mShowAnim = AlphaAnimation(0f, 1f)
        (mShowAnim as AlphaAnimation).duration = 300
        mHideAnim = AlphaAnimation(1f, 0f)
        (mHideAnim as AlphaAnimation).duration = 300
        mActivity = scanForActivity(context)
    }

    /**
     * Set the controller layout file, the subclass must implement
     */
    protected abstract val layoutId: Int

    /**
     * Important: This method is used to bind [VideoView] to the controller
     */
    @CallSuper
    open fun setMediaPlayer(mediaPlayer: IMediaPlayerControl?) {
        mControlWrapper = ControlWrapper(mediaPlayer!!, this)
        //Bind ControlComponent and Controller
        for ((component) in mControlComponents) {
            component.attach(mControlWrapper!!)
        }
        //Start monitoring device direction
        mOrientationHelper!!.setOnOrientationChangeListener(this)
    }

    /**
     * Add control components, add the last one at the bottom, rationally organize the order of addition,
     * so that ControlComponent can be located at a different level
     */
    fun addControlComponent(vararg component: IControlComponent) {
        for (item in component) {
            addControlComponent(item, false)
        }
    }

    /**
     * Add control components, add the last one at the bottom, and organize the addition order reasonably, allowing ControlComponent to be located at different levels
     * @param isPrivate is it a unique component, if it is, it will not be added to the controller
     */
    fun addControlComponent(component: IControlComponent, isPrivate: Boolean) {
        mControlComponents[component] = isPrivate
        if (mControlWrapper != null) {
            component.attach(mControlWrapper!!)
        }
        val view = component.view
        if (view != null && !isPrivate) {
            addView(view, 0)
        }
    }

    /**
     * Remove control components
     */
    fun removeControlComponent(component: IControlComponent) {
        removeView(component.view)
        mControlComponents.remove(component)
    }

    fun removeAllControlComponent() {
        for ((key) in mControlComponents) {
            removeView(key.view)
        }
        mControlComponents.clear()
    }

    private fun removeAllPrivateComponents() {
        val it: MutableIterator<Map.Entry<IControlComponent, Boolean>> = mControlComponents.entries.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (next.value) {
                it.remove()
            }
        }
    }

    /**
     * [VideoView] Call this method to set the playback status to the controller
     */
    @CallSuper
    open fun setPlayState(playState: Int) {
        handlePlayStateChanged(playState)
    }

    /**
     * [VideoView] Call this method to set the player state to the controller
     */
    @CallSuper
    open fun setPlayerState(playerState: Int) {
        handlePlayerStateChanged(playerState)
    }

    /**
     * Set the playback view to automatically hide timeout
     */
    fun setDismissTimeout(timeout: Int) {
        if (timeout > 0) {
            mDefaultTimeout = timeout
        }
    }

    /**
     * Hide playback view
     */
    override fun hide() {
        if (mShowing) {
            stopFadeOut()
            handleVisibilityChanged(false, mHideAnim)
            mShowing = false
        }
    }

    /**
     * Show playback view
     */
    override fun show() {
        if (!mShowing) {
            handleVisibilityChanged(true, mShowAnim)
            startFadeOut()
            mShowing = true
        }
    }

    override val isShowing: Boolean
        get() = mShowing

    /**
     * Start timer
     */
    override fun startFadeOut() {
        //Restart timing
        stopFadeOut()
        postDelayed(mFadeOut, mDefaultTimeout.toLong())
    }

    /**
     * Cancel timing
     */
    override fun stopFadeOut() {
        removeCallbacks(mFadeOut)
    }

    /**
     * Hide the play view Runnable
     */
    private val mFadeOut = Runnable { hide() }

    override var isLocked: Boolean
        get() = mIsLocked
        set(value) {
            mIsLocked = value
            handleLockStateChanged(value)
        }

    /**
     * Start refreshing progress, note: it needs to be called at STATE_PLAYING to start refreshing progress
     */
    override fun startProgress() {
        if (mIsStartProgress) return
        post(mShowProgress)
        mIsStartProgress = true
    }

    /**
     * Stop refreshing progress
     */
    override fun stopProgress() {
        if (!mIsStartProgress) return
        removeCallbacks(mShowProgress)
        mIsStartProgress = false
    }

    /**
     * Refresh progress Runnable
     */
    private var mShowProgress: Runnable = object : Runnable {
        override fun run() {
            val pos = setProgress()
            if (mControlWrapper!!.isPlaying) {
                postDelayed(this, ((1000 - pos % 1000) / mControlWrapper!!.speed).toLong())
            } else {
                mIsStartProgress = false
            }
        }
    }

    private fun setProgress(): Int {
        val position = mControlWrapper!!.currentPosition.toInt()
        val duration = mControlWrapper!!.duration.toInt()
        handleSetProgress(duration, position)
        return position
    }

    /**
     * Set whether to adapt to Chinese phone
     */
    fun setAdaptCutout(adaptCutout: Boolean) {
        mAdaptCutout = adaptCutout
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkCutout()
    }

    /**
     * Check if you need to fit bangs
     */
    private fun checkCutout() {
        if (!mAdaptCutout) return
        if (mActivity != null) {
            mHasCutout = allowDisplayToCutout(mActivity!!)
            if (mHasCutout) {
                //The height of the status bar in the vertical screen can be considered the height of the bangs
                mCutoutHeight = getStatusBarHeightPortrait(mActivity!!).toInt()
            }
        }
        L.d("hasCutout: $mHasCutout cutout height: $mCutoutHeight")
    }

    /**
     * Is there bangs
     */
    override fun hasCutout(): Boolean {
        return mHasCutout
    }

    override val cutoutHeight: Int
        get() = mCutoutHeight

    /**
     * Show mobile network playback tips
     *
     * @return returns the conditions for displaying the mobile network playback prompts, false: do not display, true display
     * The default is to decide whether to display according to the mobile phone network type, and the developer can rewrite the relevant logic
     */
    open fun showNetWarning(): Boolean {
        return (getNetworkType(context) == PlayerUtils.NETWORK_MOBILE
                && !instance()!!.playOnMobileNetwork())
    }

    /**
     * Play and pause
     */
    protected fun togglePlay() {
        mControlWrapper!!.togglePlay()
    }

    /**
     * Switch between horizontal and vertical screens
     */
    protected open fun toggleFullScreen() {
        mControlWrapper!!.toggleFullScreen(mActivity)
    }

    /**
     * Please use this method to enter the full screen in the subclass
     *
     * @return Whether to successfully enter the full screen
     */
    protected fun startFullScreen(): Boolean {
        if (mActivity == null || mActivity!!.isFinishing) return false
        mActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        mControlWrapper!!.startFullScreen()
        return true
    }

    /**
     * Please use this method to exit the full screen in the subclass
     *
     * @return whether to exit the full screen successfully
     */
    protected fun stopFullScreen(): Boolean {
        if (mActivity == null || mActivity!!.isFinishing) return false
        mActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mControlWrapper!!.stopFullScreen()
        return true
    }

    /**
     * Change the return key logic for activity
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (mControlWrapper!!.isPlaying
                && (mEnableOrientation || mControlWrapper!!.isFullScreen)) {
            if (hasWindowFocus) {
                postDelayed({ mOrientationHelper!!.enable() }, 800)
            } else {
                mOrientationHelper!!.disable()
            }
        }
    }

    /**
     * Whether to rotate automatically, the default is not to rotate automatically
     */
    fun setEnableOrientation(enableOrientation: Boolean) {
        mEnableOrientation = enableOrientation
    }

    private var mOrientation = 0
    @CallSuper
    override fun onOrientationChanged(orientation: Int) {
        if (mActivity == null || mActivity!!.isFinishing) return

        //Record the location of the user's mobile phone last time
        val lastOrientation = mOrientation
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            //When the phone is placed horizontally, no valid angle can be detected
            //Reset to original position -1
            mOrientation = -1
            return
        }
        if (orientation > 350 || orientation < 10) {
            val o = mActivity!!.requestedOrientation
            //Manually switch between horizontal and vertical screens
            if (o == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && lastOrientation == 0) return
            if (mOrientation == 0) return
            //0 degrees, the user holds the phone vertically
            mOrientation = 0
            onOrientationPortrait(mActivity!!)
        } else if (orientation in 81..99) {
            val o = mActivity!!.requestedOrientation
            //Manually switch between horizontal and vertical screens
            if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && lastOrientation == 90) return
            if (mOrientation == 90) return
            //90 degrees, the user holds the phone on the right side of the screen
            mOrientation = 90
            onOrientationReverseLandscape(mActivity!!)
        } else if (orientation in 261..279) {
            val o = mActivity!!.requestedOrientation
            //Manually switch between horizontal and vertical screens
            if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && lastOrientation == 270) return
            if (mOrientation == 270) return
            //270 degrees, the user holds the phone on the left side of the screen
            mOrientation = 270
            onOrientationLandscape(mActivity!!)
        }
    }

    /**
     * portrait
     */
    private fun onOrientationPortrait(activity: Activity) {
        //Screen lock situation
        if (mIsLocked) return
        //If the device direction monitoring is not turned on
        if (!mEnableOrientation) return
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mControlWrapper!!.stopFullScreen()
    }

    /**
     * landscape
     */
    private fun onOrientationLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (mControlWrapper!!.isFullScreen) {
            handlePlayerStateChanged(VideoView.PLAYER_FULL_SCREEN)
        } else {
            mControlWrapper!!.startFullScreen()
        }
    }

    /**
     * Reverse landscape
     */
    private fun onOrientationReverseLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        if (mControlWrapper!!.isFullScreen) {
            handlePlayerStateChanged(VideoView.PLAYER_FULL_SCREEN)
        } else {
            mControlWrapper!!.startFullScreen()
        }
    }

    //------------------------ start handle event change ------------------------//
    private fun handleVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (!mIsLocked) { //没锁住时才向ControlComponent下发此事件
            for ((component) in mControlComponents) {
                component.onVisibilityChanged(isVisible, anim)
            }
        }
        onVisibilityChanged(isVisible, anim)
    }

    /**
     * Subclass overrides this method to monitor the display and hiding of the control
     *
     * @param isVisible is it visible
     * @param anim show/hide animation
     */
    protected open fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {}
    private fun handlePlayStateChanged(playState: Int) {
        for ((component) in mControlComponents) {
            component.onPlayStateChanged(playState)
        }
        onPlayStateChanged(playState)
    }

    /**
     * The subclass overrides this method and updates the ui of the controller in different playback states.
     */
    @CallSuper
    protected open fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_IDLE -> {
                mOrientationHelper!!.disable()
                mOrientation = 0
                mIsLocked = false
                mShowing = false
                removeAllPrivateComponents()
            }
            VideoView.STATE_PLAYBACK_COMPLETED -> {
                mIsLocked = false
                mShowing = false
            }
            VideoView.STATE_ERROR -> mShowing = false
        }
    }

    private fun handlePlayerStateChanged(playerState: Int) {
        for ((component) in mControlComponents) {
            component.onPlayerStateChanged(playerState)
        }
        onPlayerStateChanged(playerState)
    }

    /**
     * The subclass overrides this method and updates the ui of the controller in different player states.
     */
    @CallSuper
    protected open fun onPlayerStateChanged(playerState: Int) {
        when (playerState) {
            VideoView.PLAYER_NORMAL -> {
                if (mEnableOrientation) {
                    mOrientationHelper!!.enable()
                } else {
                    mOrientationHelper!!.disable()
                }
                if (hasCutout()) {
                    adaptCutoutAboveAndroidP(context, false)
                }
            }
            VideoView.PLAYER_FULL_SCREEN -> {
                //Force monitoring of device orientation when in full screen
                mOrientationHelper!!.enable()
                if (hasCutout()) {
                    adaptCutoutAboveAndroidP(context, true)
                }
            }
            VideoView.PLAYER_TINY_SCREEN -> mOrientationHelper!!.disable()
        }
    }

    private fun handleSetProgress(duration: Int, position: Int) {
        for ((component) in mControlComponents) {
            component.setProgress(duration, position)
        }
        setProgress(duration, position)
    }

    /**
     * Refresh progress callback, subclasses can monitor the progress refresh in this method, and then update the ui
     *
     * @param duration total video duration
     * @param position The current length of the video
     */
    protected fun setProgress(duration: Int, position: Int) {}
    private fun handleLockStateChanged(isLocked: Boolean) {
        for ((component) in mControlComponents) {
            component.onLockStateChanged(isLocked)
        }
        onLockStateChanged(isLocked)
    }

    /**
     * Subclasses can override this method to listen for changes in the lock state, and then update the ui
     */
    protected open fun onLockStateChanged(isLocked: Boolean) {} //------------------------ end handle event change ------------------------//

    init {
        inv
    }
}