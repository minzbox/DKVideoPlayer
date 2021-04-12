package com.dueeeke.dkplayer.util

import android.text.TextUtils
import androidx.collection.LruCache
import com.dueeeke.videoplayer.player.ProgressManager

class ProgressManagerImpl : ProgressManager() {
    override fun saveProgress(url: String?, progress: Long) {
        if (TextUtils.isEmpty(url)) return
        if (progress == 0L) {
            clearSavedProgressByUrl(url)
            return
        }
        mCache.put(url.hashCode(), progress)
    }

    override fun getSavedProgress(url: String?): Long {
        return if (TextUtils.isEmpty(url)) 0 else mCache[url.hashCode()] ?: return 0
    }

    fun clearAllSavedProgress() {
        mCache.evictAll()
    }

    private fun clearSavedProgressByUrl(url: String?) {
        mCache.remove(url.hashCode())
    }

    companion object {
        //Save 100 records
        private val mCache = LruCache<Int, Long>(100)
    }
}