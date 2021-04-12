package com.dueeeke.videoplayer.render

import android.content.Context

/**
 * This interface is used to extend your own rendering View. The method of use is as follows:
 * 1. Inherit IRenderView to implement your own rendering View.
 * 2. Override createRenderView to return to the rendered View in step 1.
 * Please refer to the implementation of [TextureRenderView] and [TextureRenderViewFactory].
 */
abstract class RenderViewFactory {
    abstract fun createRenderView(context: Context?): IRenderView?
}