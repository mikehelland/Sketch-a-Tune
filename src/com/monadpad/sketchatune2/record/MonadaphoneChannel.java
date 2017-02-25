package com.monadpad.sketchatune2.record;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import com.monadpad.sketchatune2.record.dsp.*;

import java.util.ArrayList;

public class MonadaphoneChannel {

    private boolean looping = false;
    private final ArrayList<Path> paths = new ArrayList<Path>();
    private Path path;

    private float[] scale;
    private int octaves;
    private float base;

    private boolean envActive = false;
    private final WtOsc ugOscA1 = new WtOsc();
    private final ExpEnv ugEnvA = new ExpEnv();
    public final Dac ugDac = new Dac();
    private final Delay ugDelay = new Delay(UGen.SAMPLE_RATE / 2);
    private final Flange ugFlange = new Flange(UGen.SAMPLE_RATE / 64, 0.25f);
    private boolean delayed = false;
    private boolean flanged = false;

    private final ArrayList<Float> xHistory = new ArrayList<Float>();
    private final ArrayList<Float> yHistory = new ArrayList<Float>();
    private final ArrayList<Float> xpHistory = new ArrayList<Float>();
    private final ArrayList<Float> ypHistory = new ArrayList<Float>();
    private final ArrayList<Long> tHistory = new ArrayList<Long>();
    private final ArrayList<Float> fHistory =  new ArrayList<Float>();
    private int currentI = 0;

    private float originX = -1;
    private float originY = -1;

    private Paint pathPaint;
    private Paint fingerPaint;

    private int instrument;
    private boolean virgin = true;

    public MonadaphoneChannel(int pinstrument, String pScale, int pOctave, int pBase){
        setup(pinstrument, pScale,  pOctave, pBase);
        ugDac.open();

    }

    public void setup(int pinstrument, String pScale, int pOctave, int pBase){

        boolean delay, flange, softTimbre, softEnvelope, softe, softt;
        pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(4);
        pathPaint.setShadowLayer(6, 0,0, 0xFFFFFFFF);
        instrument = pinstrument;
        if (pinstrument == 0)
        {
            pathPaint.setARGB(255, 255,255,255);
            delay = true; flange = false; softt = true; softe = true;
        }
        else if (pinstrument == 1)
        {
            pathPaint.setARGB(255, 255,0,0);
            delay = false; flange = false; softt = true; softe = false;
        }
        else if (pinstrument == 2)
        {
            pathPaint.setARGB(255, 255,255,0);
            delay = true; flange = false; softt = false; softe = true;
        }
        else if (pinstrument == 3)
        {
            pathPaint.setARGB(255, 30,255,130);
            delay = true; flange = false; softt = false; softe = false;
        }
        else if (pinstrument == 4)
        {
            pathPaint.setARGB(255, 30,30,255);
            delay = false; flange = true; softt = false; softe = true;
        }
        else if (pinstrument == 5)
        {
            pathPaint.setARGB(255, 255,140,0);
            delay = false; flange = false; softt = false; softe = false;
        }
        else if (pinstrument == 6)
        {
            pathPaint.setARGB(255, 158,158,158);
            delay = false; flange = false; softt = false; softe = true;
        }
        else
        {
            pathPaint.setARGB(255, 255,255,255);
            delay = false; flange = false; softt = true; softe = true;
        }
        softEnvelope = softe;
        softTimbre = softt;

        scale = buildScale(pScale);
        octaves = pOctave;
        base = (float) pBase;


        fingerPaint = new Paint();
        fingerPaint.setColor(pathPaint.getColor());
        fingerPaint.setStyle(Paint.Style.FILL);
        fingerPaint.setShadowLayer(10, 0, 0, 0xFFFFFFFF);


        paths.clear();
        path = new Path();
        paths.add(path);


        if (softTimbre) {
            ugOscA1.fillWithHardSin(7.0f);
        } else {
            ugOscA1.fillWithSqrDuty(0.6f);
        }

        if (delayed){
            ugEnvA.unchuck(ugDelay);
            if (flanged){
    //            ugDelay.unchuck(ugFlange).unchuck(ugDac);
            } else {
    //            ugDelay.unchuck(ugDac);
            }
        }else {
            if (flanged) {
                ugEnvA.unchuck(ugFlange);
    //            ugFlange.unchuck(ugDac);
            } else {
      //          ugEnvA.unchuck(ugDac);
            }
        }
        delayed = false;
        flanged = false;


        if (delay) {
            delayed = true;
            ugEnvA.chuck(ugDelay);

            if (flange) {
                flanged = true;
                ugDelay.chuck(ugFlange).chuck(ugDac);
            } else {
                ugDelay.chuck(ugDac);
            }
        } else {
            if (flange) {
                flanged = true;
                ugEnvA.chuck(ugFlange);
                ugFlange.chuck(ugDac);
            } else {
                ugEnvA.chuck(ugDac);
            }
        }


        ugOscA1.chuck(ugEnvA);
        if (!softEnvelope) {
            ugEnvA.setFactor(ExpEnv.hardFactor);
        }

        ugEnvA.setActive(true);
        envActive = true;

        //set the gain?
        //ugEnvA.setGain(2*y*y);
        ugEnvA.setGain(2);

    }

