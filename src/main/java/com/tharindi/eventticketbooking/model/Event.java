package com.tharindi.eventticketbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventName;
    private String location;
    private double ticketPrice;
    private String imageUrl;
    private String description;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
private LocalDateTime eventDateTime;

private int availableTickets;

    public Event() {
    }

    public Long getId() {
        return id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }
    public String getImageUrl() {
    return imageUrl;
}

public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
}

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getDescription() {
    return description;
}

public void setDescription(String description) {
    this.description = description;
}

public LocalDateTime getEventDateTime() {
    return eventDateTime;
}

public void setEventDateTime(LocalDateTime eventDateTime) {
    this.eventDateTime = eventDateTime;
}

public int getAvailableTickets() {
    return availableTickets;
}

public void setAvailableTickets(int availableTickets) {
    this.availableTickets = availableTickets;
}

}
