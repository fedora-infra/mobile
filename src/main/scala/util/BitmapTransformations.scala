package org.fedoraproject.mobile.util

import android.graphics.{ Bitmap, BitmapShader, Canvas, Paint, RectF, Shader }

object BitmapTransformations {
  def roundCorners(b: Bitmap, r: Float): Bitmap = {
    val bitmapRounded = Bitmap.createBitmap(b.getWidth, b.getHeight, b.getConfig)
    val canvas = new Canvas(bitmapRounded)
    val paint = new Paint()
    paint.setAntiAlias(true)
    paint.setShader(new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
    canvas.drawRoundRect(new RectF(0.0f, 0.0f, b.getWidth.toFloat, b.getHeight.toFloat), r, r, paint)
    bitmapRounded
  }
}
