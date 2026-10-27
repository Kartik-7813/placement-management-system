package com.placementmanagementsystem.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityIdentityTest {

    @Test
    @DisplayName("Skill equals and hashCode should match when name is identical across instances")
    void testSkillEqualsAndHashCode() {
        Skill skill1 = new Skill(1L, "Java");
        Skill skill2 = new Skill(2L, "Java");

        assertEquals(skill1, skill2);
        assertEquals(skill1.hashCode(), skill2.hashCode());
    }

    @Test
    @DisplayName("Skill instances with different names should not be equal")
    void testSkillDifferentNameNotEqual() {
        Skill skill1 = new Skill(1L, "Java");
        Skill skill2 = new Skill(1L, "Python");

        assertNotEquals(skill1, skill2);
    }

    @Test
    @DisplayName("HashSet should maintain uniqueness of Skills by natural key name")
    void testSkillInHashSet() {
        Set<Skill> skills = new HashSet<>();
        Skill skill1 = new Skill(1L, "Spring Boot");
        Skill skill2 = new Skill(2L, "Spring Boot");

        skills.add(skill1);
        skills.add(skill2);

        assertEquals(1, skills.size());
        assertTrue(skills.contains(new Skill(99L, "Spring Boot")));

        // Verify removing with an equivalent detached instance works
        boolean removed = skills.remove(new Skill(100L, "Spring Boot"));
        assertTrue(removed);
        assertEquals(0, skills.size());
    }

    @Test
    @DisplayName("Skill equals handles nulls, different types, and identity correctly")
    void testSkillEdgeCases() {
        Skill skill = new Skill(1L, "SQL");

        assertTrue(skill.equals(skill));
        assertFalse(skill.equals(null));
        assertFalse(skill.equals("SQL"));

        Skill unsaved1 = new Skill(null);
        Skill unsaved2 = new Skill(null);
        assertNotEquals(unsaved1, unsaved2);
    }

    @Test
    @DisplayName("Student equals and hashCode should match when rollNumber is identical")
    void testStudentEqualsAndHashCode() {
        Student s1 = new Student();
        s1.setStudentId(1L);
        s1.setRollNumber("CS2026001");
        s1.setName("Alice");

        Student s2 = new Student();
        s2.setStudentId(2L);
        s2.setRollNumber("CS2026001");
        s2.setName("Alice Updated");

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    @DisplayName("Student instances with different roll numbers should not be equal")
    void testStudentDifferentRollNumberNotEqual() {
        Student s1 = new Student();
        s1.setStudentId(1L);
        s1.setRollNumber("CS2026001");

        Student s2 = new Student();
        s2.setStudentId(1L);
        s2.setRollNumber("CS2026002");

        assertNotEquals(s1, s2);
    }

    @Test
    @DisplayName("HashSet should maintain uniqueness of Students by rollNumber")
    void testStudentInHashSet() {
        Set<Student> students = new HashSet<>();
        Student s1 = new Student();
        s1.setRollNumber("CS2026001");
        Student s2 = new Student();
        s2.setRollNumber("CS2026001");

        students.add(s1);
        students.add(s2);

        assertEquals(1, students.size());
    }
}
