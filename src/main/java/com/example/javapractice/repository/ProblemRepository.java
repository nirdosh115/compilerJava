package com.example.javapractice.repository;

import com.example.javapractice.model.Problem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
	List<Problem> findByDifficulty(String difficulty);

}
