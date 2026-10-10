package com.college.erp.config;

import com.college.erp.entity.*;
import com.college.erp.repository.*;
import com.college.erp.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Fills an empty database with an admin account and demo data so the portal is ready to show. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;

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
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        userService.createUser("admin", "admin123", Role.ADMIN);

        Faculty ravi = faculty("Dr. Ravi Kumar", "CSE", "ravi@college.edu");
        Faculty meena = faculty("Prof. Meena Sharma", "CSE", "meena@college.edu");

        Course ds = course("CS201", "Data Structures", 4, ravi);
        Course dbms = course("CS202", "Database Management Systems", 4, meena);
        Course java = course("CS203", "Object Oriented Programming in Java", 3, ravi);

        String[] names = {"Aarav Patel", "Diya Nair", "Karthik Raman", "Priya Menon", "Rohan Gupta", "Sneha Reddy"};
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String rollNo = "CSE200" + (i + 1);
            Student s = new Student();
            s.setRollNo(rollNo);
            s.setName(names[i]);
            s.setDepartment("CSE");
            s.setYear(2);
            s.setEmail(names[i].split(" ")[0].toLowerCase() + "@student.college.edu");
            s.setPhone("98765432" + String.format("%02d", i + 1));
            s.setUser(userService.createUser(rollNo, rollNo, Role.STUDENT));
            students.add(studentRepository.save(s));
        }

        // Attendance for the last 10 weekdays. The last student is absent often, to show the red < 75% warning.
        List<LocalDate> days = lastWeekdays(10);
        for (Course course : List.of(ds, dbms, java)) {
            for (int d = 0; d < days.size(); d++) {
                for (int i = 0; i < students.size(); i++) {
                    boolean present = (i == students.size() - 1) ? d % 2 == 1 : (i + d) % 7 != 0;
                    attendanceRepository.save(new Attendance(students.get(i), course, days.get(d), present));
                }
            }
        }

        // Marks for two courses; the Java course is left empty to show "not entered yet".
        int[] dsMarks = {92, 78, 85, 67, 54, 38};
        int[] dbmsMarks = {88, 81, 73, 95, 61, 45};
        for (int i = 0; i < students.size(); i++) {
            markRepository.save(new Mark(students.get(i), ds, dsMarks[i]));
            markRepository.save(new Mark(students.get(i), dbms, dbmsMarks[i]));
        }
    }

    private Faculty faculty(String name, String department, String email) {
        Faculty f = new Faculty();
        f.setName(name);
        f.setDepartment(department);
        f.setEmail(email);
        f.setUser(userService.createUser(email, email, Role.FACULTY));
        return facultyRepository.save(f);
    }

    private Course course(String code, String name, int credits, Faculty faculty) {
        Course c = new Course();
        c.setCode(code);
        c.setName(name);
        c.setDepartment("CSE");
        c.setYear(2);
        c.setCredits(credits);
        c.setFaculty(faculty);
        return courseRepository.save(c);
    }

    private List<LocalDate> lastWeekdays(int count) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate day = LocalDate.now().minusDays(1);
        while (days.size() < count) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(0, day);
            }
            day = day.minusDays(1);
        }
        return days;
    }
}
