package com.wyy;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * Hello world!
 *
 */
public class ManusApplication
{
    public static void main( String[] args )
        {
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey(ModelConfig.API_KEY)
                    .baseUrl(ModelConfig.BASE_URL)
                    .build();
            //manus
            ManusAgent manusAgent = new ManusAgent(client);

            //上下文工具（提示词工程）
            String prompt = """
                请帮我使用HTML，CSS，JS创建一个简单的贪吃蛇游戏，分成三个文件，分别是snake.html,snake.css,snake.js
            """;

        }
}
