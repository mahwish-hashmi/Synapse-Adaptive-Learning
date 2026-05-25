# 🧠 SYNAPSE AI – Adaptive Learning Platform

An intelligent learning and assessment platform designed to personalize educational experiences through adaptive quizzes, performance analytics, and AI-driven learning recommendations.

---

## 🚀 Overview

**SYNAPSE AI** is an adaptive learning platform designed to deliver personalized educational experiences through AI-driven analytics and intelligent assessment systems.

The platform continuously analyzes learner performance, topic mastery, response patterns, and progression trends to dynamically:
- personalize quizzes,
- identify weak concepts,
- generate learning recommendations,
- and optimize learning paths.

By combining adaptive assessments, performance analytics, and AI-powered recommendations, SYNAPSE AI transforms traditional quiz systems into an intelligent and scalable learning ecosystem.
---

## ✨ Core Features

### 🧠 Adaptive Quiz System
- Dynamic difficulty adjustment
- Topic-based assessments
- Personalized quiz progression
- Intelligent revision flow

---

### 📊 Weak Topic Analysis
- Topic mastery tracking
- Weakness heatmaps
- Performance analytics
- Learning consistency metrics
- Recommendation generation

Example:
- Arrays → 91%
- Trees → 43%
- Dynamic Programming → 18%

Recommendation:
> “Strengthen recursion fundamentals before progressing into Dynamic Programming.”

---

### 🤖 AI-Powered Learning Assistance
- AI-generated quizzes
- Personalized recommendations
- Intelligent explanation engine
- Adaptive learning suggestions

---

### 🔐 Authentication & Security
- JWT-based authentication
- Role-based authorization
- Secure REST APIs
- Protected routes & endpoints

---

### 📈 Analytics Dashboard
- Progress tracking
- Topic mastery graphs
- Performance trends
- Learning analytics
- Activity insights

---

### 🎮 Gamification
- XP system
- Achievement badges
- Streak tracking
- Leaderboards

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React + Tailwind CSS |
| Backend | Spring Boot |
| Database | PostgreSQL |
| Authentication | JWT + Spring Security |
| ORM | Spring Data JPA / Hibernate |
| AI Integration | OpenAI / Gemini API |
| Caching | Redis |
| Realtime | WebSocket |
| Deployment | Docker |

---

# 🧱 System Architecture

```text
┌──────────────────────────────┐
│        React Frontend        │
│      (Client Interface)      │
└──────────────┬───────────────┘
               │
               │ REST APIs / WebSocket
               ▼
┌──────────────────────────────┐
│      Spring Boot APIs        │
│    (Business Logic Layer)    │
└──────────────┬───────────────┘
               │
 ┌─────────────┼─────────────┐
 ▼             ▼             ▼
Authentication Analytics  AI Engine
   Layer        Engine      Layer
 │               │            │
 └───────────────┼────────────┘
                 ▼
┌──────────────────────────────┐
│ PostgreSQL + Redis Caching   │
│      (Data Persistence)      │
└──────────────────────────────┘
```

---

# 🔄 Adaptive Learning Flow

```text
User Attempts Quiz
        ↓
Performance Data Captured
        ↓
Analytics Engine Evaluates:
- Accuracy
- Weak Topics
- Response Speed
- Learning Consistency
        ↓
Recommendation Engine
        ↓
Personalized Learning Suggestions
        ↓
Adaptive Quiz Generation
```

---

# 🧠 AI Intelligence Layer

## Weak Topic Detection

The analytics engine evaluates:
- response accuracy,
- repeated failures,
- topic consistency,
- performance trends,
- and improvement patterns.

### Example Formula

```math
Mastery Index = 0.4(Accuracy) + 0.3(Consistency) + 0.2(Improvement Rate) + 0.1(Response Confidence)
```

---

## Personalized Learning Path

The platform dynamically generates:
- topic recommendations,
- adaptive revision plans,
- personalized learning sequences,
- and progressive difficulty adjustments.

### Example Learning Path

```text
Beginner:
Basics → Easy Problems → Medium → Advanced

Advanced Learner:
Skip Fundamentals → Hard Problems → Competitive Mode
```

---

# 🔐 Authentication Flow

```text
User Login / Registration
        ↓
JWT Token Generated
        ↓
Token Attached with Requests
        ↓
Backend Validation
        ↓
Authorized API Access
```

---

# 👥 Role-Based Access

```text
Student
 ├── Attempt Quizzes
 ├── View Analytics
 ├── Track Progress
 └── Access Recommendations

Instructor
 ├── Create Assessments
 ├── Manage Topics
 ├── Review Performance
 └── Access Analytics

Admin
 ├── Manage Platform
 ├── User Management
 ├── System Monitoring
 └── Analytics Overview
```

---

# 📊 Dashboard Features

The platform provides:
- topic mastery insights,
- weakness analysis,
- progress graphs,
- learning trends,
- recommendation cards,
- and performance analytics.

---

