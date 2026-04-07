package com.wyy.ai_agent;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1. 创建实例
 * 由于 record 自动生成了全参构造方法，直接使用 new 创建对象：
 *
 * ParsedOutput output = new ParsedOutput(
 *     "action",
 *     "",
 *     "用户询问天气",
 *     "get_weather",
 *     "北京",
 *     "正在查询北京天气..."
 * );
 * 2. 访问字段
 * record 为每个字段自动生成了同名的访问方法（注意不是传统的 getXxx()）：
 *
 * String type = output.type();           // 返回 "action"
 * String action = output.action();       // 返回 "get_weather"
 * String actionInput = output.actionInputStr(); // 返回 "北京"
 * 3. 不可变性
 * 所有字段都是 final 的，创建后不能修改。如果需要改变，只能重新创建一个新实例（可以使用 record 的“拷贝”模式，例如通过添加自定义方法实现修改部分字段）。
 *
 * 4. 可以添加自己的方法
 * 虽然 record 主要用于数据，但也可以添加自定义方法（包括静态方法、实例方法）：
 *
 * private record ParsedOutput(...) {
 *     public boolean isAction() {
 *         return "action".equals(type);
 *     }
 * }
 * 为什么用 record 而不是普通类？
 * 代码极简：不需要手写构造方法、equals、hashCode、toString。
 *
 * 语义明确：一眼看出这是纯粹的数据容器。
 *
 * 安全性：不可变性避免了意外修改。
 *
 * 典型应用场景
 * AI 响应解析：将模型返回的文本解析为结构化指令（如 type=action, action=search, actionInputStr=Java record）。
 *
 * 多值返回：一个方法需要返回多个相关信息（如结果、状态、消息）时，用 record 包装比使用 Object[] 或 Map 更安全、更清晰。
 *
 * DTO（数据传输对象）：在不同层之间传递数据。
 */

//实现思路

/**
 * 1.首先我们得拿到我们需要的
 */
public class ReActAgent {


//    private static final String REACT_PROMPT_TEMPLATE = """
//            你是一个强大的 AI 助手，通过思考和使用工具来解决用户的问题。
//
//            你的任务是尽你所能回答以下问题。你可以使用以下工具：
//
//            {tools}
//
//            请严格遵循以下规则和格式：
//            1. 你的行动必须基于一个清晰的“Thought”过程。
//            2. 你必须按顺序使用 "Thought:", "Action:", "Action Input:"。
//            3. 在每次回复中，你只能生成 **一个** Thought/Action/Action Input 组合。
//            4. **绝对不要** 自己编造 "Observation:"。系统会在你执行动作后，将真实的结果作为 Observation 提供给你。
//            5. 当你拥有足够的信息来直接回答用户的问题时，请使用 "Final Answer:" 来输出最终答案。
//            6. 在每次回复中，"Thought:", "Action:", "Action Input:"和"Final Answer:"不能同时出现。
//
//            下面是你的思考和行动格式：
//            Thought: 我需要做什么来解决问题？下一步是什么？
//            Action: 我应该使用哪个工具？必须是 [{tool_names}] 中的一个。
//            Action Input: 我应该给这个工具提供什么输入？这必须是一个 JSON 对象。
//
//            --- 开始 ---
//
//            Question: {input}
//            {agent_scratchpad}
//            """;

    private static final String REACT_PROMPT_TEMPLATE = """
            
            ## 角色定义
            你是一个强大的 AI 助手，通过思考和使用工具来解决用户的问题。
            
            ## 任务
            你的任务是尽你所能回答以下问题。你可以使用以下工具：
            {tools}
            
            ## 规则
            - Action中只需要返回工具的名字，比如writeFile，不要返回以下格式toolName=writeFile
            - 每次只做一次Reason/Action/ActionInput或者FinalAnswer的输出过程，不要一次性都做了
            - 每次返回的过程中不要自己生成Observation的内容
            - 返回Reason/Action/ActionInput的时候不要生成并返回Observation的内容
            
            ## 输出过程参考
            第一轮
            Reason: 你思考的过程
            Action: 你的下一步动作，你想要执行的工具是哪个，必须是{tools}中的一个
            ActionInput: 你要调用的工具的输入参数是什么
            
            第二轮
            Reason: 你思考的过程
            Action: 你的下一步动作，你想要执行的工具是哪个，必须是{tools}中的一个
            ActionInput: 你要调用的工具的输入参数是什么
            
            第三轮
            Reason: 你思考的过程
            Action: 你的下一步动作，你想要执行的工具是哪个，必须是{tools}中的一个
            ActionInput: 你要调用的工具的输入参数是什么
            
            ...
            
            最后一轮
            FinalAnswer: 表示最终的答案，只需要最后输出就可以了
            
            
            ## 用户需求
            Question: {input}
            
            ## 历史聊天记录
            {history}
            """;

