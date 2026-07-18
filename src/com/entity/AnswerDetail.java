package com.entity;

import java.io.Serializable;

public class AnswerDetail implements Serializable {
    private int detailId;
    private int sheetId;
    private int questionId;
    private String userAnswer;
    private double score;
    private String graderComment;
    private int graderId;
    private Question question;

    // 构造方法
    public AnswerDetail() {
    }

    public AnswerDetail(int detailId, int sheetId, int questionId, String userAnswer,
            double score, String graderComment, int graderId, Question question) {
        this.detailId = detailId;
        this.sheetId = sheetId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.score = score;
        this.graderComment = graderComment;
        this.graderId = graderId;
        this.question = question;
    }

    // Getter & Setter
    public int getDetailId() {
        return detailId;
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }

    public int getSheetId() {
        return sheetId;
    }

    public void setSheetId(int sheetId) {
        this.sheetId = sheetId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getGraderComment() {
        return graderComment;
    }

    public void setGraderComment(String graderComment) {
        this.graderComment = graderComment;
    }

    public int getGraderId() {
        return graderId;
    }

    public void setGraderId(int graderId) {
        this.graderId = graderId;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}