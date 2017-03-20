package com.example.poxiaoge.tvserver.utils;

import android.media.Image;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by poxiaoge on 2016/12/24.
 */

public class MediaUtil {

    public static String[] VideoSuffix = {".mp4",".mkv",".flv",".avi",".rmvb",".rm",".divx",".m4v",".mpg",".mts",
            ".wmv",".m3u8",".m3u",".mov",".3gp",".pls"};
    public static String[] AudioSuffix = {".mp3",".wma",".mid",".ape","flac"};
    public static String[] ImageSuffix = {".jpg",".jpeg",".png",".bmp","gif",".tiff"};


    public static String verifyMediaTypes(String suffix){
        String suf = suffix.toLowerCase();
        for (String s : VideoSuffix) {
            if (s.equals(suf)) {
                return "video";
            }
        }
        for (String s : AudioSuffix) {
            if (s.equals(suf)) {
                return "audio";
            }
        }
        for (String s : ImageSuffix) {
            if (s.equals(suf)) {
                return "image";
            }
        }
        return "*";
    }
}
