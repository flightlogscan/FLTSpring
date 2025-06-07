package com.flt.fltspring.service.dummy;

import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.dummy.DummyAnalyzeResult;
import com.flt.fltspring.model.dummy.DummyCellAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyResultConverterService {


    public List<TableStructure> convertToTable(DummyAnalyzeResult dummyResult) {
        if (dummyResult.getTables() == null) {
            log.warn("No tables found in dummy analyze result");
            return new ArrayList<>();
        }

        return dummyResult.getTables().stream()
                .map(table -> new TableStructure(
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DummyCellAdapter::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
