package com.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.List;
import com.entity.User;
import com.entity.Question;
import com.entity.ExamPaper;
import com.entity.PaperQuestion;
import com.entity.AnswerSheet;
import com.entity.AnswerDetail;

public class ExamClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8888;
    // UDP 相关的类替换 TCP 的 Socket 和 Stream
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private BufferedReader reader;
    private User currentUser;

    public static void main(String[] args) {
        new ExamClient().start();
    }

    // 构造方法
    public ExamClient() {
        try {
            this.reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            System.out.println("客户端输入使用UTF-8编码");
        } catch (Exception e) {
            System.err.println("编码设置失败: " + e.getMessage());
            this.reader = new BufferedReader(new InputStreamReader(System.in));
        }
    }

    // ================== UDP 通信核心方法 ==================
    private Object sendAndReceive(Object request) throws IOException, ClassNotFoundException {
        // 1. 序列化请求对象
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        byte[] data = bos.toByteArray();
        oos.close();
        bos.close();

        // 2. 准备并发送 DatagramPacket
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        socket.send(sendPacket);

        // 3. 接收响应
        byte[] receiveBuffer = new byte[65535]; // UDP最大包大小
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket); // 阻塞等待接收

        // 4. 反序列化响应对象
        ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object response = ois.readObject();
        ois.close();
        bis.close();

        return response;
    }
    // ======================================================

    public void start() {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_IP);
            socket.setSoTimeout(10000);

            login();

            Object[] loginResponse = (Object[]) sendAndReceive(currentUser);
            String loginResult = (String) loginResponse[0];
            System.out.println(loginResult);

            if (loginResult.contains("登录成功")) {
                this.currentUser = (User) loginResponse[3];
                String role = (String) loginResponse[1];
                String initialMenu = (String) loginResponse[2];

                if ("ADMIN".equals(role)) {
                    adminInteraction(initialMenu);
                } else if ("GRADER".equals(role)) {
                    graderInteraction(initialMenu);
                } else if ("STUDENT".equals(role)) {
                    studentInteraction(initialMenu);
                }
            }

        } catch (SocketTimeoutException e) {
            System.err.println("服务器响应超时，客户端关闭。");
        } catch (Exception e) {
            System.err.println("客户端异常: " + e.getMessage());
            // e.printStackTrace();
        } finally {
            close();
        }
    }

    private String readInput() throws IOException {
        return reader.readLine();
    }

    private void login() throws IOException {
        System.out.println("欢迎使用在线考试系统！");
        System.out.print("请输入用户名: ");
        String username = readInput();
        System.out.print("请输入密码: ");
        String password = readInput();

        currentUser = new User();
        currentUser.setUsername(username);
        currentUser.setPassword(password);
    }

    private void adminInteraction(String initialMenu) throws Exception {
        System.out.println("\n===== 管理员功能交互 =====");
        String serverMsg = initialMenu;

        while (true) {
            System.out.println("\n" + serverMsg);

            if (serverMsg.contains("退出成功")) {
                break;
            }

            boolean awaitingNextStep = serverMsg.contains("请输入试卷名称") ||
                    serverMsg.contains("请输入课程名称");

            if (serverMsg.contains("功能列表：") || serverMsg.contains("操作取消") || serverMsg.contains("成功！")
                    || awaitingNextStep) {
                System.out.print(">>> ");
                String choice = readInput().trim();

                if ("5".equals(choice)) { // 退出
                    Object[] response = (Object[]) sendAndReceive(new Object[] { currentUser, "ADMIN_EXIT" });
                    serverMsg = (String) response[0];
                    continue;
                }

                Object[] requestData = collectAdminInput(choice);
                if (requestData == null) {
                    serverMsg = "操作取消：输入不完整或格式错误";
                    continue;
                }

                Object[] response = (Object[]) sendAndReceive(requestData);
                serverMsg = (String) response[0];
            } else {
                Object[] response = (Object[]) sendAndReceive(new Object[] { currentUser, "ADMIN_CHECK_MENU" });
                serverMsg = (String) response[0];
            }
        }
    }

    private Object[] collectAdminInput(String choice) throws IOException {
        String command = "";
        Object[] data = null;

        System.out.println("请输入操作所需的参数...");

        switch (choice) {
            case "1": // 创建试卷
                command = "ADMIN_CREATE_PAPER";
                System.out.print("请输入试卷名称: ");
                String paperName = readInput().trim();
                System.out.print("请输入课程名称: ");
                String course = readInput().trim();
                System.out.print("请输入考试时长（分钟）: ");
                String durationStr = readInput().trim();
                System.out.print("请输入试卷总分: ");
                String totalScoreStr = readInput().trim();
                try {
                    data = new Object[] { paperName, course, Integer.parseInt(durationStr),
                            Integer.parseInt(totalScoreStr) };
                } catch (NumberFormatException e) {
                    return null;
                }
                break;
            case "2": // 自动组卷
                command = "ADMIN_AUTO_GENERATE_PAPER";

                System.out.print("请输入要自动组卷的试卷ID: ");
                int pid = Integer.parseInt(readInput());
                System.out.print("请输入要组卷的题目类型 (SINGLE, MULTIPLE, FILL, ESSAY): ");
                String type = readInput().toUpperCase();
                System.out.print("请输入题目数量: ");
                int cnt = Integer.parseInt(readInput());
                System.out.print("难度 (1-3): ");
                int diff = Integer.parseInt(readInput());
                System.out.print("知识点: ");
                String kp = readInput();
                return new Object[] { currentUser, "ADMIN_AUTO_GENERATE_PAPER", pid, type, cnt, diff, kp };
            case "3": // 发布试卷
                command = "ADMIN_PUBLISH_PAPER";
                System.out.print("请输入要发布试卷ID: ");
                String publishPaperIdStr = readInput().trim();
                try {
                    data = new Object[] { Integer.parseInt(publishPaperIdStr) };
                } catch (NumberFormatException e) {
                    return null;
                }
                break;
            case "4": // 添加题目
                command = "ADMIN_ADD_QUESTION";
                System.out.print("请输入试卷ID: ");
                String qPaperIdStr = readInput().trim();
                System.out.print("请输入题目类型 (SINGLE, MULTIPLE, ESSAY): ");
                String qType = readInput().trim().toUpperCase();
                System.out.print("请输入题干: ");
                String content = readInput().trim();
                String optionsStr = "";
                if ("SINGLE".equals(qType) || "MULTIPLE".equals(qType)) {
                    System.out.print("请输入选项（格式：A.xxx|B.xxx...）: ");
                    optionsStr = readInput().trim();
                }
                System.out.print("请输入答案: ");
                String answer = readInput().trim();
                System.out.print("请输入知识点: ");
                String knowledgePoint = readInput().trim();
                System.out.print("请输入难度（1-3）: ");
                String difficultyStr = readInput().trim();
                System.out.print("请输入分值: ");
                String scoreStr = readInput().trim();
                try {
                    data = new Object[] { Integer.parseInt(qPaperIdStr), qType, content, optionsStr, answer,
                            knowledgePoint, Integer.parseInt(difficultyStr), Integer.parseInt(scoreStr) };
                } catch (NumberFormatException e) {
                    return null;
                }
                break;
            default:
                command = "ADMIN_INVALID_CHOICE";
                data = new Object[] { choice };
                break;
        }

        Object[] request = new Object[2 + (data != null ? data.length : 0)];
        request[0] = currentUser;
        request[1] = command;
        if (data != null) {
            System.arraycopy(data, 0, request, 2, data.length);
        }
        return request;
    }

    private void studentInteraction(String initialMenu) throws Exception {
        System.out.println("\n===== 学生功能交互 =====");
        String serverMsg = initialMenu;

        while (true) {
            System.out.println("\n" + serverMsg);

            if (serverMsg.contains("退出成功")) {
                break;
            }

            System.out.print(">>> ");
            String choice = readInput().trim();

            Object[] response;

            if ("4".equals(choice)) { // 退出
                response = (Object[]) sendAndReceive(new Object[] { currentUser, "STUDENT_EXIT" });
                serverMsg = (String) response[0];
                continue;
            } else if ("1".equals(choice)) { // 查看待考试卷
                response = (Object[]) sendAndReceive(new Object[] { currentUser, "STUDENT_VIEW_PAPERS" });
                serverMsg = (String) response[0];
            } else if ("2".equals(choice)) { // 开始考试/继续答题
                System.out.print("请输入试卷ID: ");
                String paperIdStr = readInput().trim();

                // 1. 发送开始考试请求
                response = (Object[]) sendAndReceive(new Object[] { currentUser, "STUDENT_START_EXAM", paperIdStr });
                serverMsg = (String) response[0];

                if (response.length > 1 && response[1] != null && response[1] instanceof ExamPaper) {
                    // 接收到试卷信息，开始答题流程（每题都是一个 UDP 事务）
                    ExamPaper paper = (ExamPaper) response[1];
                    @SuppressWarnings("unchecked")
                    List<PaperQuestion> paperQuestions = (List<PaperQuestion>) response[2];
                    int sheetId = (Integer) response[3];

                    System.out.println("\n--- 开始考试: " + paper.getPaperName() + " ---");

                    // 答题逻辑：逐题进行，每次发送一个答案
                    for (int i = 0; i < paperQuestions.size(); i++) {
                        PaperQuestion pq = paperQuestions.get(i);
                        Question q = pq.getQuestion();

                        // 显示题目
                        StringBuilder questionDisplay = new StringBuilder();
                        questionDisplay.append(String.format("\n[%s] 第 %d 题 (%d分): %s\n", q.getType(), (i + 1),
                                q.getScore(), q.getContent()));
                        if (q.getOptions() != null) {
                            for (String option : q.getOptions()) {
                                questionDisplay.append(option.trim()).append(" ");
                            }
                            questionDisplay.append("\n");
                        }
                        System.out.println(questionDisplay.toString());

                        // 读取答案
                        System.out.print("请输入答案: ");
                        String userAnswer = readInput().trim();

                        // 发送答案（每次发送都是一个事务）
                        Object[] answerRequest = new Object[] { currentUser, "STUDENT_SAVE_ANSWER", sheetId, q.getId(),
                                userAnswer };
                        Object[] answerResponse = (Object[]) sendAndReceive(answerRequest);
                        System.out.println((String) answerResponse[0]); // 打印保存结果
                    }

                    // 考试结束，提交试卷
                    System.out.println("\n所有题目作答完毕，自动提交试卷...");
                    Object[] submitRequest = new Object[] { currentUser, "STUDENT_SUBMIT_EXAM", sheetId };
                    Object[] submitResponse = (Object[]) sendAndReceive(submitRequest);
                    serverMsg = (String) submitResponse[0];
                } else {
                    serverMsg = (String) response[0]; // 打印错误信息
                }
            } else if ("3".equals(choice)) { // 查看成绩
                response = (Object[]) sendAndReceive(new Object[] { currentUser, "STUDENT_VIEW_SCORE" });
                serverMsg = (String) response[0];
            } else {
                serverMsg = "无效选择，请重新输入";
            }
        }
    }

    private void graderInteraction(String initialMenu) throws Exception {
        System.out.println("\n===== 阅卷人功能交互 =====");
        String serverMsg = initialMenu;

        while (true) {
            System.out.println("\n" + serverMsg);

            if (serverMsg.contains("退出成功")) {
                break;
            }

            System.out.print(">>> ");
            String choice = readInput().trim();

            Object[] response;

            if ("3".equals(choice)) { // 退出
                response = (Object[]) sendAndReceive(new Object[] { currentUser, "GRADER_EXIT" });
                serverMsg = (String) response[0];
                continue;
            } else if ("1".equals(choice)) { // 查看待批阅试卷
                response = (Object[]) sendAndReceive(new Object[] { currentUser, "GRADER_VIEW_PENDING" });
                serverMsg = (String) response[0];
            } else if ("2".equals(choice)) { // 批阅主观题 (客户端控制流程)
                System.out.print("请输入要批阅的答卷ID: ");
                String sheetIdStr = readInput().trim();
                int sheetId;
                try {
                    sheetId = Integer.parseInt(sheetIdStr);
                } catch (NumberFormatException e) {
                    serverMsg = "答卷ID格式错误，操作取消";
                    continue;
                }

                // 1. 获取待批阅主观题列表
                Object[] getQuestionsRequest = new Object[] { currentUser, "GRADER_GET_SUBJECTIVES", sheetIdStr };
                Object[] questionsResponse = (Object[]) sendAndReceive(getQuestionsRequest);
                String listMessage = (String) questionsResponse[0];
                System.out.println(listMessage);

                if (questionsResponse.length > 1 && questionsResponse[1] instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<AnswerDetail> details = (List<AnswerDetail>) questionsResponse[1];

                    if (details.isEmpty()) {
                        serverMsg = "该答卷没有待批阅主观题或已被批阅完毕。";
                        continue;
                    }

                    // 2. 逐题批阅 (每题一次 UDP 事务)
                    for (AnswerDetail detail : details) {
                        Question q = detail.getQuestion();
                        System.out.printf("\n--- 批阅题目ID: %d ---\n", q.getId());
                        System.out.printf("题干: %s\n", q.getContent());
                        System.out.printf("用户答案: %s\n", detail.getUserAnswer());
                        System.out.printf("标准答案: %s\n", q.getAnswer());
                        System.out.printf("满分: %d\n", q.getScore());

                        System.out.print("请输入得分: ");
                        String scoreStr = readInput().trim();
                        System.out.print("请输入评语: ");
                        String comment = readInput().trim();

                        // 3. 提交批阅结果
                        Object[] gradeRequest = new Object[] { currentUser, "GRADER_GRADE_SUBJECTIVE",
                                detail.getDetailId(), scoreStr, comment, sheetId };
                        Object[] gradeResponse = (Object[]) sendAndReceive(gradeRequest);
                        System.out.println((String) gradeResponse[0]);
                    }

                    // 批阅完成后，强制刷新菜单
                    Object[] checkMenu = (Object[]) sendAndReceive(new Object[] { currentUser, "GRADER_CHECK_MENU" });
                    serverMsg = (String) checkMenu[0];
                } else {
                    serverMsg = listMessage;
                }

            } else {
                serverMsg = "无效选择，请重新输入";
            }
        }
    }

    // 关闭所有资源
    private void close() {
        try {
            if (reader != null)
                reader.close();
            if (socket != null)
                socket.close();
            System.out.println("客户端已关闭。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}