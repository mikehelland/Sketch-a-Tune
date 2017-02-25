package com.monadpad.sketchatune2.record;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.monadpad.sketchatune2.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

class MonadaphoneView extends View {

    private MonadaphoneThread mThread ;
    private MonadaphoneChannel currentChannel;
    private boolean currentChannelIsLive = false;

    private long loopLengthCounter;
    private boolean loopIsSetup = false;

	Paint pathPaint = new Paint();
	final Paint fingerPaint = new Paint();

	float x, y;
	boolean isTouching = false;
	float lastX;
	float lastY;
	float lastx;
	long lastUp = 0;
	long nBreakThreshold = 700;
	int instrument = 0;
	int tutorial = 0;
    final private Context ctx;

    private PcmWriter pcmWriter = null;
    private Handler pcmHandler ;
    private Runnable pcmCallback;

    private int channelCount = 0;

	public MonadaphoneView(Context context, AttributeSet attrs) {
		super(context, attrs);
        ctx = context;

		setBackgroundColor(0xFF000000);

		pathPaint.setARGB(255, 255, 255, 255);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setStrokeWidth(4);
		pathPaint.setShadowLayer(6, 0, 0, 0xFFFFFFFF);

		fingerPaint.setARGB(128, 255, 0, 0);
		fingerPaint.setStyle(Paint.Style.FILL);
		fingerPaint.setShadowLayer(10, 0, 0, 0xFFFFFFFF);
        fingerPaint.setTextSize(20);

		setKeepScreenOn(true);
		setFocusable(true);

      //  mThread = new MonadaphoneThread(this, pcmWriter);
	}
    private Activity mActivity;
    public void setActivity(Activity a){
        mActivity = a;
    }
    public Activity getActivity(){
        return mActivity;
    }

	public void resetClear(){
        boolean saveIt = !fresh;
            
        reset();

        if (saveIt)
            save();
	}
    private ProgressDialog pdialog = null;
    private String ojName;
    private boolean wavSaved = false;

