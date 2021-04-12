package com.dueeeke.videoplayer.exo

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Handler
import android.view.Surface
import android.view.SurfaceHolder
import com.dueeeke.videoplayer.player.AbstractPlayer
import com.dueeeke.videoplayer.player.VideoViewManager.Companion.config
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.MediaItem.AdsConfiguration
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory.AdsLoaderProvider
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.video.VideoListener
import java.io.IOException

open class ExoMediaPlayer(context: Context) : AbstractPlayer(), VideoListener, Player.EventListener {
    private var mAppContext: Context = context.applicationContext
    private var playerView: PlayerView? = null
    private var mInternalPlayer: SimpleExoPlayer? = null
    private var adsLoader: ImaAdsLoader? = null

    @JvmField
    protected var mMediaSource: MediaSource? = null
    protected var adsMediaSource: AdsMediaSource? = null
    private var mMediaSourceHelper: ExoMediaSourceHelper? = ExoMediaSourceHelper.getInstance(context)
    private var mSpeedPlaybackParameters: PlaybackParameters? = null
    private var mLastReportedPlaybackState = Player.STATE_IDLE
    private var mLastReportedPlayWhenReady = false
    private var mIsPreparing = false
    private var mIsBuffering = false
    private var mLoadControl: LoadControl? = null
    private var mRenderersFactory: RenderersFactory? = null
    private var mTrackSelector: TrackSelector? = null

    override fun initPlayer() {
        mMediaSourceHelper?.let {
            mInternalPlayer = SimpleExoPlayer.Builder(
                    mAppContext,
                    (if (mRenderersFactory == null) DefaultRenderersFactory(mAppContext).also { mRenderersFactory = it } else mRenderersFactory)!!,
                    (if (mTrackSelector == null) DefaultTrackSelector(mAppContext).also { mTrackSelector = it } else mTrackSelector)!!,
                    DefaultMediaSourceFactory(mAppContext),
                    (if (mLoadControl == null) DefaultLoadControl().also { mLoadControl = it } else mLoadControl)!!,
                    DefaultBandwidthMeter.getSingletonInstance(mAppContext),
                    AnalyticsCollector(Clock.DEFAULT))
                    .setMediaSourceFactory(DefaultMediaSourceFactory(mMediaSourceHelper!!.dataSourceFactory))
                    .build()
            playerView = PlayerView(mAppContext)
            playerView!!.player = mInternalPlayer
            // Create an AdsLoader with the ad tag url.
            adsLoader = ImaAdsLoader.Builder(mAppContext).build()
            adsLoader!!.setPlayer(mInternalPlayer)
            setOptions()

            //Player log
            if (config!!.mIsEnableLog && mTrackSelector is MappingTrackSelector) {
                mInternalPlayer!!.addAnalyticsListener(EventLogger(mTrackSelector as MappingTrackSelector?, "ExoPlayer"))
            }
            mInternalPlayer!!.addListener(this)
            mInternalPlayer!!.addVideoListener(this)
        }
    }

    fun setTrackSelector(trackSelector: TrackSelector?) {
        mTrackSelector = trackSelector
    }

    fun setRenderersFactory(renderersFactory: RenderersFactory?) {
        mRenderersFactory = renderersFactory
    }

    fun setLoadControl(loadControl: LoadControl?) {
        mLoadControl = loadControl
    }

    override fun setDataSource(path: String?, headers: Map<String, String>?) {
        mMediaSource = path?.let { mMediaSourceHelper?.getMediaSource(it, headers) }
        //        DataSpec dataSpec = new DataSpec.Builder().build();
        val factory = mMediaSourceHelper?.let { DefaultMediaSourceFactory(it.dataSourceFactory) }
        val adsLoaderProvider = AdsLoaderProvider { adsLoader }
        factory?.setAdsLoaderProvider(adsLoaderProvider)
        factory?.setAdViewProvider(playerView)
        //        adsMediaSource = new AdsMediaSource(mMediaSource, null, factory,
//                adsLoader, playerView);
//        adsMediaSource = new AdsMediaSource(mMediaSource, null, "", factory, adsLoader, adsLoaderProvider);
    }

    override fun setDataSource(fd: AssetFileDescriptor?) {
        //no support
    }

    override fun start() {
        if (mInternalPlayer == null) return
        mInternalPlayer!!.playWhenReady = true
    }

    override fun pause() {
        if (mInternalPlayer == null) return
        mInternalPlayer!!.playWhenReady = false
    }

    override fun stop() {
        if (mInternalPlayer == null) return
        mInternalPlayer!!.stop()
    }

    override fun prepareAsync() {
        if (mInternalPlayer == null) return
        if (mMediaSource == null) return
        if (mSpeedPlaybackParameters != null) {
            mInternalPlayer!!.setPlaybackParameters(mSpeedPlaybackParameters)
        }
        mIsPreparing = true
        mMediaSource!!.addEventListener(Handler(mAppContext.mainLooper), mMediaSourceEventListener)
        mInternalPlayer!!.setMediaItem(mMediaSource!!.mediaItem)
        mInternalPlayer!!.prepare()
    }

