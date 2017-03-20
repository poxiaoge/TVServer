package com.example.poxiaoge.tvserver.model;

import java.io.Serializable;

/**
 * Created by poxiaoge on 2016/12/19.
 */

public class DeviceItem implements Serializable{

    private String uuid;
    private String name;
    private String type;
    private String ip;
    private Boolean dlnaOk;
    private Boolean miracastOk;
    private Boolean rdpOk;
    private String deviceName;
    private String screen;


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDlnaOk(Boolean dlnaOk) {
        this.dlnaOk = dlnaOk;
    }

    public Boolean getDlnaOk() {
        return dlnaOk;
    }

    public void setMiracastOk(Boolean miracastOk) {
        this.miracastOk = miracastOk;
    }

    public Boolean getMiracastOk() {
        return miracastOk;
    }

    public void setRdpOk(Boolean rdpOk) {
        this.rdpOk = rdpOk;
    }

    public Boolean getRdpOk() {
        return rdpOk;
    }


    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    public String getDeviceName() {
        return deviceName;
    }


    public void setScreen(String screen) {
        this.screen = screen;
    }
    public String getScreen() {
        return screen;
    }
}
