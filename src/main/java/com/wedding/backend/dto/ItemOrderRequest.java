package com.wedding.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ItemOrderRequest {

    @NotEmpty(message = "Item IDs list cannot be empty")
    private List<UUID> itemIds;
}
