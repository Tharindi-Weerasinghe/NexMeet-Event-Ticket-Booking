package com.tharindi.eventticketbooking.model;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, updatable = false)
private String ticketNumber;

    private int quantity;

    @ManyToOne
    private User user;

    @ManyToOne
    private Event event;

    private boolean checkedIn;

    private LocalDateTime checkedInAt;

    @PrePersist
public void generateTicketNumber() {
    if (ticketNumber == null) {
        ticketNumber = "NEX-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase();
    }
}

    public Booking() {}

    public Long getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getTicketNumber() {
    return ticketNumber;
}

public boolean isCheckedIn() {
    return checkedIn;
}

public void setCheckedIn(boolean checkedIn) {
    this.checkedIn = checkedIn;
}

public LocalDateTime getCheckedInAt() {
    return checkedInAt;
}

public void setCheckedInAt(LocalDateTime checkedInAt) {
    this.checkedInAt = checkedInAt;
}
}