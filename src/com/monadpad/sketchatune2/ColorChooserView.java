package com.monadpad.sketchatune2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorChooserView extends View {

    private boolean hasChosen = false;
    private Paint[] colors;
    private String[] colorDesc;
    private int nShowingColumns = 0;
    private int nRowsPerColumn = 0;
    private int nGrooveletSize = 90;
    private int nInstrumentWidth = nGrooveletSize * 2;

    private IColorChangeListener mColorChangeListener;

    private Paint instrumentDescPaint;

    public ColorChooserView(Context context, AttributeSet attrs) {
        super(context, attrs);

        colors = InstrumentFactory.getColors();
        colorDesc = context.getResources().getStringArray(R.array.instruments);

        instrumentDescPaint = new Paint();
        instrumentDescPaint.setARGB(255, 0, 0, 0);
        instrumentDescPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        instrumentDescPaint.setTextSize(20);
    }

    @Override
    public void onDraw(Canvas canvas){

        nShowingColumns = 0;
        int iRow = 0;

        Rect r = new Rect() ;
        String desc = "";

        for (int ig = 0; ig < colors.length; ig++){
            colors[ig].setAlpha(255);
            if (iRow * nGrooveletSize + nGrooveletSize > getHeight()){
                nShowingColumns++;
                nRowsPerColumn = iRow;
                iRow = 0;
            }
            r.set(nShowingColumns * nInstrumentWidth, nGrooveletSize * iRow ,
                    nShowingColumns * nInstrumentWidth + nInstrumentWidth, nGrooveletSize + nGrooveletSize * iRow);

            canvas.drawRect(r, colors[ig]);
//            if (showColorDesc){
                if (ig < colorDesc.length)
                    desc = colorDesc[ig];
                canvas.drawText(desc,
                        nShowingColumns * nInstrumentWidth + nGrooveletSize/5,
                        nGrooveletSize * iRow + nGrooveletSize/2, instrumentDescPaint);
//            }
            iRow++;

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){

        if (hasChosen)
            return true;

        float y = e.getY();
        float x = e.getX();
        int nStart, nStop;

        for (int ic = 0; ic < nShowingColumns + 1; ic++){
            if (x < nInstrumentWidth * (ic + 1)){

                if (nRowsPerColumn == 0){
                    nStart = 0;
                    nStop = colors.length;
                }
                else {
                    nStart = ic * nRowsPerColumn;
                    nStop = Math.min(nRowsPerColumn, colors.length -  ic  * nRowsPerColumn);
                }
                for (int ig= 0 ; ig < nStop; ig++){
                    if (y <= nGrooveletSize * (ig + 1)){

//                        instrumentSettings(ig + nStart);
                        if (mColorChangeListener != null)
                            mColorChangeListener.onColorChange(colors[ig + nStart], ig + nStart);
                        setBackgroundColor(colors[ig + nStart].getColor());
                        hasChosen = true;
                        return true;
                    }
                }
                break;
            }
        }
        return true;
    }

    interface IColorChangeListener{
        void onColorChange(Paint p, int index);

    }

    void setColorChangeListener(IColorChangeListener l){
        mColorChangeListener = l;
    }

}
