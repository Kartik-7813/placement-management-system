package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.SkillResponse;
import com.placementmanagementsystem.dto.StudentRequest;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.exception.SkillNotFoundException;
import com.placementmanagementsystem.exception.StudentNotFoundException;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StudentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("StudentController Unit Tests")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ADMIN_TOKEN = "admin-test-token";
    private static final String STUDENT_TOKEN = "student-test-token";

    private void mockToken(String token, String email, String role) {
        UserDetails userDetails = new User(
                email,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    private static final String VALID_STUDENT_JSON = """
            {
                "rollNumber": "CS2026001",
                "name": "Jane Doe",
                "email": "jane.doe@example.com",
                "phone": "9876543210",
                "dob": "2003-05-15",
                "gender": "Female",
                "department": "Computer Science",
                "cgpa": 8.75,
                "passingYear": 2026,
                "address": "123 Tech Park",
                "status": "ACTIVE"
            }
            """;

    private static final String INVALID_STUDENT_JSON = """
            {
                "rollNumber": "",
                "name": "",
                "email": "invalid-email",
                "cgpa": 12.00
            }
            """;

    @Test
    @DisplayName("POST /api/students should return 201 Created when admin creates student")
    void createStudent_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        StudentResponse response = new StudentResponse(
                1L, "CS2026001", "Jane Doe", "jane.doe@example.com", "9876543210",
                LocalDate.of(2003, 5, 15), "Female", "Computer Science",
                new BigDecimal("8.75"), 2026, "123 Tech Park", StudentStatus.ACTIVE
        );
        when(studentService.createStudent(any(StudentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId", is(1)))
                .andExpect(jsonPath("$.rollNumber", is("CS2026001")))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane.doe@example.com")))
                .andExpect(jsonPath("$.department", is("Computer Science")))
                .andExpect(jsonPath("$.cgpa", is(8.75)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(studentService).createStudent(any(StudentRequest.class));
    }

    @Test
    @DisplayName("POST /api/students with invalid data should return 400 Bad Request with validation errors")
    void createStudent_ValidationError() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_STUDENT_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.rollNumber", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.cgpa", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/students with duplicate rollNumber/email should return 409 Conflict")
    void createStudent_DuplicateConflict() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        when(studentService.createStudent(any(StudentRequest.class)))
                .thenThrow(new DuplicateStudentException("A student with email or roll number already exists"));

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("A student with email or roll number already exists")));
    }

    @Test
    @DisplayName("GET /api/students should return 200 OK with paginated students")
    void getAllStudents_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        StudentResponse student = new StudentResponse(
                1L, "CS2026001", "Jane Doe", "jane.doe@example.com", "9876543210",
                LocalDate.of(2003, 5, 15), "Female", "Computer Science",
                new BigDecimal("8.75"), 2026, "123 Tech Park", StudentStatus.ACTIVE
        );
        when(studentService.getAllStudents(eq("Computer Science"), eq(StudentStatus.ACTIVE), eq("Jane"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("department", "Computer Science")
                        .param("status", "ACTIVE")
                        .param("name", "Jane")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].studentId", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Jane Doe")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/students/{id} should return 200 OK when student exists")
    void getStudentById_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        StudentResponse student = new StudentResponse(
                1L, "CS2026001", "Jane Doe", "jane.doe@example.com", "9876543210",
                LocalDate.of(2003, 5, 15), "Female", "Computer Science",
                new BigDecimal("8.75"), 2026, "123 Tech Park", StudentStatus.ACTIVE
        );
        when(studentService.getStudentById(1L)).thenReturn(student);

        mockMvc.perform(get("/api/students/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId", is(1)))
                .andExpect(jsonPath("$.name", is("Jane Doe")));

        verify(studentService).getStudentById(1L);
    }

    @Test
    @DisplayName("GET /api/students/{id} should return 404 Not Found when student does not exist")
    void getStudentById_NotFound() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        when(studentService.getStudentById(99L)).thenThrow(new StudentNotFoundException("Student not found with ID: 99"));

        mockMvc.perform(get("/api/students/99")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Student not found with ID: 99")));
    }

    @Test
    @DisplayName("PUT /api/students/{id} should return 200 OK when update succeeds")
    void updateStudent_Success() throws Exception {
        mockToken(STUDENT_TOKEN, "jane.doe@example.com", "ROLE_STUDENT");

        StudentResponse response = new StudentResponse(
                1L, "CS2026001", "Jane Updated", "jane.doe@example.com", "9876543210",
                LocalDate.of(2003, 5, 15), "Female", "Computer Science",
                new BigDecimal("8.90"), 2026, "456 Tech Park", StudentStatus.ACTIVE
        );
        when(studentService.updateStudent(eq(1L), any(StudentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/students/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId", is(1)))
                .andExpect(jsonPath("$.name", is("Jane Updated")));

        verify(studentService).updateStudent(eq(1L), any(StudentRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/students/{id} should return 204 No Content on deactivation")
    void deactivateStudent_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        doNothing().when(studentService).deactivateStudent(1L);

        mockMvc.perform(delete("/api/students/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNoContent());

        verify(studentService).deactivateStudent(1L);
    }

    @Test
    @DisplayName("GET /api/students/{id}/skills should return 200 OK with skills list")
    void getStudentSkills_Success() throws Exception {
        mockToken(STUDENT_TOKEN, "jane.doe@example.com", "ROLE_STUDENT");
        SkillResponse skill = new SkillResponse(10L, "Java");
        when(studentService.getStudentSkills(1L)).thenReturn(List.of(skill));

        mockMvc.perform(get("/api/students/1/skills")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].skillId", is(10)))
                .andExpect(jsonPath("$[0].name", is("Java")));
    }

    @Test
    @DisplayName("POST /api/students/{studentId}/skills/{skillId} should return 200 OK on assignment")
    void assignSkillToStudent_Success() throws Exception {
        mockToken(STUDENT_TOKEN, "jane.doe@example.com", "ROLE_STUDENT");
        SkillResponse skill = new SkillResponse(10L, "Java");
        when(studentService.assignSkillToStudent(1L, 10L)).thenReturn(List.of(skill));

        mockMvc.perform(post("/api/students/1/skills/10")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Java")));

        verify(studentService).assignSkillToStudent(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/students/{studentId}/skills/{skillId} should return 404 when skill not found")
    void assignSkillToStudent_SkillNotFound() throws Exception {
        mockToken(STUDENT_TOKEN, "jane.doe@example.com", "ROLE_STUDENT");
        when(studentService.assignSkillToStudent(1L, 999L)).thenThrow(new SkillNotFoundException("Skill not found with ID: 999"));

        mockMvc.perform(post("/api/students/1/skills/999")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Skill not found with ID: 999")));
    }

    @Test
    @DisplayName("DELETE /api/students/{studentId}/skills/{skillId} should return 204 No Content")
    void removeSkillFromStudent_Success() throws Exception {
        mockToken(STUDENT_TOKEN, "jane.doe@example.com", "ROLE_STUDENT");
        doNothing().when(studentService).removeSkillFromStudent(1L, 10L);

        mockMvc.perform(delete("/api/students/1/skills/10")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isNoContent());

        verify(studentService).removeSkillFromStudent(1L, 10L);
    }
}
