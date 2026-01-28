package com.telusko.quizapp.service;

import com.telusko.quizapp.Question;
import com.telusko.quizapp.dao.QuestionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    @Autowired
    QuestionDao questionDao;

    public List<Question> getAllQuestions() {
        return questionDao.findAll();
    }

    public List<Question> getQuestionByCategory(String category) {
        return questionDao.findByCategory(category);
    }

    public String addQuestion(Question question) {
        questionDao.save(question);
        return "Successfully added";
    }

    public String deleteQuestion(Integer id) {
        questionDao.deleteById(id);
        return "Deleted";
    }


    public Question updateQuestion(Integer id, Question question) {

        Question existingQuestion = questionDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // update fields
        existingQuestion.setQuestionTitle(question.getQuestionTitle());
        existingQuestion.setOption1(question.getOption1());
        existingQuestion.setOption2(question.getOption2());
        existingQuestion.setOption3(question.getOption3());
        existingQuestion.setOption4(question.getOption4());
        existingQuestion.setRightAnswer(question.getRightAnswer());
        existingQuestion.setDifficultyLevel(question.getDifficultyLevel());
        existingQuestion.setCategory(question.getCategory());

        return questionDao.save(existingQuestion);
    }
}
