package com.placementmanagementsystem.dto;

public class SkillResponse {

    private Long skillId;
    private String name;

    public SkillResponse() {
    }

    public SkillResponse(Long skillId, String name) {
        this.skillId = skillId;
        this.name = name;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
