package com.example.ragdemo.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DemoDataLoader implements ApplicationRunner {

  private final VectorStore vectorStore;

  public DemoDataLoader(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Override
  public void run(ApplicationArguments args) {

    if (alreadySeeded()) {
      System.out.println("🔁 Demo vectors already present – skipping bootstrap.");
      return;
    }

    System.out.println("👉 Bootstrapping demo documents into Pinecone …");

    /* build a few sample documents */
    List<Document> docs = List.of(
            new Document(
                    "spring_ai",
                    "Spring AI is a Spring.io project that wraps popular AI services " +
                            "like OpenAI and vector databases such as Pinecone.",
                    Map.of("source", "demo", "type", "info")),

            new Document(
                    "pinecone",
                    "Pinecone is a fully managed vector database for high-performance, " +
                            "production-ready similarity search.",
                    Map.of("source", "demo", "type", "info")),

            new Document(
                    "rag_definition",
                    "Retrieval-Augmented Generation (RAG) combines a vector search step " +
                            "with a language model to ground answers in private data.",
                    Map.of("source", "demo", "type", "glossary"))
    );

    vectorStore.add(docs);

    System.out.println("✅  Inserted {} demo vectors: " + docs.size());
  }

  /** Returns true when at least one demo vector is already in the index. */
  private boolean alreadySeeded() {
    return !vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .topK(1)
                            .filterExpression("source == 'demo'")
                            .build())
            .isEmpty();
  }
}