package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.CompanyStatus;

public class CompanyResponse {

    private Long companyId;
    private String name;
    private String website;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private CompanyStatus status;

    public CompanyResponse() {
    }

    public CompanyResponse(Long companyId, String name, String website, String contactPerson, String email, String phone, String address, CompanyStatus status) {
        this.companyId = companyId;
        this.name = name;
        this.website = website;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.status = status;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = status;
    }
}
