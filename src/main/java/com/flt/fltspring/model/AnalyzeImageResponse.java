package com.flt.fltspring.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyzeImageResponse {

    final String status; // Status of result analysis
    final String rawResults;  // Serialized raw results
}
