package com.flt.fltspring.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class AnalyzeImageResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    final List<RowDTO> tables;
    final String status;
    final String errorMessage;
}