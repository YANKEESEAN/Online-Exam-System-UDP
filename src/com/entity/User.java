package com.entity;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String password;
    private String role; // STUDENT/ADMIN/GRADER
    private String realName;
    private int totalScore;
    private int examCount;

    // 构造方法
    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // 【新增的构造器】解决编译错误：对于User(int,String,String,String), 找不到合适的构造器
    public User(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        // 为其他字段赋予默认值，防止后续操作出现问题
        this.realName = null;
        this.totalScore = 0;
        this.examCount = 0;
    }

    // Getter & Setter（全部生成）
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getExamCount() {
        return examCount;
    }

    public void setExamCount(int examCount) {
        this.examCount = examCount;
    }
}