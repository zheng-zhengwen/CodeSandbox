package com.wen.codesandbox.utils;


import cn.hutool.core.util.StrUtil;
import com.sun.management.ThreadMXBean;
import com.wen.codesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {
    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        Thread thread = new Thread(() -> {
        });
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待程序执行，获取错误码
            int exitValue  = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            //正常退出，我现在想获取控制台输出
            if(exitValue == 0){
                System.out.println(opName+"成功");
                //分批读取输入流（控制台输出）
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
//                StringBuilder compileOutputStringBuilder = new StringBuilder();
                List<String> outputList=new ArrayList<>();
                //逐行读取，控制台输出信息
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null){
                    outputList.add(compileOutputLine);
                    System.out.println("读取到的是："+compileOutputLine+"\n读取结束\n");
                }
                executeMessage.setMessage(StringUtils.join(outputList,"\n"));
            }else{
                //异常提出
                System.out.println(opName+"失败，错误码：" + exitValue);
                //分批读取正常输出流：正常输出里写一些错误日志之类的
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String>outputList=new ArrayList<>();
//                StringBuilder compileOutputStringBuilder = new StringBuilder();
                //逐行读取，控制台输出信息
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null){
                    outputList.add(compileOutputLine);
                    System.out.println("异常了，读取到的是："+compileOutputLine+"\n读取结束\n");
                }
                executeMessage.setMessage(StringUtils.join(outputList,"\n"));

                //分批读取错误输出：
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
//                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();

                //逐行读取，控制台输出信息
                List<String>errorOutputList=new ArrayList<>();
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null){
                    errorOutputList.add(errorCompileOutputLine);
//                    System.out.println(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutputList,"\n"));
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
            long finalMemory = getUsedMemory();
            // 计算内存使用量，单位字节，转换成kb需要除以1024
            executeMessage.setTime(lastTaskTimeMillis);
            executeMessage.setMemory(finalMemory / 102400);
//            System.out.println("1111executeMessage:"+executeMessage);
        }catch (Exception e){
//            e.printStackTrace();
            throw  new RuntimeException(e);
        }
        return  executeMessage;
    }



    /**
     * 执行交互式进程，并获取信息
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess,String args){
        ExecuteMessage executeMessage=new ExecuteMessage();
        try {
            //向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s)+"\n";
            outputStreamWriter.write(join);
            //相当于按下回车，执行发送
            outputStreamWriter.flush();
            //分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            //逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            //记得资源释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        }catch (Exception e){
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 获取当前已使用的内存量
     * 单位是byte
     *
     * @return
     */
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }


}