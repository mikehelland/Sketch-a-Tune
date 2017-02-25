package com.monadpad.sketchatune2;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 6/19/12
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class CubeRenderer implements GLSurfaceView.Renderer {
    public float mAngleX;
    public float mAngleY;

    private Cube[][][] tesseract;

    public CubeRenderer(Cube[][][] t){
        tesseract = t;
    }



    public void onDrawFrame(GL10 gl) {

        /*
        * Usually, the first thing one might want to do is to clear
        * the screen. The most efficient way of doing this is to use
        * glClear(). However we must make sure to set the scissor
        * correctly first. The scissor is always specified in window
        * coordinates:
        */

        gl.glClearColor(0.5f,0.5f,0.5f,1);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        /*
        * Now we're ready to draw some 3D object
        */

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -2.71f);
        gl.glScalef(0.5f, 0.5f, 0.5f);
//        gl.glRotatef(mAngle,        0, 1, 0);
//        gl.glRotatef(mAngle*0.25f,  1, 0, 0);
        gl.glRotatef(mAngleX, 0, 1, 0);
        gl.glRotatef(mAngleY, 1, 0, 0);
//        gl.glRotatef(5, 0, 0, 1);


        gl.glColor4f(0.7f, 0.7f, 0.7f, 1.0f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        //    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc (GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

//        mWorld.draw(gl);

        for (int ix = 0; ix < 6; ix++){
            for (int iy = 0; iy < 6; iy++){
                for (int iz = 6 - 1; iz > -1 ; iz--){

                    tesseract[ix][iy][iz].draw(gl);



                }
            }
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        /*
        * Set our projection matrix. This doesn't have to be done
        * each time we draw, but usually a new projection needs to be set
        * when the viewport is resized.
        */

        float ratio = (float)width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 2, 12);

        /*
        * By default, OpenGL enables features that improve quality
        * but reduce performance. One might want to tweak that
        * especially on software renderer.
        */
        gl.glDisable(GL10.GL_DITHER);
        gl.glActiveTexture(GL10.GL_TEXTURE0);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Nothing special, don't have any textures we need to recreate.
    }

}