    private OpenAIClient apiClient;

    public ReActAgent(OpenAIClient apiClient) {
        this.apiClient = apiClient;
    }

    public String run(String input) throws NoSuchMethodException {

        HashMap<String, Method> tools = new HashMap<>();
        tools.put("writeFile", AgentTools.class.getMethod("writeFile", String.class));


        // 记忆
        StringBuilder history = new StringBuilder();

        int i = 0;
        while (i < 10) {
            try {
                String prompt = buildPrompt(input, history.toString());

                ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                        .addUserMessage(prompt)
                        .model(ModelConfig.LLM_NAME)
                        .build();

                ChatCompletion chatCompletion = apiClient.chat().completions().create(params);
                String rawLlmOutput = chatCompletion.choices().get(0).message().content().get();
                System.out.println("大模型原始输出：" + rawLlmOutput);

                ParsedOutput parsedOutput = parseLlmOutput(rawLlmOutput);

                if (parsedOutput.type.equals("final_answer")) {
                    return parsedOutput.answer;
                }

                String observation = executeTool(parsedOutput, tools);
                System.out.println("工具执行结果：" + observation);

                history.append("Reason: ").append(parsedOutput.reason).append("\n")
                        .append("Action: ").append(parsedOutput.action).append("\n")
                        .append("ActionInput: ").append(parsedOutput.actionInputStr).append("\n")
                        .append("Observation: ").append(observation).append("\n");
            } catch (Exception e) {
                e.printStackTrace();
                i++;
            }
        }

        return "达到了循环最大次数";
    }

    private static String executeTool(ParsedOutput parsedOutput, HashMap<String, Method> tools) throws IllegalAccessException, InvocationTargetException {
        String toolName = parsedOutput.action;
        String toolParams = parsedOutput.actionInputStr;
        Method toolMethod = tools.get(toolName);
        Object observation = toolMethod.invoke(new AgentTools(), toolParams);
        return String.valueOf(observation);
    }

    private String buildPrompt(String input, String history) {
        String prompt = REACT_PROMPT_TEMPLATE.replace("{tools}", ToolUtil.getToolDescription(AgentTools.class));
        prompt = prompt.replace("{input}", input);
        prompt = prompt.replace("{history}", history);

        return prompt;
    }

    private ParsedOutput parseLlmOutput(String llmOutput) {
        if (llmOutput.contains("FinalAnswer: ")) {
            //创建实例
            return new ParsedOutput("final_answer", llmOutput.split("FinalAnswer: ")[1].strip(), null, null, null, null);
        }

        Pattern actionPattern = Pattern.compile("Reason:(.*?)Action:(.*?)ActionInput:(.*)", Pattern.DOTALL);
        Matcher matcher = actionPattern.matcher(llmOutput);

        if (matcher.find()) {
            String reason = matcher.group(1).trim();
            String action = matcher.group(2).trim();
            String actionInputStr = matcher.group(3).trim();

            if (actionInputStr.startsWith("```json")) {
                actionInputStr = actionInputStr.substring(7);
            }
            if (actionInputStr.endsWith("```")) {
                actionInputStr = actionInputStr.substring(0, actionInputStr.length() - 3);
            }
            actionInputStr = actionInputStr.trim();

            return new ParsedOutput("action", null, reason, action, actionInputStr, null);
        }

        return new ParsedOutput("error", null, null, null, null, String.format("解析LLM输出失败: '%s'", llmOutput));
    }

    //Java 16 引入的 record 类型定义。record 是一种特殊的类，专门用来表示不可变的数据载体，它会自动生成构造方法、equals()、hashCode() 和 toString() 等方法，让代码更简洁。
    private record ParsedOutput(
            String type, String answer, String reason, String action, String actionInputStr, String message
    ) {
    }


    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        OpenAIClient apiClient = OpenAIOkHttpClient.builder()
                .apiKey(ModelConfig.API_KEY)
                .baseUrl(ModelConfig.BASE_URL)
                .build();

        ReActAgent reActAgent = new ReActAgent(apiClient);
//        String promptString = "将1到10中间的所有整数写到文件中";
        String promptString = "请帮我用HTML、CSS、JS创建一个简单的贪吃蛇游戏，分成三个文件，分别是snake.html、snake.css、snake.js";

        String result = reActAgent.run(promptString);

        System.out.println(result);
    }
}
