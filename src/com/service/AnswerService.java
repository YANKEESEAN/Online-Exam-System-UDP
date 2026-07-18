package com.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import com.db.DBUtil;
import com.entity.AnswerDetail;
import com.entity.AnswerSheet;
import com.entity.Question;
import com.entity.User;

public class AnswerService {
    // 开始考试（创建答题记录并返回自增的sheet_id）
    public int startExam(int userId, int paperId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int sheetId = -1;

        try {
            conn = DBUtil.getConnection();

            String getSeqSql = "SELECT answer_sheets_seq.NEXTVAL FROM DUAL";
            pstmt = conn.prepareStatement(getSeqSql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                sheetId = rs.getInt(1);
            }

            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();

            String sql = "INSERT INTO answer_sheets(sheet_id, user_id, paper_id, status) " +
                    "VALUES(?, ?, ?, 'DOING')";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sheetId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, paperId);
            pstmt.executeUpdate();

        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return sheetId;
    }

    // 保存答案（自动保存）
    public void saveAnswer(int sheetId, int questionId, String userAnswer) throws SQLException {
        Connection conn = DBUtil.getConnection();
        PreparedStatement checkPstmt = null;
        ResultSet rs = null;
        PreparedStatement updatePstmt = null;
        PreparedStatement insertPstmt = null;
        PreparedStatement seqStmt = null;
        ResultSet seqRs = null;

        try {
            String checkSql = "SELECT detail_id FROM answer_details WHERE sheet_id = ? AND question_id = ?";
            checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setInt(1, sheetId);
            checkPstmt.setInt(2, questionId);
            rs = checkPstmt.executeQuery();

            if (rs.next()) {
                // 更新答案
                String updateSql = "UPDATE answer_details SET user_answer = ? WHERE detail_id = ?";
                updatePstmt = conn.prepareStatement(updateSql);
                updatePstmt.setString(1, userAnswer);
                updatePstmt.setInt(2, rs.getInt("detail_id"));
                updatePstmt.executeUpdate();
            } else {
                // 新增答案
                String getSeqSql = "SELECT answer_details_seq.NEXTVAL FROM DUAL";
                seqStmt = conn.prepareStatement(getSeqSql);
                seqRs = seqStmt.executeQuery();
                int detailId = -1;
                if (seqRs.next()) {
                    detailId = seqRs.getInt(1);
                }

                String insertSql = "INSERT INTO answer_details(detail_id, sheet_id, question_id, user_answer) " +
                        "VALUES(?, ?, ?, ?)";
                insertPstmt = conn.prepareStatement(insertSql);
                insertPstmt.setInt(1, detailId);
                insertPstmt.setInt(2, sheetId);
                insertPstmt.setInt(3, questionId);
                insertPstmt.setString(4, userAnswer);
                insertPstmt.executeUpdate();
            }
        } finally {
            DBUtil.close(null, seqStmt, seqRs);
            DBUtil.close(null, checkPstmt, rs);
            DBUtil.close(null, updatePstmt, null);
            DBUtil.close(conn, insertPstmt, null);
        }
    }

    // 客观题判分（从原 submitPaper 拆分出来）
    public void gradeObjectiveQuestions(int sheetId) throws SQLException {
        Connection conn = DBUtil.getConnection();
        conn.setAutoCommit(false);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PreparedStatement scorePstmt = null;

        try {
            // 1. 客观题判分准备
            String sql = "SELECT " +
                    "ad.detail_id, " +
                    "ad.question_id, " +
                    "ad.user_answer, " +
                    "q.type, " +
                    "q.answer, " +
                    "q.score " +
                    "FROM answer_details ad " +
                    "JOIN questions q ON ad.question_id = q.id " +
                    "WHERE ad.sheet_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sheetId);
            rs = pstmt.executeQuery();

            double objectiveScore = 0;
            while (rs.next()) {
                int questionId = rs.getInt("question_id");
                String type = rs.getString("type");
                String correctAnswer = rs.getString("answer");
                String userAnswer = rs.getString("user_answer");
                int score = rs.getInt("score");
                int detailId = rs.getInt("detail_id");

                if ("SINGLE".equals(type) || "MULTIPLE".equals(type) || "FILL".equals(type)) {
                    boolean isCorrect = false;
                    double newScore = 0.0;

                    if (userAnswer != null && !userAnswer.trim().isEmpty() && correctAnswer != null
                            && !correctAnswer.trim().isEmpty()) {
                        if ("SINGLE".equals(type) || "FILL".equals(type)) {
                            isCorrect = correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
                        } else if ("MULTIPLE".equals(type)) {
                            String userClean = userAnswer.replaceAll("\\s+", "").toUpperCase();
                            String correctClean = correctAnswer.replaceAll("\\s+", "").toUpperCase();

                            String[] userArr = userClean.isEmpty() ? new String[0] : userClean.split(",");
                            String[] correctArr = correctClean.isEmpty() ? new String[0] : correctClean.split(",");

                            java.util.Arrays.sort(userArr);
                            java.util.Arrays.sort(correctArr);

                            isCorrect = java.util.Arrays.equals(userArr, correctArr);
                        }
                    }

                    newScore = isCorrect ? (double) score : 0.0;

                    // 更新答题详情
                    String updateDetailSql = "UPDATE answer_details SET is_correct = ?, score = ? WHERE detail_id = ?";
                    PreparedStatement detailPstmt = conn.prepareStatement(updateDetailSql);
                    detailPstmt.setBoolean(1, isCorrect);
                    detailPstmt.setDouble(2, newScore);
                    detailPstmt.setInt(3, detailId);
                    detailPstmt.executeUpdate();
                    detailPstmt.close();

                    objectiveScore += newScore;
                }
            }

            // 2. 更新答卷客观分
            String updateScoreSql = "UPDATE answer_sheets SET objective_score = ?, total_score = ? WHERE sheet_id = ?";
            scorePstmt = conn.prepareStatement(updateScoreSql);
            scorePstmt.setDouble(1, objectiveScore);
            scorePstmt.setDouble(2, objectiveScore); // 总分初始化为客观题得分
            scorePstmt.setInt(3, sheetId);
            scorePstmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            DBUtil.close(null, pstmt, rs);
            DBUtil.close(conn, scorePstmt, null);
        }
    }

