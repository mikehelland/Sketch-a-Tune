package com.monadpad.sketchatune2;

/**
 * User: m
 * Date: 11/12/13
 * Time: 10:15 PM
 */
public class MonadSettings {

    String scale = "0,2,4,5,7,9,11";
    int octaves = 4;
    int base  = 24;
    boolean autoTime = true;
    int autoRemove = 4;
    int testMode = 0;

    LibenizMode libenizMode = LibenizMode.MELODIFY;

    enum LibenizMode {
        OFF,
        JUST_QUANTIZE,
        MELODIFY
    }
}
