package com.tharindi.eventticketbooking.controller;

import com.tharindi.eventticketbooking.model.Event;
import com.tharindi.eventticketbooking.repository.EventRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PostMapping
    public Event addEvent(@Valid @RequestBody Event event) {
        return eventRepository.save(event);
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable Long id,
                             @Valid @RequestBody Event updatedEvent) {
        Event event = eventRepository.findById(id).orElseThrow();

        event.setEventName(updatedEvent.getEventName());
        event.setLocation(updatedEvent.getLocation());
        event.setTicketPrice(updatedEvent.getTicketPrice());
        event.setDescription(updatedEvent.getDescription());
        event.setImageUrl(updatedEvent.getImageUrl());
        event.setEventDateTime(updatedEvent.getEventDateTime());
        event.setAvailableTickets(updatedEvent.getAvailableTickets());

        return eventRepository.save(event);
    }

    @DeleteMapping("/{id}")
    public String deleteEvent(@PathVariable Long id) {
        eventRepository.deleteById(id);
        return "Event deleted successfully";
    }
}