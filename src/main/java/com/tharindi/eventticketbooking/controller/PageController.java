package com.tharindi.eventticketbooking.controller;

import com.tharindi.eventticketbooking.model.Booking;
import com.tharindi.eventticketbooking.model.Event;
import com.tharindi.eventticketbooking.model.Review;
import com.tharindi.eventticketbooking.model.User;
import com.tharindi.eventticketbooking.repository.BookingRepository;
import com.tharindi.eventticketbooking.repository.EventRepository;
import com.tharindi.eventticketbooking.repository.ReviewRepository;
import com.tharindi.eventticketbooking.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Controller
public class PageController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder();

    public PageController(EventRepository eventRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository,
                          ReviewRepository reviewRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    // ================= HOME =================

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // ================= AUTH =================

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

@PostMapping("/register")
public String registerUser(@ModelAttribute User user) {

    if (userRepository.existsByEmail(user.getEmail())) {
        return "redirect:/register?error=email";
    }

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setRole("CUSTOMER");

    userRepository.save(user);

    return "redirect:/login?registered=true";
}

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

 @PostMapping("/login")
public String loginUser(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        return "redirect:/login?error=true";
    }

    String savedPassword = user.getPassword();
    boolean validPassword;

    if (savedPassword.startsWith("$2")) {
        validPassword = passwordEncoder.matches(password, savedPassword);
    } else {
        validPassword = savedPassword.equals(password);

        if (validPassword) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }
    }

    if (!validPassword) {
        return "redirect:/login?error=true";
    }

    session.setAttribute("loggedUser", user);

    if ("ADMIN".equals(user.getRole())) {
        return "redirect:/admin";
    }

    return "redirect:/events-page";
}

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ================= CUSTOMER EVENTS =================

    @GetMapping("/events-page")
    public String eventsPage(Model model) {
        model.addAttribute("events", eventRepository.findAll());
        return "events";
    }

    @GetMapping("/search-events")
    public String searchEvents(@RequestParam String keyword, Model model) {
        model.addAttribute("events",
                eventRepository.findByEventNameContainingIgnoreCaseOrLocationContainingIgnoreCase(keyword, keyword));
        return "events";
    }

    @GetMapping("/event-details/{id}")
    public String eventDetails(@PathVariable Long id, Model model) {
        Event event = eventRepository.findById(id).orElseThrow();

        model.addAttribute("event", event);
        model.addAttribute("reviews", reviewRepository.findByEvent(event));

        return "event-details";
    }

    // ================= BOOKING =================

    @GetMapping("/book-ticket")
    public String showBookTicketForm(@RequestParam(required = false) Long eventId,
                                     Model model,
                                     HttpSession session) {

        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }

        model.addAttribute("events", eventRepository.findAll());
        model.addAttribute("selectedEventId", eventId);

        return "book-ticket";
    }

    @PostMapping("/save-booking")
    public String saveBooking(@RequestParam Long eventId,
                              @RequestParam int quantity,
                              HttpSession session) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
           return "redirect:/my-tickets?success=booked";
        }

        Event event = eventRepository.findById(eventId).orElseThrow();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setQuantity(quantity);

        bookingRepository.save(booking);

        return "redirect:/my-tickets";
    }

    @GetMapping("/my-tickets")
    public String myTickets(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("bookings", bookingRepository.findByUser(user));

        return "my-tickets";
    }

    @GetMapping("/cancel-ticket/{id}")
    public String cancelTicket(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(id).orElseThrow();

        if (!booking.getUser().getId().equals(user.getId())) {
            return "redirect:/my-tickets";
        }

        bookingRepository.deleteById(id);

        return "redirect:/my-tickets";
    }

    // ================= PROFILE =================

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        return "profile";
    }

    // ================= REVIEWS =================

   @PostMapping("/save-review/{eventId}")
public String saveReview(@PathVariable Long eventId,
                         @ModelAttribute Review review,
                         HttpSession session) {

    User user = (User) session.getAttribute("loggedUser");

    if (user == null) {
        return "redirect:/login";
    }

    Event event = eventRepository.findById(eventId).orElseThrow();

    if (!bookingRepository.existsByUserAndEvent(user, event)) {
        return "redirect:/my-tickets?error=not-booked";
    }

    if (reviewRepository.existsByUserAndEvent(user, event)) {
        return "redirect:/my-tickets?error=already-reviewed";
    }

    review.setEvent(event);
    review.setUser(user);
    review.setReviewerName(user.getName());

    reviewRepository.save(review);

    return "redirect:/event-details/" + eventId;
}

    // ================= ADMIN =================

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBookings", bookingRepository.count());

        return "admin";
    }

    

    @GetMapping("/bookings-page")
    public String bookingsPage(HttpSession session, Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("bookings", bookingRepository.findAll());

        return "bookings";
    }

    @GetMapping("/add-event")
    public String showAddEventForm(Model model, HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("event", new Event());

        return "add-event";
    }

    @PostMapping("/save-event")
    public String saveEvent(@ModelAttribute Event event, HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        eventRepository.save(event);

        return "redirect:/admin/events";
    }

    @GetMapping("/edit-event/{id}")
    public String showEditEventForm(@PathVariable Long id,
                                    Model model,
                                    HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Event event = eventRepository.findById(id).orElseThrow();

        model.addAttribute("event", event);

        return "edit-event";
    }

    @PostMapping("/update-event/{id}")
    public String updateEventFromForm(@PathVariable Long id,
                                      @ModelAttribute Event updatedEvent,
                                      HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Event event = eventRepository.findById(id).orElseThrow();

        event.setEventName(updatedEvent.getEventName());
        event.setLocation(updatedEvent.getLocation());
        event.setTicketPrice(updatedEvent.getTicketPrice());
        event.setDescription(updatedEvent.getDescription());
        event.setImageUrl(updatedEvent.getImageUrl());

        eventRepository.save(event);

        return "redirect:/admin/events";
    }

    @GetMapping("/delete-event/{id}")
    public String deleteEvent(@PathVariable Long id, HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        eventRepository.deleteById(id);

        return "redirect:/admin/events";
    }

    // ================= HELPER METHODS =================

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        return user != null && "ADMIN".equals(user.getRole());
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedUser") != null;
    }

   @GetMapping("/review-event/{eventId}")
public String reviewEventPage(@PathVariable Long eventId,
                              HttpSession session,
                              Model model) {

    User user = (User) session.getAttribute("loggedUser");

    if (user == null) {
        return "redirect:/login";
    }

    Event event = eventRepository.findById(eventId).orElseThrow();

    if (!bookingRepository.existsByUserAndEvent(user, event)) {
        return "redirect:/my-tickets?error=not-booked";
    }

    if (reviewRepository.existsByUserAndEvent(user, event)) {
        return "redirect:/my-tickets?error=already-reviewed";
    }

    model.addAttribute("event", event);
    model.addAttribute("review", new Review());

    return "review-event";
}

@GetMapping("/admin/events")
public String adminEvents(HttpSession session, Model model) {

    if (!isAdmin(session)) {
        return "redirect:/login";
    }

    model.addAttribute("events", eventRepository.findAll());

    return "admin-events";
}
@GetMapping("/ticket/{id}")
public String viewTicket(@PathVariable Long id,
                         HttpSession session,
                         Model model) {

    User user = (User) session.getAttribute("loggedUser");

    if (user == null) {
        return "redirect:/login";
    }

    Booking booking = bookingRepository.findById(id).orElseThrow();

    if (!booking.getUser().getId().equals(user.getId())) {
        return "redirect:/my-tickets";
    }

    model.addAttribute("booking", booking);
    return "ticket";
}

}