    // 提交试卷（更新状态和总分）
    public AnswerSheet submitExam(int sheetId) throws SQLException {
        Connection conn = DBUtil.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement sheetPstmt = null;
        PreparedStatement checkPstmt = null;
        ResultSet checkRs = null;
        AnswerSheet sheet = null;

        try {
            // 1. 获取答卷分数和主观题未批阅数量
            String checkPendingSql = "SELECT asheet.objective_score, asheet.subjective_score, " +
                    "SUM(CASE WHEN q.type = 'ESSAY' AND ad.score = 0 THEN 1 ELSE 0 END) AS pending_count " +
                    "FROM answer_sheets asheet " +
                    "LEFT JOIN answer_details ad ON asheet.sheet_id = ad.sheet_id " +
                    "LEFT JOIN questions q ON ad.question_id = q.id " +
                    "WHERE asheet.sheet_id = ? GROUP BY asheet.objective_score, asheet.subjective_score";

            checkPstmt = conn.prepareStatement(checkPendingSql);
            checkPstmt.setInt(1, sheetId);
            checkRs = checkPstmt.executeQuery();

            double objScore = 0;
            double subjScore = 0;
            int pendingCount = 0;

            if (checkRs.next()) {
                objScore = checkRs.getDouble("objective_score");
                subjScore = checkRs.getDouble("subjective_score");
                pendingCount = checkRs.getInt("pending_count");
            }

            // 2. 确定状态和总分
            String status = (pendingCount > 0) ? "SUBMITTED" : "GRADED";
            double totalScore = objScore + subjScore;

            // 3. 更新答卷状态、提交时间、总分
            String updateSheetSql = "UPDATE answer_sheets SET submit_time = ?, status = ?, total_score = ? WHERE sheet_id = ?";
            sheetPstmt = conn.prepareStatement(updateSheetSql);
            sheetPstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
            sheetPstmt.setString(2, status);
            sheetPstmt.setDouble(3, totalScore);
            sheetPstmt.setInt(4, sheetId);
            sheetPstmt.executeUpdate();

            conn.commit();

            sheet = new AnswerSheet();
            sheet.setSheetId(sheetId);
            sheet.setStatus(status);
            sheet.setObjectiveScore(objScore);
            sheet.setSubjectiveScore(subjScore);
            sheet.setTotalScore(totalScore);

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            DBUtil.close(null, sheetPstmt, null);
            DBUtil.close(null, checkPstmt, checkRs);
            DBUtil.close(conn, null, null);
        }
        return sheet;
    }

    // 获取最新已批阅答卷
    public AnswerSheet getLatestGradedSheet(int userId) throws SQLException {
        AnswerSheet sheet = null;
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT * FROM answer_sheets WHERE user_id = ? AND status = 'GRADED' " +
                "ORDER BY submit_time DESC";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            sheet = new AnswerSheet();
            sheet.setSheetId(rs.getInt("sheet_id"));
            sheet.setPaperId(rs.getInt("paper_id"));
            sheet.setObjectiveScore(rs.getDouble("objective_score"));
            sheet.setSubjectiveScore(rs.getDouble("subjective_score"));
            sheet.setTotalScore(rs.getDouble("total_score"));
        }
        DBUtil.close(conn, pstmt, rs);
        return sheet;
    }

    public double getObjectiveScore(int sheetId) {
        double score = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT objective_score FROM answer_sheets WHERE sheet_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sheetId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                score = rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return score;
    }
}