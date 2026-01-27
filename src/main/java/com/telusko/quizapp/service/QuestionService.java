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
                .orElseGet(() -> {
                    Question q = new Question();
                    q.setId(id);
                    return q;
                });

        existingQuestion.setQuestionTitle(newQuestion.getQuestionTitle());
        existingQuestion.setOption1(newQuestion.getOption1());
        existingQuestion.setOption2(newQuestion.getOption2());
        existingQuestion.setOption3(newQuestion.getOption3());
        existingQuestion.setOption4(newQuestion.getOption4());
        existingQuestion.setRightAnswer(newQuestion.getRightAnswer());
        existingQuestion.setDifficultyLevel(newQuestion.getDifficultyLevel());
        existingQuestion.setCategory(newQuestion.getCategory());

        return questionDao.save(existingQuestion);
    }
}