    private val mMediaSourceEventListener: MediaSourceEventListener = object : MediaSourceEventListener {
        override fun onLoadStarted(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
            if (mPlayerEventListener != null && mIsPreparing) {
                mPlayerEventListener!!.onPrepared()
            }
        }

        override fun onLoadCompleted(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {}
        override fun onLoadCanceled(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {}
        override fun onLoadError(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {}
        override fun onUpstreamDiscarded(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId, mediaLoadData: MediaLoadData) {}
        override fun onDownstreamFormatChanged(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, mediaLoadData: MediaLoadData) {}
    }

    override fun reset() {
        if (mInternalPlayer != null) {
            mInternalPlayer!!.stop(true)
            mInternalPlayer!!.setVideoSurface(null)
            mIsPreparing = false
            mIsBuffering = false
            mLastReportedPlaybackState = Player.STATE_IDLE
            mLastReportedPlayWhenReady = false
        }
    }

    override val isPlaying: Boolean
        get() {
            if (mInternalPlayer == null) return false
            val state = mInternalPlayer!!.playbackState
            return when (state) {
                Player.STATE_BUFFERING, Player.STATE_READY -> mInternalPlayer!!.playWhenReady
                Player.STATE_IDLE, Player.STATE_ENDED -> false
                else -> false
            }
        }

    override fun seekTo(time: Long) {
        if (mInternalPlayer == null) return
        mInternalPlayer!!.seekTo(time)
    }

    override fun release() {
        if (mInternalPlayer != null) {
            mInternalPlayer!!.removeListener(this)
            mInternalPlayer!!.removeVideoListener(this)
            val player: SimpleExoPlayer = mInternalPlayer as SimpleExoPlayer
            mInternalPlayer = null
            object : Thread() {
                override fun run() {
                    //异步释放，防止卡顿
                    player.release()
                }
            }.start()
        }
        mIsPreparing = false
        mIsBuffering = false
        mLastReportedPlaybackState = Player.STATE_IDLE
        mLastReportedPlayWhenReady = false
        mSpeedPlaybackParameters = null
    }

    override val currentPosition: Long
        get() = if (mInternalPlayer == null) 0 else mInternalPlayer!!.currentPosition
    override val duration: Long
        get() = if (mInternalPlayer == null) 0 else mInternalPlayer!!.duration
    override val bufferedPercentage: Int
        get() = if (mInternalPlayer == null) 0 else mInternalPlayer!!.bufferedPercentage

    override fun setSurface(surface: Surface?) {
        if (mInternalPlayer != null) {
            mInternalPlayer!!.setVideoSurface(surface)
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        if (holder == null) setSurface(null) else setSurface(holder.surface)
    }

    override fun setVolume(v1: Float, v2: Float) {
        if (mInternalPlayer != null) mInternalPlayer!!.volume = (v1 + v2) / 2
    }

    override fun setLooping(isLooping: Boolean) {
        if (mInternalPlayer != null) mInternalPlayer!!.repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    override fun setOptions() {
        //准备好就开始播放
        mInternalPlayer!!.playWhenReady = true
    }

    override var speed: Float
        get() = if (mSpeedPlaybackParameters != null) {
            mSpeedPlaybackParameters!!.speed
        } else 1f
        set(speed) {
            val playbackParameters = PlaybackParameters(speed)
            mSpeedPlaybackParameters = playbackParameters
            if (mInternalPlayer != null) {
                mInternalPlayer!!.setPlaybackParameters(playbackParameters)
            }
        }

    // no support
    override val tcpSpeed: Long
        get() =// no support
            0

    override fun onPlaybackStateChanged(state: Int) {
        if (mPlayerEventListener == null) return
        if (mIsPreparing) return
        if (!mLastReportedPlayWhenReady || mLastReportedPlaybackState != state) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    mPlayerEventListener!!.onInfo(MEDIA_INFO_BUFFERING_START, bufferedPercentage)
                    mIsBuffering = true
                }
                Player.STATE_READY -> if (mIsBuffering) {
                    mPlayerEventListener!!.onInfo(MEDIA_INFO_BUFFERING_END, bufferedPercentage)
                    mIsBuffering = false
                }
                Player.STATE_ENDED -> mPlayerEventListener!!.onCompletion()
                Player.STATE_IDLE -> {
                }
            }
            mLastReportedPlaybackState = state
            mLastReportedPlayWhenReady = true
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (mPlayerEventListener != null) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        if (mPlayerEventListener != null) {
            mPlayerEventListener!!.onVideoSizeChanged(width, height)
            if (unappliedRotationDegrees > 0) {
                mPlayerEventListener!!.onInfo(MEDIA_INFO_VIDEO_ROTATION_CHANGED, unappliedRotationDegrees)
            }
        }
    }

    override fun onRenderedFirstFrame() {
        if (mPlayerEventListener != null && mIsPreparing) {
            mPlayerEventListener!!.onInfo(MEDIA_INFO_VIDEO_RENDERING_START, 0)
            mIsPreparing = false
        }
    }
}