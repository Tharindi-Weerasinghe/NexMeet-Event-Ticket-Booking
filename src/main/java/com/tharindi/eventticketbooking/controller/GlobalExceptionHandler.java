package com.tharindi.eventticketbooking.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(Model model) {
        model.addAttribute("title", "Not Found");
        model.addAttribute("message", "The page or record you requested could not be found.");
        return "custom-error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralError(Model model) {
        model.addAttribute("title", "Something Went Wrong");
        model.addAttribute("message", "An unexpected error occurred. Please try again.");
        return "custom-error";
    }
}