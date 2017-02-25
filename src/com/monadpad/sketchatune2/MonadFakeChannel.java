package com.monadpad.sketchatune2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

public class MonadFakeChannel {
    private List<Path> paths = new ArrayList<Path>();
    private Path path;
    private Paint mPaint;

    public MonadFakeChannel(Paint paint){
        mPaint = paint;
    }

    public void draw(Canvas canvas){
        for (int ip = 0; ip < paths.size(); ip++){
            canvas.drawPath(paths.get(ip), mPaint);
        }
    }


    public void addXY(float ex, float ey, float x, float y){

        if (path == null || y == -1){
            path = new Path();
            paths.add(path);
        }
        else {
            path.lineTo(ex, ey);
        }
        path.moveTo(ex, ey);
    }

}
