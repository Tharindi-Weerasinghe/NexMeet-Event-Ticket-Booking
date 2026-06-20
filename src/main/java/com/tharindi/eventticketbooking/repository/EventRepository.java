package com.tharindi.eventticketbooking.repository;

import com.tharindi.eventticketbooking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEventNameContainingIgnoreCaseOrLocationContainingIgnoreCase(String eventName, String location);
}