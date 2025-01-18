package com.flt.fltspring.model;

import lombok.Data;

import java.util.List;

// DTOs for API
@Data
public class TableResponseDTO {
    private List<RowDTO> rows;
}
