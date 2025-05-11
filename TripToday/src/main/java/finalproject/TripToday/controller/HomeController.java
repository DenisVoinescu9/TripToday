package finalproject.TripToday.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {


    static class Testimonial {
        private final String imageSrc;
        private final String imageAlt;
        private final String body;


        public Testimonial(String imageSrc, String body) {
            this.imageSrc = imageSrc;
            this.imageAlt = "Traveler image";
            this.body = body;
        }


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


    @GetMapping(value = {"/", "/home"})
    public String home(Model model) {


        model.addAttribute("aboutUsHeading", "Discover the world with us");
        model.addAttribute("aboutUsParagraph", "Explore unforgettable destinations with TripToday! We create immersive guided tours led by passionate experts. Our guides share authentic stories and insights, making your journey seamless and memorable. Leave the details to us, enjoy the discovery. Find your next adventure with TripToday!");
        model.addAttribute("aboutUsImageSrc", "/images/guide_group.png");
        model.addAttribute("aboutUsImageAlt", "Guides group photo");
        model.addAttribute("travelersTestimonialsHeading", "Our travelers' testimonials");


        List<Testimonial> testimonials = new ArrayList<>();
        testimonials.add(new Testimonial("/images/traveler2.png", "\"What a fantastic adventure! Signing up was a breeze online. The guides knew the nature trails inside out and were super friendly. Saw breathtaking landscapes thanks to their expert planning\""));
        testimonials.add(new Testimonial("/images/traveler1.png", "\"Booking our trip through this company was incredibly straightforward. Our guides were so experienced and passionate, making the stunning historical sites truly come alive. A seamless and amazing experience!\""));
        testimonials.add(new Testimonial("/images/traveler3.png", "\"Everything was handled perfectly, from the easy booking to the tour itself. Our guide was exceptionally knowledgeable and made exploring the region fun. Absolutely brilliant service and location!\""));


        model.addAttribute("testimonials", testimonials);


        return "home-page";
    }
}