package com.wen.codesandbox.Python;

import com.wen.codesandbox.model.ExecuteCodeRequest;
import com.wen.codesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * Python 原生代码沙箱实现
 */
@Component
public class PythonNativeCodeSandbox extends PythonCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}