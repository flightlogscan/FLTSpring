package com.flt.fltspring.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
@Builder
public class AnalyzeImageResponse implements Serializable {
    final String rawResults;  // Serialized raw results for transition
    final List<RowDTO> tables;
    final String status;
}