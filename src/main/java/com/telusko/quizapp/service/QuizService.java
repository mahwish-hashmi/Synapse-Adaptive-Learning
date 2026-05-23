package com.telusko.quizapp.service;

import com.telusko.quizapp.dto.request.QuizStartRequest;
import com.telusko.quizapp.dto.request.QuizSubmitRequest;
import com.telusko.quizapp.dto.response.QuizHistoryResponse;
import com.telusko.quizapp.dto.response.QuizResultResponse;
import com.telusko.quizapp.dto.response.QuizStartResponse;
import com.telusko.quizapp.entity.Question;
import com.telusko.quizapp.entity.QuestionAttempt;
import com.telusko.quizapp.entity.QuizAttempt;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.exception.ResourceNotFoundException;
import com.telusko.quizapp.repository.QuestionAttemptRepository;
import com.telusko.quizapp.repository.QuestionRepository;
import com.telusko.quizapp.repository.QuizAttemptRepository;
import com.telusko.quizapp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final QuestionRepository questionRepository;
    private final SecurityUtils securityUtils;

    // ─────────────────────────────────────────────────────────────────────────
    // START QUIZ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public QuizStartResponse startQuiz(QuizStartRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Fetch questions — filtered by category and optionally difficulty
        List<Question> questions = fetchQuestions(
                request.getCategory(),
                request.getDifficultyLevel(),
                request.getNumberOfQuestions()
        );

        if (questions.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No questions found for category: " + request.getCategory()
                    + (request.getDifficultyLevel() != null
                       ? " and difficulty: " + request.getDifficultyLevel() : ""));
        }

        // Create a new quiz attempt with IN_PROGRESS status
        QuizAttempt attempt = QuizAttempt.builder()
                .user(currentUser)
                .category(request.getCategory())
                .status(QuizAttempt.Status.IN_PROGRESS)
                .totalQuestions(questions.size())
                .build();

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        log.info("Quiz started: userId={}, category={}, attemptId={}",
                currentUser.getId(), request.getCategory(), saved.getId());

        // Map questions to response — NEVER include rightAnswer
        List<QuizStartResponse.QuestionResponse> questionResponses = questions.stream()
                .map(q -> QuizStartResponse.QuestionResponse.builder()
                        .id(q.getId())
                        .questionTitle(q.getQuestionTitle())
                        .option1(q.getOption1())
                        .option2(q.getOption2())
                        .option3(q.getOption3())
                        .option4(q.getOption4())
                        .difficultyLevel(q.getDifficultyLevel())
                        .category(q.getCategory())
                        .build())
                .collect(Collectors.toList());

        return QuizStartResponse.builder()
                .quizAttemptId(saved.getId())
                .category(request.getCategory())
                .totalQuestions(questions.size())
                .questions(questionResponses)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT QUIZ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public QuizResultResponse submitQuiz(QuizSubmitRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Load the quiz attempt — verify it belongs to this user
        QuizAttempt quizAttempt = quizAttemptRepository
                .findById(request.getQuizAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QuizAttempt", "id", request.getQuizAttemptId()));

        // Security check: users can only submit their own quizzes
        if (!quizAttempt.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to submit this quiz");
        }

        // Prevent double submission
        if (quizAttempt.getStatus() == QuizAttempt.Status.COMPLETED) {
            throw new RuntimeException("This quiz has already been submitted");
        }

        // Load questions for the answers provided
        Map<Integer, Question> questionsMap = questionRepository
                .findAllById(request.getAnswers().keySet())
                .stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int correct = 0;
        int incorrect = 0;
        List<QuizResultResponse.QuestionResultDetail> details =
                new java.util.ArrayList<>();

        // Process each answer
        for (Map.Entry<Integer, String> entry : request.getAnswers().entrySet()) {
            Integer questionId = entry.getKey();
            String selectedAnswer = entry.getValue();

            Question question = questionsMap.get(questionId);
            if (question == null) continue;

            boolean isCorrect = question.getRightAnswer().equals(selectedAnswer);
            if (isCorrect) correct++;
            else incorrect++;

            // How many times has this user attempted this specific question before?
            int previousAttempts = questionAttemptRepository
                    .countByUserIdAndQuestionId(currentUser.getId(), questionId);

            // Time spent on this question (from client, or 0 if not provided)
            Long timeTaken = request.getTimingsPerQuestion() != null
                    ? request.getTimingsPerQuestion().getOrDefault(questionId, 0L)
                    : 0L;

            // Save per-question result — this row feeds Phase 3 AI
            QuestionAttempt qa = QuestionAttempt.builder()
                    .quizAttempt(quizAttempt)
                    .user(currentUser)
                    .question(question)
                    .selectedAnswer(selectedAnswer)
                    .correctAnswer(question.getRightAnswer())
                    .isCorrect(isCorrect)
                    .category(question.getCategory())
                    .difficultyLevel(question.getDifficultyLevel())
                    .timeTakenSeconds(timeTaken)
                    .attemptNumber(previousAttempts + 1)
                    .build();

            questionAttemptRepository.save(qa);

            details.add(QuizResultResponse.QuestionResultDetail.builder()
                    .questionId(questionId)
                    .questionTitle(question.getQuestionTitle())
                    .selectedAnswer(selectedAnswer)
                    .correctAnswer(question.getRightAnswer())
                    .isCorrect(isCorrect)
                    .timeTakenSeconds(timeTaken)
                    .build());
        }

        // Calculate score
        int total = request.getAnswers().size();
        double scorePercentage = total > 0 ? ((double) correct / total) * 100.0 : 0.0;

        // Total quiz duration in seconds
        long timeTakenSeconds = ChronoUnit.SECONDS.between(
                quizAttempt.getStartedAt(), LocalDateTime.now());

        // Update quiz attempt with results
        quizAttempt.setStatus(QuizAttempt.Status.COMPLETED);
        quizAttempt.setCorrectAnswers(correct);
        quizAttempt.setIncorrectAnswers(incorrect);
        quizAttempt.setTotalQuestions(total);
        quizAttempt.setScorePercentage(scorePercentage);
        quizAttempt.setTimeTakenSeconds(timeTakenSeconds);
        quizAttempt.setCompletedAt(LocalDateTime.now());
        quizAttemptRepository.save(quizAttempt);

        log.info("Quiz submitted: userId={}, attemptId={}, score={}%",
                currentUser.getId(), quizAttempt.getId(), scorePercentage);

        return QuizResultResponse.builder()
                .quizAttemptId(quizAttempt.getId())
                .category(quizAttempt.getCategory())
                .totalQuestions(total)
                .correctAnswers(correct)
                .incorrectAnswers(incorrect)
                .scorePercentage(Math.round(scorePercentage * 10.0) / 10.0)
                .timeTakenSeconds(timeTakenSeconds)
                .performanceLabel(getPerformanceLabel(scorePercentage))
                .message(getEncouragementMessage(scorePercentage))
                .questionResults(details)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET RESULT (view a past attempt)
    // ─────────────────────────────────────────────────────────────────────────

    public QuizResultResponse getResult(Long attemptId) {
        User currentUser = securityUtils.getCurrentUser();

        QuizAttempt attempt = quizAttemptRepository
                .findByIdWithQuestionAttempts(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QuizAttempt", "id", attemptId));

        if (!attempt.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<QuizResultResponse.QuestionResultDetail> details = attempt
                .getQuestionAttempts().stream()
                .map(qa -> QuizResultResponse.QuestionResultDetail.builder()
                        .questionId(qa.getQuestion().getId())
                        .questionTitle(qa.getQuestion().getQuestionTitle())
                        .selectedAnswer(qa.getSelectedAnswer())
                        .correctAnswer(qa.getCorrectAnswer())
                        .isCorrect(qa.getIsCorrect())
                        .timeTakenSeconds(qa.getTimeTakenSeconds() != null
                                ? qa.getTimeTakenSeconds() : 0)
                        .build())
                .collect(Collectors.toList());

        return QuizResultResponse.builder()
                .quizAttemptId(attempt.getId())
                .category(attempt.getCategory())
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .incorrectAnswers(attempt.getIncorrectAnswers())
                .scorePercentage(attempt.getScorePercentage())
                .timeTakenSeconds(attempt.getTimeTakenSeconds() != null
                        ? attempt.getTimeTakenSeconds() : 0)
                .performanceLabel(getPerformanceLabel(attempt.getScorePercentage()))
                .message(getEncouragementMessage(attempt.getScorePercentage()))
                .questionResults(details)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    public QuizHistoryResponse getHistory() {
        User currentUser = securityUtils.getCurrentUser();

        List<QuizAttempt> attempts = quizAttemptRepository
                .findByUserIdOrderByStartedAtDesc(currentUser.getId());

        Double avg = quizAttemptRepository.findAverageScoreByUserId(currentUser.getId());

        List<QuizHistoryResponse.AttemptSummary> summaries = attempts.stream()
                .map(a -> QuizHistoryResponse.AttemptSummary.builder()
                        .attemptId(a.getId())
                        .category(a.getCategory())
                        .totalQuestions(a.getTotalQuestions() != null
                                ? a.getTotalQuestions() : 0)
                        .correctAnswers(a.getCorrectAnswers() != null
                                ? a.getCorrectAnswers() : 0)
                        .scorePercentage(a.getScorePercentage() != null
                                ? a.getScorePercentage() : 0.0)
                        .timeTakenSeconds(a.getTimeTakenSeconds() != null
                                ? a.getTimeTakenSeconds() : 0)
                        .status(a.getStatus().name())
                        .completedAt(a.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        return QuizHistoryResponse.builder()
                .totalAttempts(attempts.size())
                .averageScore(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .attempts(summaries)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private List<Question> fetchQuestions(String category, String difficulty, int count) {
        if (difficulty != null && !difficulty.isBlank()) {
            return questionRepository.findRandomByCategoryAndDifficulty(
                    category, difficulty, count);
        }
        return questionRepository.findRandomByCategory(category, count);
    }

    private String getPerformanceLabel(double score) {
        if (score >= 90) return "Excellent";
        if (score >= 75) return "Good";
        if (score >= 50) return "Needs Practice";
        return "Keep Trying";
    }

    private String getEncouragementMessage(double score) {
        if (score >= 90) return "Outstanding! You have a strong grasp of this topic.";
        if (score >= 75) return "Great work! A little more practice and you will master this.";
        if (score >= 50) return "Good effort. Review the questions you got wrong and try again.";
        return "Don't give up. Focus on the fundamentals and attempt this topic again.";
    }
}
