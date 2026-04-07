package com.wyy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Role role;
    private String content;
    private List<ToolCall> toolCalls;
    private String name;
    private String toolCallId;
    private String base64Image;

    public Message(Role role) {
        this.role = role;
    }

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    // Static factory methods
    public static Message userMessage(String content) {
        return new Message(Role.USER, content);
    }

    public static Message userMessage(String content, String base64Image) {
        Message msg = new Message(Role.USER, content);
        msg.setBase64Image(base64Image);
        return msg;
    }

    public static Message systemMessage(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message assistantMessage(String content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message assistantMessage(String content, String base64Image) {
        Message msg = new Message(Role.ASSISTANT, content);
        msg.setBase64Image(base64Image);
        return msg;
    }

    public static Message toolMessage(String content, String name, String toolCallId) {
        return toolMessage(content, name, toolCallId, null);
    }

    public static Message toolMessage(String content, String name, String toolCallId, String base64Image) {
        Message msg = new Message(Role.TOOL, content);
        msg.setName(name);
        msg.setToolCallId(toolCallId);
        msg.setBase64Image(base64Image);
        return msg;
    }

    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("role", role.getValue());
        
        if (content != null) {
            dict.put("content", content);
        }
        if (toolCalls != null) {
            dict.put("tool_calls", toolCalls);
        }
        if (name != null) {
            dict.put("name", name);
        }
        if (toolCallId != null) {
            dict.put("tool_call_id", toolCallId);
        }
        if (base64Image != null) {
            dict.put("base64_image", base64Image);
        }
        
        return dict;
    }
}