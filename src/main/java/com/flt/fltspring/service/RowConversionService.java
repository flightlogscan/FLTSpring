package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableRow;
import com.flt.fltspring.model.service.AnalyzeImageResponse;
import com.flt.fltspring.model.service.RowDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RowConversionService {
    private static final String SUCCESS_STATUS = "SUCCESS";

    public AnalyzeImageResponse toRowDTO(List<TableRow> rows) {
        List<RowDTO> dtos = rows.stream()
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
            !CollectionUtils.isEmpty(row.getParentHeaders()) ? row.getParentHeaders() : null,
            row.isHeader()
        );
    }
}