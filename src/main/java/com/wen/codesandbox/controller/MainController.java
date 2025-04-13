package com.wen.codesandbox.controller;

import com.wen.codesandbox.CPP.CppNativeCodeSandbox;
import com.wen.codesandbox.CodeSandbox;
import com.wen.codesandbox.JAVA.JavaNativeCodeSandbox;
import com.wen.codesandbox.Python.PythonNativeCodeSandbox;
import com.wen.codesandbox.model.ExecuteCodeRequest;
import com.wen.codesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private CppNativeCodeSandbox cppNativeCodeSandbox;

    @Resource
    private PythonNativeCodeSandbox pythonNativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                           HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        String language = executeCodeRequest.getLanguage();
        CodeSandbox codeSandbox;
        if ("java".equalsIgnoreCase(language)) {
            codeSandbox = javaNativeCodeSandbox;
        } else if ("cpp".equalsIgnoreCase(language)) {
            codeSandbox = cppNativeCodeSandbox;
        } else if ("python".equalsIgnoreCase(language)) {
            codeSandbox = pythonNativeCodeSandbox;
        } else {
            throw new RuntimeException("不支持的语言: " + language);
        }
        return codeSandbox.executeCode(executeCodeRequest);
    }
}