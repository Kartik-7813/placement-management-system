# Placement Management System — Project Specification

## 1. Project Overview

The Placement Management System is a Spring Boot backend application designed to manage college placement activities.

The system manages:

- Students
- Companies
- Job openings
- Applications
- Interviews
- Placements
- Skills
- Dashboard statistics

The core workflow is:

Company registers
→ Job opening is created
→ Student views job
→ Student applies
→ Application is reviewed
→ Student is shortlisted
→ Interview is scheduled
→ Interview result is recorded
→ Student is selected
→ Placement record is created

---

## 2. Technology Stack

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- JPA
- Hibernate as the JPA implementation
- MySQL
- Maven
- REST APIs
- Postman
- Git/GitHub

Spring Data JPA must be used for database access.

Do NOT use:
- Hibernate Session
- Hibernate SessionFactory
- manual Hibernate DAO implementation

---

## 3. Architecture

The application must follow:

Client / Postman
        ↓
REST Controller
        ↓
DTO
        ↓
Service
        ↓
Repository
        ↓
Spring Data JPA
        ↓
JPA / Hibernate
        ↓
MySQL

Controllers must handle HTTP requests.

Services must contain business logic.

Repositories must handle database access.

DTOs must be used for API request/response data.

Entities represent database tables.

---

## 4. Database Tables

The database will contain these tables:

1. students
2. companies
3. jobs
4. applications
5. interviews
6. placements
7. skills
8. student_skills

---

## 5. Students Table

students

- student_id BIGINT PRIMARY KEY
- roll_number VARCHAR(50) UNIQUE
- name VARCHAR(100)
- email VARCHAR(100) UNIQUE
- phone VARCHAR(15)
- dob DATE
- gender VARCHAR(20)
- department VARCHAR(100)
- cgpa DECIMAL(4,2)
- passing_year INT
- address VARCHAR(255)
- status VARCHAR(20)

---

## 6. Companies Table

companies

- company_id BIGINT PRIMARY KEY
- name VARCHAR(150)
- website VARCHAR(200)
- contact_person VARCHAR(100)
- email VARCHAR(100)
- phone VARCHAR(15)
- address VARCHAR(255)
- status VARCHAR(20)

---

## 7. Jobs Table

jobs

- job_id BIGINT PRIMARY KEY
- company_id BIGINT FOREIGN KEY
- title VARCHAR(150)
- description TEXT
- eligibility_criteria TEXT
- required_skills TEXT
- min_cgpa DECIMAL(4,2)
- package DECIMAL(12,2)
- job_type VARCHAR(50)
- deadline DATE
- status VARCHAR(20)

---

## 8. Applications Table

applications

- application_id BIGINT PRIMARY KEY
- student_id BIGINT FOREIGN KEY
- job_id BIGINT FOREIGN KEY
- applied_on TIMESTAMP
- status VARCHAR(30)
- resume_url VARCHAR(255)
- remarks TEXT

Database constraint:

UNIQUE(student_id, job_id)

A student cannot apply to the same job more than once.

---

## 9. Interviews Table

interviews

- interview_id BIGINT PRIMARY KEY
- application_id BIGINT FOREIGN KEY
- round_number INT
- round_type VARCHAR(50)
- scheduled_on DATETIME
- interviewer VARCHAR(150)
- feedback TEXT
- result VARCHAR(30)
- created_at TIMESTAMP

---

## 10. Placements Table

placements

- placement_id BIGINT PRIMARY KEY
- student_id BIGINT FOREIGN KEY UNIQUE
- application_id BIGINT FOREIGN KEY UNIQUE
- job_role VARCHAR(150)
- package DECIMAL(12,2)
- joining_date DATE
- status VARCHAR(30)
- placed_on TIMESTAMP

A student can have at most one placement.

---

## 11. Skills Table

skills

- skill_id BIGINT PRIMARY KEY
- name VARCHAR(100) UNIQUE

---

## 12. Student Skills Table

student_skills

