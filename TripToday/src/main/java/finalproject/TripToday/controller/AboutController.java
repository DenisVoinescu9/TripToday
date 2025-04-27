package finalproject.TripToday.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {

    @GetMapping("/about")
    public String about() {

        // Returning "about.html" Thymeleaf Template

        return "about";
    }
}
