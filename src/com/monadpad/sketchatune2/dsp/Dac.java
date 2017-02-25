package com.monadpad.sketchatune2.dsp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class Dac extends UGen {
	private final float[] localBuffer;
	private boolean isClean;
	private AudioTrack track;
	private final short [] target = new short[UGen.CHUNK_SIZE];
	private final short [] silentTarget = new short[UGen.CHUNK_SIZE];

    private boolean goodToGo = false;

    private float lastPan = 0;
    private float lastLeft = 1;
    private float lastRight = 1;

    private boolean closed = false;

	public Dac() {
		localBuffer = new float[CHUNK_SIZE];

//        setupTrack();
    }

    private boolean setupTrack(){
        int minSize = AudioTrack.getMinBufferSize(
                UGen.SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                UGen.SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                Math.max(UGen.CHUNK_SIZE*4, minSize),
                AudioTrack.MODE_STREAM);

        try {
            track.play();
            track.setStereoVolume(1, 1);


            goodToGo = true;
        } catch (IllegalStateException e){
            Log.w("MGH", "AudioTrack didn't play()");
            goodToGo = false;
        }
        return goodToGo;
	}
	
	public boolean render(final float[] _buffer) {
		if(!isClean) {
			zeroBuffer(localBuffer);

			isClean = true;
		}
		// localBuffer is always clean right here, does it stay that way?
		isClean = !renderKids(localBuffer);
		return !isClean; // we did some work if the buffer isn't clean
	}
	
//	public void open() {
//
//		track.play();
//	}
	
	public void tick() {
        if (closed)
            return;

        if (goodToGo || setupTrack()){
            render(localBuffer);

            if(isClean) {
                // sleeping is messy, so lets just queue this silent buffer
                track.write(silentTarget, 0, silentTarget.length);
            } else {
                for(int i = 0; i < CHUNK_SIZE; i++) {
                    target[i] = (short)(32768.0f*localBuffer[i]);
                }

                track.write(target, 0, target.length);
            }

        }
	}
	
	public void close() {
        closed = true;
        if (goodToGo){
            try {
                track.stop();
                track.release();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            goodToGo = false;
        }
	}

    public void fade(float level){

        if (goodToGo){
            track.setStereoVolume(lastLeft - level, lastRight - level);
        }
    }

    public void pan(float level){
        if (goodToGo || setupTrack()){
            if ( lastPan != level){
                lastPan = level;

                float left = 1;
                float right = 1;
                if (level > 0)
                    left = left - level;
                else
                    right = right + level;

                lastLeft = left;
                lastRight = right;
                track.setStereoVolume(left, right);
            }
        }
    }

    public float getLastPan() {
        return lastPan;
    }
}
