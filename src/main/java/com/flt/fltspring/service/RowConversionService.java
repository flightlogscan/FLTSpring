package com.flt.fltspring.service;

import com.flt.fltspring.model.RowDTO;
import com.flt.fltspring.model.TableResponseDTO;
import com.flt.fltspring.model.TableRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RowConversionService {

    private final LogbookValidationService validationService;
    private final TableDataTransformerService transformer;

    public TableResponseDTO convert(List<TableRow> rows) {
        List<TableRow> transformed = transformer.transformData(rows);
        List<TableRow> validated = validationService.validateAndCorrect(transformed);

        List<RowDTO> dtos = validated.stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();

        TableResponseDTO response = new TableResponseDTO();
        response.setRows(dtos);
        return response;
    }

    private RowDTO toDto(TableRow row) {
        RowDTO dto = new RowDTO();
        dto.setRowIndex(row.getRowIndex());
        dto.setContent(row.getColumnData());
        dto.setHeader(row.isHeader());
        if (row.getParentHeaders() != null && !row.getParentHeaders().isEmpty()) {
            dto.setParentHeaders(row.getParentHeaders());
        }
        return dto;
    }
}