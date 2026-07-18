package com.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.db.DBUtil;
import com.entity.ExamPaper;
import com.entity.PaperQuestion;
import com.entity.Question;

public class PaperService {
    // 创建试卷并返回自增的paper_id
    public int createPaper(ExamPaper paper) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int paperId = -1;
        try {
            conn = DBUtil.getConnection();

            String sql = "INSERT INTO exam_papers(paper_id, paper_name, course, duration, total_score, creator_id) " +
                    "VALUES(exam_papers_seq.NEXTVAL, ?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(sql, new String[] { "paper_id" });

            pstmt.setString(1, paper.getPaperName());
            pstmt.setString(2, paper.getCourse());
            pstmt.setInt(3, paper.getDuration());
            pstmt.setInt(4, paper.getTotalScore());
            pstmt.setInt(5, paper.getCreatorId());

            pstmt.executeUpdate();

            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                paperId = rs.getInt(1);
            }

            if (paperId == -1) {
                String seqSql = "SELECT exam_papers_seq.CURRVAL FROM DUAL";
                PreparedStatement seqStmt = conn.prepareStatement(seqSql);
                ResultSet seqRs = seqStmt.executeQuery();
                if (seqRs.next()) {
                    paperId = seqRs.getInt(1);
                }
                seqRs.close();
                seqStmt.close();
            }
        } finally {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
            if (conn != null)
                conn.close();
        }
        return paperId;
    }

    // 自动组卷：增加难度和知识点参数
    public void autoGeneratePaper(int paperId, String type, int count, int difficulty, String knowledgePoint)
            throws SQLException {
        Connection conn = DBUtil.getConnection();
        try {
            System.out.println("开始自动组卷...");
            // 使用 Oracle 的随机查询配合 ROWNUM
            String sql = "SELECT * FROM (SELECT id FROM questions WHERE type = ? AND difficulty = ? " +
                    "AND knowledge_point LIKE ? ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= ?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, type);
            pstmt.setInt(2, difficulty);
            pstmt.setString(3, "%" + knowledgePoint + "%");
            pstmt.setInt(4, count);

            ResultSet rs = pstmt.executeQuery();
            int order = 1;
            int foundCount = 0;
            while (rs.next()) {
                foundCount++;
                int questionId = rs.getInt("id");
                // 插入关联表
                String linkSql = "INSERT INTO paper_question(id, paper_id, question_id, question_order) " +
                        "VALUES(paper_question_seq.NEXTVAL, ?, ?, ?)";
                PreparedStatement linkPstmt = conn.prepareStatement(linkSql);
                linkPstmt.setInt(1, paperId);
                linkPstmt.setInt(2, questionId);
                linkPstmt.setInt(3, order++);
                linkPstmt.executeUpdate();
                linkPstmt.close();
            }
            System.out.println("自动组卷完成，共匹配并添加题目: " + foundCount);
            DBUtil.close(conn, pstmt, rs);
        } catch (SQLException e) {
            System.err.println("自动组卷数据库操作异常: " + e.getMessage());
            throw e;
        }
    }

    // 添加题目
    public int addQuestion(Question question) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int questionId = -1;
        try {
            conn = DBUtil.getConnection();

            String sql = "INSERT INTO questions(id, type, content, options, answer, knowledge_point, difficulty, score) "
                    +
                    "VALUES(questions_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)";

            String optionsStr = question.getOptions() != null ? String.join("|", question.getOptions()) : null;

            pstmt = conn.prepareStatement(sql, new String[] { "id" });

            pstmt.setString(1, question.getType());
            pstmt.setString(2, question.getContent());
            pstmt.setString(3, optionsStr);
            pstmt.setString(4, question.getAnswer());
            pstmt.setString(5, question.getKnowledgePoint());
            pstmt.setInt(6, question.getDifficulty());
            pstmt.setDouble(7, question.getScore());

            pstmt.executeUpdate();

            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                questionId = rs.getInt(1);
            }
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return questionId;
    }

    // 将题目添加到试卷
    public void addQuestionToPaper(int paperId, int questionId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();

            String orderSql = "SELECT NVL(MAX(question_order), 0) + 1 FROM paper_question WHERE paper_id = ?";
            pstmt = conn.prepareStatement(orderSql);
            pstmt.setInt(1, paperId);
            rs = pstmt.executeQuery();
            int order = 1;
            if (rs.next()) {
                order = rs.getInt(1);
            }
            rs.close();
            pstmt.close();

            String linkSql = "INSERT INTO paper_question(id, paper_id, question_id, question_order) " +
                    "VALUES(paper_question_seq.NEXTVAL, ?, ?, ?)";
            pstmt = conn.prepareStatement(linkSql);
            pstmt.setInt(1, paperId);
            pstmt.setInt(2, questionId);
            pstmt.setInt(3, order);
            pstmt.executeUpdate();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
    }

    // 发布试卷
    public void publishPaper(int paperId) throws SQLException {
        Connection conn = DBUtil.getConnection();
        String sql = "UPDATE exam_papers SET status = 'PUBLISHED' WHERE paper_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, paperId);
        pstmt.executeUpdate();
        DBUtil.close(conn, pstmt, null);
    }

    // 根据ID获取试卷
    public ExamPaper getPaperById(int paperId) throws SQLException {
        ExamPaper paper = null;
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT * FROM exam_papers WHERE paper_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, paperId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            paper = new ExamPaper();
            paper.setPaperId(rs.getInt("paper_id"));
            paper.setPaperName(rs.getString("paper_name"));
            paper.setCourse(rs.getString("course"));
            paper.setDuration(rs.getInt("duration"));
            paper.setTotalScore(rs.getInt("total_score"));
            paper.setStatus(rs.getString("status"));
        }
        DBUtil.close(conn, pstmt, rs);
        return paper;
    }

    // 获取用户可用试卷列表（已发布且用户未完成）
    public List<ExamPaper> getAvailablePapers(int userId) throws SQLException {
        List<ExamPaper> papers = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT ep.* FROM exam_papers ep " +
                "WHERE ep.status = 'PUBLISHED' " +
                "AND ep.paper_id NOT IN (" +
                "  SELECT asheet.paper_id FROM answer_sheets asheet WHERE asheet.user_id = ? AND asheet.status = 'GRADED'"
                +
                ")";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            ExamPaper paper = new ExamPaper();
            paper.setPaperId(rs.getInt("paper_id"));
            paper.setPaperName(rs.getString("paper_name"));
            paper.setCourse(rs.getString("course"));
            paper.setDuration(rs.getInt("duration"));
            paper.setTotalScore(rs.getInt("total_score"));
            paper.setStatus(rs.getString("status"));
            papers.add(paper);
        }
        DBUtil.close(conn, pstmt, rs);
        return papers;
    }

    // 获取已发布试卷列表
    public List<ExamPaper> getPublishedPapers() throws SQLException {
        List<ExamPaper> papers = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT * FROM exam_papers WHERE status = 'PUBLISHED'";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            ExamPaper paper = new ExamPaper();
            paper.setPaperId(rs.getInt("paper_id"));
            paper.setPaperName(rs.getString("paper_name"));
            paper.setCourse(rs.getString("course"));
            paper.setDuration(rs.getInt("duration"));
            paper.setTotalScore(rs.getInt("total_score"));
            papers.add(paper);
        }
        DBUtil.close(conn, pstmt, rs);
        return papers;
    }

    // 获取试卷题目列表
    public List<PaperQuestion> getPaperQuestions(int paperId) throws SQLException {
        List<PaperQuestion> questions = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        String sql = "SELECT pq.*, q.id as q_id, q.type, q.content, q.options, q.answer, " +
                "q.knowledge_point, q.difficulty, q.score " +
                "FROM paper_question pq " +
                "JOIN questions q ON pq.question_id = q.id " +
                "WHERE pq.paper_id = ? ORDER BY pq.question_order";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, paperId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            PaperQuestion pq = new PaperQuestion();
            pq.setId(rs.getInt("id"));
            pq.setPaperId(rs.getInt("paper_id"));
            pq.setQuestionId(rs.getInt("question_id"));
            pq.setQuestionOrder(rs.getInt("question_order"));

            Question q = new Question();
            q.setId(rs.getInt("question_id"));
            q.setType(rs.getString("type"));
            q.setContent(rs.getString("content"));
            q.setOptions(rs.getString("options") != null ? rs.getString("options").split("\\|") : null);
            q.setAnswer(rs.getString("answer"));
            q.setKnowledgePoint(rs.getString("knowledge_point"));
            q.setDifficulty(rs.getInt("difficulty"));
            q.setScore(rs.getInt("score"));
            pq.setQuestion(q);

            questions.add(pq);
        }
        DBUtil.close(conn, pstmt, rs);
        return questions;
    }
}