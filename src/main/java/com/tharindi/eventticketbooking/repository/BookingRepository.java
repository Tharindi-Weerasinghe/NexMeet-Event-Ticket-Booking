package com.tharindi.eventticketbooking.repository;

import com.tharindi.eventticketbooking.model.Booking;
import com.tharindi.eventticketbooking.model.Event;
import com.tharindi.eventticketbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByTicketNumber(String ticketNumber);

    List<Booking> findByUser(User user);

    boolean existsByUserAndEvent(User user, Event event);
}