package com.tharindi.eventticketbooking.repository;

import com.tharindi.eventticketbooking.model.Event;
import com.tharindi.eventticketbooking.model.Review;
import com.tharindi.eventticketbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByEvent(Event event);

    boolean existsByUserAndEvent(User user, Event event);
}