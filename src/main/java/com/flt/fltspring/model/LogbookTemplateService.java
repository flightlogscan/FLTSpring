package com.flt.fltspring.model;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// Service to manage templates
@Service
public class LogbookTemplateService {
    private final Map<LogbookType, LogbookTemplate> templates;

    public LogbookTemplateService() {
        templates = new HashMap<>();
        initializeTemplates();
    }

    private void initializeTemplates() {
        // Initialize Jeppesen template
        LogbookTemplate jeppesen = new LogbookTemplate();
        jeppesen.setType(LogbookType.JEPPESEN);
        Map<Integer, LogbookTemplate.HeaderDefinition> headers = new HashMap<>();
        headers.put(0, new LogbookTemplate.HeaderDefinition("DATE", LogbookTemplate.DataType.DATE, true));
        headers.put(1, new LogbookTemplate.HeaderDefinition("AIRCRAFT TYPE", LogbookTemplate.DataType.TEXT, true));
        headers.put(2, new LogbookTemplate.HeaderDefinition("AIRCRAFT IDENT", LogbookTemplate.DataType.TEXT, true));
        // Add more headers...
        jeppesen.setExpectedHeaders(headers);
        templates.put(LogbookType.JEPPESEN, jeppesen);

        // Add other templates...
    }

    public LogbookTemplate getTemplate(LogbookType type) {
        return templates.get(type);
    }
}
