package com.server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.db.DBUtil;
import com.entity.User;
import com.entity.Question;
import com.entity.ExamPaper;
import com.entity.PaperQuestion;
import com.entity.AnswerSheet;
import com.entity.AnswerDetail;
import com.service.PaperService;
import com.service.AnswerService;
import com.service.GradingService;

public class ExamServer {
    private static final int PORT = 8888;
    // UDP Socket 替换 TCP ServerSocket
    private DatagramSocket socket;
    private PaperService paperService = new PaperService();
    private AnswerService answerService = new AnswerService();
    private GradingService gradingService = new GradingService();

    public static void main(String[] args) {
        new ExamServer().start();
        System.out.println("系统默认编码: " + System.getProperty("file.encoding"));
        System.out.println("系统默认字符集: " + java.nio.charset.Charset.defaultCharset());
    }

    public void start() {
        try {
            // 创建 DatagramSocket 监听指定端口
            socket = new DatagramSocket(PORT);
            System.out.println("UDP 服务器启动，端口：" + PORT);

            while (true) {
                // 缓冲区大小设为 UDP 最大包大小
                byte[] receiveBuffer = new byte[65535];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket); // 阻塞等待接收客户端请求

                // 为每个请求开启一个新线程处理
                new Thread(() -> {
                    try {
                        // 1. 反序列化请求对象
                        ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData(), 0,
                                receivePacket.getLength());
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        Object request = ois.readObject();
                        ois.close();
                        bis.close();

                        // 2. 处理请求并生成响应
                        Object response = handleRequest(request);

                        // 3. 序列化响应对象
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(response);
                        oos.flush();
                        byte[] data = bos.toByteArray();
                        oos.close();
                        bos.close();

                        // 4. 发送响应 DatagramPacket
                        InetAddress clientAddress = receivePacket.getAddress();
                        int clientPort = receivePacket.getPort();
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, clientAddress, clientPort);
                        socket.send(sendPacket);

                    } catch (Exception e) {
                        e.printStackTrace();
                        // 发生异常时，尝试给客户端发送一个错误响应
                        sendErrorResponse(receivePacket.getAddress(), receivePacket.getPort(),
                                "服务器处理请求时发生错误: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    // 发送错误响应的辅助方法
    private void sendErrorResponse(InetAddress address, int port, String message) {
        try {
            Object[] errorResponse = new Object[] { message, null, null, null };
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(errorResponse);
            oos.flush();
            byte[] data = bos.toByteArray();
            oos.close();
            bos.close();

            DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
            socket.send(sendPacket);
        } catch (Exception e) {
            System.err.println("发送错误响应失败: " + e.getMessage());
        }
    }

    // 处理核心请求的逻辑
    private Object handleRequest(Object request) throws Exception {
        // 登录请求
        if (request instanceof User) {
            return handleLogin((User) request);
        }

        // 所有其他命令请求
        else if (request instanceof Object[]) {
            Object[] requestArray = (Object[]) request;
            if (requestArray.length < 2 || !(requestArray[0] instanceof User) || !(requestArray[1] instanceof String)) {
                return new Object[] { "无效的请求格式", null };
            }

            User currentUser = (User) requestArray[0];
            String command = (String) requestArray[1];

            switch (command) {
                // ============== 管理员功能 ==============
                case "ADMIN_CREATE_PAPER":
                    return handleAdminCreatePaper(currentUser, requestArray);
                case "ADMIN_AUTO_GENERATE_PAPER":
                    return handleAdminAutoGeneratePaper(currentUser, requestArray);
                case "ADMIN_PUBLISH_PAPER":
                    return handleAdminPublishPaper(currentUser, requestArray);
                case "ADMIN_ADD_QUESTION":
                    return handleAdminAddQuestion(currentUser, requestArray);
                case "ADMIN_EXIT":
                    return new Object[] { "退出成功", null };
                case "ADMIN_INVALID_CHOICE":
                    return new Object[] { "无效选择，请重新输入" + getAdminMenu(), null };
                case "ADMIN_CHECK_MENU":
                    return new Object[] { getAdminMenu(), null };

                // ============== 学生功能 ==============
                case "STUDENT_VIEW_PAPERS":
                    return handleStudentViewPapers(currentUser);
                case "STUDENT_START_EXAM":
                    return handleStudentStartExam(currentUser, requestArray);
                case "STUDENT_SAVE_ANSWER":
                    return handleStudentSaveAnswer(currentUser, requestArray);
                case "STUDENT_SUBMIT_EXAM":
                    return handleStudentSubmitExam(currentUser, requestArray);
                case "STUDENT_VIEW_SCORE":
                    return handleStudentViewScore(currentUser);
                case "STUDENT_EXIT":
                    return new Object[] { "退出成功", null };

                // ============== 阅卷人功能 ==============
                case "GRADER_VIEW_PENDING":
                    return handleGraderViewPending();
                case "GRADER_GET_SUBJECTIVES":
                    return handleGraderGetSubjectives(requestArray);
                case "GRADER_GRADE_SUBJECTIVE":
                    return handleGraderGradeSubjective(currentUser, requestArray);
                case "GRADER_EXIT":
                    return new Object[] { "退出成功", null };
                case "GRADER_CHECK_MENU":
                    return new Object[] { getGraderMenu(), null };

                default:
                    return new Object[] { "服务器无法识别的命令: " + command, null };
            }
        }

        return new Object[] { "无法处理的请求类型", null };
    }

    // 登录处理
    private Object[] handleLogin(User user) throws SQLException {
        User loggedInUser = null;
        String role = null;
        String menu = null;

        // 模拟数据库查询和验证
        if ("admin".equals(user.getUsername()) && "123".equals(user.getPassword())) {
            loggedInUser = new User(1, "admin", "123", "ADMIN");
            role = "ADMIN";
            menu = getAdminMenu();
        } else if ("student".equals(user.getUsername()) && "123".equals(user.getPassword())) {
            loggedInUser = new User(2, "student", "123", "STUDENT");
            role = "STUDENT";
            menu = getStudentMenu();
        } else if ("grader".equals(user.getUsername()) && "123".equals(user.getPassword())) {
            loggedInUser = new User(3, "grader", "123", "GRADER");
            role = "GRADER";
            menu = getGraderMenu();
        }

        if (loggedInUser != null) {
            return new Object[] { "登录成功，欢迎您，" + loggedInUser.getUsername() + "！", role, menu, loggedInUser };
        } else {
            return new Object[] { "登录失败：用户名或密码错误。", null, null, null };
        }
    }

    // 管理员菜单
    private String getAdminMenu() {
        return "管理员功能列表：\n1-创建新试卷 2-自动组卷 3-发布试卷 4-添加题目 5-退出";
    }

    // 学生菜单
    private String getStudentMenu() {
        return "学生功能列表：\n1-查看待考试卷 2-开始考试/继续答题 3-查看成绩 4-退出";
    }

    // 阅卷人菜单
    private String getGraderMenu() {
        return "阅卷人功能列表：\n1-查看待批阅试卷 2-批阅主观题 3-退出";
    }

    // 管理员：创建试卷
    private Object[] handleAdminCreatePaper(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 6)
            return new Object[] { "参数不足" + getAdminMenu(), null };

        String paperName = (String) requestArray[2];
        String course = (String) requestArray[3];
        int duration = (Integer) requestArray[4];
        int totalScore = (Integer) requestArray[5];

        ExamPaper paper = new ExamPaper();
        paper.setPaperName(paperName);
        paper.setCourse(course);
        paper.setDuration(duration);
        paper.setTotalScore(totalScore);
        paper.setCreatorId(currentUser.getId());

        int paperId = paperService.createPaper(paper);

        return new Object[] { "试卷 ID: " + paperId + " 创建成功。\n" + getAdminMenu(), null };
    }

    private Object[] handleAdminAutoGeneratePaper(User currentUser, Object[] requestArray) throws Exception {
        try {
            // 解析 5 个业务参数 (paperId, type, count, difficulty, knowledgePoint)
            int paperId = (Integer) requestArray[2];
            String type = (String) requestArray[3];
            int count = (Integer) requestArray[4];
            int difficulty = (Integer) requestArray[5];
            String kp = (String) requestArray[6];

            paperService.autoGeneratePaper(paperId, type, count, difficulty, kp);
            return new Object[] { "自动组卷成功！已添加 " + count + " 道 " + type + " 题目到试卷 " + paperId, null };
        } catch (Exception e) {
            return new Object[] { "自动组卷失败: " + e.getMessage(), null };
        }
    }

    // 管理员：发布试卷
    private Object[] handleAdminPublishPaper(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 3)
            return new Object[] { "参数不足" + getAdminMenu(), null };

        int paperId = (Integer) requestArray[2];
        paperService.publishPaper(paperId);

        return new Object[] { "试卷 ID: " + paperId + " 发布成功。\n" + getAdminMenu(), null };
    }

    // 管理员：添加题目
    private Object[] handleAdminAddQuestion(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 10)
            return new Object[] { "参数不足" + getAdminMenu(), null };

        int paperId = (Integer) requestArray[2];
        String type = (String) requestArray[3];
        String content = (String) requestArray[4];
        String optionsStr = (String) requestArray[5];
        String answer = (String) requestArray[6];
        String knowledgePoint = (String) requestArray[7];
        int difficulty = (Integer) requestArray[8];
        int score = (Integer) requestArray[9];

        Question question = new Question();
        question.setType(type);
        question.setContent(content);
        if (optionsStr != null && !optionsStr.isEmpty()) {
            // 选项以 | 分割
            question.setOptions(optionsStr.split("\\|"));
        }
        question.setAnswer(answer);
        question.setKnowledgePoint(knowledgePoint);
        question.setDifficulty(difficulty);
        question.setScore(score);

        // 1. 添加题目
        int questionId = paperService.addQuestion(question); // 修复 Error 2

        // 2. 题目添加到试卷
        paperService.addQuestionToPaper(paperId, questionId); // 修复 Error 3

        return new Object[] { "题目 ID: " + questionId + " 添加成功到试卷 ID: " + paperId + "。\n" + getAdminMenu(), null };
    }

    // 学生：查看待考试卷
    private Object[] handleStudentViewPapers(User currentUser) throws Exception {
        // 调用 PaperService 中新增的方法
        List<ExamPaper> papers = paperService.getAvailablePapers(currentUser.getId());

        StringBuilder sb = new StringBuilder("待考和已考完试卷列表：\n");
        if (papers.isEmpty()) {
            sb.append("暂无可用试卷。");
        } else {
            for (ExamPaper paper : papers) {
                sb.append(String.format("ID: %d | 名称: %s | 课程: %s | 状态: %s\n",
                        paper.getPaperId(),
                        paper.getPaperName(),
                        paper.getCourse(),
                        paper.getStatus()));
            }
        }
        return new Object[] { sb.toString() + "\n" + getStudentMenu(), null };
    }

    // 学生：开始考试/继续答题
    private Object[] handleStudentStartExam(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 3)
            return new Object[] { "参数不足" + getStudentMenu(), null };

        int paperId;
        try {
            paperId = Integer.parseInt((String) requestArray[2]);
        } catch (NumberFormatException e) {
            return new Object[] { "试卷ID格式错误。" + getStudentMenu(), null };
        }

        // 调用 PaperService 中新增的方法
        ExamPaper selectedPaper = paperService.getPaperById(paperId); // 修复 Error 5
        if (selectedPaper == null) {
            return new Object[] { "试卷 ID: " + paperId + " 不存在。" + getStudentMenu(), null };
        }

        if (!"PUBLISHED".equals(selectedPaper.getStatus())) {
            return new Object[] { "试卷 ID: " + paperId + " 尚未发布，无法开始考试。" + getStudentMenu(), null };
        }

        // 创建或获取答卷记录
        int sheetId = answerService.startExam(currentUser.getId(), paperId);
        // 获取试卷题目列表
        List<PaperQuestion> paperQuestions = paperService.getPaperQuestions(paperId);

        if (paperQuestions.isEmpty()) {
            return new Object[] { "试卷中没有题目，考试提前结束。\n" + getStudentMenu(), null };
        }

        String message = String.format("考试开始：%s，时长：%d分钟，答题记录ID：%d",
                selectedPaper.getPaperName(),
                selectedPaper.getDuration(),
                sheetId);

        // 返回试卷对象、题目列表和答卷ID给客户端
        return new Object[] { message, selectedPaper, paperQuestions, sheetId };
    }

