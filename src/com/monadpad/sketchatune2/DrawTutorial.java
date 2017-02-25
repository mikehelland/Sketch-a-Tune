package com.monadpad.sketchatune2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class DrawTutorial {

    public static void drawTutorial(MonadView view, int tutorial, Canvas canvas) {

        boolean lAbc = view.lAbc;
        float topMargin = 5f;
        Paint startDrawingPaint = view.startDrawingPaint;
        float height = view.getHeight();
        float width = view.getWidth();

        String tutorialCaption = view.getContext().getString(R.string.tutorial);
        canvas.drawText(tutorialCaption, 0, height, view.startDrawingPaint);
        tutorialCaption = "STEP " + Integer.toString(tutorial);
        canvas.drawText(tutorialCaption, view.getWidth() - view.startDrawingPaint.measureText(tutorialCaption),
                height, view.startDrawingPaint);

        float textSize = view.startDrawingPaint.getTextSize();

        if (!lAbc){
            canvas.drawRect(0, 0, view.getWidth(), textSize * 2 + topMargin * 2, view.abcRectPaint);
            canvas.drawText("A tune has a ....", view.getWidth() * 0.1f,
                    topMargin + textSize, view.startDrawingPaint);

        }
        canvas.drawRect(0, view.getHeight() - textSize * 4, view.getWidth(), view.getHeight(),view.abcRectPaint );


        if (tutorial == 1){

            String nextS = "... MELODY";
            canvas.drawText(nextS, view.getWidth() / 2 - view.startDrawingPaint.measureText(nextS) / 2,
                    topMargin + textSize * 2, view.startDrawingPaint);

            String trace = "Trace the red line above";
            canvas.drawText(trace, view.getWidth() / 2 - view.startDrawingPaint.measureText(trace) / 2,
                    view.getHeight() - textSize * 3, view.startDrawingPaint);
            trace = "Draw from left to right";
            canvas.drawText(trace, view.getWidth() / 2 - view.startDrawingPaint.measureText(trace) / 2,
                    view.getHeight() - textSize * 2, view.startDrawingPaint);

            canvas.drawPath(getTutorialRed(view), view.colors[1]);
        }
        else if (tutorial == 2){

            String nextS = "... TEMPO";
            canvas.drawText(nextS, view.getWidth() / 2 - startDrawingPaint.measureText(nextS) / 2,
                    topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);

            String trace = "Hit the CLEAR button";
            canvas.drawText(trace, view.getWidth() / 2 - startDrawingPaint.measureText(trace) / 2,
                    view.getHeight() - textSize * 3, startDrawingPaint);


        }
        else if (tutorial == 3){

            String nextS = "... TEMPO";
            canvas.drawText(nextS, view.getWidth() / 2 - startDrawingPaint.measureText(nextS) / 2,
                    topMargin + textSize * 2, startDrawingPaint);

            String trace = "Now SLOWLY trace";
            canvas.drawText(trace, view.getWidth() / 2 - startDrawingPaint.measureText(trace) / 2,
                    view.getHeight() - textSize * 3, startDrawingPaint);

            String info = "for a slower loop";
            canvas.drawText(info, view.getWidth() / 2 - startDrawingPaint.measureText(info) / 2,
                    view.getHeight() - textSize * 2, startDrawingPaint);

            canvas.drawPath(getTutorialRed(view), view.colors[1]);

        }
        else if (tutorial == 4){
            String nextS = "... BASS LINE";
            canvas.drawText(nextS, view.getWidth() / 2 - startDrawingPaint.measureText(nextS) / 2,
                    topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);

            String trace = "Trace the green lines";
            canvas.drawText(trace, view.getWidth() / 2 - startDrawingPaint.measureText(trace) / 2,
                    view.getHeight() - textSize * 3, startDrawingPaint);

            Path melody = new Path();
            melody.moveTo(0.25f * view.getWidth(), 0.75f * view.getHeight());
            melody.lineTo(0.4f * view.getWidth(), 0.75f * view.getHeight());
            canvas.drawPath(melody, view.colors[3]);

            melody = new Path();
            melody.moveTo(0.6f * width, 0.55f * height);
            melody.lineTo(0.75f * width, 0.55f * height);
            canvas.drawPath(melody, view.colors[3]);
        }
        else if (tutorial == 5){

            String nextS = "... SOLO";
            canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                    topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);

            String trace = "Draw in the space";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);
            trace = "right of the loop";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, startDrawingPaint);

        }
        else if (tutorial == 6){

            String nextS = "... CHORD PROGRESSION";
            canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                    topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);

            String trace = "Use two fingers";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);

            trace = "to move the chord";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, startDrawingPaint);
            trace = "up and down";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize , startDrawingPaint);
        }
        else if (tutorial == 7){

            if (!lAbc){
                String nextS = "... VERSE, CHORUS, ect";
                canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                        topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);
            }

            String trace = "Hit the Parts menu ";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);

            trace = "and touch ADD + ";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, startDrawingPaint);
            trace = "to store this as \"A\"";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize , startDrawingPaint);
        }
        else if (tutorial == 8){

            if (!lAbc){
                String nextS = "... VERSE, CHORUS, ect";
                canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                        topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);
            }

            String trace = "Now trace the yellow line";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);


            Path melody = new Path();
            melody.moveTo(0.15f * width, 0.48f * height);
            melody.lineTo(0.75f * width, 0.48f * height);

            canvas.drawPath(melody, view.colors[2]);

        }
        else if (tutorial == 9){

            if (!lAbc){
                String nextS = "... VERSE, CHORUS, ect";
                canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                        topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);
            }

            String trace = "Hit ADD + again";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);
            trace = "to store this as \"B\"";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, startDrawingPaint);

        }
        else if (tutorial == 10){
            if (!lAbc){
                String nextS = "... VERSE, CHORUS, ect";
                canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                        topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);
            }
            String trace = "Hit A and B to switch";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);
            trace = "Drag & Drop to combine";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, startDrawingPaint);

        }

        else if (tutorial == 11){

            String nextS = "... BREAK DOWN";
            canvas.drawText(nextS, width / 2 - startDrawingPaint.measureText(nextS) / 2,
                    topMargin + startDrawingPaint.getTextSize() * 2, startDrawingPaint);

            String trace = "Touch and hold UNDO";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, startDrawingPaint);
            trace = "Select a line to remove";
            canvas.drawText(trace, width / 2 - startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, startDrawingPaint);

        }

        else if (tutorial == 12){

            String nextS = "... ENDING";
            canvas.drawText(nextS, view.getWidth() / 2 - view.startDrawingPaint.measureText(nextS) / 2,
                    topMargin + view.startDrawingPaint.getTextSize() * 2, view.startDrawingPaint);

            String trace = "Touch and hold";
            canvas.drawText(trace, view.getWidth() / 2 - view.startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 3, view.startDrawingPaint);
            trace = "CLEAR to fade out";
            canvas.drawText(trace, view.getWidth() / 2 - view.startDrawingPaint.measureText(trace) / 2,
                    height - textSize * 2, view.startDrawingPaint);
        }

    }

    private static Path getTutorialRed(MonadView view){
        Path melody = new Path();
        melody.moveTo(0.05f * view.getWidth(), 0.15f * view.getHeight());
        melody.lineTo(0.25f * view.getWidth(), 0.33f * view.getHeight());
        melody.moveTo(0.25f * view.getWidth(), 0.33f * view.getHeight());
        melody.lineTo(0.4f * view.getWidth(), 0.33f * view.getHeight());
        melody.moveTo(0.4f * view.getWidth(), 0.33f * view.getHeight());
        melody.lineTo(0.6f * view.getWidth(), 0.20f * view.getHeight());
        melody.moveTo(0.6f * view.getWidth(), 0.2f * view.getHeight());
        melody.lineTo(0.75f * view.getWidth(), 0.20f * view.getHeight());
        return melody;
    }
}
