package com.example.demo.enums;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 返回时间 yyyyMMdd 格式的枚举类型
 */
public enum DateEnum {
        DATETIME_FILEPATH(new SimpleDateFormat("yyyyMMdd").format(new Date()));

    private String TIMENAME;

    public String getTimeName() {
        return TIMENAME;
    }

    public void setTimeName(String timeName) {
        this.TIMENAME = timeName;
    }

    private DateEnum(String a){
        this.TIMENAME = a;
    }
}
