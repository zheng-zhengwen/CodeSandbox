package com.wen.codesandbox.CPP;

import com.wen.codesandbox.model.ExecuteCodeRequest;
import com.wen.codesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * C++ 原生代码沙箱
 */
@Component
public class CppNativeCodeSandbox extends CppCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}