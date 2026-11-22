package com.solvestack.controller;

import com.solvestack.dto.CatalogResponse;
import com.solvestack.dto.TopicResponse;
import com.solvestack.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public CatalogResponse getCatalog() {
        return catalog.getCatalog();
    }

    @GetMapping("/{slug}")
    public TopicResponse getTopic(@PathVariable String slug) {
        return catalog.getTopic(slug);
    }
}
