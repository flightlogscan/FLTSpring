package com.flt.fltspring.service;

import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.model.service.RowDTO;
import com.flt.fltspring.model.bizlogic.TableRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RowConversionService {
    private static final String SUCCESS_STATUS = "SUCCESS";

    private final LogbookValidationService validationService;
    private final TableDataTransformerService transformer;

    public AnalyzeImageResponse toRowDTO(List<TableRow> rows) {
        List<TableRow> transformed = transformer.transformData(rows);
        List<TableRow> validated = validationService.validateAndCorrect(transformed);

        List<RowDTO> dtos = validated.stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();

        return AnalyzeImageResponse.builder()
                .status(SUCCESS_STATUS)
                .tables(dtos)
                .build();
    }

    private RowDTO toDto(TableRow row) {
        return new RowDTO(
            row.getRowIndex(),
            row.getColumnData(),
            (row.getParentHeaders() != null && !row.getParentHeaders().isEmpty()) ? row.getParentHeaders() : null,
            row.isHeader()
        );
    }
}