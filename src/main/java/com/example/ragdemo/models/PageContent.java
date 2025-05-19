package com.example.ragdemo.models;

import com.example.ragdemo.service.WebScraperService;

import java.util.List;

public record PageContent(
        String title,
        List<Section> sections
) {}

