package com.telusko.quizapp.service;

import com.telusko.quizapp.entity.Question;
import com.telusko.quizapp.exception.ResourceNotFoundException;
import com.telusko.quizapp.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    // Any authenticated user can view questions
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public Question getQuestionById(Integer id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", id));
    }

    public List<Question> getQuestionsByCategory(String category) {
        return questionRepository.findByCategory(category);
    }

    public List<Question> getQuestionsByCategoryAndDifficulty(String category, String difficulty) {
        return questionRepository.findByCategoryAndDifficultyLevel(category, difficulty);
    }

    // Only ADMIN can create/edit/delete questions
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    public Question addQuestion(Question question) {
        Question saved = questionRepository.save(question);
        log.info("Question added: id={}, category={}", saved.getId(), saved.getCategory());
        return saved;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    public Question updateQuestion(Integer id, Question updated) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", id));

        existing.setQuestionTitle(updated.getQuestionTitle());
        existing.setOption1(updated.getOption1());
        existing.setOption2(updated.getOption2());
        existing.setOption3(updated.getOption3());
        existing.setOption4(updated.getOption4());
        existing.setRightAnswer(updated.getRightAnswer());
        existing.setDifficultyLevel(updated.getDifficultyLevel());
        existing.setCategory(updated.getCategory());

        return questionRepository.save(existing);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    public void deleteQuestion(Integer id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Question", "id", id);
        }
        questionRepository.deleteById(id);
        log.info("Question deleted: id={}", id);
    }
}
