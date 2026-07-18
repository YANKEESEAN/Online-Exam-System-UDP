package com.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Properties;

public class DBUtil {
    // Oracle连接配置
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "exam_user";
    private static final String PASSWORD = "123456";

    static {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle驱动加载失败：" + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("oracle.jdbc.defaultNChar", "true");
        props.setProperty("oracle.jdbc.convertNcharLiterals", "true");
        props.setProperty("oracle.jdbc.useNio", "true");
        props.setProperty("oracle.jdbc.mapDateToTimestamp", "false");
        props.setProperty("oracle.net.encoding", "UTF8");
        props.setProperty("oracle.net.ns_charset", "UTF8");

        return DriverManager.getConnection(URL, props);
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (stmt != null)
                stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}