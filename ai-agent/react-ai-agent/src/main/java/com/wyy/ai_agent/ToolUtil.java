package com.wyy.ai_agent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：IT周瑜
 * 公众号：IT周瑜
 * 微信号：it_zhouyu
 */
public class ToolUtil {

    public static String getToolDescription(Class<?> clazz) {

        List<String> toolNameList = new ArrayList<>();
        List<String> formattedToolList = new ArrayList<>();
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            if (declaredMethod.isAnnotationPresent(Tool.class)) {
                Tool toolAnnotation = declaredMethod.getAnnotation(Tool.class);
                String toolName = declaredMethod.getName();
                String toolDescription = toolAnnotation.description();
                String paramDescription = declaredMethod.getParameters()[0].getAnnotation(ToolParam.class).description();
                String formattedTool = String.format("- toolName=%s, toolDescription=%s, paramDescription=%s", toolName, toolDescription, paramDescription);
                formattedToolList.add(formattedTool);
                toolNameList.add(toolName);
            }
        }

        String formattedTools = String.join("/n/n", formattedToolList);
        String toolNames = String.join(",", toolNameList);

//        System.out.println(formattedTools);
//        System.out.println(toolNames);

        return formattedTools;
    }

    public static void main(String[] args) {
        getToolDescription(AgentTools.class);
    }
}
