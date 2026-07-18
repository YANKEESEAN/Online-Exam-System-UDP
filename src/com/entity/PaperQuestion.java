package com.entity;

import java.io.Serializable;

public class PaperQuestion implements Serializable {
    private int id;
    private int paperId;
    private int questionId;
    private int questionOrder;
    private Question question;

    // 构造方法
    public PaperQuestion() {
    }

    public PaperQuestion(int id, int paperId, int questionId, int questionOrder, Question question) {
        this.id = id;
        this.paperId = paperId;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
        this.question = question;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}