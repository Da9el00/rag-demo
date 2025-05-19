package com.example.ragdemo.service;

import com.example.ragdemo.models.DocumentChunk;
import com.example.ragdemo.models.PageContent;
import com.example.ragdemo.models.QaPair;
import com.example.ragdemo.models.Section;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Page;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProductIngestionService {

  private final ChatClient chatClient;
  private final VectorStore vectorStore;

  private final ObjectMapper mapper = new ObjectMapper();

  /** Maximum number of characters per chunk */
  private final int CHUNK_SIZE = 3000;

  public ProductIngestionService(ChatClient.Builder chatClientBuilder,
                                 VectorStore vectorStore) {
    this.chatClient = chatClientBuilder.build();
    this.vectorStore = vectorStore;
  }

  /**
   * Takes raw website text, splits it into chunks, generates QA for each,
   * wraps into DocumentChunk, and adds them to the vector store.
   */
  public String addProduct(PageContent pageContent) {
    List<Section> sections = pageContent.sections();
    List<DocumentChunk> docs = new ArrayList<>();

    for (Section section: sections) {

      // 1) chunk the text
      List<String> chunks = chunkText(section.text());

      // 2) for each chunk, call the LLM to get QA pairs and build DocumentChunk
      for (String chunk : chunks) {
        if(chunk.length() < 50) {
          continue;
        }
        String rawQa = chatClient.prompt()
                .system("""
        Generate exactly 3 concise question/answer pairs that cover the main points of the provided text.
        Respond **only** with a JSON array of objects, each having exactly two keys: "question" and "answer".
        For example:
        [
          {"question":"What is X?","answer":"X is ..."},
          {"question":"How does Y work?","answer":"Y works by ..."},
          {"question":"Why use Z?","answer":"You use Z because ..."}
        ]
        """)
                .user("Context:\n" + chunk)
                .call()
                .content();

        List<QaPair> qaPairs;
        try {
          qaPairs = mapper.readValue(
                  rawQa,
                  new TypeReference<>() {
                  }
          );
        } catch (JsonProcessingException e) {
          qaPairs = List.of();
        }

        docs.add(new DocumentChunk(chunk, qaPairs, pageContent.title()));
      }
    }

    List<Document> documents = toVectorDocuments(docs);
    vectorStore.add(documents);

    return documents.toString();
  }

  /**
   * Naive character‐based chunker; breaks on sentence boundaries when possible.
   */
  private List<String> chunkText(String text) {
    List<String> out = new ArrayList<>();
    int start = 0;
    while (start < text.length()) {
      int end = Math.min(text.length(), start + CHUNK_SIZE);
      if (end < text.length()) {
        int lastDot = text.lastIndexOf('.', end);
        if (lastDot > start) {
          end = lastDot + 1;
        }
      }
      out.add(text.substring(start, end).trim());
      start = end;
    }
    return out;
  }

  private List<Document> toVectorDocuments(List<DocumentChunk> chunks) {
    List<Document> out = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      DocumentChunk chunk = chunks.get(i);

      String id = UUID.randomUUID() + "-" + i;

      String qaJson;
      try {
        qaJson = mapper.writeValueAsString(chunk.qaPairs());
      } catch (JsonProcessingException e) {
        qaJson = "[]";
      }

      Map<String,Object> metadata = new HashMap<>();
      metadata.put("chunkIndex", String.valueOf(i));
      metadata.put("title", chunk.pageTitle());
      metadata.put("qaPairs", qaJson);

      Document doc = new Document(
              id,
              chunk.text(),
              metadata
      );

      out.add(doc);
    }
    return out;
  }
}
