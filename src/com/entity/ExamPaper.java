package com.entity;

import java.io.Serializable;
import java.util.List;

public class ExamPaper implements Serializable {
    private int paperId;
    private String paperName;
    private String course;
    private int duration; // 分钟
    private int totalScore;
    private String status; // DRAFT/PUBLISHED/CLOSED
    private int creatorId;
    private List<PaperQuestion> questions;

    // 构造方法
    public ExamPaper() {
    }

    public ExamPaper(int paperId, String paperName, String course, int duration,
            int totalScore, String status, int creatorId, List<PaperQuestion> questions) {
        this.paperId = paperId;
        this.paperName = paperName;
        this.course = course;
        this.duration = duration;
        this.totalScore = totalScore;
        this.status = status;
        this.creatorId = creatorId;
        this.questions = questions;
    }

    // Getter & Setter
    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public List<PaperQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<PaperQuestion> questions) {
        this.questions = questions;
    }
}