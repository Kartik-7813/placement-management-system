package com.placementmanagementsystem.repository;

import com.placementmanagementsystem.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByName(String name);

    boolean existsByName(String name);

    List<Skill> findByNameContainingIgnoreCase(String name);

    Page<Skill> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