    public void save(){
        ojName = "";
        wavSaved = false;

        final Thread wavThread = new Thread(new SaveWAV());
        //wavThread.setPriority(Thread.MAX_PRIORITY);
        Log.d("MGH waveThread created and set", Integer.toString(wavThread.getPriority()));
        pcmHandler = new Handler();
        pcmCallback = new Runnable (){
            public void run(){

                if (pdialog != null){
                    pdialog.dismiss();
                }
                if (ojName.length() > 0){
                    copyAndPlay();
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                }
                else {
                    wavSaved = true;
                }
            }
        };

        final Dialog dl = new Dialog(getContext());
        dl.setTitle("Store this to your SD card?");
        dl.setContentView(R.layout.savewav);
        Button okButton = (Button)dl.findViewById(R.id.okButton);
        okButton.setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
                ojName = ((EditText)dl.findViewById(R.id.txtOjName)).getText().toString();
                dl.dismiss();
                wavThread.start();

                if (wavSaved){
                    copyAndPlay();
                }
                else {
                    //the pcmCallback will handle it
                    pdialog = ProgressDialog.show(getContext(), "",
                            "Saving Audio (WAV). Please wait...", true);
                }
            }
        } );
        ((Button)dl.findViewById(R.id.cancelButton)).setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
                dl.dismiss();
                pcmWriter.stop();
                pcmWriter = null;

            }
        } );

        dl.show();

    }

    public void reset(){
        if (!fresh){
            mThread.reset();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        loopIsSetup = false;
        lastUp = 0;
        fresh = true;
        channelCount = 0;

        invalidate();
	}
    public void hardReset(){
        if (pcmWriter != null){
            pcmWriter.stop();
            pcmWriter = null;
        }

        if (!fresh){
            mThread.reset(true);
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        loopIsSetup = false;
        lastUp = 0;
        fresh = true;
        channelCount = 0;

        invalidate();

    }


    private void copyAndPlay(){
        Log.d("MGH", "copyAndPlay start");
        MediaPlayer mp = new MediaPlayer();

        try {
            mp.setDataSource(ctx.openFileInput("temp.wav").getFD());

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            mp.prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mp.start();


        try{

        if (ojName.length() > 0){
            File ringDir = new File("/sdcard/ringtones/");
            if (!ringDir.exists())
                ringDir.mkdirs();
            FileChannel inChannel = ctx.openFileInput("temp.wav").getChannel();
            FileChannel outChannel = new FileOutputStream("/sdcard/ringtones/" + ojName + ".wav").getChannel();
            try
            {
                inChannel.transferTo(0, inChannel.size(), outChannel);
                Toast.makeText(ctx, "File /sdcard/ringtones/" + ojName + ".wav was saved.", Toast.LENGTH_LONG);

                inChannel.close();
                inChannel = null;
                File k = new File(ringDir, ojName + ".wav");

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
                values.put(MediaStore.MediaColumns.TITLE, ojName);
                values.put(MediaStore.MediaColumns.SIZE, k.length());
                values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav");
                values.put(MediaStore.Audio.Media.ARTIST, "MonadPad");
                values.put(MediaStore.Audio.Media.DURATION, mp.getDuration());
                values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                values.put(MediaStore.Audio.Media.IS_ALARM, false);
                values.put(MediaStore.Audio.Media.IS_MUSIC, true);

                //Insert it into the database
                Uri uri = MediaStore.Audio.Media.getContentUriForPath(k.getAbsolutePath());
                Uri newUri = ctx.getContentResolver().insert(uri, values);

            }
            finally
            {
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();

            }
        }
        }
        catch (IOException e){
            Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG);
            System.out.println(e.getMessage());
        }
        Log.d("MGH", "copyAndPlay stop");
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float ex = event.getX();
		float ey = event.getY();

		x = ex / getWidth();
		y = 1 - ey / getHeight();

	
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (isTouching)
			    currentChannel.addXY(ex, ey, x, y);
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {

            isTouching = true;
            if (fresh) {
                //lock the screen
                if (getWidth() > getHeight()){
                     mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                else {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }

                pcmWriter = new PcmWriter(ctx, "temp.pcm" );
                mThread = new MonadaphoneThread(ctx, pcmWriter);
                mThread.start();
                pcmWriter.start();
                loopLengthCounter = System.currentTimeMillis();
                fresh = false;
            }

            instrumentSettings(instrument);

            if (lastUp > 0 && System.currentTimeMillis() - lastUp < nBreakThreshold) {
                currentChannel.addXY(ex, ey, x, -1);
                currentChannel.addXY(ex, ey, x, y);

                if (channelCount == 1){
                    loopIsSetup = false;
                }

            } else {
                if (loopIsSetup && ex > mThread.getBoundary()){
                    currentChannel = mThread.newLiveChannel(instrument);
                    currentChannelIsLive = true;
                } else{
//                        currentChannel = mThread.newChannel(instrument);
                    currentChannel = mThread.getNextChannel(instrument);
                    channelCount++;
                }
                currentChannel.addXY(ex, ey, x, y);
            }
		}

		if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isTouching)
            {

                isTouching = false;
                currentChannel.addXY(ex, ey, x, y);
                if (currentChannelIsLive){
                    lastUp = 0;
                }
                else {
                    lastUp = System.currentTimeMillis();
                }
                currentChannelIsLive = false;

                if (!loopIsSetup){
                    long loopLength = System.currentTimeMillis() - loopLengthCounter;
                    mThread.setupLoop(loopLength);
                    currentChannel.popCherry();
                }

                mThread.prepareChannel(currentChannel);

                if (!loopIsSetup){
                    mThread.startLoop();
                    loopIsSetup = true;
                }
            }
		}
		lastx = x;
		lastX = ex;
		lastY = ey;

		invalidate();

		return true;
	}

	private boolean fresh = true;

	@Override
	public void onDraw(Canvas canvas) {
		if (fresh && tutorial == 0) {

            canvas.drawText(getResources().getString(R.string.record_mode),
                    getWidth() * 0.20f, getHeight() * 0.12f, fingerPaint);

            canvas.drawText(getResources().getString(R.string.record_channels),
                    getWidth() * 0.20f, getHeight() * 0.26f, fingerPaint);
        }
        else{
            mThread.drawChannels(canvas, fingerPaint);
            if (isTouching) {
                canvas.drawCircle(lastX, lastY, 15, fingerPaint);
            }
        }
	}


	public void instrumentSettings(int pos){
		instrument = pos;
	}


    public String getGrooveInfo(){

        // need the linestart, line length, and duration
        String orientation = "P";
        if (getWidth() > getHeight())
            orientation = "L";
        String grooveInfo;
        if (fresh){
            grooveInfo = "";
        }
        else{
            grooveInfo = mThread.getGrooveInfo(orientation);
        }
        return grooveInfo;
    }


    public void setupFromGalleryPreDraw(String params, ViewTreeObserver.OnPreDrawListener obs){
        _setupFromGallery(params);
        getViewTreeObserver().removeOnPreDrawListener(obs);
    }

    private void _setupFromGallery(String params){
        int firstInst = instrument;
        String[] sLines = params.split(":");

        //the first line should have the duration, linestart, and linelength
        String[] line1 = sLines[0].split(";");
        long loopDuration = Long.valueOf(line1[1]);
        float lineStart = Float.valueOf(line1[2]);
        float lineLength = Float.valueOf(line1[3]);
        String scale = "";
        int octaves = 0;
        int base = 0;
        int autoremove = 0;
        if (line1.length > 4){
            scale = line1[4];
            octaves = Integer.valueOf(line1[5]);
            base= Integer.valueOf(line1[6]);
            autoremove = Integer.valueOf(line1[7]);

            if (line1[8].equals("P") && getHeight() > getWidth()){
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            else if (line1[8].equals("L") && getHeight() < getWidth()){
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                ((Main)mActivity).setGalleryData(params);
                if (line1[8].equals( "P")){
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                return;
            }
        }
        mThread = new MonadaphoneThread(ctx, pcmWriter);
        mThread.start();
        mThread.setupLoopFromGallery(loopDuration, lineStart, lineLength, scale, octaves, base, autoremove);

        //the next lines should have X and Y coords
        for (int ils=1; ils<sLines.length; ils++ ){

            if (!sLines[ils].contentEquals("0")){

                String[] sCoords = sLines[ils].split(";");

                if (sCoords.length > 1){
                    // the first value should be the instrument setting
                    instrumentSettings(Integer.valueOf(sCoords[0]));

                    MonadaphoneChannel chan = mThread.newChannel(instrument);
                    chan.mute();

                    float width = getWidth();
                    float height = getHeight();
                    float ojX;
                    float ojY;
                    float ojEY;
                    for (int iss=1; iss<sCoords.length - 1; iss =iss+ 2 ){

                        ojX = Float.valueOf(sCoords[iss]);
                        ojY = Float.valueOf(sCoords[iss+1]);
                        if (ojY == -1){
                            //see if there's a next digit to steal
                            if (sCoords.length >= iss+3){
                               ojEY = Float.valueOf(sCoords[iss+3]);
                            }  else {
                                ojEY = 0;
                            }
                        }
                        else{
                            ojEY = ojY;
                        }
                        chan.addXY(ojX * width, (1 - ojEY) * height, ojX, ojY);

                    }
                    chan.prepareLoop(loopDuration, lineStart, lineLength);

                }
            }
        }
        mThread.startLoop();
        loopIsSetup = true;
        fresh = false;

        instrumentSettings(firstInst);
    }

    public void setupFromGallery(String params){
        _setupFromGallery(params);
        postInvalidate();
    }

    //public Context getContext(){
    //    return ctx;
    //}

    private class SaveWAV implements Runnable {
        //@Override
        public void run() {

            final long timer = System.currentTimeMillis();
             //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            pcmWriter.finish();
            //pcmWriter.interrupt();
            try {
                pcmWriter.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long l = System.currentTimeMillis() - timer;
            int samp = pcmWriter.getSamples();

            System.out.println(Integer.toString(samp) + " samples in " +
                    Long.toString(l) + " seconds. " + Float.toString((float)samp / (float)l) + " samples per second");

            pcmWriter = null;

            pcmHandler.post(pcmCallback);

        }
    }
}
