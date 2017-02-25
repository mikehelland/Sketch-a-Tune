package com.monadpad.sketchatune2.dsp;

/**
 * User: m
 * Date: 7/5/13
 * Time: 9:37 AM
 */
public class SampleRate {
    private static int sample_rate = 44100;

    static int get() {
        return sample_rate;
    }

    public static void setLow() {
        sample_rate = 22050;
    }
}
