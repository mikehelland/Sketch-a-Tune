package com.monadpad.sketchatune2;

import android.os.Process;

import java.util.List;

public class MonadThread extends Thread {

    private MonadJam mJam;

    public MonadThread(MonadJam jam){
        mJam = jam;
    }

    private List<MonadChannel> channels;
    private List<MonadChannel> qChannels ;

    private long loopDuration;
    private boolean looping = false;
    private long loopCounter = 0;
    long nowInLoop;

    private boolean loopBack = false;

    int mode = 0;

    public boolean setupLoop(long pLoopDuration) {
//        if (channels.size() > 0){
            loopDuration = pLoopDuration;
            return true;
//        }   else return false;
    }

    public long startLoop(){
        long now = System.currentTimeMillis();
        startLoop(now);
        return now;
    }

    public void startLoop(long started) {
        loopCounter = started;
        looping = true;
    }

    @Override
    public void run() {

        System.gc();
        AudioThread audioThread = new AudioThread();
        if (mode == 0){
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        }
        else {
            audioThread.start();
        }
        long now;
        boolean resetChannels;

        MonadChannel liveChannel;
        while (!isInterrupted()) {
            channels = mJam.getChannels();
            qChannels = mJam.getQChannels();
            liveChannel = mJam.getLiveChannel();
            if (liveChannel == null && channels.size() == 0 && qChannels.size() == 0){
                try{
                    Thread.sleep(50);
                } catch (InterruptedException ignored){}
            }
            nowInLoop = 0;
            resetChannels = false;
            if (looping){
                now = System.currentTimeMillis();

                if (loopBack){
                    loopCounter = now;
                    resetChannels = true;
                    nowInLoop = 0;
                    loopBack = false;
                }
                else {
                    nowInLoop = now - loopCounter;
                    if (nowInLoop > loopDuration){
                        loopCounter = loopCounter + loopDuration;
                        resetChannels = true;
                        nowInLoop = 0;
                    }
                }

            }
            for (MonadChannel channel : channels) {
                if (resetChannels)
                    channel.reset();
                channel.update(nowInLoop);
            }
            if (!(liveChannel == null)){
                liveChannel.update(nowInLoop);
            }
            for (int ic = 0;ic < qChannels.size(); ic++){
                //TODO greater than 4?
                if (qChannels.size() > 4 || System.currentTimeMillis() > qChannels.get(ic).getFinishTime()){
                    qChannels.get(ic).close();
                    qChannels.remove(ic);
                    ic--;
                }
                else{
                    qChannels.get(ic).update(nowInLoop);
                }
            }

            // if its over, let's interuupt
            if (resetTime > 0 && System.currentTimeMillis() > resetTime){

                for (MonadChannel mc : channels){
                    mc.close();
                }
                for (MonadChannel mc : qChannels){
                    mc.close();
                }
                channels.clear();
                qChannels.clear();
                interrupt();

                //mainView.postInvalidate();
            }

            if (mode == 1){
                try {
                    Thread.sleep(70);
                } catch (InterruptedException ignored){
                    interrupt();
                }
            }
        }

        looping = false;
    }


    private long resetTime = 0;
    public void reset() {
        reset(500);
    }
    public void resetHardIn(int stopTime){
        resetTime = System.currentTimeMillis() + stopTime;
    }

    public void reset(int stopTime) {
        if (resetTime == 0){
            looping = false;

            // this reports errors, possibly because channels wasn't set by run
            // so check if there's a channels first
            if (channels == null)
                return;

            while( channels.size() > 0){
                channels.get(0).finish(stopTime);
                qChannels.add(channels.get(0));
                channels.remove(0);
            }

            resetTime = System.currentTimeMillis() +stopTime;
        }
    }

    public long getLoopCounter(){
        return loopCounter;
    }

    public void setLoopBack() {
        loopBack = true;
    }

    class AudioThread extends Thread {
        @Override
        public void run() {
//            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            while (MonadThread.this.isAlive()){
                List<MonadChannel> chans = mJam.getChannels();
                for (MonadChannel channel : chans) {
                    channel.tick();
                }
                chans =  mJam.getQChannels();
                for (MonadChannel channel : chans) {
                    channel.tick();
                }
                MonadChannel liveChannel = mJam.getLiveChannel();
                if (!(liveChannel == null)){
                    liveChannel.tick();
                }
            }
        }
    }
}
