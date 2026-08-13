package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.StudentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StudentResponse {

    private Long studentId;
    private String rollNumber;
    private String name;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String department;
    private BigDecimal cgpa;
    private Integer passingYear;
    private String address;
    private StudentStatus status;

    public StudentResponse() {
    }

    public StudentResponse(Long studentId, String rollNumber, String name, String email, String phone, LocalDate dob, String gender, String department, BigDecimal cgpa, Integer passingYear, String address, StudentStatus status) {
        this.studentId = studentId;
        this.rollNumber = rollNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.department = department;
        this.cgpa = cgpa;
        this.passingYear = passingYear;
        this.address = address;
        this.status = status;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public BigDecimal getCgpa() {
        return cgpa;
    }

    public void setCgpa(BigDecimal cgpa) {
        this.cgpa = cgpa;
    }

    public Integer getPassingYear() {
        return passingYear;
    }

    public void setPassingYear(Integer passingYear) {
        this.passingYear = passingYear;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }
}
