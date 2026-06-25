package com.tharindi.eventticketbooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Reviewer name is required")
private String reviewerName;

@Min(value = 1, message = "Rating must be at least 1")
@Max(value = 5, message = "Rating cannot be more than 5")
private int rating;

@NotBlank(message = "Comment is required")
@Size(max = 1000, message = "Comment cannot be longer than 1000 characters")
@Column(length = 1000)
private String comment;

    @ManyToOne
    private Event event;

    @ManyToOne
    private User user;

    public Review() {}

    public Long getId() {
        return id;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUser() {
    return user;
}

public void setUser(User user) {
    this.user = user;
}
}