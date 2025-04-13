package com.wen.codesandbox.security;

import java.security.Permission;

/**
 * 默认安全管理器
 */
public class DefaultSecurityManager extends SecurityManager {
    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        // 根据实际需求添加额外的权限检查逻辑
        super.checkPermission(perm);
        System.out.println("默认不做任何限制");
//        System.out.println(perm);
//         super.checkPermission(perm);
//        throw new SecurityException("权限异常："+perm.toString());
    }
}
