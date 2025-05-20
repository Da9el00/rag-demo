package com.example.ragdemo.models;

import java.util.List;

public record PageContent(
        String title,
        List<Section> sections
) {}

