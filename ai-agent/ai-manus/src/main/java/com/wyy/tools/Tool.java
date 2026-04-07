package com.wyy.tools;

import com.wyy.model.ToolDefinition;

import java.util.Map;

public interface Tool {
    String getName();
    String getDescription();
    Map<String, Object> getParametersSchema();
    ToolResult execute(Map<String, Object> parameters);
    default ToolDefinition toDefinition() {
        return new ToolDefinition(getName(), getDescription(), getParametersSchema());
    }
}