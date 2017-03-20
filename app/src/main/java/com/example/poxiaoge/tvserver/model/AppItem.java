package com.example.poxiaoge.tvserver.model;

/**
 * Created by poxiaoge on 2016/12/21.
 */

public class AppItem {
    //TODO：增加app的图标支持
    private String appName;
    private String packageName;
    private String location;
//    private String openMethod;

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }


    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

//    public void setOpenMethod(String openMethod) {
//        this.openMethod = openMethod;
//    }
//
//    public String getOpenMethod() {
//        return openMethod;
//    }
}
