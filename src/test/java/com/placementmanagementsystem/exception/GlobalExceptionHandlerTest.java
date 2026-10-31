package com.placementmanagementsystem.exception;

import com.placementmanagementsystem.controller.ApplicationController;
import com.placementmanagementsystem.controller.StudentController;
import com.placementmanagementsystem.dto.StudentRequest;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.service.ApplicationService;
import com.placementmanagementsystem.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {StudentController.class, ApplicationController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private ApplicationService applicationService;

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

    @Test
    @DisplayName("Should return 400 Bad Request when request body contains malformed JSON")
    void testMalformedJsonReturnsBadRequest() throws Exception {
        String malformedJson = "{ \"rollNumber\": \"CS101\", \"name\": ";

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON request or invalid field value/format")))
                .andExpect(jsonPath("$.path", is("/api/students")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when enum value is invalid")
    void testInvalidEnumValueReturnsBadRequest() throws Exception {
        String invalidEnumJson = """
                {
                    "rollNumber": "CS2026001",
                    "name": "Jane Doe",
                    "email": "jane@example.com",
                    "status": "NON_EXISTENT_STATUS"
                }
                """;

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnumJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is("/api/students")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when path variable type is mismatched")
    void testPathVariableTypeMismatchReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/students/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Parameter 'id' should be of type Long")))
                .andExpect(jsonPath("$.path", is("/api/students/not-a-number")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when required query parameter is missing")
    void testMissingRequiredRequestParamReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/applications/1/status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Required request parameter 'status' is missing")))
                .andExpect(jsonPath("$.path", is("/api/applications/1/status")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request with field-level validation errors on Bean Validation failure")
    void testBeanValidationFailureReturnsBadRequestWithValidationErrors() throws Exception {
        String invalidRequestJson = """
                {
                    "rollNumber": "",
                    "name": "",
                    "email": "invalid-email-format",
                    "cgpa": 15.50,
                    "dob": "2099-01-01",
                    "phone": "12345678901234567890"
                }
                """;

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed for one or more fields")))
                .andExpect(jsonPath("$.validationErrors.rollNumber", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.cgpa", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.dob", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.phone", notNullValue()));
    }

    @Test
    @DisplayName("Should return 404 Not Found when custom NotFoundException is thrown")
    void testCustomNotFoundExceptionReturnsNotFound() throws Exception {
        when(studentService.getStudentById(999L))
                .thenThrow(new StudentNotFoundException("Student not found with id: 999"));

        mockMvc.perform(get("/api/students/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Student not found with id: 999")))
                .andExpect(jsonPath("$.path", is("/api/students/999")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when custom ConflictException is thrown")
    void testCustomConflictExceptionReturnsConflict() throws Exception {
        when(studentService.createStudent(any(StudentRequest.class)))
                .thenThrow(new DuplicateStudentException("Student with roll number CS2026001 already exists"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Student with roll number CS2026001 already exists")))
                .andExpect(jsonPath("$.path", is("/api/students")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when custom business validation exception is thrown")
    void testCustomBadRequestBusinessExceptionReturnsBadRequest() throws Exception {
        when(studentService.updateStudent(eq(1L), any(StudentRequest.class)))
                .thenThrow(new IllegalStateException("Invalid status transition"));

        mockMvc.perform(put("/api/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid status transition")))
                .andExpect(jsonPath("$.path", is("/api/students/1")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when DataIntegrityViolationException is thrown")
    void testDataIntegrityViolationReturnsConflictWithoutExposingSql() throws Exception {
        when(studentService.createStudent(any(StudentRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'CS2026001' for key 'students.uk_roll_number'"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("Database constraint violation")))
                .andExpect(jsonPath("$.path", is("/api/students")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when CannotAcquireLockException (deadlock/concurrency failure) is thrown")
    void testCannotAcquireLockExceptionReturnsConflictWithoutExposingSql() throws Exception {
        when(studentService.createStudent(any(StudentRequest.class)))
                .thenThrow(new CannotAcquireLockException("Deadlock found when trying to get lock; try restarting transaction"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STUDENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("A concurrency conflict occurred while processing the request")))
                .andExpect(jsonPath("$.path", is("/api/students")));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when unexpected exception is thrown without leaking details")
    void testUnexpectedExceptionReturnsGeneric500WithoutLeakingStacktrace() throws Exception {
        when(studentService.getStudentById(1L))
                .thenThrow(new NullPointerException("Internal null pointer crash in service layer"));

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected internal error occurred. Please contact support.")))
                .andExpect(jsonPath("$.path", is("/api/students/1")));
    }
}
