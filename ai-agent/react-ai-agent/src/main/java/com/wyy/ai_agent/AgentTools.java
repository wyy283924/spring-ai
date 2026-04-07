package com.wyy.ai_agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 作者：IT周瑜
 * 公众号：IT周瑜
 * 微信号：it_zhouyu
 */
public class AgentTools {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将指定内容写入本地文件。
     * @param jsonInput 一个包含 'file_path' 和 'content' 的 JSON 字符串。
     * @return 执行结果的描述字符串。
     */
    @Tool(description = "将指定内容写入本地文件。")
    public String writeFile(@ToolParam(description = "包含 'file_path' 和 'content' 的 JSON 字符串。") String jsonInput) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonInput);
            String filePath = rootNode.get("file_path").asText();
            String content = rootNode.get("content").asText();

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
//                return String.format("成功将内容写入文件 '%s'。", filePath);
                return String.format("写入成功");
            } catch (IOException e) {
                return String.format("写入文件 '%s' 时发生错误: %s", filePath, e.getMessage());
            }
        } catch (Exception e) {
            return String.format("解析 Action Input 或执行 writeFile 工具时出错: %s", e.getMessage());
        }
    }
}