    public void record(PcmWriter pcw){
        ugDac.record(pcw);
    }
    public void stopRecord(){
        ugDac.stopRecord();
    }

    public void setFreq(float freq){
        ugOscA1.setFreq(freq);
    }

    public void update(long nowInLoop){
       // ugDac.open();

        if (looping){
            if (virgin){
                //find out where we are in the loop and set the right I
                for (int ii=0; ii<tHistory.size(); ii++){
                    if (tHistory.get(ii) >= nowInLoop){
                        currentI = ii;
                        virgin =false;
                        break;
                    }
                }
            }
            if (tHistory.size() > currentI){
                float freq = fHistory.get(currentI);
                if (freq == -1){
                    ugEnvA.setActive(false);
                   envActive = false;
                    currentI++;
                }
                else {
                    if (tHistory.get(currentI) <= nowInLoop){
                            if (!envActive){
                                 ugEnvA.setActive(true);
                                envActive = true;
                            }

                            ugOscA1.setFreq(freq);

                        currentI++;
                    }

                }
           }
            else{
                ugEnvA.setActive(false);
                envActive = false;

            }

        }

        ugDac.tick();

    }

    public void draw(Canvas canvas){

        if (originX > -1){
            canvas.drawCircle(originX, originY, 8, fingerPaint);

            for (int ip = 0; ip < paths.size(); ip++){
                   canvas.drawPath(paths.get(ip), pathPaint);
            }

        }
    }


    public void addXY(float ex, float ey, float x, float y){

        float freq;
        if (y == -1){
        	looping = false;
            path = new Path();
            paths.add(path);
            path.moveTo(ex, ey);
            freq = -1;
        }
        else{

            if (originX == -1){
                originX = ex;
                originY = ey;
            } else {
                path.lineTo(ex, ey);
            }
            path.moveTo(ex, ey);

            //set the freq
            freq = buildFrequency(scale, octaves, y, base);
            setFreq(freq);

        }


        xHistory.add(ex);
        yHistory.add(ey);
        xpHistory.add(x);
        ypHistory.add(y);
        fHistory.add(freq);

    }

    public void clear(){
        xHistory.clear();
        yHistory.clear();
        xpHistory.clear();
        ypHistory.clear();
        fHistory.clear();
        tHistory.clear();

        originX = -1;
        originY = -1;
        virgin = true;
        looping = false;
        currentI = 0;
    }

    public float getHigh(){
        float high = 0;
        if (xHistory.size() > 0){
            high = xHistory.get(0);
            for (int ix = 0; ix < xHistory.size(); ix++){
                float newNumber = xHistory.get(ix);
                if (newNumber > high) high = newNumber;
            }
        }
        return high;
    }

    public float getLow(){
        float low = 0;
        if (xHistory.size() > 0){
            low = xHistory.get(0);
            for (int ix = 0; ix < xHistory.size(); ix++){
                float newNumber = xHistory.get(ix);
                if (newNumber < low) low = newNumber;
            }
        }
        return low;
    }


    public void prepareLoop(long loopDuration, float lineStart, float lineLength){
       tHistory.clear();
       for (int ix = 0; ix < xHistory.size(); ix++){
            tHistory.add( (long) (((xHistory.get(ix) - lineStart) / lineLength) *  (float) loopDuration));
       }
       mute();
       looping = true;

    }

        public void popCherry(){
            virgin = false;
        }
    public void reset(){

        if(looping){
            mute();
        }
        currentI = 0;
    }

    private long finishTime = 0;
    public void finish(){
        looping = false;
        ugEnvA.setGain(0);
        mute();
        finishTime = System.currentTimeMillis() + 3000;
    }
    public long getFinishTime(){
        return finishTime;
    }

    public void mute(){
        ugEnvA.setActive(false);
        envActive = false;

    }


    static float[] buildScale(String quantizerString) {
        if (quantizerString != null && quantizerString.length() > 0) {
            String[] parts = quantizerString.split(",");
            float[] scale = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                scale[i] = Float.parseFloat(parts[i]);
            }
            return scale;
        } else {
            return null;
        }
    }

    static float buildFrequency(final float[] scale, final int octaves, float input, float pBase) {
        input = Math.min(Math.max(input, 0.0f), 1.0f);
        //final float base = 24;
        //final float base = 48;
        final float base = pBase;

        float mapped;
        if (scale == null) {
            mapped = base + input * octaves * 12.0f;
        } else {
            int idx = (int) ((scale.length * octaves + 1) * input);
            mapped = base + scale[idx % scale.length] + 12 * (idx / scale.length);
        }
        return (float) Math.pow(2, (mapped - 69.0f) / 12.0f) * 440.0f;

    }

    public String getChannelInfo(){
        String channelInfo = Integer.toString(instrument) + ";";
        for (int ix = 0; ix<xpHistory.size(); ix++){
            if (ix > 0) channelInfo = channelInfo + ";";
            channelInfo = channelInfo + Float.toString(xpHistory.get(ix)) + ";" + Float.toString(ypHistory.get(ix));
        }
        return channelInfo;
    }

    public void close(){
        ugDac.close();
        ugDac.stopRecord();
    }
}
