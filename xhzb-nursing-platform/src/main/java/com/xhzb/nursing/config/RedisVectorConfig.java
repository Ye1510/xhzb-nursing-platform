package com.xhzb.nursing.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorConfig {

    @Value("${xhzb.vector-store.host:localhost}")
    private String vectorStoreHost;

    @Value("${xhzb.vector-store.port:6378}")
    private int vectorStorePort;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(vectorStoreHost, vectorStorePort);
    }

    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, OpenAiEmbeddingModel openAiEmbeddingModel) {
        return RedisVectorStore.builder(jedisPooled, openAiEmbeddingModel)
                .indexName("spring-ai-index")                // Optional: defaults to "spring-ai-index"
                .prefix("doc:")                  // Optional: defaults to "embedding:"
                .metadataFields(                         // Optional: define metadata fields for filtering
                        RedisVectorStore.MetadataField.tag("country"),
                        RedisVectorStore.MetadataField.numeric("year"))
                .initializeSchema(true)                   // Optional: defaults to false
                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
    }

}
