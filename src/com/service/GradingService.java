package com.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.db.DBUtil;
import com.entity.AnswerDetail;
import com.entity.AnswerSheet;
import com.entity.Question;
import com.entity.User;

public class GradingService {
    // 获取待批阅答卷
    public List<AnswerSheet> getPendingSheets() throws SQLException {
        List<AnswerSheet> sheets = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT * FROM answer_sheets WHERE status = 'SUBMITTED'";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            AnswerSheet sheet = new AnswerSheet();
            sheet.setSheetId(rs.getInt("sheet_id"));
            sheet.setUserId(rs.getInt("user_id"));
            sheet.setPaperId(rs.getInt("paper_id"));
            sheet.setObjectiveScore(rs.getDouble("objective_score"));
            sheets.add(sheet);
        }
        DBUtil.close(conn, pstmt, rs);
        return sheets;
    }

    // 获取答卷的主观题
    public List<AnswerDetail> getSubjectiveQuestions(int sheetId) throws SQLException {
        List<AnswerDetail> details = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT ad.*, q.content, q.answer, q.score AS question_score, q.type FROM answer_details ad " +
                "JOIN questions q ON ad.question_id = q.id " +
                "WHERE ad.sheet_id = ? AND q.type = 'ESSAY' AND ad.score = 0";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, sheetId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            AnswerDetail detail = new AnswerDetail();
            detail.setDetailId(rs.getInt("detail_id"));
            detail.setQuestionId(rs.getInt("question_id"));
            detail.setUserAnswer(rs.getString("user_answer"));

            Question q = new Question();
            q.setContent(rs.getString("content"));
            q.setAnswer(rs.getString("answer"));
            q.setType(rs.getString("type"));
            q.setScore(rs.getInt("question_score"));
            detail.setQuestion(q);

            details.add(detail);
        }
        DBUtil.close(conn, pstmt, rs);
        return details;
    }

    // 批阅主观题
    public String gradeSubjective(int detailId, double score, String comment, int graderId, int sheetId)
            throws SQLException {
        Connection conn = DBUtil.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement detailPstmt = null;
        PreparedStatement sumPstmt = null;
        ResultSet sumRs = null;
        PreparedStatement objPstmt = null;
        ResultSet objRs = null;
        PreparedStatement checkPstmt = null;
        ResultSet checkRs = null;
        PreparedStatement updatePstmt = null;

        try {
            // 1. 更新答题详情
            String updateDetailSql = "UPDATE answer_details SET score = ?, grader_comment = ?, grader_id = ?, is_correct = ? WHERE detail_id = ?";
            detailPstmt = conn.prepareStatement(updateDetailSql);
            detailPstmt.setDouble(1, score);
            detailPstmt.setString(2, comment);
            detailPstmt.setInt(3, graderId);
            detailPstmt.setBoolean(4, score > 0);
            detailPstmt.setInt(5, detailId);
            detailPstmt.executeUpdate();

            // 2. 计算主观题总分
            String sumSql = "SELECT SUM(ad.score) AS subjective_total FROM answer_details ad JOIN questions q ON ad.question_id = q.id WHERE ad.sheet_id = ? AND q.type = 'ESSAY'";
            sumPstmt = conn.prepareStatement(sumSql);
            sumPstmt.setInt(1, sheetId);
            sumRs = sumPstmt.executeQuery();
            double subjectiveScore = 0;
            if (sumRs.next()) {
                subjectiveScore = sumRs.getDouble("subjective_total");
            }

            // 3. 获取客观题得分
            String getObjSql = "SELECT objective_score FROM answer_sheets WHERE sheet_id = ?";
            objPstmt = conn.prepareStatement(getObjSql);
            objPstmt.setInt(1, sheetId);
            objRs = objPstmt.executeQuery();
            double objectiveScore = 0;
            if (objRs.next()) {
                objectiveScore = objRs.getDouble("objective_score");
            }

            // 计算总分
            double totalScore = objectiveScore + subjectiveScore;

            // 4. 检查是否所有主观题已批阅
            String checkPendingSql = "SELECT COUNT(*) FROM answer_details ad " +
                    "JOIN questions q ON ad.question_id = q.id " +
                    "WHERE ad.sheet_id = ? AND q.type = 'ESSAY' AND ad.score = 0";
            checkPstmt = conn.prepareStatement(checkPendingSql);
            checkPstmt.setInt(1, sheetId);
            checkRs = checkPstmt.executeQuery();
            checkRs.next();
            int pendingCount = checkRs.getInt(1);

            // 状态判断
            String sheetStatus = (pendingCount > 0) ? "SUBMITTED" : "GRADED";

            // 5. 更新答卷
            String updateSheetSql = "UPDATE answer_sheets SET subjective_score = ?, total_score = ?, status = ? WHERE sheet_id = ?";
            updatePstmt = conn.prepareStatement(updateSheetSql);
            updatePstmt.setDouble(1, subjectiveScore);
            updatePstmt.setDouble(2, totalScore);
            updatePstmt.setString(3, sheetStatus);
            updatePstmt.setInt(4, sheetId);
            updatePstmt.executeUpdate();

            conn.commit();

            // 6. 返回总分信息
            String resultMessage = "该题批阅完成！";
            if (pendingCount == 0) {
                resultMessage += String.format("\n该答卷批阅完成！总分：客观题 %.0f分 + 主观题 %.0f分 = %.0f分",
                        objectiveScore, subjectiveScore, totalScore);
            }
            return resultMessage;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            DBUtil.close(null, detailPstmt, null);
            DBUtil.close(null, sumPstmt, sumRs);
            DBUtil.close(null, objPstmt, objRs);
            DBUtil.close(null, checkPstmt, checkRs);
            DBUtil.close(conn, updatePstmt, null);
        }
    }
}