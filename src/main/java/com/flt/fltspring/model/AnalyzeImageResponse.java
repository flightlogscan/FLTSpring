package com.flt.fltspring.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class AnalyzeImageResponse {
    final String rawResults;  // Serialized raw results for transition
    final List<RowDTO> tables;
    final String status;
}