package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.RevisionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevisionScheduleRepository extends JpaRepository<RevisionSchedule, Long> {

    Optional<RevisionSchedule> findByUserIdAndCategory(Long userId, String category);

    List<RevisionSchedule> findByUserId(Long userId);

    // Topics that are overdue or due today — highest priority
    @Query("SELECT rs FROM RevisionSchedule rs WHERE rs.user.id = :userId " +
           "AND rs.nextRevisionDate <= :today ORDER BY rs.nextRevisionDate ASC")
    List<RevisionSchedule> findDueForRevision(Long userId, LocalDate today);

    // Topics due in next 7 days — for upcoming revision alerts
    @Query("SELECT rs FROM RevisionSchedule rs WHERE rs.user.id = :userId " +
           "AND rs.nextRevisionDate > :today AND rs.nextRevisionDate <= :inSevenDays " +
           "ORDER BY rs.nextRevisionDate ASC")
    List<RevisionSchedule> findUpcomingRevisions(Long userId, LocalDate today, LocalDate inSevenDays);
}
