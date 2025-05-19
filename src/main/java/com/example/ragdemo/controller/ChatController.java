package com.example.ragdemo.controller;

import com.example.ragdemo.dto.AddProductRequest;
import com.example.ragdemo.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

  private final RagService ragService;

  public ChatController(RagService ragService) {
    this.ragService = ragService;
  }

  @PostMapping("/ask")
  public ResponseEntity<String> rag(@RequestBody Map<String,String> body) {
    String question = body.get("question");
    if (question == null || question.isBlank())
      return ResponseEntity.badRequest().body("JSON must contain a 'question' field");
    return ResponseEntity.ok(ragService.answer(question));
  }

  @PostMapping(value    = "/ask-stream",
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> askStream(@RequestBody Map<String, String> body) {

    String question = body.get("question");
    if (question == null || question.isBlank()) {
      return Flux.error(
              new IllegalArgumentException("JSON must contain a non-empty 'question' field"));
    }

    return ragService.answerStream(question);
  }


  @PostMapping("/products")
  public ResponseEntity<Object> addNewProduct(@RequestBody AddProductRequest request) {
    String website = request.website();
    if (website == null || website.isBlank())
      return ResponseEntity.badRequest().body("JSON must contain a 'website' field");
    return ResponseEntity.ok(ragService.addProduct(website));
  }
}

