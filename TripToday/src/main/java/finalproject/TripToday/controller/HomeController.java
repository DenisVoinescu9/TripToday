package finalproject.TripToday.controller;

import finalproject.TripToday.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HomeService homeService;

    @Autowired
    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping(value = {"/", "/home"})
    public String home(Model model) {
        model.addAttribute("aboutUsHeading", homeService.getAboutUsHeading());
        model.addAttribute("aboutUsParagraph", homeService.getAboutUsParagraph());
        model.addAttribute("aboutUsImageSrc", homeService.getAboutUsImageSrc());
        model.addAttribute("aboutUsImageAlt", homeService.getAboutUsImageAlt());

        model.addAttribute("travelersTestimonialsHeading", homeService.getTravelersTestimonialsHeading());
        model.addAttribute("testimonials", homeService.getTestimonialsData());

        return "home-page";
    }
}