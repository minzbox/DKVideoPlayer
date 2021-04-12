package com.dueeeke.videoplayer.render

import android.content.Context

class TextureRenderViewFactory : RenderViewFactory() {

    companion object {
        fun create(): TextureRenderViewFactory {
            return TextureRenderViewFactory()
        }
    }

    override fun createRenderView(context: Context?): IRenderView? {
        if (context == null)
            return null
        return TextureRenderView(context)
    }
}