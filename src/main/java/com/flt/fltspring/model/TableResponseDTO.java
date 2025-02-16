package com.flt.fltspring.model;

import lombok.Data;

import java.util.List;
@Data
public class TableResponseDTO {
    private List<RowDTO> rows;
}