- student_id BIGINT FOREIGN KEY
- skill_id BIGINT FOREIGN KEY

PRIMARY KEY(student_id, skill_id)

This implements:

Student N : N Skill

---

## 13. Entity Relationships

Company 1 : N Job

Student 1 : N Application

Job 1 : N Application

Application 1 : N Interview

Student 1 : 0..1 Placement

Application 1 : 0..1 Placement

Student N : N Skill through student_skills

---

## 14. Main API Resources

Students:

POST   /api/students
GET    /api/students
GET    /api/students/{id}
PUT    /api/students/{id}
DELETE /api/students/{id}

Companies:

POST   /api/companies
GET    /api/companies
GET    /api/companies/{id}
PUT    /api/companies/{id}
DELETE /api/companies/{id}

Jobs:

POST   /api/jobs
GET    /api/jobs
GET    /api/jobs/{id}
PUT    /api/jobs/{id}
DELETE /api/jobs/{id}

Applications:

POST /api/applications
GET  /api/applications
GET  /api/applications/{id}
PUT  /api/applications/{id}

Interviews:

POST /api/interviews
GET  /api/interviews
PUT  /api/interviews/{id}

Placements:

POST /api/placements
GET  /api/placements

Additional useful APIs may be added when required by the implementation.

---

## 15. Business Rules

1. Student name cannot be empty.
2. Student email must be valid.
3. Student email must be unique.
4. Student roll number must be unique.
5. CGPA must be valid.
6. Student must exist before applying for a job.
7. Job must exist before applying.
8. Student must satisfy job eligibility.
9. Student cannot apply to the same job more than once.
10. Inactive jobs should not accept applications.
11. Applications can be shortlisted or rejected.
12. An application can have multiple interview rounds.
13. A student can have at most one placement.
14. Placement should be associated with the successful application.
15. Company/job/student not found must produce appropriate exceptions.

---

## 16. Validation and Exception Handling

Use Jakarta Bean Validation.

Implement appropriate custom exceptions for:

- Student not found
- Company not found
- Job not found
- Application not found
- Interview not found
- Placement not found
- Skill not found
- Duplicate student
- Application already submitted
- Student not eligible
- Invalid status

Use a global exception handler with:

@RestControllerAdvice

Return appropriate HTTP status codes.

---

## 17. Required Features

The project must support:

- CRUD
- Search
- Filtering
- Sorting
- Pagination
- Validation
- Exception handling
- Proper HTTP status codes
- REST APIs
- JPA relationships
- Dashboard statistics

---

## 18. Dashboard

Endpoint:

GET /api/dashboard

Dashboard should provide:

- total students
- total companies
- active jobs
- total applications
- shortlisted students
- selected students

Do not create a dashboard database table.

Use aggregate queries.

---

## 19. Package Structure

com.placementmanagementsystem

├── controller
├── service
├── repository
├── entity
├── dto
└── exception

---

## 20. Project Scope

This is a 2-day academic project.

Keep the implementation simple, clean and understandable.

Do NOT implement these unless explicitly requested:

- Spring Security
- JWT
- role-based authentication
- email notifications
- resume file upload
- AI resume analysis
- job recommendation system
- aptitude tests
- placement prediction
- React frontend
- cloud deployment
- microservices
- Docker
- Kubernetes
- Redis
- Kafka
- Elasticsearch

These are outside the current implementation scope.

---

## 21. Implementation Order

Implement the project strictly in this order:

Step 1 — Entities + JPA Relationships

Step 2 — Spring Data JPA Repositories

Step 3 — DTOs

Step 4 — Student Module

Step 5 — Company Module

Step 6 — Job Module

Step 7 — Application Workflow

Step 8 — Interview Workflow

Step 9 — Placement Workflow

Step 10 — Dashboard

Step 11 — Validation + Exception Handling

Step 12 — Postman Testing

Do not implement multiple steps at once.

After completing each step:

1. Build the project.
2. Test the implementation.
3. Review for errors.
4. Stop and wait for the next instruction.