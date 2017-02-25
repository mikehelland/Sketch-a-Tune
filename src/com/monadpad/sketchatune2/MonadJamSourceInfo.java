package com.monadpad.sketchatune2;


public class MonadJamSourceInfo {

    public long id = -1L;
    public String artist = "";
    public String title = "";
    public int source = 0;
    public String authCode = "";

    public final static int GALLERY_SOURCE_USER = 0;
    public final static int GALLERY_SOURCE_GALLERY = 1;
    public final static int GALLERY_SOURCE_SD = 2;
    public final static int GALLERY_SOURCE_MODIFIED_GALLERY = 3;

}
