package com.flt.fltspring.service;

import com.flt.fltspring.model.RowDTO;
import com.flt.fltspring.model.TableResponseDTO;
import com.flt.fltspring.model.TableRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RowProcessorService {

    private final LogbookValidationService validationService;
    private final TableDataTransformerService transformer;

    public TableResponseDTO processTableRows(List<TableRow> tableRows) {
        log.info("Processing {} table rows", tableRows.size());
        log.info("Input rows: {}", tableRows);
        List<TableRow> transformedRows = transformer.transformData(tableRows);
        log.info("After transformation: {} rows", transformedRows.size());
        log.info("Transformed rows: {}", transformedRows);
        List<TableRow> validatedRows = validationService.validateAndCorrect(transformedRows);
        log.info("After validation: {} rows", validatedRows.size());
        log.info("Validated rows: {}", validatedRows);
        return mapToResponse(validatedRows);
    }

    private TableResponseDTO mapToResponse(List<TableRow> rows) {
        log.info("Mapping {} rows to response", rows.size());
        final TableResponseDTO response = new TableResponseDTO();
        final List<RowDTO> dtos = rows.stream()
                .map(this::convertToRowDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.info("Converted to {} DTOs", dtos.size());
        log.info("Final DTOs: {}", dtos);
        response.setRows(dtos);
        return response;
    }

    private RowDTO convertToRowDTO(TableRow row) {
        if (row == null) {
            log.info("Skipping null row");
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Converting row to DTO: {}", row);
        }
        
        final RowDTO dto = new RowDTO();
        dto.setRowIndex(row.getRowIndex());
        dto.setContent(row.getColumnData());
        dto.setHeader(row.isHeader());
        
        // Include parent headers if available
        if (row.getParentHeaders() != null && !row.getParentHeaders().isEmpty()) {
            dto.setParentHeaders(row.getParentHeaders());
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Created DTO: {}", dto);
        }
        return dto;
    }
}
