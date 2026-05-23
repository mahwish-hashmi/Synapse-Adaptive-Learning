package com.telusko.quizapp.config;

import com.telusko.quizapp.entity.Question;
import com.telusko.quizapp.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds sample questions on startup if the table is empty.
 * This lets you test the quiz engine immediately without manually inserting data.
 * Safe to keep in production — only runs when table is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        if (questionRepository.count() > 0) {
            log.info("Questions already seeded — skipping.");
            return;
        }

        List<Question> questions = List.of(
            // ── Java — Easy ─────────────────────────────────────────────────
            Question.builder()
                .questionTitle("Which keyword is used to create a class in Java?")
                .option1("class").option2("Class").option3("new").option4("create")
                .rightAnswer("class").difficultyLevel("Easy").category("Java").build(),

            Question.builder()
                .questionTitle("What is the default value of an int in Java?")
                .option1("null").option2("0").option3("1").option4("undefined")
                .rightAnswer("0").difficultyLevel("Easy").category("Java").build(),

            Question.builder()
                .questionTitle("Which of these is NOT a primitive type in Java?")
                .option1("int").option2("boolean").option3("String").option4("char")
                .rightAnswer("String").difficultyLevel("Easy").category("Java").build(),

            Question.builder()
                .questionTitle("What does JVM stand for?")
                .option1("Java Virtual Machine").option2("Java Variable Manager")
                .option3("Java Verified Module").option4("Java Value Manager")
                .rightAnswer("Java Virtual Machine").difficultyLevel("Easy").category("Java").build(),

            Question.builder()
                .questionTitle("Which method is the entry point of a Java program?")
                .option1("start()").option2("run()").option3("main()").option4("init()")
                .rightAnswer("main()").difficultyLevel("Easy").category("Java").build(),

            // ── Java — Medium ────────────────────────────────────────────────
            Question.builder()
                .questionTitle("What is the difference between == and .equals() in Java?")
                .option1("No difference")
                .option2("== compares references, .equals() compares content")
                .option3("== compares content, .equals() compares references")
                .option4(".equals() only works on primitives")
                .rightAnswer("== compares references, .equals() compares content")
                .difficultyLevel("Medium").category("Java").build(),

            Question.builder()
                .questionTitle("Which collection does NOT allow duplicate elements?")
                .option1("ArrayList").option2("LinkedList").option3("HashSet").option4("Vector")
                .rightAnswer("HashSet").difficultyLevel("Medium").category("Java").build(),

            Question.builder()
                .questionTitle("What is autoboxing in Java?")
                .option1("Converting a class to an interface")
                .option2("Automatic conversion between primitive types and their wrapper classes")
                .option3("A design pattern")
                .option4("Memory allocation technique")
                .rightAnswer("Automatic conversion between primitive types and their wrapper classes")
                .difficultyLevel("Medium").category("Java").build(),

            // ── Java — Hard ──────────────────────────────────────────────────
            Question.builder()
                .questionTitle("What is the output of: System.out.println(1 + 2 + \"3\")?")
                .option1("123").option2("33").option3("6").option4("Compilation error")
                .rightAnswer("33").difficultyLevel("Hard").category("Java").build(),

            Question.builder()
                .questionTitle("Which interface must be implemented to use an object as a HashMap key safely?")
                .option1("Comparable").option2("Serializable")
                .option3("hashCode and equals from Object").option4("Cloneable")
                .rightAnswer("hashCode and equals from Object")
                .difficultyLevel("Hard").category("Java").build(),

            // ── Data Structures — Easy ───────────────────────────────────────
            Question.builder()
                .questionTitle("What is the time complexity of accessing an element in an array by index?")
                .option1("O(n)").option2("O(log n)").option3("O(1)").option4("O(n²)")
                .rightAnswer("O(1)").difficultyLevel("Easy").category("Data Structures").build(),

            Question.builder()
                .questionTitle("Which data structure uses LIFO order?")
                .option1("Queue").option2("Stack").option3("Array").option4("Linked List")
                .rightAnswer("Stack").difficultyLevel("Easy").category("Data Structures").build(),

            Question.builder()
                .questionTitle("Which data structure uses FIFO order?")
                .option1("Stack").option2("Tree").option3("Queue").option4("Graph")
                .rightAnswer("Queue").difficultyLevel("Easy").category("Data Structures").build(),

            // ── Data Structures — Medium ─────────────────────────────────────
            Question.builder()
                .questionTitle("What is the worst-case time complexity of binary search?")
                .option1("O(1)").option2("O(n)").option3("O(log n)").option4("O(n log n)")
                .rightAnswer("O(log n)").difficultyLevel("Medium").category("Data Structures").build(),

            Question.builder()
                .questionTitle("In a singly linked list, what is the time complexity of inserting at the beginning?")
                .option1("O(n)").option2("O(log n)").option3("O(1)").option4("O(n²)")
                .rightAnswer("O(1)").difficultyLevel("Medium").category("Data Structures").build(),

            Question.builder()
                .questionTitle("Which traversal of a BST gives elements in sorted order?")
                .option1("Pre-order").option2("Post-order").option3("In-order").option4("Level-order")
                .rightAnswer("In-order").difficultyLevel("Medium").category("Data Structures").build(),

            // ── Data Structures — Hard ───────────────────────────────────────
            Question.builder()
                .questionTitle("What is the time complexity of building a heap from an unsorted array?")
                .option1("O(n log n)").option2("O(n)").option3("O(log n)").option4("O(n²)")
                .rightAnswer("O(n)").difficultyLevel("Hard").category("Data Structures").build(),

            Question.builder()
                .questionTitle("In a hash table with chaining, what is the average-case lookup time?")
                .option1("O(n)").option2("O(log n)").option3("O(1)").option4("O(n²)")
                .rightAnswer("O(1)").difficultyLevel("Hard").category("Data Structures").build(),

            // ── Spring Boot — Easy ───────────────────────────────────────────
            Question.builder()
                .questionTitle("Which annotation marks a class as a Spring REST controller?")
                .option1("@Controller").option2("@RestController").option3("@Service").option4("@Component")
                .rightAnswer("@RestController").difficultyLevel("Easy").category("Spring Boot").build(),

            Question.builder()
                .questionTitle("Which annotation is used for dependency injection in Spring?")
                .option1("@Inject").option2("@Resource").option3("@Autowired").option4("@Component")
                .rightAnswer("@Autowired").difficultyLevel("Easy").category("Spring Boot").build(),

            // ── Spring Boot — Medium ─────────────────────────────────────────
            Question.builder()
                .questionTitle("What is the purpose of @Transactional in Spring?")
                .option1("To cache results")
                .option2("To wrap a method in a database transaction with automatic rollback on exception")
                .option3("To validate input")
                .option4("To schedule tasks")
                .rightAnswer("To wrap a method in a database transaction with automatic rollback on exception")
                .difficultyLevel("Medium").category("Spring Boot").build(),

            Question.builder()
                .questionTitle("What does spring.jpa.hibernate.ddl-auto=update do?")
                .option1("Drops and recreates all tables on startup")
                .option2("Updates the schema to match entities without dropping data")
                .option3("Does nothing to the schema")
                .option4("Validates the schema and throws error if mismatch")
                .rightAnswer("Updates the schema to match entities without dropping data")
                .difficultyLevel("Medium").category("Spring Boot").build()
        );

        questionRepository.saveAll(questions);
        log.info("Seeded {} sample questions across Java, Data Structures, Spring Boot", questions.size());
    }
}
