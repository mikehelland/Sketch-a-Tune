package com.monadpad.sketchatune2.record;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Process;
import android.preference.PreferenceManager;

import java.util.ArrayList;

public class MonadaphoneThread extends Thread {

    private ArrayList<MonadaphoneChannel> channels = new ArrayList<MonadaphoneChannel>();
    private ArrayList<MonadaphoneChannel> qChannels = new ArrayList<MonadaphoneChannel>();

    private MonadaphoneChannel liveChannel = null;
    private long loopDuration;
    private float lineLength;
    private float lineStart;
    private boolean looping = false;
    private long loopCounter = 0;
    long nowInLoop;

    private String scale;
    private int octaves;
    private int autoremove;
    private int base;

    private PcmWriter pcmWriter;
    private long resetTime = 0;
    private int nextChannel = 0;

    public MonadaphoneThread(Context ctx, PcmWriter pcmW){
        final SharedPreferences synthPrefs = PreferenceManager
        .getDefaultSharedPreferences(ctx);

        scale = synthPrefs.getString("quantizer", "0,2,4,5,7,9,11");
        octaves = Integer.parseInt(synthPrefs.getString("octaves", "4"));
        autoremove = 2;
        base = Integer.parseInt(synthPrefs.getString("base", "36"));

        pcmWriter = pcmW;


        MonadaphoneChannel mpc = newChannel(0);
        mpc.setup(0, scale, octaves, base);
        mpc.record(pcmWriter);
        mpc = newChannel(0);
        mpc.setup(0, scale, octaves, base);
        mpc.record(pcmWriter);

    }

    public void setupLoop(long pLoopDuration) {
        loopDuration = pLoopDuration;
        lineStart =  channels.get(0).getLow();
        lineLength = channels.get(0).getHigh() - lineStart;
    }

    public void setupLoopFromGallery(long pLoopDuration, float lStart, float lLength,
                                     String pscale, int poctaves, int pbase, int pautoremove) {
        loopDuration = pLoopDuration;
        lineStart =  lStart;
        lineLength = lLength;
        if (poctaves > 0){
            scale = pscale;
            octaves = poctaves;
            base = pbase;
            autoremove = pautoremove;
        }
    }


    public void startLoop(){
        loopCounter = System.currentTimeMillis();
        looping = true;
    }

    public void prepareChannel(MonadaphoneChannel chan){
        chan.prepareLoop(loopDuration, lineStart, lineLength);
        if (!(liveChannel == null)){
            liveChannel.finish();
            qChannels.add(liveChannel);
            liveChannel = null;
        }
    }

    public MonadaphoneChannel getNextChannel(int instrument){
        MonadaphoneChannel mpc;
        if(nextChannel == 0){
            nextChannel = 1;
            mpc =  channels.get(0);
        }else{
            nextChannel = 0;
            mpc = channels.get(1);
        }
        mpc.clear();
        mpc.setup(instrument, scale, octaves, base);
        return mpc;
    }

    public MonadaphoneChannel newChannel(int instrument) {
        MonadaphoneChannel mpC = new MonadaphoneChannel(instrument, scale, octaves, base);

        channels.add(mpC);


        //if there's an autoremove in place, get rid of the last one
        if (autoremove > 0 && channels.size() > autoremove){
            channels.get(0).finish();
            qChannels.add(channels.get(0));
            channels.remove(0);
        }

        return mpC;
    }

    public MonadaphoneChannel newLiveChannel(int instrument) {
        MonadaphoneChannel mpC = new MonadaphoneChannel(instrument, scale, octaves, base);
        liveChannel = mpC;
        return mpC;
    }

    public void drawChannels(Canvas canvas, Paint fPaint) {
        for (int ic = 0; ic < channels.size(); ic++) {
            channels.get(ic).draw(canvas);
        }
        if (!(liveChannel == null)){
            liveChannel.draw(canvas);
        }

        if (looping){
            //float x = ((float)((nowInLoop / loopDuration)) * lineLength) + lineStart;
            float x = lineStart;
            canvas.drawLine(x, 0, x, canvas.getHeight(), fPaint);
            x = lineLength + lineStart;
            canvas.drawLine(x, 0, x, canvas.getHeight(), fPaint);
        }

    }


    @Override
    public void run() {
//        Log.d(TAG, "started audio rendering");

        System.gc();
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        long now;
        long lastNowInLoop = 0;
        boolean resetChannels;
        boolean keepRunning = true;
        while (keepRunning) {
            nowInLoop = 0;
            resetChannels = false;
            if (looping){
                now = System.currentTimeMillis();
                nowInLoop = now - loopCounter;
                if (nowInLoop > loopDuration){
                    loopCounter = now;
                    resetChannels = true;
                    nowInLoop = 0;
                }

            }
            for (int ic = 0; ic < channels.size(); ic++) {
                if (resetChannels)
                      channels.get(ic).reset();
                channels.get(ic).update(nowInLoop);
            }
            if (!(liveChannel == null)){
                liveChannel.update(nowInLoop);
            }
            for (int ic = 0;ic < qChannels.size(); ic++){
                if (System.currentTimeMillis() > qChannels.get(ic).getFinishTime()){
                    qChannels.get(ic).close();
                    qChannels.remove(ic);
                    ic--;
                }
                else{
                    qChannels.get(ic).update(nowInLoop);
                }
            }
            if (channels.size() + qChannels.size() > 0)
                pcmWriter.flush();

            // if its over, let's interuupt
            if (resetTime > 0 && System.currentTimeMillis() > resetTime){
                keepRunning = false;
            }

        }

        looping = false;
    }

    public void reset(){
        reset(false);
    }
    
    public void reset(boolean hard) {
        looping = false;
        while( channels.size() > 0){
            channels.get(0).finish();
            qChannels.add(channels.get(0));
            channels.remove(0);
        }


        resetTime = System.currentTimeMillis() + (hard ? 100 : 3300);
        //channels.clear();
    }

    public int getChannelCount(){
        return channels.size();
    }

    public float getBoundary(){
        return lineStart + lineLength;
    }

    public String getGrooveInfo(String orientation){
        //duration,linestart,linelength
        String grooveInfo = Long.toString(loopDuration) + ";" + Float.toString(lineStart)
                + ";" + Float.toString(lineLength) + ";" + scale + ";" + Integer.toString(octaves)
                + ";" + Integer.toString(base) + ";" + Integer.toString(autoremove) + ";" + orientation;
        for (int ic = 0; ic<channels.size(); ic++){
            grooveInfo = grooveInfo.concat(":");
            grooveInfo = grooveInfo.concat(channels.get(ic).getChannelInfo());
        }
        return grooveInfo;
    }



}

//            ugDac.close();
