package com.wyy;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class SpringAIApplication
{
    @Bean("bbb")
    public ChatMemory jdcChatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();
    }
    //保存聊天记录
    @Bean("aaa")
    public ChatMemory chatMemory(){
        InMemoryChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();
        return MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(memoryRepository)
                .build();
    }


    public static void main( String[] args )
    {
        SpringApplication.run(SpringAIApplication.class,args);
    }
}
