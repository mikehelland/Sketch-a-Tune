package com.monadpad.sketchatune2;

import android.graphics.Color;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {

    private int mColor = -1;
    float red, green, blue;

    float size = 0.25f;
    FloatBuffer bb;

    float[] vertex;

    int fires = 0;

    Cube(float xoffset, float yoffset, float zoffset){
        setColor(Color.RED);
        vertex = new float[] {

                //back
                xoffset, yoffset, zoffset,
                xoffset, yoffset+size, zoffset,
                xoffset + size, yoffset, zoffset,

                xoffset, yoffset + size, zoffset,
                xoffset + size, yoffset + size, zoffset,
                xoffset+size, yoffset, zoffset,
                //front

                xoffset, yoffset, zoffset +size,
                xoffset +size,  yoffset,  zoffset + size,
                xoffset,  yoffset + size,  zoffset + size,

                xoffset,  yoffset + size,  zoffset + size,
                xoffset + size,  yoffset,  zoffset + size,
                xoffset + size,  yoffset + size,  zoffset+size ,

                //left
                xoffset,  yoffset,  zoffset ,
                xoffset,  yoffset,  zoffset +size ,
                xoffset,  yoffset + size,  zoffset ,

                xoffset,  yoffset+size,  zoffset ,
                xoffset,  yoffset,  zoffset +size ,
                xoffset,  yoffset + size,  zoffset +size,

                //RIGHT
                xoffset+size,  yoffset,  zoffset+size ,
                xoffset+size,  yoffset,  zoffset ,
                xoffset+size,  yoffset+size,  zoffset ,

                xoffset +size,  yoffset,  zoffset +size,
                xoffset+size,  yoffset+size,  zoffset ,
                xoffset+size,  yoffset+size,  zoffset +size,


                //top
                xoffset,  yoffset+size,  zoffset ,
                xoffset,  yoffset+size,  zoffset+size ,
                xoffset+size,  yoffset+size,  zoffset ,

                xoffset +size,  yoffset+size,  zoffset ,
                xoffset,  yoffset+size,  zoffset +size,
                xoffset+size,  yoffset+size,  zoffset+size ,

                //bottom
                xoffset,  yoffset,  zoffset +size,
                xoffset,  yoffset,  zoffset ,
                xoffset+size,  yoffset,  zoffset ,

                xoffset,  yoffset,  zoffset +size,
                xoffset+size,  yoffset,  zoffset ,
                xoffset+size,  yoffset,  zoffset +size

        };
        bb = ByteBuffer.allocateDirect(vertex.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        bb.put(vertex);

        //FloatBuffer bb2 = ByteBuffer.allocateDirect(vertex2.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        //bb2.put(vertex2);
        //bb2.position(0);
    }

    void draw(GL10 gl){
        bb.position(0);

        if (fires > 0){
            gl.glColor4f(red, green, blue, 1.0f);
        }
        else {
            gl.glColor4f(0.0f, 0.0f, 0.0f, 0.2f);

        }
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bb);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertex.length / 3);


        //gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bb2);
        //gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertex2.length / 3);



    }


    public void fire(int color){
        setColor(color);
        fires = fires + 1;

    }

    public void off(){

        fires = Math.max(0, fires - 1);
    }

    void setColor(int color){
        if (mColor != color){
            mColor = color;
            red = Color.red(color) / 255f;
            blue = Color.blue(color) / 255f;
            green = Color.green(color) / 255f;
        }
    }

}

