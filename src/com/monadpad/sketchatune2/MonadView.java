package com.monadpad.sketchatune2;



import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonadView extends View {

    private MonadJam mJam;
    private MonadJam mFadeJam;

    private boolean fresh = true;

    Paint pathPaint = new Paint();
    final Paint fingerPaint = new Paint();
    final Paint startDrawingPaint = new Paint();
    final Paint abcRectPaint = new Paint();
    final Paint fadePaint = new Paint();
    final Paint instrumentDescPaint = new Paint();

    final Paint fPaint;
    final Paint fretPaint;
    final Paint fP2;

    float x, y;
    boolean isTouching = false;
    boolean areTwoTouching = false;
    //private long touchCounter = 0l;
    float lastX;
    float lastY;
    float lastx;
    int instrument = 0;

    private int tutorial = 0;
    private int tutorial2Ups = 0 ;
    private boolean wasTutorial = false;
    private float tutorialWidth;

    private int nGrooveletSize = 0;
    private int nInstrumentWidth = 0;
    private ArrayList<String> grooveLets = new ArrayList<String>();
    boolean isTouchingGroovelet = false;
    private int lastGroovelet = -1;

    private boolean bending = false;
    private int bendPointer;
    private float bendStart;
    private float bendCount;

    boolean lAbc = false;
    private boolean lChangeInstrument = false;

    public final Paint[] colors;
    private final String[] colorDesc;
    private boolean showColorDesc = true;

    private int nShowingColumns = 0;
    private int nRowsPerColumn = 0;

    private boolean draggingUndo = false;

    //private boolean lockScreen;
    private boolean showBeats;
    private boolean showTime;
    private boolean showFrets;

    private Path path;

    private Thread mRefreshThread;

    private long fadeOutStarted = 0;
    private int fadeOutLength = 0;

    public final static int STATE_DRAW = 0;
    public final static int STATE_SELECT_FOR_LEVELS = 1;
    public final static int STATE_CHANGE_LEVELS = 2;
    private int state = STATE_DRAW;
    public final static int STATE_ANIMATE_CHANGE_LEVELS = 3;
    private float percentThruChange = 0.0f;
    public final static int STATE_MID_DRAW = 4;


    private MonadChannel channelForLevels;
    private float panLevel = 0.5f;
    private float volumeLevel = 0.75f;
    private boolean movingPan = false;
    private boolean movingVolume = false;

    private int faderRadius = 30;
    private float volumeWidth;

    private boolean isSuperMonad = false;

    private boolean hasTakenTutorial = false;

    private boolean autoPan = false;

    private boolean shownAutoRemoveTip = false;

    private int originalPointerId = -1;
    private int restingPointerId = -1;

    private boolean wasResting = false;

    private long lastDown = 0l;

    private int fretCount = 0;
    private int scaleLength = 0;


    public final static int TOOL_FINGER = 0;
    public final static int TOOL_PEN = 1;

    private boolean waitingForHoverExit = false;

    private int lastUsed = 0;

    private boolean autoTime = true;

    private int tutorialMargin = 12;

    public MonadView(Context context, AttributeSet attrs) {
        super(context, attrs);

        isSuperMonad = true; //context.getString(R.string.app_name).toUpperCase().contains("SUPER");

        colorDesc = getResources().getStringArray(R.array.instruments);
        colors = InstrumentFactory.getColors();

        setBackgroundColor(0xFF000000);

        pathPaint.setARGB(255, 255, 255, 255);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(4);
        pathPaint.setShadowLayer(6, 0, 0, 0xFFFFFFFF);

        fingerPaint.setARGB(128, 255, 255, 255);
        fingerPaint.setStyle(Paint.Style.FILL);
        fingerPaint.setShadowLayer(10, 0, 0, 0xFFFFFFFF);
        fingerPaint.setTextSize(20);

        fretPaint = new Paint();
        fretPaint.setARGB(100, 80, 80, 80);
        fretPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        instrumentDescPaint.setARGB(255, 0, 0, 0);
        instrumentDescPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        instrumentDescPaint.setTextSize(20);

        abcRectPaint.setARGB(196, 128, 128, 128);
        abcRectPaint.setStyle(Paint.Style.FILL);

        startDrawingPaint.setARGB(255, 255, 255, 255);
        startDrawingPaint.setStyle(Paint.Style.FILL);
        startDrawingPaint.setTextSize(40);
        startDrawingPaint.setStrokeWidth(5);

        volumeWidth = startDrawingPaint.measureText("volume");
        tutorialWidth = startDrawingPaint.measureText("Tutorial");

        fPaint = new Paint();
        fP2 = new Paint();
        fPaint.setARGB(128, 255, 255, 255);
        fPaint.setStyle(Paint.Style.FILL);
        fPaint.setShadowLayer(10, 0, 0, 0xFFFFFFFF);
        fPaint.setTextSize(20);

        fP2.setARGB(64, 255, 255, 255);
        fP2.setStyle(Paint.Style.FILL);

        setKeepScreenOn(true);
        setFocusable(true);

        fadePaint.setARGB(196, 0, 0, 0);
        fadePaint.setStyle(Paint.Style.FILL);

        makeRefreshThread();

        new MonadSPen(this);
    }

    boolean setJam(MonadJam jam){
        if (tutorial > 0  && tutorial != 3){
            tutorial = 0;
        }
        if (mFadeJam == null){
            fresh = true;
        }
        mJam = jam;
        mJam.changeNextInstrument(instrument);
        setPreferences();

        if (mJam.getChannels().size() > 0){
            getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {

                try {
                    // if its got channels, we may need to make paths for them
                    for (MonadChannel c : mJam.getChannels()){
                        makePaths(c);
                    }
                } catch (Exception e){
                    Toast.makeText(getContext(), "Something went wrong making paths" +
                            "\n\nYou may need to update or upgrade", Toast.LENGTH_LONG).show();
                }
                getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
                }
            });
        }

        mJam.setAutoRemoveCallback(new Runnable() {
            public void run() {
                grooveLets.add(mJam.getGrooveInfo());

                if (!shownAutoRemoveTip) {
                    shownAutoRemoveTip = true;
                    Toast.makeText(getContext(), "Auto remove setting is on." +
                            "\n\nPrevious state has been stored in the Parts menu.", Toast.LENGTH_LONG).show();
                }
            }
        });


        if (mJam.isSetup()) {
            fresh = false;
        }


        return true;
    }
    public void resetClear(){
        tutorial2Ups = 0;

        if (tutorial == 2){
            tutorial++;
        }
        else if (tutorial > 0) {
            instrumentSettings(0);
            tutorial = 0;
        }

        draggingUndo = false;
        state = STATE_DRAW;
        fadeOutLength = 0;
        fadeOutStarted = 0;
        fresh = true;
        lastGroovelet = -1;

        lAbc = false;

        collapseInstruments();

/*        if (mRefreshThread != null){
            mRefreshThread.interrupt();
            mRefreshThread = null;
        }
        invalidate();
        */

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (state == STATE_CHANGE_LEVELS || state == STATE_ANIMATE_CHANGE_LEVELS){
            changeLevels(event);
            return true;
        }


        float ex = event.getX();
        float ey = event.getY();

        x = ex / getWidth();
        y = 1 - ey / getHeight();

        // 2+ fingers down
        if (isBending(event))
            return true;

        float divide = mJam.isSetup() ? mJam.getLineLength() : 1.0f;
        float pan = autoPan ? Math.min(1.0f, x / divide * 2.0f - 1.0f) : -2.0f;

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (isTouching && restingPointerId == -1){

                if (autoTime){
                    mJam.addXYTP(x, y, -3, pan);
                }
                else {
                    mJam.addXY(x, y);
                }
                if (!mJam.isCurrentChannelAnimating()) {
                    if (!wasResting) {
                        path.lineTo(ex, ey);
                    }
                    path.moveTo(ex, ey);
                }
                wasResting = false;
            }
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
            final int index = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

            if (isTouching && event.getPointerCount() == 2) {

                if (lastDown > 0l && System.currentTimeMillis() - lastDown < 250) {
                    mJam.cancelCurrentChannel();
                    isTouching = false;

                    bendCount = 0;
                    bending = true;
                    bendPointer = event.getPointerId(0);
                    bendStart = event.getY(0);
                    if (tutorial == 6) {
                        tutorial = 7;
                        grooveLets.clear();
                    }
                    return true;
                }


                if (restingPointerId == -1) {
                    restingPointerId = event.getPointerId(index);

                    if (autoTime) {
                        // -3 means calculate time in mJam..addXYTP, which also
                        // resets the variables above so they're stored first
                        // -2 means pause but don't stop
                        long newTime = -3;
                        mJam.addXYTP(x, -2, newTime, pan);
                    }
                    else {
                        mJam.addXY(x, -2);
                    }
                }
            }
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
            final int index = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

            if (restingPointerId > -1) {
                int id = event.getPointerId(index);
                if (id == restingPointerId) {

                    wasResting = true;
                    restingPointerId = -1;
                    mJam.unmuteCurrentChannel();
                }
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            lastDown = System.currentTimeMillis();
            originalPointerId = event.getPointerId(0);
            //check to see if this is a groovelet
            if (!closeSettings() && state == STATE_DRAW &&
                    !draggingUndo && !touchingGroovelet(event) && !touchingInstrument(event)
                    ){

                if (fresh && tutorialBoundary(ex, ey)){
                    startTutorial();
                }
                else{
                    changeState(STATE_MID_DRAW);
                    onModify();
                    isTouching = true;
                    if (autoTime){
                        mJam.addXYTP(x, y, -2, pan);
                    }
                    else {
                        mJam.addXY(x, y);
                    }

                    if (!mJam.continuedChannel()){
                        path = new Path();
                        mJam.addPath(path, ex, ey);
                    }
                    if (!mJam.isCurrentChannelAnimating()) {
                        path.moveTo(ex, ey);
                    }

                }
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            touchEventUp(event, -1);
        }
        lastx = x;
        lastX = ex;
        lastY = ey;

//        invalidate();

/*        if (lockScreen){
            if (mainView.getHeight() > mainView.getWidth())
                mainView.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else
                mainView.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        */


        return true;
    }

    boolean touchEventUp(MotionEvent event, int yParam) {
        float ex = event.getX();
        float ey = event.getY();

        float divide = mJam.isSetup() ? mJam.getLineLength() : 1.0f;
        float pan = autoPan ? Math.min(1.0f, x / divide * 2.0f - 1.0f) : -2.0f;

        if (bending){
            bending = false;
            mJam.bend((-1 * bendCount) , (-1 * bendCount) / getHeight());
            return false;
        }

        if (draggingUndo){
            undoDragFinal(ex, ey);
            draggingUndo = false;
            return false;
        }

        if (state == STATE_SELECT_FOR_LEVELS){
            selectForLevels(ex, ey);
            return false;
        }

        if (isTouchingGroovelet){
            dropGroovelet(event);
            isTouchingGroovelet = false;
            return false;
        }

        if (isTouching || waitingForHoverExit)
        {
            changeState(STATE_DRAW);
            boolean onsetup = !mJam.isSetup();

            isTouching = false;
            if (autoTime) {
                boolean continued = mJam.continuedChannel();
                boolean live = mJam.getLiveChannel() != null;
                boolean animating = mJam.isCurrentChannelAnimating();
                // -3 means calculate time in mJam..addXYTP, which also
                // resets the variables above so they're stored first
                long newTime = -3;
                mJam.addXYTP(x, y, newTime, pan);
                mJam.addXYTP(x, yParam, newTime, pan);
                if (!live) {
                    if (!continued || !animating) {
                        if (mJam.getMoveLinesInAutoTime()) {
                            mJam.animateAutoTimePath(getWidth(), getHeight());
                        }
                    }
                }
            }
            else {
                mJam.addXY(x, y);
                mJam.addXY(x, yParam);
            }
            if (!mJam.isCurrentChannelAnimating()) {
                if (restingPointerId > -1 || waitingForHoverExit) {
                    restingPointerId = -1;
                }
                else {
                    path.lineTo(ex, ey);
                }
                path.moveTo(ex, ey);
            }

            if (onsetup) {
                ((MainActivity)getContext()).onSetupLoop();
            }


            if (tutorial == 1) tutorial = 2;
            else if (tutorial == 3) {
                tutorial = 4;
                instrumentSettings(3);
            }
            else if (tutorial == 4){
                tutorial2Ups++;
                if (tutorial2Ups > 1){
                    tutorial = 5;
                    instrumentSettings(7);
                }
            }
            else if (tutorial == 5) tutorial = 6;
            else if (tutorial == 8) tutorial = 9;

            return true;
        }

        restingPointerId = -1;

        return false;
    }

    private boolean closeSettings() {
        if (((MainActivity)getContext()).closeMenus()){
            return true;
        }
        return false;
    }

    private boolean tutorialBoundary(float ex, float ey) {
        return ex > getWidth() - tutorialWidth * 1.5f - tutorialMargin
                && ey > getHeight() - startDrawingPaint.getTextSize() * 1.8f - tutorialMargin;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mJam == null) {
            return;
        }

        if (nGrooveletSize == 0){
            nGrooveletSize = 90;
            //nGrooveletSize = Math.round((float)mActivity.findViewById(R.id.abc).getWidth() * 1.5f);
        }

        if (showFrets) {
            drawFrets(canvas);
        }

        if (tutorial > 0){
            DrawTutorial.drawTutorial(this, tutorial, canvas);
        }

        if (fadeOutLength > 0){

            float fadePerct = (float)(System.currentTimeMillis() - fadeOutStarted) / fadeOutLength;
            if (fadePerct > 1.0f){
                fadeOutStarted = 0;
                fadeOutLength = 0;
                mFadeJam = null;

                if (wasTutorial){
                    fresh = true;
                    wasTutorial = false;
                }
            }
            else {
                drawChannels(canvas, mFadeJam);

                fadePaint.setAlpha((int)(fadePerct * 255f));
                canvas.drawRect(0, 0, getWidth(), getHeight(), fadePaint);
            }
        }

        Context ctx = getContext();
        if (fresh && tutorial == 0) {
            float topOffset = getHeight() / 2 - startDrawingPaint.getTextSize();

            String startDrawing = getContext().getString(R.string.start_drawing);
            float startDrawingLength = startDrawingPaint.measureText(startDrawing);
            canvas.drawText(startDrawing,
                    getWidth() / 2 - startDrawingLength / 2 , topOffset, startDrawingPaint);
            String firstDraw;
            if (autoTime) {
                //firstDraw = ctx.getString(R.string.pref_autotime) + " " +
                //        ctx.getString(R.string.is) + " " + ctx.getString(R.string.on);
                //float firstDrawLength = fingerPaint.measureText(firstDraw);
                //canvas.drawText(firstDraw,
                //        getWidth() / 2 - firstDrawLength / 2, topOffset + fingerPaint.getTextSize() * 2, fingerPaint);
            }
            else {
                firstDraw = "Tip: The first draw sets the tempo";
                canvas.drawText(firstDraw,
                        getWidth() / 2 - startDrawingLength / 2, topOffset + fingerPaint.getTextSize() * 2, fingerPaint);
                firstDraw = "Tip: Draw from left to right";
                canvas.drawText(firstDraw,
                        getWidth() / 2 - startDrawingLength / 2, topOffset + fingerPaint.getTextSize() * 3
                        , fingerPaint);
            }


            firstDraw = getContext().getString(R.string.tutorial_menu);
            canvas.drawRoundRect(new RectF(getWidth() - tutorialWidth * 1.5f - tutorialMargin,
                    getHeight()- startDrawingPaint.getTextSize() * 1.8f - tutorialMargin,
                    getWidth() - tutorialMargin, getHeight() - tutorialMargin), 10, 10, abcRectPaint);
            canvas.drawText(firstDraw,
                    getWidth() - tutorialWidth * 1.25f - tutorialMargin,
                    getHeight()- startDrawingPaint.getTextSize() / 3 - tutorialMargin, startDrawingPaint);
            firstDraw = getContext().getString(R.string.press_for);
            canvas.drawText(firstDraw,
                    getWidth() - tutorialWidth - tutorialMargin,
                    getHeight()- startDrawingPaint.getTextSize()  - fingerPaint.getTextSize() / 2 - tutorialMargin,
                    fingerPaint);

        }

        if (state == STATE_ANIMATE_CHANGE_LEVELS){
            startDrawingPaint.setAlpha((int)(255.0f * percentThruChange));
            drawChangeLevels(canvas);
        }
        else if (state == STATE_CHANGE_LEVELS){
            drawChangeLevels(canvas);
        }
        else {
            drawChannels(canvas, mJam);
        }

        if (mJam.showTip > 0) {
            if (System.currentTimeMillis() - mJam.showTip > 5000) {
                mJam.showTip = 0;
            }
            else {
                canvas.drawText("The loop is very fast",
                        tutorialMargin,
                        getHeight()- tutorialMargin * 2 - fingerPaint.getTextSize(),
                        fingerPaint);
                canvas.drawText("Press clear and try drawing slower",
                        tutorialMargin,
                        getHeight()- tutorialMargin,
                        fingerPaint);
            }

        }

        nShowingColumns = 0;
        if (changeInstrumentAnimation > -1){
            drawInstruments(canvas);
        }
        else if (lAbc ){
            drawABC(canvas);
        }

        if (isTouching) {
            canvas.drawCircle(lastX, lastY, 15, fingerPaint);
        }
    }

    void drawFrets(Canvas canvas) {

        float y;
        int frets = fretCount;

        if (scaleLength > 0) {

            for (int ifret = 0; ifret < frets; ifret++) {
                y = (float)ifret / (float)frets * getHeight();
                canvas.drawLine(0, y, getWidth(), y, fretPaint);

                if (ifret % scaleLength == 0) {
                    canvas.drawRect(0f, y, (float)getWidth(),
                            y + (1f / (float)frets) * (float)getHeight(),
                            fretPaint);
                }
            }
        }
    }

    public void instrumentSettings(int pos){
        instrument = pos;
        int a = colors[pos].getAlpha();
        colors[pos].setAlpha(255);
        ((MainActivity)getContext()).newColor(colors[pos].getColor(), pos);

        colors[pos].setAlpha(a);
        mJam.changeNextInstrument(instrument);
    }

    private boolean touchingGroovelet(MotionEvent e){

        if (!lAbc)
            return false;

        float y = e.getY();
        float x = e.getX();
        int ig = getGrooveletSquare(x, y);

        if (ig == -1){
            return false;
        }
        else if (ig == 0){
            grooveLets.add(mJam.getGrooveInfo());
            lastGroovelet = grooveLets.size() - 1;

            if (tutorial == 7) {
                tutorial = 8;
                instrumentSettings(2);
            }
            else if (tutorial == 9) tutorial = 10;

            return true;
        }
        else if (ig == 1){
            grooveLets.clear();
            return true;
        }
        else {
            ig = ig - 2;
            if (lastGroovelet == ig){
                mJam.setLoopBack();
            } else {
                if (tutorial == 10) tutorial = 11;

                mJam.finishAllChannels();

                int firstInst = instrument;

                JSONObject jsO = null;
                try {
                    jsO = new JSONObject(grooveLets.get(ig));
                    JSONArray jsChans = jsO.getJSONArray("channels");
                    for (int ils=0; ils<jsChans.length(); ils++ ){
                        MonadChannel mpc = mJam.loadChannel(jsChans.getJSONObject(ils));
                        makePaths(mpc);

                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                //TODO adjust the bounds of the loops if necessary
                /*if (line1.length > 1){
                    float lineStart = Float.valueOf(line1[1]) ;
                    float lineLength = Float.valueOf(line1[2]) ;
                    mJam.adjustBounds(lineStart, lineLength);
                }
                float gain = Float.parseFloat(line1[16 + (ils - 1) * 2]);
                float pan = Float.parseFloat(line1[16 + 1 + (ils - 1) * 2]);
                mpc.setGain(gain);
                mpc.pan(pan);
                */

                instrumentSettings(firstInst);
                onModify();
                lastGroovelet = ig;
            }

            isTouchingGroovelet = true;
            return true;
        }
    }

    private boolean touchingInstrument(MotionEvent e){

        if (!lChangeInstrument)
            return false;

        collapseInstruments();
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

                        instrumentSettings(ig + nStart);

                        return true;
                    }
                }
                break;
            }
        }

        return true;
    }


    private boolean dropGroovelet(MotionEvent e){
        int ig = getGrooveletSquare(e.getX(),  e.getY());

        if (ig < 2){
            return false;
        }
        else {
            ig = ig - 2;

            if (!(ig == lastGroovelet)){
                int firstInst = instrument;

                JSONObject jsO = null;
                try {
                    jsO = new JSONObject(grooveLets.get(ig));
                    JSONArray jsChans = jsO.getJSONArray("channels");
                    for (int ils=0; ils<jsChans.length(); ils++ ){
                        MonadChannel mpc = mJam.loadChannel(jsChans.getJSONObject(ils));
                        makePaths(mpc);
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }

                instrumentSettings(firstInst);
                lastGroovelet = -1;
            }
            return true;
        }
    }

    public boolean undo(){
        collapseInstruments();
        onModify();
        if (draggingUndo){
            draggingUndo = false;
//            mThread.undoDragCancel();
//            invalidate();
        }
        else if (mJam.isSetup()){
            mJam.undo();
//            invalidate();
        }
        else return false;
        return true;
    }
    public void undoDrag(){
        if (mJam.isSetup()){
            if (draggingUndo){
                draggingUndo = false;
//                invalidate();
            }
            else{
                collapseInstruments();
                lAbc = false;
                draggingUndo = true;
                Toast.makeText(getContext(), "Select a channel to UNDO", Toast.LENGTH_LONG).show();
                if (mJam.isSetup()){
//                    invalidate();
                }
            }
        }
    }

    public void fadeOut(){
        lAbc = false;
        collapseInstruments();
        mFadeJam = mJam;
        fadeOutStarted = System.currentTimeMillis();
        fadeOutLength = MonadJam.FADE_OUT_LENGTH;

        if (tutorial == 12){
            tutorial = 0;
            wasTutorial = true;
            instrumentSettings(0);
        }

        if (mRefreshThread == null)
            makeRefreshThread();
    }

    public boolean abc(){
        draggingUndo = false;
        state = STATE_DRAW;
        collapseInstruments();
        lAbc = !lAbc;
        return lAbc;
    }

    public void changeInstrument(){

        nInstrumentWidth = nGrooveletSize * (showColorDesc ? 2 : 1);
        state = STATE_DRAW;
        draggingUndo = false;
        lAbc = false;

        if (!lChangeInstrument) {
            lChangeInstrument = true;
            if (Build.VERSION.SDK_INT < 11) {
                setChangeInstrumentAnimation(0);
            }
            else {
                ObjectAnimator anim = ObjectAnimator.ofFloat(this, "changeInstrumentAnimation", -1f, 0f);
                anim.setDuration(400);
                anim.start();
            }
        }
        else {
            collapseInstruments();
        }
    }

    private void collapseInstruments() {
        if (changeInstrumentAnimation != -1f){
            if (Build.VERSION.SDK_INT < 11) {
                setChangeInstrumentAnimation(-1);
            }
            else {
                ObjectAnimator anim = ObjectAnimator.ofFloat(this, "changeInstrumentAnimation", 0f, -1f);
                anim.setDuration(400);
                anim.start();
            }
            mJam.resetLastUp();
        }
        lChangeInstrument = false;
    }

    private float changeInstrumentAnimation = -1f;
    public void setChangeInstrumentAnimation(float f){
        changeInstrumentAnimation = f;
    }

    public String[] getGroovelets(){
        String[] ret = new String[grooveLets.size()];
        for (int ig = 0; ig< ret.length;ig++){
            ret[ig] = grooveLets.get(ig);
        }
        return ret;
    }

    public void setGroovelets(String[] gs){
        grooveLets.clear();
        grooveLets.addAll(Arrays.asList(gs));
    }

    private int getGrooveletSquare(float x, float y){
        int ret = -1;
        for (int ic = 0; ic <nShowingColumns + 1; ic++){
            if (x < nGrooveletSize * (ic + 1)){
                int nStop = nRowsPerColumn == 0 ? grooveLets.size() + 2 :
                        Math.min(nRowsPerColumn, grooveLets.size() + 2 - ic * nRowsPerColumn);
                for (int ir = 0; ir < nStop; ir++){
                    if (y < nGrooveletSize * (ir + 1)){

                        ret = ir + nRowsPerColumn * ic;
                        return  ret;
                    }
                }
            }
        }
        return ret;
    }

    public void onModify(){
        if (fresh){
            fresh = false;
            /*if (showTime){
                if (mRefreshThread == null)
                    makeRefreshThread();
            }
            else{
                invalidate();
            }*/
        }
        lastGroovelet = -1;
        if (mJam.getSourceInfo().source == MonadJamSourceInfo.GALLERY_SOURCE_GALLERY){
            mJam.getSourceInfo().source = MonadJamSourceInfo.GALLERY_SOURCE_MODIFIED_GALLERY;
            mJam.getSourceInfo().id = -1;
        }
        else if (mJam.getSourceInfo().source == MonadJamSourceInfo.GALLERY_SOURCE_SD){
            mJam.getSourceInfo().source = MonadJamSourceInfo.GALLERY_SOURCE_USER;
        }

        mJam.getSourceInfo().id = -1;
    }

    private void makeRefreshThread() {
        mRefreshThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()){
                    postInvalidate();
                    try{
                        Thread.sleep(1000/60);
                    } catch (InterruptedException e) {break;}
                }
            }
        });
        mRefreshThread.start();
    }

    private boolean isBending(MotionEvent event){

        if (!bending)
            return false;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            bending = false;
            return true;
        }

        if (event.getPointerCount() > 1){
            if (event.getAction() == MotionEvent.ACTION_MOVE) {

                try {
                    float bendY = event.getY(event.findPointerIndex(bendPointer));
                    float bendDiff = bendStart - bendY;

                    if (scaleLength > 0) {
                        float fretSize = 1.0f / (float)fretCount * getHeight();
                        int frets = (int)(bendDiff / fretSize);
                        bendDiff = frets * fretSize;
                    }

                    if (bendDiff != 0) {
                        mJam.bend(bendDiff , bendDiff / getHeight());
                        bendCount = bendCount + bendDiff;
                        bendStart = bendY;
                    }
                }
                catch (Exception e){
                    Log.d("MGH", "exception in isBending");
//                    bending = false;
                }
            }

//            invalidate();
            return true;
        }

        //return false;
        return true;

    }

    public void drawChannels(Canvas canvas, MonadJam jam) {
        if (jam == null)
            return;
        Paint p;

        float nowPercent = jam.getNowInLoop();

        int circleRadius = draggingUndo || state == STATE_SELECT_FOR_LEVELS ? 20 : 8;

        List<MonadChannel> chans = jam.getChannels();
        MonadChannelViewInfo info;
        for (MonadChannel channel : chans){
            info = channel.getViewInfo();
            p = colors[info.instrument];
            if (channel.overlaps > 0 && !channel.animatingAutoTime
                    && jam.getMoveLinesInAutoTime()) {
                if (info.lastDrawnOverlap != channel.overlapped) {
                    info.path.offset(-1 * channel.overlapped * mJam.getLineLength() * getWidth(),
                            0, info.movedPath);
                    info.lastDrawnOverlap = channel.overlapped;
                }
                // crash reports
                // TODO find out why this would be missing
                if (info.movedPath!= null)
                    canvas.drawPath(info.movedPath, p);
            }
            else {
                // or maybe its this one
                // TODO
                if (info.path != null)
                    canvas.drawPath(info.path, p);
            }

            canvas.drawCircle(Math.max(info.originX, circleRadius), info.originY, circleRadius, p);

            if (autoTime && !jam.moveLinesInAutoTime) {
                float[] xy = channel.getCurrentXY();
                if (xy[0] > -1.0f) {
                    canvas.drawCircle(xy[0] * getWidth(), (1 - xy[1]) * getHeight(), circleRadius /2, p);
                }
            }

        }
        MonadChannel channel = jam.getLiveChannel();
        //todo or maybe this one
        if (channel != null && channel.getViewInfo() != null &&
                channel.getViewInfo().path != null) {
            p = colors[channel.getViewInfo().instrument];
            canvas.drawPath(channel.getViewInfo().path, p);
        }

        if (jam.isSetup()){

            float x, lineLength, lineStart;
            lineStart = jam.getLineStart() * getWidth();
            lineLength = jam.getLineLength() * getWidth();
            if (showBeats){
                x= lineStart;
                canvas.drawLine(x, 0, x, getHeight(), fPaint);
                x = lineStart + lineLength * 0.25f;
                canvas.drawLine(x, 0, x, getHeight(), fP2);
                x = lineStart + lineLength * 0.5f;
                canvas.drawLine(x, 0, x, getHeight(), fP2);
                x = lineStart + lineLength * 0.75f;
                canvas.drawLine(x, 0, x, getHeight(), fP2);
                x = lineLength + lineStart;
                canvas.drawLine(x, 0, x, getHeight(), fPaint);
            }
            if (showTime){
                x = lineStart + lineLength * nowPercent;
                canvas.drawLine(x, 0, x, canvas.getHeight(), fPaint);
            }
        }
    }

    public void setPreferences(){
        final SharedPreferences synthPrefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());

