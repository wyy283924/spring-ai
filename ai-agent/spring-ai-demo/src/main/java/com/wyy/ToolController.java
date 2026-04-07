package com.wyy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class ToolController {

    private final ChatClient chatClient;
    @Autowired
    private WYYTool wyyTool;

    public ToolController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Bean
    ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(true);
    }
    /**
     * 执行工具
     */
    @GetMapping("/tool")
    public String tool(String question){
        return chatClient.prompt()
                .user(question)
                .tools(wyyTool)
                .call()
                .content();
    }
    @GetMapping("/toolCallback")
    public String toolChat(String question) {

        ToolCallback[] wyyTools = ToolCallbacks.from(new WYYTool());

        return chatClient
                .prompt()
                .user(question)
                .toolCallbacks(wyyTools)
                .call()
                .content();
    }
    //toolcall->执行工具->工具结果->发送给大模型
    /**
     * 用户控制工具执行
     */
    @GetMapping("/userControlledTool")
    public String userControlledTool(String question) {
        ToolCallback[] wyyTools = ToolCallbacks.from(new WYYTool());

        ToolCallingChatOptions toolCallingChatOptions = ToolCallingChatOptions.builder().toolCallbacks(wyyTools).internalToolExecutionEnabled(false).build();
        Prompt prompt = Prompt.builder().chatOptions(toolCallingChatOptions).content(question).build();
        ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();

        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            chatResponse = chatClient.prompt(new Prompt(toolExecutionResult.conversationHistory(), toolCallingChatOptions)).call().chatResponse();
        }
        return chatResponse.getResult().getOutput().getText();
    }

}
