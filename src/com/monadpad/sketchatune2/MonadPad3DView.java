package com.monadpad.sketchatune2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MonadPad3DView extends GLSurfaceView {

    MonadJam mJam = new MonadJam(getContext());

    private TesseractThread mJamThread = null;

    private TouchList touches;
    private boolean isTouching = false;
    private long counter = 0;

    int dimensions = 6;
    Cube[][][] tesseract;

    int mode = 0;
    int loopMode = MODE_LOOP_SEPARATE;
    final static int MODE_PITCH = 0;
    final static int MODE_LOOP = 1;
    final static int MODE_ROTATE = 2;

    final static int MODE_LOOP_TOGETHER = 3;
    final static int MODE_LOOP_SEPARATE = 4;

    final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private CubeRenderer mRenderer;
    private float mPreviousX;
    private float mPreviousY;
    Paint paintPath;
    Path path;

    ArrayList<Thread> mThreads = new ArrayList<Thread>();

    private int nextInstrument = 0;

    public MonadPad3DView(Context context, AttributeSet aa) {
        super(context, aa);
        paintPath = new Paint();
        paintPath.setARGB(255, 0, 255, 0);
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setStrokeWidth(5f);

        setKeepScreenOn(true);
        setFocusable(true);

    }

    void setup(Cube[][][] t, CubeRenderer renderer){
        mRenderer = renderer;
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        tesseract = t;
    }

    void refresh(){
        requestRender();
    }


    class Touch{
        float  x, y;
        long when;

        Touch(float x, float y, long when){
            this.x = x;
            this.y = y;
            this.when = when;
        }
    }

    public void onDraw(Canvas canvas){
        if (isTouching){
            canvas.drawPath(path, paintPath);
        }
    }

    public boolean onTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();

        if (mode == MODE_ROTATE){
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dx = x - mPreviousX;
                    float dy = y - mPreviousY;
                    mRenderer.mAngleX += dx * TOUCH_SCALE_FACTOR;
                    mRenderer.mAngleY += dy * TOUCH_SCALE_FACTOR;
                    requestRender();
                    break;
            }

            mPreviousX = x;
            mPreviousY = y;
            return true;
        }

        float pan = 2 * (0.5f - x / getWidth());
        long timeX;
        if (mode == MODE_PITCH)
            timeX = -1;
        else
            timeX = 0;

        if (event.getAction() == MotionEvent.ACTION_DOWN){
            mJam.changeNextInstrument(nextInstrument);

            counter = System.currentTimeMillis();
            path = new Path();
            touches = new TouchList();
            touches.color = InstrumentFactory.getInstrumentColor(nextInstrument);
            touches.add(new Touch(x, y, 0));
            path.moveTo(x, y);
            isTouching = true;

            mJam.addXYTP(timeX, 1 - (y / getHeight()), timeX, pan);

        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE){
/*            for (int h = 0 ; h < event.getHistorySize() ; h++){
                prevEventTime = prevEventTime + event.getHistoricalEventTime(h);
                //touches.add(new Touch(event.getHistoricalX(h), event.getHistoricalY(h),
                //        prevEventTime - counter));
                path.lineTo(x, y);
                path.moveTo(x, y);
            }*/

            touches.add(new Touch(x, y, System.currentTimeMillis() - counter));
            path.lineTo(x, y);
            path.moveTo(x, y);

            mJam.addXYTP(timeX, 1 - (y / getHeight()), System.currentTimeMillis() - counter, pan);


        }
        else if (event.getAction() == MotionEvent.ACTION_UP){
            Thread thread = null;

            if (mode == MODE_PITCH)
                thread = new TesseractThread(touches, System.currentTimeMillis() - counter, false);
            else if (loopMode == MODE_LOOP_TOGETHER) {
                // this needs to be done before mJam.addXY(x -1)
                mJam.addTouchList(touches);
                if (mJamThread == null){
                    mJamThread = new TesseractThread(touches, System.currentTimeMillis() - counter, true);
                    thread = mJamThread;
                }
            }
            else if (loopMode == MODE_LOOP_SEPARATE) {
                // this needs to be done before mJam.addXY(x -1)
                mJam.addTouchList(touches);
                thread = new TesseractThread(touches, System.currentTimeMillis() - counter, true);
            }


            if (thread != null){
                mThreads.add(thread);
                thread.start();
            }

            mJam.addXYTP(timeX, 1 - (y / getHeight()), System.currentTimeMillis() - counter, pan);
            mJam.addXYTP(timeX, -1f, System.currentTimeMillis() - counter, pan);

            if (loopMode == MODE_LOOP_SEPARATE){
                mJam = new MonadJam(getContext());
            }

            isTouching = false;

        }
        invalidate();

        return true;
    }


    class TesseractThread extends Thread{

        TouchList touchList;
        long duration;
        long startTime;
        long chunkLength;
        boolean loop = false;
        MonadJam thisJam;

        TesseractThread(TouchList touchList, Long duration, boolean loop){

            this.duration = duration;
            chunkLength = duration / dimensions;
            this.loop = loop;

            if (loop){
                thisJam = mJam;
            }
            else {
                this.touchList = touchList;
            }
        }

        @Override
        public void run(){

//            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);

            startTime = System.currentTimeMillis();
            long now = 0;
            long nowInChunk = 0;
            long startChunk = startTime;
            int chunk = 0;
            Touch nextTouch;
            boolean resetI = false;
            TouchList tempTouch;

            List<TouchList> touchLists = new ArrayList<TouchList>();
            List<TouchList> newTouchLists;

            if (!loop)
                touchLists.add(touchList);

            while (now < duration && !isInterrupted()){
                if (nowInChunk > chunkLength){
                    chunk++;
                    startChunk = startChunk + chunkLength;
                }

                if (loop){
                    newTouchLists = new ArrayList<TouchList>();
                    for (MonadChannel chan : thisJam.getChannels()){

                        tempTouch = chan.getViewInfo().getTouchList();
                        if (tempTouch != null){
                            newTouchLists.add(tempTouch);
                        }
                    }
                    for (TouchList tl : touchLists){
                        if (!newTouchLists.contains(tl)){
                            tl.node.off();
                        }
                    }
                    touchLists = newTouchLists;
                }
                for (TouchList touchList : touchLists){

                    if (resetI)
                        touchList.iTouch = -1;

                    nextTouch = null;

                    if (touchList.size() > touchList.iTouch + 1)
                        nextTouch = touchList.get(touchList.iTouch + 1);

                    if (nextTouch != null && nextTouch.when <= now){
                        if (touchList.node != null)
                            touchList.node.off();

                        touchList.node = getNearestNode(nextTouch.x, nextTouch.y, chunk);
                        touchList.node.fire(touchList.color);

                        touchList.iTouch++;

                    }
                    refresh();
                }
                resetI = false;

                now = System.currentTimeMillis();
                nowInChunk = now - startChunk;
                now -= startTime;

                if (now >= duration){
                    if (loop){
                        startTime = startTime + duration;
                        startChunk = startTime;
                        now = 0;
                        nowInChunk = 0;
                        resetI = true;
                        chunk = 0;
                    }
                }

            }
            for (TouchList touchList : touchLists){
                if (touchList.node != null)
                    touchList.node.off();
            }
            if (thisJam != null)
                thisJam.niceStop();
            refresh();
        }
    }

    class TouchList extends CopyOnWriteArrayList<Touch> {
        int iTouch = -1;
        int color;
        Cube node;
    }

    void clearReset(){
        clear();
        mJam = new MonadJam(getContext());
    }

    void clear(){
        for (Thread t : mThreads){
            if (t != null && !t.isInterrupted())
                t.interrupt();
        }
        mThreads.clear();
        mJamThread = null;
        mJam.niceStop();
    }

    void undo(){
        if (loopMode == MODE_LOOP_TOGETHER)
            mJam.undo();
        else {
            if (mThreads.size() > 0){
                mThreads.get(mThreads.size() - 1).interrupt();
                mThreads.remove(mThreads.size() - 1);
            }
        }
    }

    Cube getNearestNode(float x, float y, int z){
        x = (x / getWidth()) * dimensions;
        y = (1 - (y / getHeight())) * dimensions;
        int ix, iy;
        ix = Math.max(0, Math.min((int)Math.floor(x), dimensions - 1));
        iy = Math.max(0, Math.min((int)Math.floor(y), dimensions - 1));
        z = Math.min(z, dimensions - 1);
        return tesseract[ix][iy][z];
    }

    void setNextInstrument(int nextInstrument){
        this.nextInstrument = nextInstrument;
    }

}



