package com.entity;

import java.io.Serializable;

public class Question implements Serializable {
    private int id;
    private String type; // SINGLE/MULTIPLE/FILL/ESSAY
    private String content;
    private String[] options;
    private String answer;
    private String knowledgePoint;
    private int difficulty; // 1-3
    private int score;

    // 构造方法
    public Question() {
    }

    public Question(int id, String type, String content, String[] options, String answer,
            String knowledgePoint, int difficulty, int score) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.options = options;
        this.answer = answer;
        this.knowledgePoint = knowledgePoint;
        this.difficulty = difficulty;
        this.score = score;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}