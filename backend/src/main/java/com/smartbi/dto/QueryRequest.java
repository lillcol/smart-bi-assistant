package com.smartbi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryRequest {
    /**
     * Natural language question from frontend user.
     */
    @NotBlank(message = "question cannot be blank")
    private String question;
}
