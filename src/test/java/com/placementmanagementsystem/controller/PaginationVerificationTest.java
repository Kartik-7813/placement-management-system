package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.CompanyResponse;
import com.placementmanagementsystem.dto.InterviewResponse;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Interview;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Placement;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.ApplicationService;
import com.placementmanagementsystem.service.CompanyService;
import com.placementmanagementsystem.service.InterviewService;
import com.placementmanagementsystem.service.JobService;
import com.placementmanagementsystem.service.PlacementService;
import com.placementmanagementsystem.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        StudentController.class,
        CompanyController.class,
        JobController.class,
        ApplicationController.class,
        InterviewController.class,
        PlacementController.class
})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
@DisplayName("Pagination & API Response Verification Tests")
class PaginationVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private InterviewService interviewService;

    @MockitoBean
    private PlacementService placementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ADMIN_TOKEN = "admin.jwt.token";

    @BeforeEach
    void setUpSecurity() {
        UserDetails adminUser = new User("admin@test.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtService.extractUsername(ADMIN_TOKEN)).thenReturn("admin@test.com");
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(adminUser);
        when(jwtService.isTokenValid(ADMIN_TOKEN, adminUser)).thenReturn(true);
    }

    // =========================================================
    // 1. STUDENT CONTROLLER PAGINATION
    // =========================================================

    @Nested
    @DisplayName("StudentController Pagination")
    class StudentPaginationTests {

        private StudentResponse sampleStudent() {
            return new StudentResponse(
                    1L, "CS2026001", "Jane Doe", "jane@test.com", "9876543210",
                    LocalDate.of(2003, 5, 15), "Female", "Computer Science",
                    new BigDecimal("8.75"), 2026, "Tech Park", StudentStatus.ACTIVE
            );
        }

        @Test
        @DisplayName("Default pagination uses page 0, size 10, sort studentId ASC")
        void testStudentDefaultPagination() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(0, 10), 1);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].studentId").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentService).getAllStudents(any(), any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("studentId").getDirection());
        }

        @Test
        @DisplayName("Custom page, size, and sort descending are decoded and passed to service")
        void testStudentCustomPaginationAndSorting() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(2, 5), 15);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "name,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentService).getAllStudents(any(), any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(2, captured.getPageNumber());
            assertEquals(5, captured.getPageSize());
            assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("name").getDirection());
        }

        @Test
        @DisplayName("Pagination combined with filters works seamlessly")
        void testStudentPaginationWithFilters() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(0, 5), 1);
            when(studentService.getAllStudents(eq("Computer Science"), eq(StudentStatus.ACTIVE), eq("Jane"), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("department", "Computer Science")
                            .param("status", "ACTIVE")
                            .param("name", "Jane")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Jane Doe"));
        }

        @Test
        @DisplayName("Empty result page returns empty content and totalElements 0")
        void testStudentEmptyPage() throws Exception {
            Page<StudentResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.empty").value(true));
        }

        @Test
        @DisplayName("Request beyond last page returns empty content with correct totalElements")
        void testStudentBeyondLastPage() throws Exception {
            Page<StudentResponse> beyondPage = new PageImpl<>(List.of(), PageRequest.of(10, 5), 3);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(beyondPage);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("page", "10")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("Invalid sort property triggers 400 Bad Request via PropertyReferenceException")
        void testStudentInvalidSortProperty() throws Exception {
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class)))
                    .thenThrow(new PropertyReferenceException("invalidField", TypeInformation.of(Student.class), List.of()));

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("sort", "invalidField,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid sort property 'invalidField'"));
        }

        @Test
        @DisplayName("Invalid non-numeric page parameter safely falls back to default page 0 without error")
        void testStudentInvalidPageParameterTypeFallsBackSafely() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(0, 10), 1);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("page", "notANumber"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].studentId").value(1L));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentService).getAllStudents(any(), any(), any(), captor.capture());
            assertEquals(0, captor.getValue().getPageNumber());
        }

        @Test
        @DisplayName("Negative page number is safely clamped to page 0")
        void testStudentNegativePageNumber() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(0, 10), 1);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("page", "-1"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentService).getAllStudents(any(), any(), any(), captor.capture());
            assertEquals(0, captor.getValue().getPageNumber());
        }

        @Test
        @DisplayName("Zero or negative page size is safely clamped to fallback size")
        void testStudentZeroOrNegativePageSize() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(0, 10), 1);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("size", "0"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentService).getAllStudents(any(), any(), any(), captor.capture());
            assertTrue(captor.getValue().getPageSize() > 0);
        }

        @Test
        @DisplayName("Excessively large page size is safely clamped to max page size limit")
        void testStudentExcessivelyLargePageSize() throws Exception {
            Page<StudentResponse> page = new PageImpl<>(List.of(sampleStudent()), PageRequest.of(0, 2000), 1);
            when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/students")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("size", "999999"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentService).getAllStudents(any(), any(), any(), captor.capture());
            assertTrue(captor.getValue().getPageSize() <= 2000);
        }
    }

    // =========================================================
    // 2. COMPANY CONTROLLER PAGINATION
    // =========================================================

    @Nested
    @DisplayName("CompanyController Pagination")
    class CompanyPaginationTests {

        private CompanyResponse sampleCompany() {
            CompanyResponse response = new CompanyResponse();
            response.setCompanyId(1L);
            response.setName("TechCorp");
            response.setWebsite("https://techcorp.com");
            response.setContactPerson("Alice Smith");
            response.setEmail("hr@techcorp.com");
            response.setPhone("9876543210");
            response.setAddress("Silicon Valley");
            response.setStatus(CompanyStatus.ACTIVE);
            return response;
        }

        @Test
        @DisplayName("Default pagination uses page 0, size 10, sort companyId ASC")
        void testCompanyDefaultPagination() throws Exception {
            Page<CompanyResponse> page = new PageImpl<>(List.of(sampleCompany()), PageRequest.of(0, 10), 1);
            when(companyService.getAllCompanies(any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/companies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(companyService).getAllCompanies(any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("companyId").getDirection());
        }

        @Test
        @DisplayName("Custom page and sort descending on name")
        void testCompanyCustomPaginationAndSort() throws Exception {
            Page<CompanyResponse> page = new PageImpl<>(List.of(sampleCompany()), PageRequest.of(1, 20), 25);
            when(companyService.getAllCompanies(any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/companies")
                            .param("page", "1")
                            .param("size", "20")
                            .param("sort", "name,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(companyService).getAllCompanies(any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(1, captured.getPageNumber());
            assertEquals(20, captured.getPageSize());
            assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("name").getDirection());
        }

        @Test
        @DisplayName("Invalid sort property triggers 400 Bad Request")
        void testCompanyInvalidSortProperty() throws Exception {
            when(companyService.getAllCompanies(any(), any(), any(Pageable.class)))
                    .thenThrow(new PropertyReferenceException("unsupportedField", TypeInformation.of(Company.class), List.of()));

            mockMvc.perform(get("/api/companies")
                            .param("sort", "unsupportedField,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid sort property 'unsupportedField'"));
        }
    }

    // =========================================================
    // 3. JOB CONTROLLER PAGINATION
    // =========================================================

    @Nested
    @DisplayName("JobController Pagination")
    class JobPaginationTests {

        private JobResponse sampleJob() {
            JobResponse response = new JobResponse();
            response.setJobId(1L);
            response.setCompanyId(1L);
            response.setCompanyName("TechCorp");
            response.setTitle("Software Engineer");
            response.setDescription("Backend role");
            response.setRequiredSkills("Java, Spring Boot");
            response.setEligibilityCriteria("B.Tech CSE/IT");
            response.setPackageAmount(new BigDecimal("1200000.00"));
            response.setJobType("Full-time");
            response.setMinCgpa(new BigDecimal("7.00"));
            response.setDeadline(LocalDate.now().plusMonths(1));
            response.setStatus(JobStatus.ACTIVE);
            return response;
        }

        @Test
        @DisplayName("Default pagination uses page 0, size 10, sort jobId ASC")
        void testJobDefaultPagination() throws Exception {
            Page<JobResponse> page = new PageImpl<>(List.of(sampleJob()), PageRequest.of(0, 10), 1);
            when(jobService.getAllJobs(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/jobs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].jobId").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(jobService).getAllJobs(any(), any(), any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("jobId").getDirection());
        }

        @Test
        @DisplayName("Sorting by packageAmount descending with filters")
        void testJobPaginationWithFiltersAndSort() throws Exception {
            Page<JobResponse> page = new PageImpl<>(List.of(sampleJob()), PageRequest.of(0, 10), 1);
            when(jobService.getAllJobs(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/jobs")
                            .param("title", "Software")
                            .param("status", "ACTIVE")
                            .param("sort", "packageAmount,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(jobService).getAllJobs(any(), any(), any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("packageAmount").getDirection());
        }

        @Test
        @DisplayName("Invalid sort property triggers 400 Bad Request")
        void testJobInvalidSortProperty() throws Exception {
            when(jobService.getAllJobs(any(), any(), any(), any(), any(Pageable.class)))
                    .thenThrow(new PropertyReferenceException("nonExistentSalary", TypeInformation.of(Job.class), List.of()));

            mockMvc.perform(get("/api/jobs")
                            .param("sort", "nonExistentSalary,desc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid sort property 'nonExistentSalary'"));
        }
    }

    // =========================================================
    // 4. APPLICATION CONTROLLER PAGINATION
    // =========================================================

    @Nested
    @DisplayName("ApplicationController Pagination")
    class ApplicationPaginationTests {

        private ApplicationResponse sampleApplication() {
            ApplicationResponse response = new ApplicationResponse();
            response.setApplicationId(1L);
            response.setStudentId(10L);
            response.setStudentName("Jane Doe");
            response.setJobId(20L);
            response.setJobTitle("Software Engineer");
            response.setCompanyName("TechCorp");
            response.setAppliedOn(LocalDateTime.now());
            response.setStatus(ApplicationStatus.APPLIED);
            return response;
        }

        @Test
        @DisplayName("Default pagination uses page 0, size 10, sort applicationId ASC")
        void testApplicationDefaultPagination() throws Exception {
            Page<ApplicationResponse> page = new PageImpl<>(List.of(sampleApplication()), PageRequest.of(0, 10), 1);
            when(applicationService.getAllApplications(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/applications")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].applicationId").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(applicationService).getAllApplications(any(), any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("applicationId").getDirection());
        }

        @Test
        @DisplayName("Custom page and sort descending on appliedOn")
        void testApplicationCustomSort() throws Exception {
            Page<ApplicationResponse> page = new PageImpl<>(List.of(sampleApplication()), PageRequest.of(1, 15), 20);
            when(applicationService.getAllApplications(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/applications")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("page", "1")
                            .param("size", "15")
                            .param("sort", "appliedOn,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(applicationService).getAllApplications(any(), any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(1, captured.getPageNumber());
            assertEquals(15, captured.getPageSize());
            assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("appliedOn").getDirection());
        }

        @Test
        @DisplayName("Invalid sort property triggers 400 Bad Request")
        void testApplicationInvalidSortProperty() throws Exception {
            when(applicationService.getAllApplications(any(), any(), any(), any(Pageable.class)))
                    .thenThrow(new PropertyReferenceException("invalidColumn", TypeInformation.of(Application.class), List.of()));

            mockMvc.perform(get("/api/applications")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("sort", "invalidColumn,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid sort property 'invalidColumn'"));
        }
    }

    // =========================================================
    // 5. INTERVIEW CONTROLLER PAGINATION
    // =========================================================

    @Nested
    @DisplayName("InterviewController Pagination")
    class InterviewPaginationTests {

        private InterviewResponse sampleInterview() {
            InterviewResponse response = new InterviewResponse();
            response.setInterviewId(1L);
            response.setApplicationId(10L);
            response.setRoundNumber(1);
            response.setRoundType("Technical 1");
            response.setScheduledOn(LocalDateTime.now().plusDays(2));
            response.setResult(InterviewResult.PENDING);
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }

        @Test
        @DisplayName("Default pagination uses page 0, size 10, sort interviewId ASC")
        void testInterviewDefaultPagination() throws Exception {
            Page<InterviewResponse> page = new PageImpl<>(List.of(sampleInterview()), PageRequest.of(0, 10), 1);
            when(interviewService.getAllInterviews(any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/interviews")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].interviewId").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(interviewService).getAllInterviews(any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("interviewId").getDirection());
        }

        @Test
        @DisplayName("Custom sort descending on scheduledOn with filters")
        void testInterviewCustomSortWithFilters() throws Exception {
            Page<InterviewResponse> page = new PageImpl<>(List.of(sampleInterview()), PageRequest.of(0, 5), 1);
            when(interviewService.getAllInterviews(any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/interviews")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("applicationId", "10")
                            .param("result", "PENDING")
                            .param("sort", "scheduledOn,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(interviewService).getAllInterviews(any(), any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("scheduledOn").getDirection());
        }

        @Test
        @DisplayName("Invalid sort property triggers 400 Bad Request")
        void testInterviewInvalidSortProperty() throws Exception {
            when(interviewService.getAllInterviews(any(), any(), any(Pageable.class)))
                    .thenThrow(new PropertyReferenceException("badSortField", TypeInformation.of(Interview.class), List.of()));

            mockMvc.perform(get("/api/interviews")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("sort", "badSortField,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid sort property 'badSortField'"));
        }
    }

    // =========================================================
    // 6. PLACEMENT CONTROLLER PAGINATION
    // =========================================================

    @Nested
    @DisplayName("PlacementController Pagination")
    class PlacementPaginationTests {

        private PlacementResponse samplePlacement() {
            PlacementResponse response = new PlacementResponse();
            response.setPlacementId(1L);
            response.setStudentId(10L);
            response.setStudentName("Jane Doe");
            response.setApplicationId(20L);
            response.setJobRole("Software Engineer");
            response.setPackageAmount(new BigDecimal("1200000.00"));
            response.setJoiningDate(LocalDate.now().plusMonths(2));
            response.setStatus(PlacementStatus.CONFIRMED);
            response.setPlacedOn(LocalDateTime.now());
            return response;
        }

        @Test
        @DisplayName("Default pagination uses page 0, size 10, sort placementId ASC")
        void testPlacementDefaultPagination() throws Exception {
            Page<PlacementResponse> page = new PageImpl<>(List.of(samplePlacement()), PageRequest.of(0, 10), 1);
            when(placementService.getAllPlacements(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/placements")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].placementId").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(placementService).getAllPlacements(any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("placementId").getDirection());
        }

        @Test
        @DisplayName("Custom sort descending on packageAmount")
        void testPlacementCustomSort() throws Exception {
            Page<PlacementResponse> page = new PageImpl<>(List.of(samplePlacement()), PageRequest.of(0, 10), 1);
            when(placementService.getAllPlacements(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/placements")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("status", "CONFIRMED")
                            .param("sort", "packageAmount,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(placementService).getAllPlacements(any(), captor.capture());
            Pageable captured = captor.getValue();
            assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("packageAmount").getDirection());
        }

        @Test
        @DisplayName("Invalid sort property triggers 400 Bad Request")
        void testPlacementInvalidSortProperty() throws Exception {
            when(placementService.getAllPlacements(any(), any(Pageable.class)))
                    .thenThrow(new PropertyReferenceException("invalidSalaryColumn", TypeInformation.of(Placement.class), List.of()));

            mockMvc.perform(get("/api/placements")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
                            .param("sort", "invalidSalaryColumn,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid sort property 'invalidSalaryColumn'"));
        }
    }
}
