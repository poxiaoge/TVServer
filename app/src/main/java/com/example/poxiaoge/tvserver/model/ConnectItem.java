package com.example.poxiaoge.tvserver.model;

import java.util.List;
import java.util.Map;

/**
 * Created by poxiaoge on 2017/3/2.
 */

public class ConnectItem {
    private String source;
    private String type;
    private List<Map<String, String>> param;

    public void setParam(List<Map<String, String>> param) {
        this.param = param;
    }

    public List<Map<String, String>> getParam() {
        return param;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
