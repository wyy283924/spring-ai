package com.wyy.ai_agent;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(ModelConfig.API_KEY)
                .baseUrl(ModelConfig.BASE_URL)
                .build();
        // 创建响应参数
        ChatCompletionCreateParams params = ChatCompletionCreateParams .builder()
                .addUserMessage("简单介绍一下自己")
                .model(ModelConfig.LLM_NAME)
                .build();

        // 发送请求并接收响应
        ChatCompletion chatCompletion = client.chat().completions().create(params);
        String json = chatCompletion.choices().get(0).message().content().get();
        System.out.println(json);
    }
}
