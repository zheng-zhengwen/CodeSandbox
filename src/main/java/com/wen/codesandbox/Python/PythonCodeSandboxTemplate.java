package com.wen.codesandbox.Python;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.wen.codesandbox.CodeSandbox;
import com.wen.codesandbox.model.ExecuteCodeRequest;
import com.wen.codesandbox.model.ExecuteCodeResponse;
import com.wen.codesandbox.model.ExecuteMessage;
import com.wen.codesandbox.model.JudgeInfo;
import com.wen.codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Python 代码沙箱模板类
 */
@Slf4j
public abstract class PythonCodeSandboxTemplate implements CodeSandbox {
    private static final long TIME_OUT = 5000L;
    private static final String GLOBAL_CODE_DIR_NAME = "tmpPythonCode";
    private static final String GLOBAL_PYTHON_CLASS_NAME = "Main.py";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 1. 把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        // 2. 执行代码，得到结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        // 3. 收集整理输出的结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        // 4. 文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("删除文件错误,userCodeFilePath路径 = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }

    /**
     * 保存代码到文件
     *
     * @param code 用户代码
     * @return 保存代码的文件
     */
    protected File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_PYTHON_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 运行文件
     *
     * @param userCodeFile 保存代码的文件
     * @param inputList    输入列表
     * @return 执行结果列表
     */
    protected List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("python %s %s", userCodeFile.getAbsolutePath(), inputArgs);
            System.out.println("执行命令：" + runCmd);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了,立即中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("程序执行出现错误：" + e);
            }
        }
        return executeMessageList;
    }

    /**
     * 获取输出结果
     *
     * @param executeMessageList 执行结果列表
     * @return 执行代码的响应
     */
    protected ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;
        Long maxMemory = 0L;
        StringBuilder message = new StringBuilder();
        for (ExecuteMessage executeMessage : executeMessageList) {
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (maxMemory != null) {
                maxMemory = Math.max(memory, maxMemory);
            }
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            message.append(executeMessage.getMessage());
        }
        executeCodeResponse.setOutputList(outputList);
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message.toString());
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除文件
     *
     * @param userCodeFile 保存代码的文件
     * @return 是否删除成功
     */
    protected boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 获取错误响应
     *
     * @param e 异常
     * @return 错误响应
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}