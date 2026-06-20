package com.tharindi.eventticketbooking.controller;

import com.tharindi.eventticketbooking.model.Booking;
import com.tharindi.eventticketbooking.model.Event;
import com.tharindi.eventticketbooking.model.User;
import com.tharindi.eventticketbooking.repository.BookingRepository;
import com.tharindi.eventticketbooking.repository.EventRepository;
import com.tharindi.eventticketbooking.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public BookingController(BookingRepository bookingRepository, UserRepository userRepository, EventRepository eventRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @PostMapping
    public Booking createBooking(@RequestParam Long userId,
                                 @RequestParam Long eventId,
                                 @RequestParam int quantity) {

        User user = userRepository.findById(userId).orElseThrow();
        Event event = eventRepository.findById(eventId).orElseThrow();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setQuantity(quantity);

        return bookingRepository.save(booking);
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @GetMapping("/{id}")
public Booking getBookingById(@PathVariable Long id) {
    return bookingRepository.findById(id).orElseThrow();
}

@DeleteMapping("/{id}")
public String deleteBooking(@PathVariable Long id) {
    bookingRepository.deleteById(id);
    return "Booking deleted successfully";
}
}