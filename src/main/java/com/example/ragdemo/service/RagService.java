package com.example.ragdemo.service;

import com.example.ragdemo.models.PageContent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
  private final VectorStore vectorStore;
  private final ChatClient chatClient;
  private final WebScraperService webScraperService;
  private final ProductIngestionService productIngestionService;

  public RagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, WebScraperService webScraperService, ProductIngestionService productIngestionService) {
    this.vectorStore = vectorStore;
    this.chatClient = chatClientBuilder.build();
    this.webScraperService = webScraperService;
    this.productIngestionService = productIngestionService;
  }

  public String answer(String question) {
    List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(question)
                    .topK(2)
                    .build());

    String context = docs.stream()
            .map(Document::getFormattedContent)
            .collect(Collectors.joining("\n\n---\n\n"));

    ChatClient.CallResponseSpec response = chatClient.prompt()
            .system("""
                    You are a helpful assistant explaining about content on an e-commerce page.
                    Answer mainly based on the knowledge from context;
                    Do not refer to the context in you answer.
                    If the context does not help contain the answer, say you don’t know.
                    Improve the answer to help sell the product, but keep focus form context, and keep language simple, and no em dashes.
                    Respond in same language as the question is asked.
                    When appropriate, return a list of facts over long text blocks.
                    Return everything in markdown format.
                    """)
            .user("Context:\n" + context + "\n\nQuestion: " + question)
            .call();

    return response.content();
  }

  public Flux<String> answerStream(String question) {
    List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(question)
                    .topK(5)
                    .build());

    String context = docs.stream()
            .map(Document::getFormattedContent)
            .collect(Collectors.joining("\n\n---\n\n"));

    return chatClient.prompt()
            .system("""
                    You are a helpful assistant explaining about content on an e-commerce page.
                    Answer mainly based on the knowledge from context;
                    Do not refer to the context in you answer.
                    If the context does not help contain the answer, say you don’t know.
                    Improve the answer to help sell the product, but keep focus form context, and keep language simple, and no em dashes.
                    Respond in same language as the question is asked.
                    When appropriate, return a list of facts over long text blocks.
                    Return everything in markdown format.
                    """)
            .user("Context:\n" + context + "\n\nQuestion: " + question)
            .stream()
            .content();
  }

  public String addProduct(String website) {
    //Scrape website to get its content
    PageContent pageContent = webScraperService.scrapePage(website);
    //Create document for vectorDB and upload
    String result = productIngestionService.addProduct(pageContent);
    return result;
  }
}
