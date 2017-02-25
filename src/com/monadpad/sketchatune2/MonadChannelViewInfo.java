package com.monadpad.sketchatune2;

import android.graphics.Path;

public class MonadChannelViewInfo {

    int instrument;
    Path path;
    final Path movedPath;
    MonadPad3DView.TouchList touchList = null;
    float originX;
    float originY;
    int lastDrawnOverlap = -1;

    public MonadChannelViewInfo(int instrument){
        this.instrument = instrument;
        movedPath = new Path();
    }

    MonadPad3DView.TouchList getTouchList(){
        return touchList;
    }

    void setTouchList(MonadPad3DView.TouchList touchList){
        this.touchList = touchList;
    }


}
