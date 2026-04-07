package com.wyy;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作者：IT周瑜
 * 公众号：IT周瑜
 * 微信号：it_zhouyu
 */
@RestController
public class McpController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    @GetMapping("/mcp")
    public String mcp(String question) {
        return chatClient
                .prompt()
                .user(question)
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .call()
                .content();
    }

    @GetMapping("/mcpPrompt")
    public String mcpPrompt(String question) {
        McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
//        McpSchema.ListPromptsResult listPromptsResult = mcpSyncClient.listPrompts();
//        List<McpSchema.Prompt> prompts = listPromptsResult.prompts();
//        McpSchema.Prompt prompt = prompts.get(0);
//        return prompt.description();


        McpSchema.GetPromptRequest getPromptRequest = new McpSchema.GetPromptRequest("greeting", Map.of("name", "周瑜"), null);
        McpSchema.GetPromptResult prompt = mcpSyncClient.getPrompt(getPromptRequest);
        return ((McpSchema.TextContent)prompt.messages().get(0).content()).text();
    }

    @GetMapping("/mcpResource")
    public String mcpResource(String question) {
        McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
        McpSchema.ReadResourceRequest readResourceRequest = new McpSchema.ReadResourceRequest("config://username");
        McpSchema.ReadResourceResult readResourceResult = mcpSyncClient.readResource(readResourceRequest);
        McpSchema.ResourceContents resourceContents = readResourceResult.contents().get(0);
        return ((McpSchema.TextResourceContents)resourceContents).text();
    }
}
