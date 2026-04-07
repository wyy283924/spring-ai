package com.wyy;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
public class WyyController {
    private final ChatClient chatClient;
    @Autowired
    @Qualifier("bbb")
    private ChatMemory chatMemory;

    @Autowired
    private EmbeddingModel embeddingModel;
    @Value("classpath:qa.txt")
    private org.springframework.core.io.Resource resource;
    @Resource
    private VectorStore vectorStore; // 向量数据库，ES

    @Autowired
    private ChatClient.Builder chatClientBuilder;
    public WyyController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai")
    String generation(@RequestParam("userInput") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }
    //打字机效果
    @GetMapping(value = "/stream",produces = "text/html;charset=UTF-8")
    Flux<String> stream(@RequestParam("userInput") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }
    //sse
    @GetMapping(value = "/sse")
    public SseEmitter sse(String question) {
        SseEmitter sseEmitter = new SseEmitter() {
            @Override
            protected void extendResponse(ServerHttpResponse outputMessage) {
                HttpHeaders headers = outputMessage.getHeaders();
                headers.setContentType(new MediaType("text", "event-stream", StandardCharsets.UTF_8));
            }
        };

        Flux<String> stream = chatClient.prompt(question).stream().content();

        stream.subscribe(token -> {
            try {
                sseEmitter.send(token);
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
            }
        }, sseEmitter::completeWithError, sseEmitter::complete);

        return sseEmitter;
    }

    @GetMapping("/output")
    public Poem output(String topic) {

        //这里就是把我们的对象转换成json格式，以便后面可以和提示词放在一起
        //主要是用来告诉大模型怎么返回
//        {
//        "$schema" : "https://json-schema.org/draft/2020-12/schema",
//                "type" : "object",
//                "properties" : {
//            "author" : {
//                "type" : "string"
//            },
//            "content" : {
//                "type" : "string"
//            },
//            "title" : {
//                "type" : "string"
//            }
//        },
//        "additionalProperties" : false
//    }
        BeanOutputConverter<Poem> outputConverter = new BeanOutputConverter<>(new ParameterizedTypeReference<Poem>(){});

        PromptTemplate promptTemplate = new PromptTemplate("写一首关于{topic}的七言绝句，{format}");
        //获取到我们的提示词

        String prompt = promptTemplate.render(Map.of("topic", topic, "format", outputConverter.getFormat()));

        String content = chatClient.prompt(prompt).call().content();
        //直接将字符串转换成对象
        return outputConverter.convert(content);
    }
    @GetMapping("/entity")
    public Poem entity(String topic) {
        PromptTemplate promptTemplate = new PromptTemplate("写一首关于{topic}的七言绝句");
        String prompt = promptTemplate.render(Map.of("topic", topic));

        return chatClient.prompt(prompt).call().entity(Poem.class);
    }
    //这个请求经过同一个advisor，将问题和响应存储在同一个存储空间，会话隔离
    //chatcLIENT当它要发送请求的时候，
    // 先进入before方法
    //1.拿context
    //2.拿出所有历史消息，加到当前的请求中
    //call()
    //after()
    //响应加到chatMemory中
    @GetMapping("/memory")
    public String memory(@RequestParam("chatId") String chatId, @RequestParam("question") String question) {
        return chatClient
                .prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(question)
                .call()
                .content();
    }
    @GetMapping("/advisor")
    public String advisor(String question) {
        return this.chatClient
                .prompt()
                .advisors(new WYYCallAroundAdvisor())
                .user(question)
                .call()
                .content();
    }
    //RAG
    @GetMapping("/embedding")
    public float[] embedding(String question) {
        return embeddingModel.embed(question);
    }
    /**
     * 文档切分和向量存储
     */
    @GetMapping("/store")
    public List<Document> store() {
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.get();

        WYYTextSplitter wyyTextSplitter = new WYYTextSplitter();
        List<Document> list = wyyTextSplitter.apply(documents);

        for (Document document : list) {
            document.getMetadata().put("author", "wyy");
            document.getMetadata().put("article_type", "blog");
        }

        vectorStore.add(list);

        return list;
    }
    /**
     * 语义相似度查询
     */
    @GetMapping("/search")
    public List<Document> search(String question) {
        SearchRequest searchRequest = SearchRequest
                .builder()
                .query(question)
                .topK(1)
                .similarityThreshold(0.8)
                //先根据原数据进行过滤
//                .filterExpression("author in ['wyy','jill']")
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }
    @GetMapping("/ragChat")
    public String ragChat(String question) {

        // 向量搜索
        List<Document> documentList = search(question);

        // 提示词模板
        PromptTemplate promptTemplate = new PromptTemplate("{question}\n\n 用以下信息回答问题:\n {contents}");

        // 组装提示词
        Prompt prompt = promptTemplate.create(Map.of("question", question, "contents", documentList));

        // 调用大模型
        return chatClient.prompt(prompt).call().content();
    }
    /**
     * 利用RetrievalAugmentationAdvisor进行RAG
     */
    @GetMapping("/ragAdvisor")
    public String ragAdvisor(String question) {
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .build())
                .build();

        return chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();
    }
    /**
     * 利用RetrievalAugmentationAdvisor进行RAG
     */
    @GetMapping("/ragAdvisor2")
    public String ragAdvisor2(@RequestParam("chatId") String chatId, @RequestParam("question") String question) {

        // 将用户问题和聊天记录进行压缩
        CompressionQueryTransformer queryTransformer = CompressionQueryTransformer.builder().chatClientBuilder(chatClientBuilder).build();

        // 将用户问题扩写为多个问题
        MultiQueryExpander queryExpander = MultiQueryExpander.builder().chatClientBuilder(chatClientBuilder).build();

        // 从向量数据库中进行语义相似度查询
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.73)
                .topK(5)
                .build();

        // 对多个Document进行合并
        DocumentJoiner documentJoiner = new ConcatenationDocumentJoiner();

        // 增强查询，基于用户问题和检索结果进行增强查询
        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder().build();

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .queryExpander(queryExpander)
                .documentRetriever(documentRetriever)
                .documentJoiner(documentJoiner)
                .queryAugmenter(queryAugmenter)
                .build();

        return chatClient
                .prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), retrievalAugmentationAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(question)
                .call()
                .content();
    }
    /**
     * 幻觉评估
     */
    @GetMapping("/evaluation")
    public EvaluationResponse evaluation(String question) {
        // 语义相似度查询
        SearchRequest searchRequest = SearchRequest
                .builder()
                .query(question)
                .topK(1)
                .similarityThreshold(0.1)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        // 组装提示词
        PromptTemplate promptTemplate = new PromptTemplate("{userMessage}\n\n 用以下信息回答问题:\n {contents}");
        String prompt = promptTemplate.render(Map.of("userMessage", question, "contents", documents));

        // 调用大模型
        String content = chatClient.prompt(prompt).call().content();
        // String result = "我是周瑜";

        // 评估器（可以换成另外一个模型）
        RelevancyEvaluator relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);

        // 评估是否产生了幻觉
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, documents, content);

        return relevancyEvaluator.evaluate(evaluationRequest);
    }

}
