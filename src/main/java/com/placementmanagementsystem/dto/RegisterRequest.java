package com.placementmanagementsystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegisterRequest {

    // Account Credentials
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;

    // Student Profile Information
    @NotBlank(message = "Roll number is required")
    @Size(max = 50, message = "Roll number cannot exceed 50 characters")
    private String rollNumber;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Size(max = 20, message = "Gender cannot exceed 20 characters")
    private String gender;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @DecimalMin(value = "0.00", message = "CGPA must be at least 0.0")
    @DecimalMax(value = "10.00", message = "CGPA cannot exceed 10.0")
    @Digits(integer = 2, fraction = 2, message = "CGPA must have at most 2 integer digits and 2 decimal places")
    private BigDecimal cgpa;

    @Min(value = 1900, message = "Passing year must be valid")
    @Max(value = 2100, message = "Passing year must be valid")
    private Integer passingYear;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String rollNumber, String name) {
        this.email = email;
        this.password = password;
        this.rollNumber = rollNumber;
        this.name = name;
    }

    public RegisterRequest(
            String email,
            String password,
            String rollNumber,
            String name,
            String phone,
            LocalDate dob,
            String gender,
            String department,
            BigDecimal cgpa,
            Integer passingYear,
            String address
    ) {
        this.email = email;
        this.password = password;
        this.rollNumber = rollNumber;
        this.name = name;
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.department = department;
        this.cgpa = cgpa;
        this.passingYear = passingYear;
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
