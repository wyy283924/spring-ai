package com.wyy;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WYYTool {
    @Tool(description = "获取当前时间",returnDirect = true)
    String getCurrentDateTime(){
        System.out.println("获取当前时间");
        return LocalDateTime.now().toString();
    }
    @Tool(description = "用指定时间设置闹钟")
    void setAlarm(@ToolParam(description = "用中文中的年月日的格式，比如2025年3月31日") String time){
        System.out.println("闹钟时间为"+time);
    }
}
