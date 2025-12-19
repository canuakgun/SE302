# 📅 Exam Scheduler System

<div align="center">
**A sophisticated JavaFX-based examination scheduling system that intelligently assigns exams to classrooms and time slots while respecting multiple constraints.**

[Features](#-features) • [Installation](#-installation) • [Usage](#-usage) • [Algorithm](#-scheduling-algorithm) • [Screenshots](#-screenshots) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-features)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Installation](#-installation)
- [Usage Guide](#-usage)
- [Scheduling Algorithm](#-scheduling-algorithm)
- [Data Management](#-data-management)
- [Export Capabilities](#-export-capabilities)
- [Screenshots](#-screenshots)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

The **Exam Scheduler System** is a comprehensive desktop application designed to automate and optimize the complex process of examination scheduling in educational institutions. Built with JavaFX, it provides an intuitive interface for managing students, courses, classrooms, and generating conflict-free examination schedules.

### Why Exam Scheduler?

Traditional manual exam scheduling is:
- ⏰ **Time-consuming**: Hours of manual work reduced to minutes
- ❌ **Error-prone**: Human errors in conflict detection
- 📊 **Difficult to optimize**: Hard to balance resources efficiently
- 🔄 **Hard to modify**: Changes require complete rescheduling

Our system solves these problems with intelligent algorithms and real-time validation.

---

## ✨ Features

### 🎓 Core Functionality

- **Intelligent Scheduling**: Automated exam scheduling using advanced constraint satisfaction algorithms
- **Multi-Constraint Validation**: 
  - No student has overlapping exams
  - Maximum 2 exams per student per day
  - Classroom capacity constraints
  - Time slot availability
- **Real-time Conflict Detection**: Instant validation with detailed error reporting
- **Manual Schedule Editing**: Drag-and-drop interface for fine-tuning schedules

### 📊 Data Management

- **CSV Import/Export**: Easy data import from existing systems
- **Persistent Storage**: Save and load complete project states
- **Student Portal**: Dedicated view for students to see their personalized schedules
- **Course Enrollment**: Flexible student-course relationship management

### 📈 Analytics & Reporting

- **Comprehensive Statistics**: 
  - Total exams, students, and classrooms
  - Classroom utilization rates
  - Student workload distribution
- **Visual Calendar**: Interactive calendar view of the complete schedule
- **Export Options**:
  - PDF reports (main schedule)
  - Individual student schedules (consolidated PDF)
  - CSV exports for data analysis

### 🎨 User Experience

- **Modern UI**: Clean, professional interface with dark/light theme support
- **Responsive Design**: Adaptive layout for different screen sizes
- **Tooltips & Help**: Context-sensitive help throughout the application
- **Progress Tracking**: Real-time feedback during long operations

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 11+ |
| **UI Framework** | JavaFX | 21.0.1 |
| **Build Tool** | Maven | 3.8+ |
| **Data Format** | CSV | - |

### Dependencies

```xml
<dependencies>
    <!-- JavaFX Controls -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.1</version>
    </dependency>
    
    <!-- JavaFX FXML -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.1</version>
    </dependency>
    
    <!-- iText PDF -->
    <dependency>
        <groupId>com.itextpdf</groupId>
        <artifactId>itextpdf</artifactId>
        <version>5.5.13.3</version>
    </dependency>
</dependencies>
```

---

## 📁 Project Structure

```
SE302/
├── src/
│   └── main/
│       └── java/
│           └── com/examscheduler/
│               ├── logic/
│               │   ├── CSVParser.java          # CSV data import/export
│               │   └── DataManager.java        # Singleton data manager
│               ├── model/
│               │   ├── Classroom.java          # Classroom entity
│               │   ├── Course.java             # Course entity
│               │   ├── Exam.java               # Exam entity
│               │   ├── Schedule.java           # Schedule container
│               │   ├── Student.java            # Student entity
│               │   └── TimeSlot.java           # Time slot entity
│               └── ui/
│                   ├── ExamSchedulerApp.java   # Main application
│                   └── ThemeManager.java       # Theme management
├── pom.xml                                     # Maven configuration
└── README.md                                   # This file
└──screenshots
```

### Architecture Highlights

- **MVC Pattern**: Clear separation between model, view, and logic
- **Singleton Pattern**: Centralized data management via `DataManager`
- **Observer Pattern**: Real-time UI updates on data changes
- **Factory Pattern**: Dynamic UI component generation

---

## 🚀 Installation

### Prerequisites

Ensure you have the following installed:

- ☕ **Java Development Kit (JDK) 11 or higher**
  ```bash
  java -version  # Should show 11 or higher
  ```

- 📦 **Apache Maven 3.8 or higher**
  ```bash
  mvn -version
  ```

### Step-by-Step Installation

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd SE302-18Aralık
   ```

2. **Build the Project**
   ```bash
   mvn clean install
   ```
   This will:
   - Download all dependencies
   - Compile the source code
   - Run tests (if configured)
   - Create the executable JAR

3. **Run the Application**
   ```bash
   mvn javafx:run
   ```

### Alternative: Run from IDE

1. Import the project as a Maven project in your IDE (IntelliJ IDEA, Eclipse, NetBeans)
2. Configure the main class: `com.examscheduler.App`
3. Run the application

---

## 📖 Usage

### Getting Started

#### 1. Welcome Screen

When you first launch the application, you'll see a modern welcome screen with options to:
- 📂 **Start New Project**: Begin with a fresh project
- 📁 **Load Existing Project**: Continue from a saved state
- ⚙️ **Settings**: Configure application preferences

![Welcome Screen](screenshots/welcome.png)

*The welcome screen provides quick access to all main features*

---

#### 2. Import Data

To schedule exams, you need to import data via CSV files:

**Required CSV Files:**
- `students.csv` - Student information
  ```csv
  StudentID,Name
  S001,John Doe
  S002,Jane Smith
  ```

- `courses.csv` - Course information
  ```csv
  CourseCode,CourseName,EnrolledCount
  CS101,Introduction to Computer Science,50
  MATH201,Calculus II,35
  ```

- `classrooms.csv` - Classroom information
  ```csv
  ClassroomID,Capacity
  A101,50
  B205,30
  ```

- `attendance.csv` - Student-course enrollments
  ```csv
  StudentID,CourseCode
  S001,CS101
  S001,MATH201
  S002,CS101
  ```

**Import Process:**
1. Click **"📥 Import Data"** from the toolbar
2. Select all four CSV files
3. Review the import summary
4. Click **"Import"** to load the data

![Import Dialog](screenshots/load.png)
*Import dialog showing file selection*


---

#### 3. Configure Schedule Parameters

Before generating the schedule, set the following parameters:

- **📅 Exam Start Date**: First day of the exam period
- **📆 Exam Day Period**: Total number of days for the examination period
- **⏰ Time Slots**: Time periods.(e.g 15:00-17:00)

These parameters determine the available time slots for scheduling.

![Schedule Configuration](screenshots/detailed-exam-view.png)
- **⏰ Time Slots**: The exam hours.

These parameters determine the available time slots for scheduling.

![Schedule Configuration](screenshots/configiration.png)
*Configure exam period duration and time slots*

---

#### 4. Generate Schedule

Click the **"⚡ Generate Schedule"** button to:
1. Analyze all constraints
2. Run the scheduling algorithm
3. Assign exams to classrooms and time slots
4. Validate the complete schedule

The system provides real-time progress updates during generation.

**Validation Results:**
- ✅ **Critical Issues**: Must be resolved (e.g., capacity violations)
- ⚠️ **Warnings**: Should be reviewed (e.g., student workload)
- ℹ️ **Info**: General statistics and analysis

![Schedule Generation](screenshots/generate.png)
*Generate result-Validation screen will be shown later screenshots.*
---

#### 5. Review & Edit Schedule

After generation, you can:

- **📊 View by Day**: See all exams for each day
- **🏫 View by Classroom**: Check classroom utilization
- **👨‍🎓 View by Student**: See individual student schedules
- **✏️ Manual Edits**: Drag and drop exams to different slots (with automatic validation)

![Schedule View](screenshots/detailed-exam-view.png)
*Main schedule view with filtering and editing capabilities*

---

#### 6. Student Portal

Students can view their personalized exam schedule:

1. Click **"👥 Student Portal"** from the toolbar
2. Search for a student by ID or name
3. View their schedule in multiple formats:
   - 📅 **Dashboard**: Overview with upcoming exams
   - 🗓️ **Calendar**: Visual timeline
   - 📈 **Statistics**: Exam distribution analysis

![Student Portal](screenshots/student-view.png)
*Student portal showing personalized schedule and statistics*

---

#### 7. Export & Reports

Generate professional reports:

**PDF Reports:**
- 📄 **Main Schedule**: Complete schedule for administrators
- 👤 **Student Schedules**: Individual PDFs for each student (consolidated)

**CSV Exports:**
- 📊 **Schedule Data**: For further analysis or integration
- 📋 **Statistics**: Detailed metrics and analytics

All exports include:
- Comprehensive statistics
- Visual charts (PDF only)
- Conflict analysis
- Utilization metrics

![Export Options](screenshots/export.png)
*Export dialog with multiple format options*

---

## 🧮 Scheduling Algorithm

The Exam Scheduler uses a **Greedy Algorithm** with intelligent heuristics and constraint prioritization to generate optimal schedules efficiently.

### Algorithm Overview

```mermaid
graph TD
    A[Start] --> B[Load Data]
    B --> C[Initialize Time Slots]
    C --> D[Split Large Exams Into Multiple Rooms]
    D --> E[Sort Exams by Student Count DESC]
    E --> F{For Each Exam}
    F --> G[Try Each Day & Time Slot]
    G --> H{Check Constraints}
    H -->|Pass| I[Find Suitable Classroom]
    I -->|Found| J[Assign Exam]
    I -->|Not Found| K{More Slots?}
    H -->|Fail| K
    K -->|Yes| G
    K -->|No| L[Mark as Unplaced]
    J --> M{More Exams?}
    L --> M
    M -->|Yes| F
    M -->|No| N[Validate Complete Schedule]
    N --> O[Generate Statistics]
    O --> P[Done]
```

### Greedy Strategy

The algorithm follows a **first-fit decreasing** strategy:
1. **Sort exams** by enrolled student count (largest first)
2. **Iterate** through each exam once
3. **Place** in the first valid time slot + classroom combination found
4. **No backtracking**: Once placed, never reconsidered

This approach is fast and typically produces good results for exam scheduling problems.

### Constraint Resolution

The algorithm enforces the following hard and soft constraints:

#### Hard Constraints (Must be satisfied)
1. **No Student Overlap**: A student cannot have two exams at the same time
2. **Classroom Capacity**: Number of enrolled students ≤ classroom capacity
3. **Time Slot Availability**: Each classroom can only host one exam per time slot
4. **No Consecutive Exams**: Students cannot have back-to-back exams
5. **Daily Exam Limit**: Students can have maximum 2 exams per day

#### Soft Constraints (Best effort)
1. **Classroom Utilization**: Prefers balanced usage across all classrooms via shuffling
2. **Large Exam Splitting**: Automatically splits large exams across multiple rooms at the same time

### Optimization Heuristics

1. **Most Constrained First**: Schedule exams with the most students first (harder to place later)
2. **Balanced Splitting**: Large exams are split evenly across multiple rooms
3. **Course Locking**: Multiple parts of the same course are locked to the same time slot
4. **Classroom Shuffling**: Randomizes classroom order to distribute load
5. **O(1) Daily Tracking**: Uses HashMap for constant-time student daily exam count lookup

### Performance

- **Small Datasets** (< 50 exams): < 1 second
- **Medium Datasets** (50-200 exams): 1-5 seconds
- **Large Datasets** (200+ exams): 5-20 seconds

Performance depends on:
- Number of constraints
- Classroom availability
- Student enrollment patterns
- Time slot configuration

---

## 💾 Data Management

### DataManager (Singleton)

The `DataManager` class serves as the central hub for all data operations:

```java
DataManager manager = DataManager.getInstance();

// Access data
List<Student> students = manager.getStudents();
List<Course> courses = manager.getCourses();
Schedule schedule = manager.getSchedule();

// O(1) lookups using HashMaps
Student student = manager.getStudentByID("S001");
Course course = manager.getCourseByCode("CS101");
Classroom classroom = manager.getClassroomByID("A101");
```

### Features

- **Singleton Pattern**: Ensures single source of truth
- **HashMap Indexing**: O(1) lookup by ID/code
- **Automatic CSV Sync**: Changes automatically written back to CSV files
- **Serialization**: Save/load complete project states
- **Validation**: Built-in data integrity checks

### CSV Format Requirements

All CSV files must:
- Use UTF-8 encoding
- Have headers in the first row
- Use comma (`,`) as delimiter
- Contain no empty lines

---

## 📤 Export Capabilities

### PDF Exports

Generated PDF reports include:

1. **Cover Page**: Institution name, exam period, generation date
2. **Statistics Dashboard**:
   - Total exams, students, classrooms
   - Classroom utilization chart
   - Student workload distribution
3. **Complete Schedule**: Day-by-day breakdown
4. **Conflict Analysis**: Warnings and validation results

### CSV Exports

CSV exports include:
- Exam code, course name, date, time, classroom
- Student list per exam
- Metadata (generation timestamp, parameters)

### Student PDFs

Individual student schedules contain:
- Personal information
- Exam calendar
- Detailed exam list (time, location, course)
- Preparation timeline

---

## 📸 Screenshots

### Main Application Views

<table>
<tr>
<td width="50%">

![Main Screen](screenshots/main-screen.png)  
*Main dashboard with key statistics*

</td>
</tr>
</table>

---

### Advanced Features

<table>
<tr>
<td width="50%">

![Validation](screenshots/validation.png)  
*Detailed validation results with conflict detection*

</td>
<td width="50%">

![Analytics](screenshots/statistics.png)  
*Comprehensive analytics and statistics*

</td>
</tr>
</table>


---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Ways to Contribute

1. 🐛 **Report Bugs**: Open an issue with detailed reproduction steps
2. 💡 **Suggest Features**: Propose new features or improvements
3. 📝 **Improve Documentation**: Fix typos, add examples, clarify instructions
4. 🔧 **Submit Pull Requests**: Fix bugs or implement features

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes with clear, descriptive commits
4. Test thoroughly
5. Push to your fork: `git push origin feature/amazing-feature`
6. Open a Pull Request

### Code Style Guidelines

- Follow Java naming conventions
- Add JavaDoc comments for public methods
- Keep methods focused and under 50 lines when possible
- Write descriptive commit messages
- Include inline comments for complex logic

### Testing

Before submitting a PR:
- ✅ Test with sample CSV data
- ✅ Verify no regression in existing features
- ✅ Check UI responsiveness
- ✅ Test on different operating systems (if possible)

---

## 📄 License

```


Copyright (c) 2025 Exam Scheduler Team

Permission is granted, free of charge, to anyone who obtains a copy of this software and its documentation to use it freely.
This includes the rights to use, copy, modify, merge, publish, distribute, sublicense, and sell copies of the software.
The only requirement is that this license notice and copyright notice must be included in all copies or significant parts of the software.
The software is provided “as is”, without any warranties.
The authors are not responsible for any damages, losses, or claims that may arise from using this software.
```

---

## 🙏 Acknowledgments

- **JavaFX Team**: For the excellent UI framework
- **iText Team**: For powerful PDF generation capabilities
- **Contributors**: Everyone who has contributed to this project
Thanks for all help for all members.Furkan,Ali,Ahmet Emir, Can and Abdulhamid.

---

## 📧 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-repo/discussions)
- **Email**: your-email@example.com
- **Email**: examscheduler@team11.com

---

<div align="center">

**Made with ❤️ by the Exam Scheduler Team**
**Made with by the Exam Scheduler Team**

⭐ Star this repository if you find it helpful!

</div>
