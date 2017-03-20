package com.example.poxiaoge.tvserver.model;

import android.net.Uri;

/**
 * Created by poxiaoge on 2016/12/24.
 */

public class MediaItem{
    private String name;
    private String pathName;
    private String uri;
    private String location;
    private String thumbnailurl;
    private String type;
    private Boolean isFolder;


    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public String getPathName() {
        return pathName;
    }



    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setThumbnailurl(String thumbnailurl) {
        this.thumbnailurl = thumbnailurl;
    }

    public String getThumbnailurl() {
        return thumbnailurl;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


    public void setIsFolder(Boolean isFolder) {
        this.isFolder = isFolder;
    }

    public Boolean getIsFolder() {
        return this.isFolder;
    }





}
