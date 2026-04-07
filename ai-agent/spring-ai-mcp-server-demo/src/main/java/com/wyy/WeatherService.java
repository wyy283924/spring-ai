package com.wyy;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeatherService {
    //进程到进程的通信
    @Autowired
    private Environment environment;

    @McpTool(description = "获取指定城市的天气")
    public String getWeather(String cityName) {
//        log.info("正在获取天气信息...");
        if (cityName.equals("上海")) {
            return "天晴";
        } else if (cityName.equals("北京")) {
            return "下雨";
        }
        return "不知道";
    }
    //智能周报 Spring AI
    //周报的内容
    //发送邮件（人工确认）
    @McpPrompt(name = "greeting")
    public McpSchema.GetPromptResult greeting(@McpArg(name = "name") String name) {
        String message = "你好, " + name + "! 有什么可以帮您?";
        return new McpSchema.GetPromptResult("Greeting", List.of(new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(message))));
    }

    @McpResource(uri = "config://{key}", name = "configuration")
    public String getConfig(String key) {
        return environment.getProperty(key, "123");
    }
}
