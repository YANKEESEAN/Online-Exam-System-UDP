# 📚 Online Exam System based on UDP Socket Programming

## 📌 Project Overview
This is a multi-role online examination system built with **Java UDP Socket** programming. The system supports three types of users — **Administrators**, **Students**, and **Graders** — each with distinct functional permissions. It implements a complete exam workflow including question bank management, automated test paper generation, real-time exam taking with auto-save, objective question auto-grading, subjective question manual grading, and score statistics.

Different from the TCP version, this project uses **UDP (User Datagram Protocol)** as the underlying communication protocol. UDP is connectionless and datagram-oriented, making it more efficient for frequent small-data exchanges and real-time applications. To handle UDP's stateless nature, each client request carries complete contextual information (user object, operation command, and all parameters), enabling the server to process each transaction independently without maintaining a persistent connection.

This project was developed as a coursework assignment for the **Network Programming** course.

---

## 👥 Roles & Features

| Role | Features |
| :---: | :--- |
| **Administrator** | • Add questions to question bank<br>• Create and publish exam papers (manual or auto-generated)<br>• View and export score statistics |
| **Student** | • Take exams<br>• Auto-save answers during exam<br>• Submit papers<br>• View objective question scores immediately |
| **Grader** | • View pending grading list<br>• Grade subjective questions (with scores and comments)<br>• Auto-calculate total scores |

---

## 🏗️ System Architecture

| Component | Description |
| :---: | :--- |
| **Client-Server Model** | Built on UDP using Java `DatagramSocket` and `DatagramPacket` |
| **Database** | MySQL for persistent storage of users, questions, papers, and answer records |
| **Design Pattern** | Object-oriented design with layered architecture (UI → Controller → Service → DAO) |

### 📊 UML Diagrams (included in report)
- Use Case Diagrams
- Sequence Diagrams
- Class Diagrams

---

## 📁 Project Structure
```text
Online-Exam-System-UDP/
├── src/
│   ├── client/            # Client-side UI and controllers
│   ├── server/            # Server-side business logic
│   ├── model/             # Entity classes (User, Question, Paper, Answer, etc.)
│   ├── dao/               # Database access layer
│   ├── service/           # Business service layer
│   └── util/              # Utility classes
├── lib/                   # External dependencies (JDBC driver, etc.)
├── bin/                   # Compiled class files
├── exam_user.sql          # Database schema and sample data
├── README.md
└── .vscode/               # VS Code configuration
```

---

## 🗄️ Database Schema (Key Tables)

| Table | Description |
| :---: | :--- |
| `USERS` | User accounts (student/admin/grader) |
| `QUESTIONS` | Question bank with types (single choice, multiple choice, fill-in, essay) |
| `EXAM_PAPERS` | Exam paper metadata (course, duration, total score, status) |
| `PAPER_QUESTIONS` | Many-to-many relationship between papers and questions |
| `ANSWER_SHEETS` | Student answer records for each exam attempt |
| `ANSWER_DETAILS` | Detailed answers for each question in a sheet |

---

## 🚀 Quick Start

### Prerequisites
- Java 8 or higher
- MySQL 5.7 or higher
- Eclipse / IntelliJ IDEA (recommended)

### Setup Steps

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/Online-Exam-System-UDP.git
```

**2. Import the database**
```bash
mysql -u root -p < exam_user.sql
```

**3. Configure database connection**  
Update JDBC connection settings in `src/util/DBUtil.java`

**4. Run the server**  
Start `com.server.ExamServer.java`

**5. Run the client**  
Start `com.client.ExamClient.java`

### 🔑 Default Accounts

| Role | Username | Password |
| :---: | :---: | :---: |
| Administrator | admin1 | admin123 |
| Student | student1 | 123456 |
| Grader | grader1 | grader123 |

---

## 📸 Sample Workflow

### 1. Admin Login & Question Addition
Login as admin → Select "Add Question" → Enter question details (type, stem, options, answer, knowledge point, difficulty, score) → Question saved to bank.

### 2. Auto-Generate & Publish Paper
Set paper strategy (e.g., 3 single-choice, 2 fill-in, 1 multiple-choice, 1 essay) → System automatically retrieves questions from bank → Preview generated paper → Publish.

### 3. Student Takes Exam
Login as student → Select a published paper → Answer questions sequentially → Answers auto-saved on each switch → Submit → Objective questions graded automatically.

### 4. Grader Grades Subjective Questions
Login as grader → View pending papers → Grade each essay question with score and comment → System auto-calculates total score.

---

## 💻 Core Technologies
- Java SE (`DatagramSocket`, `DatagramPacket`, I/O streams, multithreading)
- UDP/IP Protocol – connectionless, datagram-oriented communication
- MySQL – data persistence
- JDBC – database connectivity
- Object-Oriented Design – inheritance, polymorphism, encapsulation
- Object Serialization – converting Java objects to byte arrays for UDP transmission

---

## 🔄 TCP vs UDP Comparison

| Aspect | TCP Version | UDP Version |
| :---: | :---: | :---: |
| **Connection** | Connection-oriented (persistent) | Connectionless (each request independent) |
| **Socket Class** | `Socket` / `ServerSocket` | `DatagramSocket` / `DatagramPacket` |
| **Data Transmission** | Stream-based (`ObjectInputStream`/`ObjectOutputStream`) | Datagram-based (serialized byte arrays) |
| **Session Management** | Maintained via persistent streams | Context carried in each request |
| **Reliability** | Reliable (guaranteed delivery) | Best-effort (no guarantee) |
| **Performance** | Lower (overhead of connection maintenance) | Higher (lightweight datagrams) |
| **Use Case** | Large data, multi-step transactions | Frequent small data, real-time scenarios |

---

## 📚 What I Learned
- Mastered UDP socket programming with Java (`DatagramSocket`, `DatagramPacket`, object serialization/deserialization)
- Solved the session management challenge in UDP by embedding user context in each request
- Practiced object-oriented analysis and design using UML (use case, sequence, and class diagrams)
- Implemented multi-role permission management and data consistency in a client-server system
- Designed auto-save mechanisms and objective question auto-grading logic
- Gained hands-on experience with MySQL-JDBC integration and transaction management
- Understood the trade-offs between TCP and UDP protocols in practical applications

---

## 📧 Contact
For questions or suggestions, please open an issue or contact the repository owner.

---

## 📜 License
This project is for educational purposes only.

