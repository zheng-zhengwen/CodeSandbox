package com.wen.codesandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.wen.codesandbox.model.ExecuteCodeRequest;
import com.wen.codesandbox.model.ExecuteCodeResponse;
import com.wen.codesandbox.model.ExecuteMessage;
import com.wen.codesandbox.model.JudgeInfo;
import com.wen.codesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Java原生代码沙箱
 */
public class JavaNativeCodeSandboxOld implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    private static final Long TIME_OUT = 5000L;
    private static final WordTree WORD_TREE;
    //黑名单
    private static final List<String> blackList = Arrays.asList("Files", "exec");
    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";
//    private static final String SECURITY_MANAGER_PATH = "E:\\online_code\\code-sandbox\\src\\main\\resources\\security";

    private static final String SECURITY_MANAGER_PATH = "src/main/resources/security";

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }
//    public static void main(String[] args) {
//        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
//        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();
//        executeCodeRequest.setInputList(Arrays.asList("1 2", "3"));
//        //ResourceUtil可以读取resources目录下的文件
////        String code = ResourceUtil.readStr("testCode/unsafeCode/Main.java", StandardCharsets.UTF_8);
////        String code = ResourceUtil.readStr("testCode/unsafeCode/SleepError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
////        String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
//        executeCodeRequest.setCode(code);
//        executeCodeRequest.setLanguage("java");
//        ExecuteCodeResponse executecodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
//        System.out.println(executecodeResponse);
//    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        System.setSecurityManager(new DefaultSecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();
        String code = executeCodeRequest.getCode();

//        //校验代码
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if(foundWord != null){
//            System.out.println("包含禁止词" + foundWord.getFoundWord());
//            return null;
//        }


        //1.把用户提交的代码保存到文件中
        //获取当前用户工作目录
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //没有全局代码目录就新建
        if (FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        //用户代码父目录
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        //2. 编译代码，得到 clss 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compilePro = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compilePro, "编译");
            System.out.println(executeMessage);
        } catch (IOException e) {
            return getErrorResponse(e);
//            throw new RuntimeException(e);
        }

        //3. 执行代码，得到输出结果
        //获取所有的输出信息列表,存储每个输入参数对应的执行信息
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
//            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            String runCmd=String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%S Main %s",userCodeParentPath,SECURITY_MANAGER_PATH,SECURITY_MANAGER_CLASS_NAME,inputArgs);
//            String runCmd=String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(()->{
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了,立即中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                System.out.println(executeMessage);
                //把所有输出信息全部 add 进来
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
//                throw new RuntimeException(e);
                return getErrorResponse(e);
            }
        }

        //4.收集整理输出的结果
        //创建 ExecuteCodeResponse 对象用于存储最终的执行结果。
        ExecuteCodeResponse executeCodeResponse=new ExecuteCodeResponse();
        List<String> outputList=new ArrayList<>();
        //取最大值判断是否超时
        long maxTime=0;
        for(ExecuteMessage executeMessage : executeMessageList){
            String errorMessage=executeMessage.getErrorMessage();

            //有的执行用例执行时出现错误，响应信息直接设为错误信息，且响应状态设为错误3,中断循环
            if(StrUtil.isNotBlank(executeMessage.getErrorMessage())){
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                //用户提交代码执行中存在错误3
                executeCodeResponse.setStatus(3);
                break;
            }
            //将输出用例添加到列表
            outputList.add(executeMessage.getMessage());
            //只要有一个程序超时，就判断为超时
            Long time = executeMessage.getTime();
            if(time != null){
                maxTime = Math.max(maxTime,time);
            }
        }
        //正常运行完成,状态都设置为1
        if(outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        //创建 JudgeInfo 对象，将最大执行时间设置到该对象中，再将该对象设置到 executeCodeResponse 中
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //要借助第三方库来获取内存，过于麻烦，不做展示
        // 不推荐在 java 原生实现代码沙箱的过程中获取
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        //5.文件清理
        if(userCodeFile.getParentFile()!=null){
            boolean del=FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
        }
        //6.错误处理，提升程序健壮性
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse=new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //表示代码沙箱错误（可能是编译错误）
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

}

