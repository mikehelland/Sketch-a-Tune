package com.monadpad.sketchatune2;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

//public class MonadPad3DActiFrag extends Actifrag implements ColorChooserView.IColorChangeListener{
public class MonadPad3DActiFrag implements ColorChooserView.IColorChangeListener{
    private int dimensions = 6;
    Cube[][][] tesseract = new Cube[dimensions][dimensions][dimensions];

    View mView;
    MonadPad3DView mMonadView;

    private Dialog colorChooserDialog;
    private Activity mActivity;

    public MonadPad3DActiFrag(Activity activity, ViewGroup container, Bundle savedInstanceState) {

        //TODO
        //super(activity);

        mActivity = activity;

        mView = mActivity.getLayoutInflater().inflate(R.layout.threedee, container, false);

        mMonadView = (MonadPad3DView)mView.findViewById(R.id.tesseract3d);
        mMonadView.requestFocus();
        mMonadView.setFocusableInTouchMode(true);

        makeCubes();
        mMonadView.setup(tesseract, new CubeRenderer(tesseract));

        mView.findViewById(R.id.color).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                colorChooserDialog = new Dialog(mActivity);
                colorChooserDialog.setTitle("Pick a Color");
                colorChooserDialog.setContentView(R.layout.color_chooser);
                ((ColorChooserView)colorChooserDialog.findViewById(R.id.color_chooser))
                        .setColorChangeListener(MonadPad3DActiFrag.this);
                colorChooserDialog.show();
            }
        });
        mView.findViewById(R.id.btn_loop_mode).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final Dialog dl = new Dialog(mActivity);
                dl.setTitle(mActivity.getString(R.string.try_both_loop_modes));
                dl.setContentView(R.layout.loop_mode);
                dl.findViewById(R.id.chkLoopSeparate).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        mMonadView.loopMode = MonadPad3DView.MODE_LOOP_SEPARATE;
                        dl.dismiss();
                    }
                });
                dl.findViewById(R.id.chkLoopTogether).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        mMonadView.loopMode = MonadPad3DView.MODE_LOOP_TOGETHER;
                        dl.dismiss();
                    }
                });
                if (mMonadView.loopMode == MonadPad3DView.MODE_LOOP_TOGETHER)
                    ((RadioButton)dl.findViewById(R.id.chkLoopTogether)).setChecked(true);

                dl.show();
            }
        });

        ((CheckBox) mView.findViewById(R.id.check_loop)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    mMonadView.mode = MonadPad3DView.MODE_LOOP;
                else
                    mMonadView.mode = MonadPad3DView.MODE_PITCH;

            }
        });
        ((CheckBox)mView.findViewById(R.id.check_rotate)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    mMonadView.mode = MonadPad3DView.MODE_ROTATE;
                else {
                    //TODO
                    //if (((CheckBox)findViewById(R.id.check_loop)).isChecked())
                        mMonadView.mode = MonadPad3DView.MODE_LOOP;
                    //else
                        mMonadView.mode = MonadPad3DView.MODE_PITCH;
                }
            }
        });

        mView.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mMonadView.clearReset();
            }
        });
        mView.findViewById(R.id.undo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mMonadView.undo();
            }
        });

    }

    public void onResume() {
        mMonadView.onResume();

        Toast.makeText(mActivity, "1. Put your finger down", Toast.LENGTH_SHORT).show();
        Toast.makeText(mActivity, "2. Move it around", Toast.LENGTH_SHORT).show();
        Toast.makeText(mActivity, "3. Lift it up \n\n"+
                "See what happens!!", Toast.LENGTH_LONG).show();
    }

    public void onPause() {
        mMonadView.onPause();
        mMonadView.clear();
    }


    void makeCubes(){

        float offsetx = -1;
        float offsety = -1;
        float offsetz = -1;

        for (int ix = 0; ix < dimensions; ix++){
            for (int iy = 0; iy < dimensions; iy++){
                for (int iz = dimensions - 1; iz > -1 ; iz--){

                    Cube myC  = new Cube(offsetx, offsety, offsetz);
                    tesseract[ix][iy][iz] = myC;

                    offsetz += 0.38f;
                }
                offsetz = -1;
                offsety += 0.38f;
            }
            offsety = -1;
            offsetx += 0.38f;
        }
    }

    public void onColorChange(Paint p, int index) {
        mMonadView.setNextInstrument(index);
        colorChooserDialog.dismiss();
    }

    public View getView(){
        return mView;
    }
}
