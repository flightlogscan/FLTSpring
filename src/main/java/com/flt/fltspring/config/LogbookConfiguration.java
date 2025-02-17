package com.flt.fltspring.config;

import com.flt.fltspring.model.DocumentAnalysisService.ColumnConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogbookConfiguration {

    @Bean
    public ColumnConfig[] columnConfigs() {
        return new ColumnConfig[] {
                new ColumnConfig("DATE", 1, "STRING"),
                new ColumnConfig("AIRCRAFT TYPE", 1, "STRING"),
                new ColumnConfig("AIRCRAFT IDENT", 1, "STRING"),
                new ColumnConfig("FROM", 1, "STRING"),
                new ColumnConfig("TO", 1, "STRING"),
                new ColumnConfig("NR INST. APP.", 1, "STRING"),
                new ColumnConfig("REMARKS AND ENDORSEMENTS", 1, "STRING"),
                new ColumnConfig("NR T/O", 1, "INTEGER"),
                new ColumnConfig("NR LDG", 1, "INTEGER"),
                new ColumnConfig("SINGLE-ENGINE LAND (DAY)", 1, "INTEGER"),
                new ColumnConfig("SINGLE-ENGINE LAND (NIGHT)", 1, "INTEGER"),
                new ColumnConfig("MULTI-ENGINE LAND", 2, "INTEGER"),
                new ColumnConfig("AND CLASS", 4, "STRING"),
                new ColumnConfig("NIGHT", 2, "INTEGER"),
                new ColumnConfig("ACTUAL INSTRUMENT", 2, "INTEGER"),
                new ColumnConfig("SIMULATED INSTRUMENT (HOOD)", 2, "INTEGER"),
                new ColumnConfig("FLIGHT SIMULATOR", 2, "INTEGER"),
                new ColumnConfig("CROSS COUNTRY", 2, "INTEGER"),
                new ColumnConfig("AS FLIGHT INSTRUCTOR", 2, "INTEGER"),
                new ColumnConfig("DUAL RECEIVED", 2, "INTEGER"),
                new ColumnConfig("PILOT IN COMMAND (INCL. SOLO)", 2, "INTEGER"),
                new ColumnConfig("TOTAL DURATION OF FLIGHT", 2, "INTEGER")
        };
    }
}