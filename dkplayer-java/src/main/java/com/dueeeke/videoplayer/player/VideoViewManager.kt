package com.dueeeke.videoplayer.player

import android.app.Application
import com.dueeeke.videoplayer.util.L
import java.util.*

/**
 * Video player manager, manage the currently playing VideoView, and player configuration
 * You can also use to save the VideoView in the resident memory, but be careful to create it through the Application Context,
 * To avoid memory leaks
 */
class VideoViewManager private constructor() {
    /**
     * The container that holds the VideoView
     */
    private val mVideoViews = LinkedHashMap<String, VideoView<*>>()

    /**
     * Whether to play the video directly under the mobile network
     */
    private var mPlayOnMobileNetwork: Boolean

    /**
     * Get whether to play the video directly under the mobile network configuration
     */
    fun playOnMobileNetwork(): Boolean {
        return mPlayOnMobileNetwork
    }

    /**
     * Set whether to play the video directly under the mobile network
     */
    fun setPlayOnMobileNetwork(playOnMobileNetwork: Boolean) {
        mPlayOnMobileNetwork = playOnMobileNetwork
    }

    /**
     * Add VideoView
     * @param tag VideoView with the same tag will only save one, if the tag is the same, it will release and remove the previous one
     */
    fun add(videoView: VideoView<*>, tag: String) {
        if (videoView.context !is Application) {
            L.w("The Context of this VideoView is not an Application Context," +
                    "you must remove it after release,or it will lead to memory leek.")
        }
        val old = get(tag)
        old?.release()
        remove(tag)
        mVideoViews[tag] = videoView
    }

    operator fun get(tag: String): VideoView<*>? {
        return mVideoViews[tag]
    }

    private fun remove(tag: String) {
        mVideoViews.remove(tag)
    }

    fun removeAll() {
        mVideoViews.clear()
    }

    /**
     * Release the VideoView associated with the tag and remove it from VideoViewManager
     */
    @JvmOverloads
    fun releaseByTag(tag: String, isRemove: Boolean = true) {
        val videoView = get(tag)
        videoView?.release()
        if (isRemove) {
            remove(tag)
        }
    }

    fun onBackPress(tag: String): Boolean {
        val videoView = get(tag)
        return videoView?.onBackPressed() ?: false
    }

    companion object {
        /**
         * VideoViewManager instance
         */
        private var sInstance: VideoViewManager? = null

        /**
         * VideoViewConfig instance
         */
        private var sConfig: VideoViewConfig? = null
        /**
         * Get VideoViewConfig
         */
        /**
         * Set up VideoViewConfig
         */
        @JvmStatic
        var config: VideoViewConfig?
            get() {
                config = null
                return sConfig
            }
            set(config) {
                if (sConfig == null) {
                    synchronized(VideoViewConfig::class.java) {
                        if (sConfig == null) {
                            sConfig = config ?: VideoViewConfig.newBuilder()
                                    .build()
                        }
                    }
                }
            }

        @JvmStatic
        fun instance(): VideoViewManager? {
            if (sInstance == null) {
                synchronized(VideoViewManager::class.java) {
                    if (sInstance == null) {
                        sInstance = VideoViewManager()
                    }
                }
            }
            return sInstance
        }
    }

    init {
        mPlayOnMobileNetwork = config!!.mPlayOnMobileNetwork
    }
}