/*
class Ball3DThread extends Thread {
    /*            if (mode == MODE_BALLS){
                    for (Thread t : mThreads)
                        if (t instanceof Ball3DThread){
                            ((Ball3DThread)t).addMoreBalls();
                            thread = t;
                            break;
                        }
                    if (thread != null)
                        thread = null;
                    else
                        thread = new Ball3DThread(this);
                }
    final CopyOnWriteArrayList<Ball> balls = new CopyOnWriteArrayList<Ball>();

    int newBalls = 4;

    Random mRandom = new Random(System.currentTimeMillis());
    MonadPad3DView mView;

    Ball3DThread(MonadPad3DView v){
        mView = v;
    }

    void addMoreBalls(){
        for (int b = 0 ; b < newBalls; b++){
            balls.add(new Ball());
        }

    }

    @Override
    public void run(){
        Log.d("MGH", "starting");

        addMoreBalls();

        mView.refresh();

        while (!isInterrupted()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            // move the ball around
            for (Ball b: balls){
//                    if (b.parent == null)
                b.move();
            }


            for (Ball b: balls){
                b.makeLinks();
            }


            for (Ball b: balls){
                b.lightUp();
            }

            mView.refresh();

        }
        for (Ball b: balls){
            b.off();
        }
        mView.refresh();

        Log.d("MGH", "stopping");
    }

    class Ball {
        int x;
        int y;
        int z;
        int dx = 1;
        int dy = 1;
        int dz = 1;

        int axis = 0;

        Ball parent = null;
        ArrayList<Ball> kids = new ArrayList<Ball>();

        boolean linked = false;
        Ball link;

        Ball(){

            x = mRandom.nextInt(5);
            dx = mRandom.nextBoolean() ? 1 : -1;
            y = mRandom.nextInt(5);
            dy = mRandom.nextBoolean() ? 1 : -1;
            z = mRandom.nextInt(5);
            dz = mRandom.nextBoolean() ? 1 : -1;
            tesseract[x][y][z].fire();
        }

        void off(){
            tesseract[x][y][z].off();
        }
        boolean move(){

            off();

            if (parent != null)
                return true;

            if (x >= dimensions || y >= dimensions || z >= dimensions)
                return false;

            if (axis == 0){
                x += dx;
                if (x >= dimensions || x < 0){
                    dx = dx *-1;
                    x += dx;
                }
            }
            else if (axis == 1){
                y += dy;
                if (y >= dimensions || y < 0){
                    dy = dy *-1;
                    y += dy;
                }
            }
            else if (axis == 2){
                z += dz;
                if (z >= dimensions || z < 0){
                    dz = dz *-1;
                    z += dz;
                }
            }
            axis++;
            if (axis == 3) axis = 0;

            return true;

        }

        void lightUp(){

            if (x < dimensions && y < dimensions && z < dimensions)
                if (kids.size() > 0)
                    tesseract[x][y][z].fire2();
                else
                    tesseract[x][y][z].fire();

        }

        void makeLinks(){
//                if (parent == null){
            if (!linked){
                // see if we're ontop any other balls
                for (Ball b : balls){
                    if (b != this && b.link != this){ //b.parent == null){
                        //            if (Math.abs(b.x - x) <= 1 && Math.abs(b.y - y) <= 1 && Math.abs(b.z - z) <= 1){
                        if (b.x == x && b.y == y && b.z == z){
                            b.parent = this;
//                                dx = b.dx;
//                                dy = b.dy;
//                                dz = b.dz;
                            kids.add(b);
                            b.off();
                            linked = true;
                            link = b;
                            Log.d("MGH", "linked");
                        }
                    }
                }
            }
        }
    }
}
*/
