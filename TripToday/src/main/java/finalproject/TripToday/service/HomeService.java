package finalproject.TripToday.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeService {

    public String getAboutUsHeading() {
        return "Discover the world with us";
    }

    public String getAboutUsParagraph() {
        return "Explore unforgettable destinations with TripToday! We create immersive guided tours led by passionate experts. Our guides share authentic stories and insights, making your journey seamless and memorable. Leave the details to us, enjoy the discovery. Find your next adventure with TripToday!";
    }

    public String getAboutUsImageSrc() {
        return "/images/guide_group.png";
    }

    public String getAboutUsImageAlt() {
        return "Guides group photo";
    }

    public String getTravelersTestimonialsHeading() {
        return "Our travelers' testimonials";
    }

    public List<Map<String, String>> getTestimonialsData() {
        List<Map<String, String>> testimonials = new ArrayList<>();

        Map<String, String> testimonial1 = new LinkedHashMap<>();
        testimonial1.put("imageSrc", "/images/traveler2.png");
        testimonial1.put("imageAlt", "Traveler image 2");
        testimonial1.put("body", "\"What a fantastic adventure! Signing up was a breeze online. The guides knew the nature trails inside out and were super friendly. Saw breathtaking landscapes thanks to their expert planning\"");
        testimonials.add(testimonial1);

        Map<String, String> testimonial2 = new LinkedHashMap<>();
        testimonial2.put("imageSrc", "/images/traveler1.png");
        testimonial2.put("imageAlt", "Traveler image 1");
        testimonial2.put("body", "\"Booking our trip through this company was incredibly straightforward. Our guides were so experienced and passionate, making the stunning historical sites truly come alive. A seamless and amazing experience!\"");
        testimonials.add(testimonial2);

        Map<String, String> testimonial3 = new LinkedHashMap<>();
        testimonial3.put("imageSrc", "/images/traveler3.png");
        testimonial3.put("imageAlt", "Traveler image 3");
        testimonial3.put("body", "\"Everything was handled perfectly, from the easy booking to the tour itself. Our guide was exceptionally knowledgeable and made exploring the region fun. Absolutely brilliant service and location!\"");
        testimonials.add(testimonial3);

        return testimonials;
    }
}