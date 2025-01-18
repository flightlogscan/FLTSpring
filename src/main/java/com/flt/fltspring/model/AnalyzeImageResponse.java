package com.flt.fltspring.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class AnalyzeImageResponse {
    final String rawResults;  // Serialized raw results for transition
    final List<TableRow> tables;
}

//// Usage in your service layer
//@Service
//@RequiredArgsConstructor
//class FlightLogService {
//    private final LogbookValidationService validationService;
//
//    public TableResponseDTO processLogbook(TableRow rawData, LogbookType type) {
//        // Convert raw data to business objects
//        final TableDataTransformer tableDataTransformer = new TableDataTransformer();
//        List<TableRow> businessRows = tableDataTransformer.transformData(Arrays.asList(rawData));
//
//        // Validate and correct using template
//        List<TableRow> correctedRows = validationService.validateAndCorrect(businessRows, type);
//
//        // Convert to DTO and return
//        // TODO: Implement mapToResponse
//        return new TableResponseDTO();
//        //return mapToResponse(correctedRows);
//    }