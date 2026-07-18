package com.db;

import java.sql.Connection;

public class OracleTest {
    public static void main(String[] args) {
        try {
            // 尝试获取连接
            Connection conn = DBUtil.getConnection();
            System.out.println("✅ Oracle连接成功！可以开始实验了");
            conn.close(); // 关闭连接
        } catch (Exception e) {
            System.err.println("❌ 连接失败：" + e.getMessage());
            // 错误提示
            if (e.getMessage().contains("invalid username/password")) {
                System.err.println("可能原因：用户名或密码错误（检查DBUtil中的USER和PASSWORD）");
            } else if (e.getMessage().contains("Listener refused the connection")) {
                System.err.println("可能原因：Oracle服务未启动或端口错误（默认1521）");
            }
        }
    }
}