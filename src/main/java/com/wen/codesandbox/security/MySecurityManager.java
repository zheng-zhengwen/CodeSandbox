package com.wen.codesandbox.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Permission;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySecurityManager extends SecurityManager {

    private static final Logger LOGGER = Logger.getLogger(MySecurityManager.class.getName());
    private static final Properties SECURITY_CONFIG = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("security.properties")) {
            SECURITY_CONFIG.load(fis);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load security configuration", e);
        }
    }

    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        LOGGER.log(Level.INFO, "Checking permission: {0}", perm);
        super.checkPermission(perm);
    }

    // 检测程序是否可执行文件
    @Override
    public void checkExec(String cmd) {
        LOGGER.log(Level.INFO, "Checking exec permission for command: {0}", cmd);
        String allowedCommands = SECURITY_CONFIG.getProperty("allowed.exec.commands");
        if (allowedCommands != null) {
            String[] commands = allowedCommands.split(",");
            for (String command : commands) {
                if (cmd.startsWith(command)) {
                    return;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Exec permission denied for command: {0}", cmd);
        throw new SecurityException("checkExec 权限异常:" + cmd);
    }

    // 检测程序是否可读文件
    @Override
    public void checkRead(String file) {
        LOGGER.log(Level.INFO, "Checking read permission for file: {0}", file);
        String allowedDirs = SECURITY_CONFIG.getProperty("allowed.read.dirs");
        if (allowedDirs != null) {
            String[] dirs = allowedDirs.split(",");
            for (String dir : dirs) {
                if (file.contains(dir)) {
                    return;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Read permission denied for file: {0}", file);
        throw new SecurityException("checkRead 权限异常:" + file);
    }

    // 检测程序是否写文件
    @Override
    public void checkWrite(String file) {
        LOGGER.log(Level.INFO, "Checking write permission for file: {0}", file);
        String allowedDirs = SECURITY_CONFIG.getProperty("allowed.write.dirs");
        if (allowedDirs != null) {
            String[] dirs = allowedDirs.split(",");
            for (String dir : dirs) {
                if (file.contains(dir)) {
                    return;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Write permission denied for file: {0}", file);
        throw new SecurityException("checkWrite 权限异常:" + file);
    }

    // 检测程序是否允许删除文件
    @Override
    public void checkDelete(String file) {
        LOGGER.log(Level.INFO, "Checking delete permission for file: {0}", file);
        String allowedDirs = SECURITY_CONFIG.getProperty("allowed.delete.dirs");
        if (allowedDirs != null) {
            String[] dirs = allowedDirs.split(",");
            for (String dir : dirs) {
                if (file.contains(dir)) {
                    return;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Delete permission denied for file: {0}", file);
        throw new SecurityException("checkDelete 权限异常:" + file);
    }

    // 检测程序是否允许连接网络
    @Override
    public void checkConnect(String host, int port) {
        LOGGER.log(Level.INFO, "Checking connect permission for host: {0}, port: {1}", new Object[]{host, port});
        String allowedHosts = SECURITY_CONFIG.getProperty("allowed.connect.hosts");
        if (allowedHosts != null) {
            String[] hosts = allowedHosts.split(",");
            for (String allowedHost : hosts) {
                if (host.equals(allowedHost)) {
                    return;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Connect permission denied for host: {0}, port: {1}", new Object[]{host, port});
        throw new SecurityException("checkConnect 权限异常:" + host + ":" + port);
    }
}