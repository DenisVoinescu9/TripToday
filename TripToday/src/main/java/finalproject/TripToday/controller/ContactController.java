package finalproject.TripToday.controller;

import finalproject.TripToday.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/contact")
public class ContactController {

    private final ContactService contactService;

    @Autowired
    public ContactController(ContactService contactPageService) {
        this.contactService = contactPageService;
    }

    @GetMapping
    public String contact(Model model) {
        model.addAttribute("pageTitle", contactService.getPageTitle());
        model.addAttribute("pageMainHeader", contactService.getPageMainHeader());

        model.addAttribute("heading", contactService.getFeedbackSectionHeading());
        model.addAttribute("paragraph", contactService.getFeedbackSectionParagraph());
        model.addAttribute("imageAlt", contactService.getFeedbackImageAltText());
        model.addAttribute("imageUrl", contactService.getFeedbackImageUrl());

        model.addAttribute("contactEmail", contactService.getContactEmail());
        model.addAttribute("contactPhone", contactService.getContactPhone());
        model.addAttribute("contactAddress", contactService.getContactAddress());
        model.addAttribute("contactHours", contactService.getContactHours());

        model.addAttribute("faqHeading", contactService.getFaqSectionTitle());
        model.addAttribute("noFaqsMessage", contactService.getNoFaqsText());
        model.addAttribute("faqs", contactService.getFaqs());

        return "contact-page";
    }
}