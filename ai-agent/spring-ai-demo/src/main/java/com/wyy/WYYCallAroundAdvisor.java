package com.wyy;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

public class WYYCallAroundAdvisor implements CallAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        System.out.println("before...");

        // 增强提示词
        Prompt prompt = chatClientRequest.prompt().augmentSystemMessage("我是周瑜");
        ChatClientRequest chatClientRequestCopy = chatClientRequest.mutate().prompt(prompt).build();

        ChatClientResponse advisedResponse = callAdvisorChain.nextCall(chatClientRequestCopy);

        System.out.println("after...");

        return advisedResponse;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // 数字越小，越先执行，升序排序
        return 0;
    }
}