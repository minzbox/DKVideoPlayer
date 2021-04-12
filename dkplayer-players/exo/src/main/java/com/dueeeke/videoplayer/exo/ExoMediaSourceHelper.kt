package com.dueeeke.videoplayer.exo

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File

class ExoMediaSourceHelper private constructor(context: Context) {
    private val mUserAgent: String
    private val mAppContext: Context = context.applicationContext
    private var mHttpDataSourceFactory: HttpDataSource.Factory? = null
    private var mCache: Cache? = null
    fun getMediaSource(uri: String): MediaSource {
        return getMediaSource(uri, null, false)
    }

    fun getMediaSource(uri: String, headers: Map<String, String>?): MediaSource {
        return getMediaSource(uri, headers, false)
    }

    fun getMediaSource(uri: String, isCache: Boolean): MediaSource {
        return getMediaSource(uri, null, isCache)
    }

    fun getMediaSource(uri: String, headers: Map<String, String>?, isCache: Boolean): MediaSource {
        val contentUri = Uri.parse(uri)
        if ("rtmp" == contentUri.scheme) {
            return ProgressiveMediaSource.Factory(RtmpDataSourceFactory(null))
                    .createMediaSource(MediaItem.Builder().setUri(uri).setAdTagUri(mAppContext.getString(R.string.ad_tag_url)).build())
        }
        val contentType = inferContentType(uri)
        val factory: DataSource.Factory = if (isCache) {
            cacheDataSourceFactory
        } else {
            dataSourceFactory
        }
        if (mHttpDataSourceFactory != null) {
            setHeaders(headers)
        }
        return when (contentType) {
            C.TYPE_DASH -> DashMediaSource.Factory(factory).createMediaSource(MediaItem.Builder().setUri(contentUri).setAdTagUri(mAppContext.getString(R.string.ad_tag_url)).build())
            C.TYPE_SS -> SsMediaSource.Factory(factory).createMediaSource(MediaItem.Builder().setUri(contentUri).setAdTagUri(mAppContext.getString(R.string.ad_tag_url)).build())
            C.TYPE_HLS -> HlsMediaSource.Factory(factory).createMediaSource(MediaItem.Builder().setUri(contentUri).setAdTagUri(mAppContext.getString(R.string.ad_tag_url)).build())
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.Builder().setUri(contentUri).setAdTagUri(mAppContext.getString(R.string.ad_tag_url)).build())
            else -> ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.Builder().setUri(contentUri).setAdTagUri(mAppContext.getString(R.string.ad_tag_url)).build())
        }
    }

    private fun inferContentType(fileName: String): Int {
        val fileNameTmp = Util.toLowerInvariant(fileName)
        return when {
            fileName.contains(".mpd") -> {
                C.TYPE_DASH
            }
            fileName.contains(".m3u8") -> {
                C.TYPE_HLS
            }
            fileName.matches(Regex(".*\\.ism(l)?(/manifest(\\(.+\\))?)?")) -> {
                C.TYPE_SS
            }
            else -> {
                C.TYPE_OTHER
            }
        }
    }

    private val cacheDataSourceFactory: DataSource.Factory
        get() {
            if (mCache == null) {
                mCache = newCache()
            }
            return CacheDataSource.Factory().setCache(mCache!!)
                    .setUpstreamDataSourceFactory(dataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }

    private fun newCache(): Cache {
        return SimpleCache(
                File(mAppContext.externalCacheDir, "exo-video-cache"),  //Cache directory
                LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024),  //Cache size, default 512M, implemented using LRU algorithm
                ExoDatabaseProvider(mAppContext))
    }

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    val dataSourceFactory: DataSource.Factory
        get() = DefaultDataSourceFactory(mAppContext, httpDataSourceFactory) //http->https redirection support

    /**
     * Returns a new HttpDataSource factory.
     *
     * @return A new HttpDataSource factory.
     */
    private val httpDataSourceFactory: DataSource.Factory
        get() {
            if (mHttpDataSourceFactory == null) {
                mHttpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent(mUserAgent)
                        .setTransferListener(null)
                        .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                        .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
                        .setAllowCrossProtocolRedirects(true)
            }
            return mHttpDataSourceFactory!!
        }

    private fun setHeaders(headers: Map<String, String>?) {
        val requestProperties: HttpDataSource.RequestProperties = HttpDataSource.RequestProperties()
        if (headers != null && headers.isNotEmpty()) {
            for ((key, value) in headers) {
                //If it is found that the user has passed the UA through the header,
                // the userAgent field in the HttpDataSourceFactory shall be replaced by the user’s forcibly
                if (TextUtils.equals(key, "User-Agent")) {
                    if (!TextUtils.isEmpty(value)) {
                        try {
                            val userAgentField = mHttpDataSourceFactory!!.javaClass.getDeclaredField("userAgent")
                            userAgentField.isAccessible = true
                            userAgentField[mHttpDataSourceFactory] = value
                        } catch (e: Exception) {
                            //ignore
                        }
                    }
                } else {
                    requestProperties.set(key, value)
                }
            }
        }
        if (requestProperties.snapshot.isNotEmpty())
            mHttpDataSourceFactory?.setDefaultRequestProperties(requestProperties.snapshot)
    }

    fun setCache(cache: Cache?) {
        mCache = cache
    }

    companion object {
        private var sInstance: ExoMediaSourceHelper? = null

        @JvmStatic
        fun getInstance(context: Context): ExoMediaSourceHelper? {
            if (sInstance == null) {
                synchronized(ExoMediaSourceHelper::class.java) {
                    if (sInstance == null) {
                        sInstance = ExoMediaSourceHelper(context)
                    }
                }
            }
            return sInstance
        }
    }

    init {
        mUserAgent = Util.getUserAgent(mAppContext, mAppContext.applicationInfo.name)
    }
}