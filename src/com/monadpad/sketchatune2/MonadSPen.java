package com.monadpad.sketchatune2;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.samsung.spen.lib.input.SPenEventLibrary;
import com.samsung.spensdk.applistener.SPenHoverListener;
import com.samsung.spensdk.applistener.SPenTouchListener;

import java.util.List;
import java.util.Locale;


/**
 * User: m
 * Date: 8/9/13
 * Time: 7:06 PM
 */
public class MonadSPen {

    private SPenEventLibrary mSPenEventLibrary;
    private Context mContext;

    private static final String SAMSUNG = "SAMSUNG";
    private static final String NOTE = "GT-N";
    private static final String SPEN_FEATURE = "com.sec.feature.spen_usp";
//    private final List<String> mSPenDevices;

    public boolean isSPenSupported() {
        FeatureInfo[] infos = mContext.getPackageManager().getSystemAvailableFeatures();
        for (FeatureInfo info : infos) {
            if (SPEN_FEATURE.equalsIgnoreCase(info.name)) {
                return true;
            }
        }

        if (Build.MODEL.toUpperCase(Locale.ENGLISH).startsWith(NOTE)) {
            return true;
        }

/*        if (SAMSUNG.equalsIgnoreCase(Build.MANUFACTURER)) {
            for (String model : mSPenDevices) {
                if (model.equalsIgnoreCase(Build.MODEL)) {
                    return true;
                }
            }
        }
  */
        return false;
    }

    public MonadSPen(final MonadView mv) {

        mContext = mv.getContext();

        if (isSPenSupported()) {

            mSPenEventLibrary = new SPenEventLibrary();
            //--------------------------------------------
            // Set S pen Touch Listener
            //--------------------------------------------
            mSPenEventLibrary.setSPenTouchListener(mv, new SPenTouchListener(){

                public boolean onTouchFinger(View view, MotionEvent event) {
        //            updateTouchUI(event.getX(), event.getY(), event.getPressure(), event.getAction(), "Finger");

                    mv.setLastUsed(MonadView.TOOL_FINGER);
                    // Update Current Color
                    return false;	// keep event in this view
                }

                public boolean onTouchPen(View view, MotionEvent event) {
        //            updateTouchUI(event.getX(), event.getY(), event.getPressure(), event.getAction(), "Pen");
                    mv.setLastUsed(MonadView.TOOL_PEN);


                    if (event.getAction() == MotionEvent.ACTION_UP) {// &&
                            //mv.getState() == MonadView.STATE_MID_DRAW) {
                        if (mv.touchEventUp(event, -2))
                            mv.waitForHoverExit();

                        return true;
                    }

                    // Update Current Color
                    return false;	// keep event in this view
                }

                public boolean onTouchPenEraser(View view, MotionEvent event) {
          //          updateTouchUI(event.getX(), event.getY(), event.getPressure(),event.getAction(),  "Pen-Eraser");

                    return false;	// keep event in this view
                }

                //@Override
                public void onTouchButtonDown(View view, MotionEvent event) {
                    Toast.makeText(mContext, "S Pen Button Down on Touch", Toast.LENGTH_SHORT).show();
                }

                //@Override
                public void onTouchButtonUp(View view, MotionEvent event) {
                    Toast.makeText(mContext, "S Pen Button Up on Touch", Toast.LENGTH_SHORT).show();
                }

            });


            //--------------------------------------------
            // [Custom Hover Icon Only]
            // Set Custom Hover Icon
            //--------------------------------------------
            //mSPenEventLibrary.setCustomHoveringIcon(mContext, mImageView, getResources().getDrawable(R.drawable.custom_hover_icon));

            //--------------------------------------------
            // [Hover Listener Only]
            // Set SPenHoverListener
            //--------------------------------------------
            mSPenEventLibrary.setSPenHoverListener(mv, new SPenHoverListener(){
                //@Override
                public boolean onHover(View view, MotionEvent event) {
//                    updateHoverUI(event.getX(), event.getY(), event.getPressure(), event.getAction(), "Hover");

                    if (event.getAction()==MotionEvent.ACTION_HOVER_EXIT) {
                        mv.onHoverExit(event);
                        Log.d("MGH spen", "HOVER EXIT");
                    }

                    return false;
                }

                //@Override
                public void onHoverButtonDown(View view, MotionEvent event) {

                }

                //@Override
                public void onHoverButtonUp(View view, MotionEvent event) {
                    //Toast.makeText(mContext, "S Pen Button Up on Hover", Toast.LENGTH_SHORT).show();
                }

            });

            Toast.makeText(mContext, "S Pen ready!", Toast.LENGTH_SHORT).show();


        //--------------------------------------------
        // [Hover Listener & Custom Hover Icon]
        // Set S pen HoverListener & Custom Hover Icon
        //--------------------------------------------
//        mSPenEventLibrary.setSPenCustomHoverListener(mContext, mv, new SPenHoverListener(){
//        }, getResources().getDrawable(R.drawable.tool_ic_pen));

        }
    }

    private void updateTouchUI(float x, float y, float pressure, int action, String tool){
//        mPressure.setText("Pressure : " + String.format("%.3f", pressure));

/*        if(action==MotionEvent.ACTION_DOWN)			Log.d("MGH spen", "DOWN");
        else if(action==MotionEvent.ACTION_MOVE)	Log.d("MGH spen", "MOVE");
        else if(action==MotionEvent.ACTION_UP)		Log.d("MGH spen", "UP");
        else if(action==MotionEvent.ACTION_CANCEL)	Log.d("MGH spen", "CANCEL");
        else 										Log.d("MGH spen", "Unknow");
        Log.d("MGH spen", tool);
        */
    }

    private void updateHoverUI(float x, float y, float pressure, int action, String tool){
/*        if(action==MotionEvent.ACTION_HOVER_ENTER)		Log.d("MGH spen", "HOVER ENTER");
        else if(action==MotionEvent.ACTION_HOVER_MOVE)	Log.d("MGH spen", "HOVER MOVE");
        else if(action==MotionEvent.ACTION_HOVER_EXIT)	Log.d("MGH spen", "HOVER EXIT");
        else 											Log.d("MGH spen", "Unknow");
 */
    }


}
