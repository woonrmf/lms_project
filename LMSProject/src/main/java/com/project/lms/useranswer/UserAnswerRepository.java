package com.project.lms.useranswer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.lms.attempt.Attempt;
import com.project.lms.quiz.Quiz;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Integer> {
	List<UserAnswer> findByAttempt (Attempt attempt);
	
	List<UserAnswer> findByAttempt_Quiz (Quiz quiz); //퀴즈의 기록 확인
}
