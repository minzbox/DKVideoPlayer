package com.dueeeke.videoplayer.player

import com.dueeeke.videoplayer.render.RenderViewFactory
import com.dueeeke.videoplayer.render.TextureRenderViewFactory

/**
 * Player global configuration
 */
class VideoViewConfig private constructor(builder: Builder) {
    val mPlayOnMobileNetwork: Boolean
    @JvmField
    val mEnableOrientation: Boolean
    @JvmField
    val mEnableAudioFocus: Boolean
    @JvmField
    val mIsEnableLog: Boolean
    @JvmField
    val mProgressManager: ProgressManager?
    @JvmField
    var mPlayerFactory: PlayerFactory<*>? = null
    @JvmField
    val mScreenScaleType: Int
    @JvmField
    var mRenderViewFactory: RenderViewFactory? = null
    @JvmField
    val mAdaptCutout: Boolean

    class Builder {
        internal var mIsEnableLog = false
        internal var mPlayOnMobileNetwork = false
        internal var mEnableOrientation = false
        internal var mEnableAudioFocus = true
        internal var mProgressManager: ProgressManager? = null
        internal var mPlayerFactory: PlayerFactory<*>? = null
        internal var mScreenScaleType = 0
        internal var mRenderViewFactory: RenderViewFactory? = null
        internal var mAdaptCutout = true

        /**
         * Whether to monitor the device direction to switch full screen/half screen, it is not turned on by default
         */
        fun setEnableOrientation(enableOrientation: Boolean): Builder {
            mEnableOrientation = enableOrientation
            return this
        }

        /**
         * Whether to continue playing after calling start() in a mobile environment (3G/4G), the default is not to continue playing
         */
        fun setPlayOnMobileNetwork(playOnMobileNetwork: Boolean): Builder {
            mPlayOnMobileNetwork = playOnMobileNetwork
            return this
        }

        /**
         * Whether to enable AudioFocus monitoring, enabled by default
         */
        fun setEnableAudioFocus(enableAudioFocus: Boolean): Builder {
            mEnableAudioFocus = enableAudioFocus
            return this
        }

        /**
         * Set the progress manager to save the playback progress
         */
        fun setProgressManager(progressManager: ProgressManager?): Builder {
            mProgressManager = progressManager
            return this
        }

        /**
         * Whether to print log
         */
        fun setLogEnabled(enableLog: Boolean): Builder {
            mIsEnableLog = enableLog
            return this
        }

        /**
         * Custom playback core
         */
        fun setPlayerFactory(playerFactory: PlayerFactory<*>?): Builder {
            mPlayerFactory = playerFactory
            return this
        }

        /**
         * Set video ratio
         */
        fun setScreenScaleType(screenScaleType: Int): Builder {
            mScreenScaleType = screenScaleType
            return this
        }

        /**
         * Custom RenderView
         */
        fun setRenderViewFactory(renderViewFactory: RenderViewFactory?): Builder {
            mRenderViewFactory = renderViewFactory
            return this
        }

        /**
         * Whether to adapt to Liu Haiping, the default adaptation
         */
        fun setAdaptCutout(adaptCutout: Boolean): Builder {
            mAdaptCutout = adaptCutout
            return this
        }

        fun build(): VideoViewConfig {
            return VideoViewConfig(this)
        }
    }

    companion object {
        @JvmStatic
        fun newBuilder(): Builder {
            return Builder()
        }
    }

    init {
        mIsEnableLog = builder.mIsEnableLog
        mEnableOrientation = builder.mEnableOrientation
        mPlayOnMobileNetwork = builder.mPlayOnMobileNetwork
        mEnableAudioFocus = builder.mEnableAudioFocus
        mProgressManager = builder.mProgressManager
        mScreenScaleType = builder.mScreenScaleType
        mPlayerFactory = if (builder.mPlayerFactory == null) {
            //The default is AndroidMediaPlayer
            AndroidMediaPlayerFactory.create()
        } else {
            builder.mPlayerFactory
        }
        mRenderViewFactory = if (builder.mRenderViewFactory == null) {
            //Use TextureView to render video by default
            TextureRenderViewFactory.create()
        } else {
            builder.mRenderViewFactory
        }
        mAdaptCutout = builder.mAdaptCutout
    }
}