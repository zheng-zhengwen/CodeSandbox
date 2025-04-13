package com.wen.codesandbox.JAVA;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeValidator {
    private static final Pattern MAIN_METHOD_PATTERN = Pattern.compile("public\\s+static\\s+(\\w+)\\s+main\\s*\\(\\s*String\\s*\\[\\]\\s*args\\s*\\)");

    public static boolean validateMainMethodReturnType(String code) {
        Matcher matcher = MAIN_METHOD_PATTERN.matcher(code);
        if (matcher.find()) {
            String returnType = matcher.group(1);
            return "void".equals(returnType);
        }
        return false;
    }
}