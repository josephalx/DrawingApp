package com.example.drawingappforkids

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context,attrs:AttributeSet):View(context, attrs){
    private var mDrawPath:CustomPath?=null
    private var mCanvasBitmap:Bitmap?=null
    private  var  mDrawPaint:Paint?=null
    private var mCanvasPaint:Paint?=null
    private  var mBrushSize:Float = 0.0f
    private var color=Color.BLACK
    private var canvas:Canvas?=null
    private val mPaths=ArrayList<CustomPath>()
    private val mUndoPath=ArrayList<CustomPath>()
    init {
        setupDrawing()
    }
    private fun setupDrawing()
    {
        mDrawPaint= Paint()
        mDrawPath=CustomPath(color,mBrushSize)
        mDrawPaint?.let {
            it.color=color
            it.style=Paint.Style.STROKE
            it.strokeJoin=Paint.Join.ROUND
            it.strokeCap=Paint.Cap.ROUND
        }
        mCanvasPaint= Paint(Paint.DITHER_FLAG)
        mBrushSize=20.0f
    }
    fun setColor(colors:Int)
    {
        color=colors
        mDrawPaint?.color = color
    }

    fun undo()
    {
        if(mPaths.size> 0)
        {
            mUndoPath.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }
    fun redo()
    {
        if(mUndoPath.size>0)
        {
            mPaths.add(mUndoPath.removeAt( mUndoPath.size-1))
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas=Canvas(mCanvasBitmap!!)


    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!,0f,0f,mCanvasPaint!!)
        for(path in mPaths)
        {
            mDrawPaint!!.strokeWidth=path.brushThickness
            mDrawPaint!!.color=path.color
            canvas.drawPath(path,mDrawPaint!!)
        }
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth=mDrawPath!!.brushThickness
            mDrawPaint!!.color=mDrawPath!!.color
        canvas.drawPath(mDrawPath!!,mDrawPaint!!)}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX=event?.x
        val touchy=event?.y
        when(event?.action)
        {
            MotionEvent.ACTION_DOWN->{
                mDrawPath!!.color=color
                mDrawPath!!.brushThickness=mBrushSize
                mDrawPath!!.reset()
                if(touchX!=null &&touchy!=null)
                { mDrawPath!!.moveTo(touchX, touchy) }
            }
            MotionEvent.ACTION_MOVE->{
                if(touchX!=null &&touchy!=null)
                { mDrawPath!!.lineTo(touchX, touchy) }
            }
            MotionEvent.ACTION_UP->{
                mPaths.add(mDrawPath!!)
                mDrawPath=CustomPath(color,mBrushSize)
            }
            else-> return false
        }
        invalidate()
        return true
    }

    fun setSize(size:Float)
    {
        mBrushSize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,size,resources.displayMetrics)
        mDrawPaint!!.strokeWidth=mBrushSize
    }
    internal inner class  CustomPath(var color:Int, var brushThickness:Float): Path()
}