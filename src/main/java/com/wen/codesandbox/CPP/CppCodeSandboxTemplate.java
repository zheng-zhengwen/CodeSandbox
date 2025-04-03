package com.wen.codesandbox.CPP;

import cn.hutool.core.util.StrUtil;
import com.wen.codesandbox.CodeSandbox;
import com.wen.codesandbox.model.ExecuteCodeRequest;
import com.wen.codesandbox.model.ExecuteCodeResponse;
import com.wen.codesandbox.model.ExecuteMessage;
import com.wen.codesandbox.model.JudgeInfo;
import com.wen.codesandbox.utils.ProcessUtils;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 代码沙箱模板类
 */
@Slf4j
public abstract class CppCodeSandboxTemplate implements CodeSandbox {
    private static final long TIME_OUT = 5000L;
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCppCode";
    private static final String GLOBAL_CPP_CLASS_NAME = "Main.cpp";
    private static final String GLOBAL_EXE_NAME = "Main.exe";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 1. 把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        // 2. 编译代码
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        // 3. 执行代码，得到结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        // 4. 收集整理输出的结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        // 5. 文件清理
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
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_CPP_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 编译文件
     *
     * @param userCodeFile 保存代码的文件
     * @return 编译结果信息
     */
    protected ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("g++ -o %s %s", userCodeFile.getParent() + File.separator + GLOBAL_EXE_NAME, userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println("编译结果=" + executeMessage);
            return executeMessage;
        } catch (IOException e) {
            throw new RuntimeException("编译代码错误:" + e);
        }
    }

    /**
     * 运行文件
     *
     * @param userCodeFile 保存代码的文件
     * @param inputList    输入列表
     * @return 执行结果列表
     */
//    protected List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
//        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
//        List<ExecuteMessage> executeMessageList = new ArrayList<>();
//        // 只使用第一个输入参数
//        if (!inputList.isEmpty()) {
//            String inputArgs = inputList.get(0);
//            String[] args = inputArgs.split(" ");
//            List<String> command = new ArrayList<>();
//            // 使用编译后的可执行文件
//            command.add(userCodeParentPath + File.separator + GLOBAL_EXE_NAME);
//            command.addAll(Arrays.asList(args));
//
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            Future<ExecuteMessage> future = executor.submit(() -> {
//                ProcessBuilder processBuilder = new ProcessBuilder(command);
//                Process runProcess = processBuilder.start();
//                return ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//            });
//
//            try {
//                ExecuteMessage executeMessage = future.get(TIME_OUT, TimeUnit.MILLISECONDS);
//                executeMessageList.add(executeMessage);
//                System.out.println("运行结束"+executeMessage+"   \n第二个:"+executeMessageList);
//            } catch (TimeoutException e) {
//                // 超时处理逻辑
//                System.err.println("程序执行超时: " + e.getMessage());
//                future.cancel(true);
//                ExecuteMessage timeoutMessage = new ExecuteMessage();
//                timeoutMessage.setExitValue(-1);
//                timeoutMessage.setErrorMessage("程序执行超时");
//                executeMessageList.add(timeoutMessage);
//            } catch (InterruptedException | ExecutionException e) {
//                throw new RuntimeException("程序执行出现错误：" + e);
//            } finally {
//                executor.shutdown();
//            }
//        }
//        return executeMessageList;
//    }
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            String[] args = inputArgs.split(" ");
            List<String> command = new ArrayList<>();
            command.add(userCodeParentPath + File.separator + GLOBAL_EXE_NAME);
            command.addAll(Arrays.asList(args));

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<ExecuteMessage> future = executor.submit(() -> {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    Process runProcess = processBuilder.start();
                    return ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                } catch (Exception e) {
                    throw new RuntimeException("程序执行出现错误：" + e);
                }
            });

            try {
                ExecuteMessage executeMessage = future.get(TIME_OUT, TimeUnit.MILLISECONDS);
                executeMessageList.add(executeMessage);
            } catch (TimeoutException e) {
                System.err.println("程序执行超时: " + e.getMessage());
                future.cancel(true);
                ExecuteMessage timeoutMessage = new ExecuteMessage();
                timeoutMessage.setExitValue(-1);
                timeoutMessage.setErrorMessage("程序执行超时");
                executeMessageList.add(timeoutMessage);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("程序执行出现错误：" + e);
            } finally {
                executor.shutdown();
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
        // 取最大值判断是否超时
        long maxTime = 0;
        Long maxMemory = 0L;
        StringBuilder message = new StringBuilder();
        for (ExecuteMessage executeMessage : executeMessageList) {
            // 只要有一个程序超时，就判断为超时
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if(maxMemory != null){
                maxMemory = Math.max(memory,maxMemory);
            }
            String errorMessage = executeMessage.getErrorMessage();
            // 有的执行用例执行时出现错误，响应信息直接设为错误信息，且响应状态设为错误3,中断循环
            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交运行成功代码执行中存在错误3
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            message.append(executeMessage.getMessage());
        }
        executeCodeResponse.setOutputList(outputList);
        // 正常运行完成,状态都设置为1
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message.toString());   //judgeInfo 的信息在判题过程中设置
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
//        executeCodeResponse.setOutputList(outputList);
//        // 创建 JudgeInfo 对象，将最大执行时间设置到该对象中，再将该对象设置到 executeCodeResponse 中
//        com.wen.codesandbox.model.JudgeInfo judgeInfo = new com.wen.codesandbox.model.JudgeInfo();
//        judgeInfo.setTime(maxTime);
//        executeCodeResponse.setJudgeInfo(judgeInfo);
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
        // 表示代码沙箱错误（可能是编译错误）
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new com.wen.codesandbox.model.JudgeInfo());
        return executeCodeResponse;
    }
}