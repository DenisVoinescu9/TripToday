package finalproject.TripToday.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    // Configure class for 'Testimonial' object

    static class Testimonial {
        private final String imageSrc;
        private final String imageAlt;
        private final String body;

        // Create constructor for 'Testimonial' object

        public Testimonial(String imageSrc, String body) {
            this.imageSrc = imageSrc;
            this.imageAlt = "Traveler image";
            this.body = body;
        }

        // Define getters

        public String getImageSrc() {
            return imageSrc;
        }

        public String getImageAlt() {
            return imageAlt;
        }

        public String getBody() {
            return body;
        }
    }

    // Define home page, also empty path will redirect to the home page

    @GetMapping(value = {"/", "/home"})
    public String home(Model model) {

        // Configure labels

        model.addAttribute("aboutUsHeading", "Discover the world with us");
        model.addAttribute("aboutUsParagraph", "Explore unforgettable destinations with TripToday! We create immersive guided tours led by passionate experts. Our guides share authentic stories and insights, making your journey seamless and memorable. Leave the details to us, enjoy the discovery. Find your next adventure with TripToday!");
        model.addAttribute("aboutUsImageSrc", "/images/guides.jpg");
        model.addAttribute("aboutUsImageAlt", "Guides group photo");
        model.addAttribute("travelersTestimonialsHeading", "Our travelers' testimonials");

        // Initialize list of testimonials that will be displayed on the home page

        List<Testimonial> testimonials = new ArrayList<>();
        testimonials.add(new Testimonial("/images/traveler1.png", "\"Booking our trip through this company was incredibly straightforward. Our guides were so experienced and passionate, making the stunning historical sites truly come alive. A seamless and amazing experience!\""));
        testimonials.add(new Testimonial("/images/guides.jpg", "\"What a fantastic adventure! Signing up was a breeze online. The guides knew the nature trails inside out and were super friendly. Saw breathtaking landscapes thanks to their expert planning\""));
        testimonials.add(new Testimonial("/images/guides.jpg", "\"Everything was handled perfectly, from the easy booking to the tour itself. Our guide was exceptionally knowledgeable and made exploring the region fun. Absolutely brilliant service and location!\""));

        // Add list of testimonials to the model

        model.addAttribute("testimonials", testimonials);

        // Return thymeleaf template

        return "home-page";
    }
}