# 📁 Project Structure

```text
synapse-ai/
│
├── backend/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── exception/
│   ├── repository/
│   ├── security/
│   ├── service/
│   ├── analytics/
│   ├── recommendation/
│   └── ai/
│
├── frontend/
│   ├── components/
│   ├── pages/
│   ├── services/
│   ├── hooks/
│   ├── context/
│   └── assets/
```

---

# ⚙️ Installation & Setup

## 🔹 Clone Repository

```bash
git clone <repository-url>
cd synapse-ai
```

---

## 🔹 Backend Setup

```bash
cd backend
mvn spring-boot:run
```

---

## 🔹 Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

---

# 🔮 Future Enhancements

- AI Mentor Chatbot
- Real-Time Quiz Battles
- Live Leaderboards
- Predictive Performance Insights
- Resume-Based Interview Assessments
- Microservices Architecture
- Kafka Event Streaming
- Kubernetes Deployment

---

# 👩‍💻 Author

**Mahwish Hashmi**  
Full-Stack Developer | Backend Engineering Enthusiast

---

<div align="center">

### SYNAPSE AI
Adaptive Learning • Performance Intelligence • Personalized Education

</div>

---

## Phase 4 — Personalized Learning Path + Adaptive Quiz Engine

### Bug fixes from Phases 1–3
- Deleted `Question.java` (root package duplicate — caused Hibernate startup conflict)
- Deleted `dao/QuestionDao.java` (stale old file — caused compilation error)
- Fixed `QuizService.java` — Phase 3 `WeakTopicDetectionService` was not injected or triggered

### How Phase 4 works

After every quiz submission, three things now run automatically:
```
submitQuiz() →
  1. weakTopicDetectionService.analyzeAfterQuiz()  [Phase 3]
  2. learningPathService.updateAfterQuiz()          [Phase 4]
     ├── updateRevisionSchedule()  ← spaced repetition
     └── regenerateLearningPath()  ← topic sequence + adaptive recommendation
```

### Spaced repetition intervals
| Session result | Next revision |
|----------------|--------------|
| Score >= 60% (pass) | interval × 2 (max 30 days) |
| Score < 60% (fail) | 1 day (review tomorrow) |

### Learning Path APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/learning-path | Full personalized learning path |
| GET | /api/v1/learning-path/user/{id} | Admin: view any student's path |

### Example response
```json
{
  "recommendedNextTopic": "Data Structures",
  "recommendedDifficulty": "Easy",
  "learningPhase": "DEVELOPMENT",
  "studyPlanSummary": "Data Structures needs focused practice. Build fundamentals before Medium.",
  "topicSequence": ["Data Structures", "Java", "Spring Boot"],
  "dueForRevision": [],
  "nextQuizRecommendation": {
    "category": "Data Structures",
    "difficulty": "Easy",
    "questionCount": 10,
    "quizType": "PROGRESSION",
    "reason": "Data Structures needs focused practice. Starting with Easy difficulty."
  },
  "estimatedCompletionDays": 14
}
```

---

## Phase 5 — AI Integration + Gamification

### After every quiz, this full pipeline now runs automatically
```
submitQuiz() →
  Phase 3: weakTopicDetectionService.analyzeAfterQuiz()
  Phase 4: learningPathService.updateAfterQuiz()
  Phase 5: gamificationService.processQuizCompletion()
```

### AI Mentor APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/ai/insight | LLM-powered personalised performance analysis |
| POST | /api/v1/ai/chat | Chat with AI Mentor (context-aware) |

> **No OpenAI key?** The app works without one. `aiAvailable: false` is returned and the rule-based engine from Phase 3 provides the insight instead. Set `OPENAI_API_KEY` in your `.env` to enable full AI.

### Gamification APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/gamification/profile | XP, level, streak, all badges |
| GET | /api/v1/gamification/leaderboard | Top 10 + your rank |

### XP System
| Action | XP |
|--------|----|
| Complete any quiz | +10 XP |
| Each correct answer | +2 XP |
| Daily streak bonus | +5 XP |
| Badge earned | +bonus XP (varies) |

### Levels
| Level | Name | XP Required |
|-------|------|-------------|
| 1 | Beginner | 0 |
| 2 | Learner | 100 |
| 3 | Scholar | 250 |
| 4 | Expert | 500 |
| 5 | Master | 1000 |

### Badges (seeded automatically)
| Badge | Condition |
|-------|-----------|
| 🎯 First Step | Complete first quiz |
| 📝 Quiz Starter | Complete 5 quizzes |
| 🔥 Quiz Enthusiast | Complete 25 quizzes |
| 🏆 Quiz Master | Complete 100 quizzes |
| 🔥 On Fire | 3-day streak |
| ⚡ Consistent Learner | 7-day streak |
| 💎 Unstoppable | 30-day streak |
| 💯 Perfect Score | Score 100% on any quiz |
| 🎓 Topic Master | 75%+ mastery in any topic |
