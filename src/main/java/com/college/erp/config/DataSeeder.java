package com.college.erp.config;

import com.college.erp.entity.*;
import com.college.erp.repository.*;
import com.college.erp.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** Fills an empty database with an admin account and realistic demo data so the portal is ready to show. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;

    /** Fixed seed so the demo data is the same on every fresh database. */
    private final Random random = new Random(42);

    public DataSeeder(UserRepository userRepository, UserService userService,
                      StudentRepository studentRepository, FacultyRepository facultyRepository,
                      CourseRepository courseRepository, AttendanceRepository attendanceRepository,
                      MarkRepository markRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            // databases created before IA/SEM marks existed get demo marks once
            if (markRepository.count() == 0) seedMarksForExistingStudents();
            return;
        }

        userService.createUser("admin", "admin123", Role.ADMIN);

        Faculty ravi = faculty("Dr. Ravi Kumar", "CSE", "ravi@college.edu");
        Faculty meena = faculty("Prof. Meena Sharma", "CSE", "meena@college.edu");
        Faculty anil = faculty("Dr. Anil Verma", "ECE", "anil@college.edu");

        // CSE Year 2 (the main demo class) - CS203 is a 2-hour lab
        List<Course> cseCourses = List.of(
                course("CS201", "Data Structures and Algorithms", "CSE", 4, ravi),
                course("CS202", "Database Management Systems", "CSE", 4, meena),
                course("CS203", "Object Oriented Programming Lab", "CSE", 2, ravi),
                course("CS204", "Operating Systems", "CSE", 3, meena),
                course("MA201", "Transforms and Boundary Value Problems", "CSE", 4, ravi),
                course("HS201", "Professional Ethics", "CSE", 1, meena));
        List<Course> eceCourses = List.of(
                course("EC201", "Signals and Systems", "ECE", 4, anil),
                course("EC202", "Analog Circuits", "ECE", 3, anil));

        // name, attendance rate (how often present)
        List<Student> cse = students("CSE", 2, new String[]{"Aarav Patel", "Diya Nair", "Karthik Raman",
                "Priya Menon", "Rohan Gupta", "Sneha Reddy"});
        double[] cseRates = {0.93, 0.88, 0.85, 0.96, 0.81, 0.62};
        List<Student> ece = students("ECE", 2, new String[]{"Arjun Das", "Meera Iyer", "Vikram Singh"});
        double[] eceRates = {0.90, 0.84, 0.70};

        seedClass(cse, cseRates, cseCourses);
        seedClass(ece, eceRates, eceCourses);
    }

    /** Attendance for ~10 weeks, then marks. */
    private void seedClass(List<Student> students, double[] rates, List<Course> courses) {
        List<LocalDate> days = weekdays(LocalDate.now().minusDays(70), LocalDate.now().minusDays(1));
        List<Attendance> rows = new ArrayList<>();
        for (int c = 0; c < courses.size(); c++) {
            Course course = courses.get(c);
            for (int d = 0; d < days.size(); d++) {
                // each course meets on a different ~60% of the weekdays
                if ((d + c) % 5 == 4 || (d * 3 + c) % 7 == 0) continue;
                int hours = course.getCredits() == 2 ? 2 : (random.nextInt(4) == 0 ? 2 : 1);
                for (int i = 0; i < students.size(); i++) {
                    rows.add(new Attendance(students.get(i), course, days.get(d), hours, status(rates[i])));
                }
            }
        }
        attendanceRepository.saveAll(rows);

        seedMarks(students, rates, courses);
    }

    /**
     * IA-1, IA-2 and SEM for every course except the last one, which only has IA-1 so far
     * (shows a course still in progress). Better attendance -> better marks, roughly.
     */
    private void seedMarks(List<Student> students, double[] rates, List<Course> courses) {
        List<Mark> marks = new ArrayList<>();
        for (int c = 0; c < courses.size(); c++) {
            boolean last = c == courses.size() - 1;
            for (int i = 0; i < students.size(); i++) {
                int level = (int) Math.round(rates[i] * 100) - 8;
                Integer ia1 = clamp(level / 2 + random.nextInt(11) - 5, 10, 50);
                Integer ia2 = last ? null : clamp(level / 2 + random.nextInt(11) - 5, 10, 50);
                Integer sem = last ? null : clamp(level + random.nextInt(21) - 10, 20, 100);
                marks.add(new Mark(students.get(i), courses.get(c), ia1, ia2, sem));
            }
        }
        markRepository.saveAll(marks);
    }

    private void seedMarksForExistingStudents() {
        Map<String, List<Student>> classes = new LinkedHashMap<>();
        for (Student s : studentRepository.findAll()) {
            classes.computeIfAbsent(s.getDepartment() + "/" + s.getYear(), k -> new ArrayList<>()).add(s);
        }
        for (List<Student> students : classes.values()) {
            Student first = students.get(0);
            List<Course> courses = courseRepository.findByDepartmentAndYearOrderByCode(first.getDepartment(), first.getYear());
            if (courses.isEmpty()) continue;
            double[] rates = new double[students.size()];
            for (int i = 0; i < rates.length; i++) rates[i] = 0.65 + random.nextDouble() * 0.3;
            seedMarks(students, rates, courses);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private AttendanceStatus status(double presentRate) {
        double roll = random.nextDouble();
        if (roll < presentRate) return AttendanceStatus.PRESENT;
        if (roll < presentRate + 0.04) return AttendanceStatus.OD;
        return AttendanceStatus.ABSENT;
    }

    private List<Student> students(String department, int year, String[] names) {
        List<Student> list = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String rollNo = department + year + "00" + (i + 1);
            Student s = new Student();
            s.setRollNo(rollNo);
            s.setName(names[i]);
            s.setDepartment(department);
            s.setYear(year);
            s.setEmail(names[i].split(" ")[0].toLowerCase() + "@student.college.edu");
            s.setPhone("98765" + String.format("%05d", (department.hashCode() & 0xff) * 100 + i + 1));
            s.setUser(userService.createUser(rollNo, rollNo, Role.STUDENT));
            list.add(studentRepository.save(s));
        }
        return list;
    }

    private Faculty faculty(String name, String department, String email) {
        Faculty f = new Faculty();
        f.setName(name);
        f.setDepartment(department);
        f.setEmail(email);
        f.setUser(userService.createUser(email, email, Role.FACULTY));
        return facultyRepository.save(f);
    }

    private Course course(String code, String name, String department, int credits, Faculty faculty) {
        Course c = new Course();
        c.setCode(code);
        c.setName(name);
        c.setDepartment(department);
        c.setYear(2);
        c.setCredits(credits);
        c.setFaculty(faculty);
        return courseRepository.save(c);
    }

    private List<LocalDate> weekdays(LocalDate from, LocalDate to) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(day);
            }
        }
        return days;
    }
}
