package com.example.ragdemo.models;

import java.util.List;
public record DocumentChunk(
        String text,
        List<QaPair> qaPairs,
        String pageTitle
) {}

