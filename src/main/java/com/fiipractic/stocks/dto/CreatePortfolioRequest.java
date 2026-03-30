package com.fiipractic.stocks.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePortfolioRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    public CreatePortfolioRequest() {
    }

    public CreatePortfolioRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
