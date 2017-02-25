package com.monadpad.sketchatune2;

import android.graphics.Paint;

public class InstrumentFactory {

    final private static Paint[] colors;
    static {

        /*        EmbossMaskFilter mEmboss;
        mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 },
                0.4f, 6, 3.5f);
        CornerPathEffect cornerPathEffect = new CornerPathEffect(10);
*/
        colors = new Paint[11] ;
        for (int i = 0; i < colors.length; i++){
            colors[i] = new Paint();

            colors[i].setAntiAlias(true);
            colors[i].setDither(true);

            colors[i].setStyle(Paint.Style.FILL_AND_STROKE);
            colors[i].setStrokeWidth(4);
            colors[i].setShadowLayer(6, 0,0, 0xFFFFFFFF);
//            colors[i].setMaskFilter(mEmboss);
//            colors[i].setPathEffect(cornerPathEffect);
        }

        colors[0].setARGB(255, 255,255,255);
        colors[1].setARGB(255, 255,0,0);
        colors[2].setARGB(255, 255,255,0);
        colors[3].setARGB(255, 30,255,130);
        colors[4].setARGB(255, 30,30,255);
        colors[5].setARGB(255, 255,140,0);
        colors[6].setARGB(255, 158,158,158);
        colors[7].setARGB(255, 0,255,255);
        colors[8].setARGB(255, 128,0,128);
        colors[9].setARGB(255, 99,45,255);
        colors[10].setARGB(255, 99,255,8);
    }


    public static Paint[] getColors(){
        return colors;
    }

    static int getInstrumentColor(int instrument){
        return colors[instrument].getColor();
    }

}
