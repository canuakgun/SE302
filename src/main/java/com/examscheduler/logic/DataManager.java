package com.examscheduler.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.examscheduler.model.Classroom;
import com.examscheduler.model.Course;
import com.examscheduler.model.Schedule;
import com.examscheduler.model.Student;

/**
 * DataManager - Singleton Pattern
 * Veri yönetimi ve otomatik dosya güncelleme merkezi.
 * CSVParser ile senkronize çalışır.
 */
public class DataManager {
    private static DataManager instance;
    
    // Hafızadaki Veri Listeleri
    private List<Student> students;
    private List<Course> courses;
    private List<Classroom> classrooms;
    
    // Oluşturulan Sınav Takvimi
    private Schedule schedule;

    // Kaynak Dosya Referansları (Otomatik güncelleme için gereklidir)
    private File studentFile;
    private File courseFile;
    private File classroomFile;
    private File attendanceFile;
    
    // Versiyon kontrolü
    private static final int DATA_VERSION = 1;
    
    private DataManager() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.classrooms = new ArrayList<>();
        this.schedule = null;
    }
    
    public static DataManager getInstance() {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Yükleme işlemi sırasında (handleLoad) kaynak dosyaların yollarını kaydeder.
     * Bu sayede ekleme/silme işlemlerinde doğru dosyaya yazılabilir.
     */
    public void setSourceFiles(File std, File crs, File room, File att) {
        this.studentFile = std;
        this.courseFile = crs;
        this.classroomFile = room;
        this.attendanceFile = att;
    }

    // ==================== ÖĞRENCİ YÖNETİMİ (OTOMATİK CSV KAYDI) ====================
    
    public void addStudent(Student s) {
        if (!students.contains(s)) {
            students.add(s);
            // Listeyi güncelledikten sonra dosyaya yansıt
            if (studentFile != null) CSVParser.updateStudentFile(studentFile, students);
        }
    }
    
    public void removeStudent(Student s) {
        if (students.remove(s)) {
            // Listeden sildikten sonra dosyaya yansıt
            if (studentFile != null) CSVParser.updateStudentFile(studentFile, students);
            
            // Öğrenci silindiğinde, derslerden de kaydını silmemiz gerekir (Opsiyonel ama iyi olur)
            for (Course c : courses) {
                c.getEnrolledStudents().remove(s);
            }
        }
    }

    // ==================== DERS YÖNETİMİ (OTOMATİK CSV KAYDI) ====================
    
    public void addCourse(Course c) {
        if (!courses.contains(c)) {
            courses.add(c);
            if (courseFile != null) CSVParser.updateCourseFile(courseFile, courses);
        }
    }
    
    public void removeCourse(Course c) {
        if (courses.remove(c)) {
            if (courseFile != null) CSVParser.updateCourseFile(courseFile, courses);
            
            // Ders silindiğinde öğrencilerin listesinden de sil
            for (Student s : students) {
                s.getCourses().remove(c);
            }
        }
    }

    // ==================== SINIF YÖNETİMİ (OTOMATİK CSV KAYDI) ====================
    
    public void addClassroom(Classroom c) {
        if (!classrooms.contains(c)) {
            classrooms.add(c);
            if (classroomFile != null) CSVParser.updateClassroomFile(classroomFile, classrooms);
        }
    }

    public void removeClassroom(Classroom c) {
        if (classrooms.remove(c)) {
            if (classroomFile != null) CSVParser.updateClassroomFile(classroomFile, classrooms);
        }
    }

    // ==================== ATTENDANCE YÖNETİMİ ====================

    public void enrollStudentToCourse(Course course, Student student) {
        if (course == null || student == null) return;

        // 1. Course sınıfına eklemeyi dene (Başarılı olursa true döner)
        boolean addedToCourse = course.addStudent(student);

        // 2. Eğer derse eklendiyse, öğrenci tarafını ve dosyayı güncelle
        if (addedToCourse) {
            student.addCourse(course);

            // Eğer attendance dosyası henüz yoksa oluştur
            if (attendanceFile == null && courseFile != null) {
                attendanceFile = new File(courseFile.getParent(), "attendance.csv");
            }

            // 3. Dosyayı güncelle
            if (attendanceFile != null) {
                CSVParser.updateAttendanceFile(attendanceFile, courses);
            }
        }
    }

    public void unenrollStudentFromCourse(Course course, Student student) {
        if (course == null || student == null) return;

        // 1. Course sınıfından silmeyi dene (Başarılı olursa true döner)
        boolean removedFromCourse = course.removeStudent(student);

        // 2. Öğrencinin kendi ders listesinden de sil
        if (student.getCourses() != null) {
            student.getCourses().removeIf(c -> c.getCourseCode().equals(course.getCourseCode()));
        }

        // 3. Eğer silme başarılıysa dosyayı güncelle
        if (removedFromCourse && attendanceFile != null) {
            CSVParser.updateAttendanceFile(attendanceFile, courses);
        }
    }
    
    // ==================== GETTER / SETTER METOTLARI ====================
    
    public void setStudents(List<Student> students) { this.students = students != null ? students : new ArrayList<>(); }
    public void setCourses(List<Course> courses) { this.courses = courses != null ? courses : new ArrayList<>(); }
    public void setClassrooms(List<Classroom> classrooms) { this.classrooms = classrooms != null ? classrooms : new ArrayList<>(); }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }
    
    public List<Student> getStudents() { return students; }
    public List<Course> getCourses() { return courses; }
    public List<Classroom> getClassrooms() { return classrooms; }
    public Schedule getSchedule() { return schedule; }
    
    // ==================== YARDIMCI METOTLAR ====================

    public boolean isDataLoaded() {
        return students != null && !students.isEmpty() &&
               courses != null && !courses.isEmpty() &&
               classrooms != null && !classrooms.isEmpty();
    }
    
    public void clearAllData() {
        if (students != null) students.clear();
        if (courses != null) courses.clear();
        if (classrooms != null) classrooms.clear();
        this.schedule = null;
        // Dosya referanslarını sıfırlama, belki kullanıcı aynı klasöre tekrar yükleme yapar
    }
    
    public Classroom getClassroomByID(String id) {
        if (classrooms == null) return null;
        return classrooms.stream().filter(c -> c.getClassroomID().equals(id)).findFirst().orElse(null);
    }
    
    public Student getStudentByID(String id) {
        if (students == null) return null;
        return students.stream().filter(s -> s.getStudentID().equals(id)).findFirst().orElse(null);
    }
    
    public Course getCourseByCode(String code) {
        if (courses == null) return null;
        return courses.stream().filter(c -> c.getCourseCode().equals(code)).findFirst().orElse(null);
    }
    
    // ==================== PERSISTENCE (Tüm Durumu Kaydetme - Opsiyonel) ====================
    // Bu metotlar, CSV dışındaki "proje dosyası" (.dat) mantığı içindir.
    
    public void saveToFile(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeInt(DATA_VERSION);
            oos.writeObject(students);
            oos.writeObject(courses);
            oos.writeObject(classrooms);
            oos.writeObject(schedule);
        }
    }
    
    public void loadFromFile(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            int version = ois.readInt();
            if (version != DATA_VERSION) throw new IOException("Incompatible data version");
            
            this.students = (List<Student>) ois.readObject();
            this.courses = (List<Course>) ois.readObject();
            this.classrooms = (List<Classroom>) ois.readObject();
            this.schedule = (Schedule) ois.readObject();
        }
    }
}
