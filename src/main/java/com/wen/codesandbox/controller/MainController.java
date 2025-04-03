package com.wen.codesandbox.controller;


import com.wen.codesandbox.CPP.CppNativeCodeSandbox;
import com.wen.codesandbox.CodeSandbox;
import com.wen.codesandbox.JAVA.JavaNativeCodeSandbox;
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
    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private CppNativeCodeSandbox cppNativeCodeSandbox;
    //http://localhost:8090/health
    //http://localhost:8090/executeCode
    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest 执行代码请求
     * @return 执行代码响应
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                           HttpServletResponse response) {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
//        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
        String language = executeCodeRequest.getLanguage();
        CodeSandbox codeSandbox;
            codeSandbox = javaNativeCodeSandbox;

        if ("java".equalsIgnoreCase(language)) {
            codeSandbox = javaNativeCodeSandbox;
        } else if ("cpp".equalsIgnoreCase(language)) {
            codeSandbox = cppNativeCodeSandbox;
        } else {
            throw new RuntimeException("不支持的语言: " + language);
        }
        return codeSandbox.executeCode(executeCodeRequest);
    }


}