//        lockScreen = synthPrefs.getBoolean("lockscreen", false);

        autoTime = mJam.getAutoTime();

        autoPan = autoTime && synthPrefs.getBoolean("autopan", false);
        mJam.moveLinesInAutoTime = synthPrefs.getBoolean("movelines", false);
        showBeats = synthPrefs.getBoolean("showbeats", true);
        showTime = synthPrefs.getBoolean("showtime", true);
        showFrets = synthPrefs.getBoolean("showfrets", true);
        showColorDesc = synthPrefs.getBoolean("showColorDesc", true);

        scaleLength = mJam.getScaleLength();
        fretCount = scaleLength * mJam.getOctaves() + 1;


/*        if (showTime && mJam.isSetup() && mRefreshThread == null){
            makeRefreshThread();
        }
*/
    }

    public void makePaths(MonadChannel c){
        float x, y;
        float[] xys;
        float lastY;

        // if its got channels, we may need to make paths for them
        path = new Path();
        lastY = -1;

        xys = c.getXYs();

        for (int ic = 0; ic < xys.length; ic++){

            x = xys[ic];
            y = xys[ic + 1];

            if (ic == 0){
                c.getViewInfo().originX =  x * getWidth();
                c.getViewInfo().originY = (1 - y) * getHeight();
            }

            if (y > -1 ){
                if (lastY > -1){
                    path.lineTo(x * getWidth(), (1 - y) * getHeight());
                }
                path.moveTo(x * getWidth(), (1 - y) * getHeight());
            }

            //go up by 2
            ic++;
            lastY = y;
        }
        c.getViewInfo().path = path;

    }

    private void drawInstruments(Canvas canvas){
        Rect r = new Rect() ;
        String desc = "";
        int iRow = 0;
        int instrumentCount = colors.length;
        //if (!isSuperMonad) instrumentCount = instrumentCount - 2;

        int offset = (int)(changeInstrumentAnimation * nInstrumentWidth * 2);
        for (int ig = 0; ig< instrumentCount; ig++) {
            colors[ig].setAlpha(255);
            if (iRow * nGrooveletSize + nGrooveletSize > getHeight()) {
                nShowingColumns++;
                nRowsPerColumn = iRow;
                iRow = 0;
            }
            r.set(offset + nShowingColumns * nInstrumentWidth, nGrooveletSize * iRow,
                    offset + nShowingColumns * nInstrumentWidth + nInstrumentWidth, nGrooveletSize + nGrooveletSize * iRow);

            canvas.drawRect(r, colors[ig]);
            if (showColorDesc && changeInstrumentAnimation == 0f) {
                if (ig < colorDesc.length)
                    desc = colorDesc[ig];
                canvas.drawText(desc,
                        nShowingColumns * nInstrumentWidth + nGrooveletSize / 5,
                        nGrooveletSize * iRow + nGrooveletSize / 2, instrumentDescPaint);
            }
            iRow++;
        }

    }

    private void drawABC(Canvas canvas){
        // draw the groovelets
        Rect r = new Rect(0, 0, nGrooveletSize, nGrooveletSize);
        canvas.drawRect(r, abcRectPaint);
        String firstBox = "Add +";
        canvas.drawText(firstBox,
                nGrooveletSize/8, nGrooveletSize/2, fingerPaint);

        if (grooveLets.size() > 0){

            r = new Rect(0, nGrooveletSize, nGrooveletSize,
                    nGrooveletSize * 2) ;

            canvas.drawRect(r, abcRectPaint);
            canvas.drawText("Clear",
                    nGrooveletSize/8,
                    nGrooveletSize + nGrooveletSize/2,
                    fingerPaint);

            int iRow = 2;

            for (int ig = 0; ig < grooveLets.size(); ig++){
                if (iRow * nGrooveletSize + nGrooveletSize > getHeight()){
                    nShowingColumns++;
                    nRowsPerColumn = iRow;
                    iRow = 0;
                }

                r.set(nGrooveletSize * nShowingColumns, nGrooveletSize * iRow ,
                        nGrooveletSize * (nShowingColumns + 1), nGrooveletSize * (iRow + 1));
                if (lastGroovelet == ig){
                    canvas.drawRect(r, fingerPaint);
                    canvas.drawText(Character.toString((char)(ig + 65)),
                            nGrooveletSize * nShowingColumns  + nGrooveletSize/3,
                            nGrooveletSize * iRow  + nGrooveletSize/2, instrumentDescPaint);

                } else {
                    canvas.drawRect(r, abcRectPaint);
                    canvas.drawText(Character.toString((char)(ig + 65)),
                            nGrooveletSize *nShowingColumns + nGrooveletSize/3,
                            nGrooveletSize * iRow + nGrooveletSize/2, fingerPaint);
                }
                iRow++;
            }
        }

        if (isTouchingGroovelet) {
            r.set((int)lastX - nGrooveletSize, (int)lastY - nGrooveletSize/2,
                    (int)lastX + nGrooveletSize, (int)lastY + nGrooveletSize/2);
            canvas.drawRect(r, fingerPaint);
        }
    }

    public void startTutorial() {
        setBackgroundDrawable(null);
        setBackgroundColor(0xFF000000);
        mJam.setSettingsToDefault();
        instrumentSettings(1);

        resetClear();
        tutorial = 1;
//        invalidate();
        hasTakenTutorial = true;
    }

    public void maybeUpdateTutorial() {
        if (tutorial == 6 ){
            tutorial = 7;
        }
    }

    public void levels() {
        if (mJam.isSetup()){
            draggingUndo = false;
            lAbc = false;
            collapseInstruments();
            if (state != STATE_DRAW)
                state = STATE_DRAW;
            else{
                state = STATE_SELECT_FOR_LEVELS;
                Toast.makeText(getContext(), "Select a channel to MIX", Toast.LENGTH_LONG).show();

            }
        }
    }

    private void undoDragFinal(float x, float y){

        for (MonadChannel mpc : mJam.getChannels()){
            if (mpc.hitTest(x, y)){
                if (tutorial == 11 ) tutorial++;
                mJam.undoChannel(mpc);
                onModify();
                break;
            }
        }
    }
    private void selectForLevels(float x, float y){


        boolean gotOne = false;

        for (MonadChannel mpc2 : mJam.getChannels()){
            if (mpc2.hitTest(x, y)){
                gotOne = true;
                channelForLevels = mpc2;
                break;
            }
        }
        if (gotOne){
            // let's animate the appearance of the levels
            new AnimateLevels().execute();
            panLevel = 0.5f + channelForLevels.getPan() / (2f / 0.66f);
            volumeLevel = 0.25f + ((1 - channelForLevels.getGain()) / 2);

            changeState(STATE_CHANGE_LEVELS);

        }
        else {
            state = STATE_DRAW;
        }
    }

    private void drawChangeLevels(Canvas canvas){

        List<MonadChannel> chans = mJam.getChannels();
        MonadChannelViewInfo info;
        Paint p;
        int alpha = 0;
        for (MonadChannel channel : chans){
            info = channel.getViewInfo();
            p = colors[info.instrument];
            if (channel != channelForLevels){
                alpha = p.getAlpha();
                p.setAlpha(128 - startDrawingPaint.getAlpha() / 2);
            }
            canvas.drawPath(info.path, p);
            if (channel != channelForLevels){
                p.setAlpha(alpha);
            }

        }

        canvas.drawText(getContext().getString(R.string.volume),
                0, getHeight() * 0.25f - startDrawingPaint.getTextSize(), startDrawingPaint);
        canvas.drawLine(volumeWidth / 2, getHeight() * 0.25f, volumeWidth / 2, getHeight() * 0.75f,
                startDrawingPaint);

        String panString = isSuperMonad ? getContext().getString(R.string.pan) : "get Super Draw Music to PAN";
        Paint panPaint = isSuperMonad ? startDrawingPaint : fingerPaint;
        canvas.drawText(panString,
                getWidth() * 0.5f - panPaint.measureText(panString) / 2,
                getHeight() * 0.80f + (float)faderRadius + startDrawingPaint.getTextSize(), panPaint);

        canvas.drawLine(getWidth() * 0.166f, getHeight() * 0.80f, getWidth() * 0.833f, getHeight() * 0.80f,
                startDrawingPaint);

        canvas.drawCircle(volumeWidth/2, volumeLevel * getHeight(), faderRadius, colors[1]);
        float pan = 0.5f + channelForLevels.getPan() / (2f / 0.66f);
        canvas.drawCircle(getWidth() * pan, getHeight() * 0.80f, faderRadius, colors[1]);

    }

    private void changeLevels(MotionEvent event){
        float ex = event.getX();
        float ey = event.getY();

        x = ex / getWidth();
        y = 1 - ey / getHeight();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isSuperMonad && Math.abs(ey - getHeight() * 0.8) < faderRadius){
                movingPan = true;
                panLevel = Math.max(0.166f, Math.min(ex / getWidth(), 0.833f));
                float panLevel2 = (panLevel - 0.5f) * (2f / 0.66f);
                channelForLevels.cancelRecordedPans();
                channelForLevels.pan( panLevel2 );

            }
            else if (ey < getHeight() * 0.8 && Math.abs(ex - volumeWidth / 2) < faderRadius){
                movingVolume = true;
                volumeLevel = Math.max(0.25f, Math.min(ey / getHeight(), 0.75f));
                float volumeLevel2 = 1 - ((volumeLevel - 0.25f) * 2f);
                channelForLevels.setGain( volumeLevel2 );
            }

        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (movingPan){
                panLevel = Math.max(0.166f, Math.min(ex / getWidth(), 0.833f));
                float panLevel2 = (panLevel - 0.5f) * (2f / 0.66f);
                channelForLevels.pan( panLevel2 );
//                invalidate();
            }
            if (movingVolume){
                volumeLevel = Math.max(0.25f, Math.min(ey / getHeight(), 0.75f));
                float volumeLevel2 = 1 - ((volumeLevel - 0.25f) * 2f);
                channelForLevels.setGain( volumeLevel2 );
//                invalidate();
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP){
            movingPan = false;
            movingVolume = false;
        }
    }

    public int getState() {
        return state;
    }

    class AnimateLevels extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... objects) {
            percentThruChange = 0.0f;
            state = STATE_ANIMATE_CHANGE_LEVELS;
            long now = System.currentTimeMillis();
            long startTime = now;
            float animationLength = 3000f;
            while (now < startTime + 1000){

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                percentThruChange = (float)(now - startTime) / 1000.0f;
                //postInvalidate();
                now = System.currentTimeMillis();

            }
            if (state == STATE_ANIMATE_CHANGE_LEVELS){
                state = STATE_CHANGE_LEVELS;
            }
            return null;
        }
    }

    void closeMenus() {
        lAbc = false;

        collapseInstruments();
    }

    boolean inTutorial() {
        return tutorial > 0;
    }

    private void changeState(int newState) {
        ((MainActivity)getContext()).changingState(newState);
    }

    void setLastUsed(int method) {
        lastUsed = method;
    }

    void onHoverExit(MotionEvent event) {
        if (waitingForHoverExit) {
            touchEventUp(event, -1);

            //dont set this until after touchEventUp()
            waitingForHoverExit = false;
        }
    }

    void waitForHoverExit() {
        waitingForHoverExit = true;
    }


}
