package com.tharindi.eventticketbooking.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tharindi.eventticketbooking.model.Booking;
import com.tharindi.eventticketbooking.model.Event;
import com.tharindi.eventticketbooking.model.Review;
import com.tharindi.eventticketbooking.model.User;
import com.tharindi.eventticketbooking.repository.BookingRepository;
import com.tharindi.eventticketbooking.repository.EventRepository;
import com.tharindi.eventticketbooking.repository.ReviewRepository;
import com.tharindi.eventticketbooking.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
public class PageController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PageController(EventRepository eventRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository,
                          ReviewRepository reviewRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/error")
    public String errorPage(Model model) {
        model.addAttribute("title", "Page Not Found");
        model.addAttribute("message", "The page you are looking for does not exist.");
        return "custom-error";
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage(Model model) {
        model.addAttribute("title", "Access Denied");
        model.addAttribute("message", "You do not have permission to access this page.");
        return "custom-error";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result) {

        if (result.hasErrors()) {
            return "register";
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            result.rejectValue("email", "error.user", "This email is already registered");
            return "register";
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

        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return "redirect:/login?error=empty";
        }

        User user = userRepository.findByEmail(email.trim());

        if (user == null) {
            return "redirect:/login?error=invalid";
        }

        String savedPassword = user.getPassword();
        boolean validPassword;

        if (savedPassword != null && savedPassword.startsWith("$2")) {
            validPassword = passwordEncoder.matches(password, savedPassword);
        } else {
            validPassword = savedPassword != null && savedPassword.equals(password);

            if (validPassword) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }

        if (!validPassword) {
            return "redirect:/login?error=invalid";
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

        User user = getLoggedUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Event event = eventRepository.findById(eventId).orElseThrow();

        if (event.getEventDateTime() != null
                && event.getEventDateTime().isBefore(LocalDateTime.now())) {
            return "redirect:/events-page?error=event-ended";
        }

        if (quantity < 1) {
            return "redirect:/book-ticket?eventId=" + eventId + "&error=invalid-quantity";
        }

        if (event.getAvailableTickets() < quantity) {
            return "redirect:/book-ticket?eventId=" + eventId + "&error=not-enough-tickets";
        }

        event.setAvailableTickets(event.getAvailableTickets() - quantity);
        eventRepository.save(event);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setQuantity(quantity);

        bookingRepository.save(booking);

        return "redirect:/my-tickets?success=booked";
    }

    @GetMapping("/my-tickets")
    public String myTickets(HttpSession session, Model model) {
        User user = getLoggedUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("bookings", bookingRepository.findByUser(user));
        return "my-tickets";
    }

    @GetMapping("/cancel-ticket/{id}")
    public String cancelTicket(@PathVariable Long id, HttpSession session) {
        User user = getLoggedUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(id).orElseThrow();

        if (!booking.getUser().getId().equals(user.getId())) {
            return "redirect:/my-tickets";
        }

        Event event = booking.getEvent();
        event.setAvailableTickets(event.getAvailableTickets() + booking.getQuantity());
        eventRepository.save(event);

        bookingRepository.deleteById(id);

        return "redirect:/my-tickets";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = getLoggedUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin";
        }

        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/review-event/{eventId}")
    public String reviewEventPage(@PathVariable Long eventId,
                                  HttpSession session,
                                  Model model) {

        User user = getLoggedUser(session);

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

    @PostMapping("/save-review/{eventId}")
    public String saveReview(@PathVariable Long eventId,
                             @Valid @ModelAttribute("review") Review review,
                             BindingResult result,
                             HttpSession session,
                             Model model) {

        User user = getLoggedUser(session);

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

        if (result.hasErrors()) {
            model.addAttribute("event", event);
            return "review-event";
        }

        review.setEvent(event);
        review.setUser(user);
        review.setReviewerName(user.getName());

        reviewRepository.save(review);

        return "redirect:/event-details/" + eventId;
    }

    @GetMapping("/ticket/{id}")
    public String viewTicket(@PathVariable Long id,
                             HttpSession session,
                             Model model) {

        User user = getLoggedUser(session);

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

    @GetMapping("/ticket/{id}/qr")
    public void ticketQrCode(@PathVariable Long id,
                             HttpSession session,
                             HttpServletResponse response)
            throws IOException, WriterException {

        User user = getLoggedUser(session);

        if (user == null) {
            response.sendRedirect("/login");
            return;
        }

        Booking booking = bookingRepository.findById(id).orElseThrow();

        if (!booking.getUser().getId().equals(user.getId())) {
            response.sendRedirect("/my-tickets");
            return;
        }

        if (booking.getTicketNumber() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix qrCode = qrCodeWriter.encode(
                booking.getTicketNumber(),
                BarcodeFormat.QR_CODE,
                250,
                250
        );

        response.setContentType("image/png");
        MatrixToImageWriter.writeToStream(
                qrCode,
                "PNG",
                response.getOutputStream()
        );
    }

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBookings", bookingRepository.count());

        return "admin";
    }

    @GetMapping("/admin/events")
    public String adminEvents(HttpSession session, Model model) {
        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        model.addAttribute("events", eventRepository.findAll());

        return "admin-events";
    }

    @GetMapping("/bookings-page")
    public String bookingsPage(HttpSession session, Model model) {
        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        model.addAttribute("bookings", bookingRepository.findAll());

        return "bookings";
    }

    @GetMapping("/add-event")
    public String showAddEventForm(Model model, HttpSession session) {
        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        model.addAttribute("event", new Event());

        return "add-event";
    }

    @PostMapping("/save-event")
    public String saveEvent(@Valid @ModelAttribute("event") Event event,
                            BindingResult result,
                            HttpSession session) {

        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        if (result.hasErrors()) {
            return "add-event";
        }

        eventRepository.save(event);

        return "redirect:/admin/events";
    }

    @GetMapping("/edit-event/{id}")
    public String showEditEventForm(@PathVariable Long id,
                                    Model model,
                                    HttpSession session) {

        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        Event event = eventRepository.findById(id).orElseThrow();
        model.addAttribute("event", event);

        return "edit-event";
    }

    @PostMapping("/update-event/{id}")
public String updateEventFromForm(@PathVariable Long id,
                                  @Valid @ModelAttribute("event") Event updatedEvent,
                                  BindingResult result,
                                  HttpSession session) {

    String accessCheck = adminAccessCheck(session);

    if (accessCheck != null) {
        return accessCheck;
    }

    if (result.hasErrors()) {
        updatedEvent.setId(id);
        return "edit-event";
    }

    Event event = eventRepository.findById(id).orElseThrow();

    event.setEventName(updatedEvent.getEventName());
    event.setLocation(updatedEvent.getLocation());
    event.setTicketPrice(updatedEvent.getTicketPrice());
    event.setDescription(updatedEvent.getDescription());
    event.setImageUrl(updatedEvent.getImageUrl());
    event.setEventDateTime(updatedEvent.getEventDateTime());
    event.setAvailableTickets(updatedEvent.getAvailableTickets());

    eventRepository.save(event);

    return "redirect:/admin/events";
}

    @GetMapping("/delete-event/{id}")
    public String deleteEvent(@PathVariable Long id, HttpSession session) {
        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        eventRepository.deleteById(id);

        return "redirect:/admin/events";
    }

    @GetMapping("/admin/verify-ticket")
    public String verifyTicketPage(@RequestParam(required = false) String ticketNumber,
                                   HttpSession session,
                                   Model model) {

        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        if (ticketNumber != null && !ticketNumber.isBlank()) {
            bookingRepository.findByTicketNumber(ticketNumber.trim())
                    .ifPresentOrElse(
                            booking -> model.addAttribute("verifiedBooking", booking),
                            () -> model.addAttribute("invalidTicket", true)
                    );
        }

        return "verify-ticket";
    }

    @PostMapping("/admin/check-in/{id}")
    public String checkInTicket(@PathVariable Long id,
                                HttpSession session) {

        String accessCheck = adminAccessCheck(session);

        if (accessCheck != null) {
            return accessCheck;
        }

        Booking booking = bookingRepository.findById(id).orElseThrow();

        if (booking.isCheckedIn()) {
            return "redirect:/admin/verify-ticket?ticketNumber="
                    + booking.getTicketNumber()
                    + "&error=already-used";
        }

        booking.setCheckedIn(true);
        booking.setCheckedInAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return "redirect:/admin/verify-ticket?ticketNumber="
                + booking.getTicketNumber()
                + "&success=checked-in";
    }

    private User getLoggedUser(HttpSession session) {
        return (User) session.getAttribute("loggedUser");
    }

    private boolean isLoggedIn(HttpSession session) {
        return getLoggedUser(session) != null;
    }

    private String adminAccessCheck(HttpSession session) {
        User user = getLoggedUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/access-denied";
        }

        return null;
    }
}