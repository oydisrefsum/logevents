package org.logevents.extend.servlets;

import org.logevents.formatting.AbstractExceptionFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JsonExceptionFormatter extends AbstractExceptionFormatter {

    public JsonExceptionFormatter(Properties properties, String prefix) {
        super(properties, prefix);
    }

    public JsonExceptionFormatter() {
    }

    public List<Map<String, Object>> createStackTrace(Throwable ex) {
        if (ex == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int uniquePrefix = uniquePrefix(ex, null);
        StackTraceElement[] stackTrace = ex.getStackTrace();
        int ignored = 0;
        int actualLines = 0;
        for (int i = 0; i < uniquePrefix && actualLines < maxLength; i++) {
            if (isIgnored(stackTrace[i])) {
                ignored++;
            } else {
                result.add(createStackTraceElement(stackTrace[i], ignored));
                actualLines++;
                ignored = 0;
            }
        }
        if (ignored > 0) {
            Map<String, Object> jsonElement = new HashMap<>();
            jsonElement.put("ignoredFrames", ignored);
            result.add(jsonElement);
        }
        return result;
    }

    private Map<String, Object> createStackTraceElement(StackTraceElement element, int ignored) {
        Map<String, Object> jsonElement = new HashMap<>();
        jsonElement.put("className", element.getClassName());
        jsonElement.put("methodName", element.getMethodName());
        jsonElement.put("lineNumber", String.valueOf(element.getLineNumber()));
        jsonElement.put("fileName", element.getFileName());
        jsonElement.put("sourceLink", getSourceLink(element));
        jsonElement.put("ignoredFrames", ignored);
        return jsonElement;
    }
}