    // 学生：保存答案
    private Object[] handleStudentSaveAnswer(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 5)
            return new Object[] { "参数不足", null };

        int sheetId = (Integer) requestArray[2];
        int questionId = (Integer) requestArray[3];
        String userAnswer = (String) requestArray[4];

        answerService.saveAnswer(sheetId, questionId, userAnswer);

        return new Object[] { "题目 " + questionId + " 的答案保存成功。", null };
    }

    // 学生：提交试卷
    private Object[] handleStudentSubmitExam(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 3)
            return new Object[] { "参数不足" + getStudentMenu(), null };

        int sheetId = (Integer) requestArray[2];

        // 1. 客观题判分
        answerService.gradeObjectiveQuestions(sheetId);

        // 2. 提交答卷，更新状态和总分
        AnswerSheet sheet = answerService.submitExam(sheetId);

        String message;
        if ("GRADED".equals(sheet.getStatus())) {
            message = String.format("试卷提交成功，已自动判分完毕。您的客观题总分是 %.0f，最终总分是 %.0f。",
                    sheet.getObjectiveScore(), sheet.getTotalScore());
        } else {
            message = String.format("试卷提交成功，客观题已判分（%.0f分），主观题需等待阅卷人批阅。", sheet.getObjectiveScore());
        }

        return new Object[] { message + "\n" + getStudentMenu(), null };
    }

    // 学生：查看成绩
    private Object[] handleStudentViewScore(User currentUser) throws Exception {
        // 调用 AnswerService 中新增的方法
        AnswerSheet sheet = answerService.getLatestGradedSheet(currentUser.getId()); // 修复 Error 8
        if (sheet == null) {
            return new Object[] { "暂无已批阅成绩可查。\n" + getStudentMenu(), null };
        }

        String message = String.format("最新成绩：试卷ID %d | 客观题分数 %.0f | 主观题分数 %.0f | 总分 %.0f",
                sheet.getPaperId(),
                sheet.getObjectiveScore(),
                sheet.getSubjectiveScore(),
                sheet.getTotalScore());

        return new Object[] { message + "\n" + getStudentMenu(), null };
    }

    // 阅卷人：查看待批阅试卷
    private Object[] handleGraderViewPending() throws Exception {
        List<AnswerSheet> sheets = gradingService.getPendingSheets();
        StringBuilder sb = new StringBuilder("待批阅试卷共 " + sheets.size() + " 份：\n");
        if (sheets.isEmpty()) {
            sb.append("暂无需要批阅的答卷。");
        } else {
            for (AnswerSheet sheet : sheets) {
                sb.append(String.format("答卷ID: %d | 试卷ID: %d | 用户ID: %d | 客观题得分: %.0f\n",
                        sheet.getSheetId(),
                        sheet.getPaperId(),
                        sheet.getUserId(),
                        sheet.getObjectiveScore()));
            }
        }
        return new Object[] { sb.toString() + "\n" + getGraderMenu(), null };
    }

    // 阅卷人：获取主观题列表
    private Object[] handleGraderGetSubjectives(Object[] requestArray) throws Exception {
        if (requestArray.length < 3 || !(requestArray[2] instanceof String)) {
            return new Object[] { "参数不足或格式错误", null };
        }
        int sheetId;
        try {
            sheetId = Integer.parseInt((String) requestArray[2]);
        } catch (NumberFormatException e) {
            return new Object[] { "答卷ID格式错误。", null };
        }

        List<AnswerDetail> details = gradingService.getSubjectiveQuestions(sheetId);

        if (details.isEmpty()) {
            return new Object[] { "答卷 ID: " + sheetId + " 中没有待批阅主观题。", null };
        }

        StringBuilder sb = new StringBuilder("答卷 ID: " + sheetId + " 待批阅主观题列表：\n");
        for (AnswerDetail detail : details) {
            Question q = detail.getQuestion();
            sb.append(String.format("题目ID: %d | 题干: %s | 用户答案: %s | 满分: %d\n",
                    q.getId(),
                    q.getContent().substring(0, Math.min(q.getContent().length(), 20)) + "...",
                    detail.getUserAnswer().substring(0, Math.min(detail.getUserAnswer().length(), 20)) + "...",
                    q.getScore()));
        }

        // 返回主观题列表消息和详情对象列表
        return new Object[] { sb.toString(), details };
    }

    // 阅卷人：批阅单个主观题
    private Object[] handleGraderGradeSubjective(User currentUser, Object[] requestArray) throws Exception {
        if (requestArray.length < 6)
            return new Object[] { "参数不足", null };

        int detailId = (Integer) requestArray[2];
        double score;
        try {
            score = Double.parseDouble((String) requestArray[3]);
        } catch (NumberFormatException e) {
            return new Object[] { "得分格式错误，请输入数字", null };
        }
        String comment = (String) requestArray[4];
        int sheetId = (Integer) requestArray[5];

        String resultMessage = gradingService.gradeSubjective(detailId, score, comment, currentUser.getId(), sheetId);

        return new Object[] { resultMessage, null };
    }
}