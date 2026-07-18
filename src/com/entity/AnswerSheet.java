package com.entity;

import java.io.Serializable;
import java.util.List;

public class AnswerSheet implements Serializable {
    private int sheetId;
    private int userId;
    private int paperId;
    private double objectiveScore;
    private double subjectiveScore;
    private double totalScore;
    private String status; // DOING/SUBMITTED/GRADED
    private List<AnswerDetail> answerDetails;

    // 构造方法
    public AnswerSheet() {
    }

    public AnswerSheet(int sheetId, int userId, int paperId, double objectiveScore,
            double subjectiveScore, double totalScore, String status,
            List<AnswerDetail> answerDetails) {
        this.sheetId = sheetId;
        this.userId = userId;
        this.paperId = paperId;
        this.objectiveScore = objectiveScore;
        this.subjectiveScore = subjectiveScore;
        this.totalScore = totalScore;
        this.status = status;
        this.answerDetails = answerDetails;
    }

    // Getter & Setter
    public int getSheetId() {
        return sheetId;
    }

    public void setSheetId(int sheetId) {
        this.sheetId = sheetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public double getObjectiveScore() {
        return objectiveScore;
    }

    public void setObjectiveScore(double objectiveScore) {
        this.objectiveScore = objectiveScore;
    }

    public double getSubjectiveScore() {
        return subjectiveScore;
    }

    public void setSubjectiveScore(double subjectiveScore) {
        this.subjectiveScore = subjectiveScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AnswerDetail> getAnswerDetails() {
        return answerDetails;
    }

    public void setAnswerDetails(List<AnswerDetail> answerDetails) {
        this.answerDetails = answerDetails;
    }
}