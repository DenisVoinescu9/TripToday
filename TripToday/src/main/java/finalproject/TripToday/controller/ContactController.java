package finalproject.TripToday.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class ContactController {

    @GetMapping("/contact")
    public String about(Model model) {

        // Texte generale pagina
        String pageTitle = "Contact us | TripToday";
        String mainHeading = "We value your feedback";
        String mainParagraph = "At TripToday, your satisfaction is paramount. To ensure the best possible experience, we encourage you to contact us with any inquiries or requests. We're dedicated to making your adventure seamless and enjoyable.";
        String imageAltText = "Guides group photo";
        String imageUrl = "/images/guides.jpg";

        // Date de contact

        String contactEmail = "support@triptoday.com";
        String contactPhone = "+40 770 123 987";
        String contactAddress = "Nicolae Balcescu Street, No. 114, Brasov, 500019, Romania";
        String contactHours = "06:00 - 14:00 UTC";

        // Texte sectiune FAQ
        String faqSectionTitle = "Frequently Asked Questions";
        String noFaqsText = "No FAQs available at the moment.";

        // Adaugare la model - General
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("heading", mainHeading);
        model.addAttribute("paragraph", mainParagraph);
        model.addAttribute("imageAlt", imageAltText);
        model.addAttribute("imageUrl", imageUrl);

        // Adaugare la model - Contact
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("contactPhone", contactPhone);
        model.addAttribute("contactAddress", contactAddress);
        model.addAttribute("contactHours", contactHours); // Valoarea e acum doar intervalul

        // Adaugare la model - FAQ
        model.addAttribute("faqHeading", faqSectionTitle);
        model.addAttribute("noFaqsMessage", noFaqsText);

        Map<String, String> faqMap = new LinkedHashMap<>();
        faqMap.put("What is TripToday about?", "TripToday connects you with experienced, world-traveled guides for unique travel experiences and tours.");
        faqMap.put("How can I book a trip I see on TripToday?", "Find a trip you like on our platform, then follow the booking steps shown on the trip's page to reserve your place.");
        faqMap.put("What kind of trips can I find here?", "We offer diverse trips, often led by guides with extensive global travel backgrounds, offering unique insights into various destinations.");
        faqMap.put("Who are the guides for the trips?", "Our guides are generally well-traveled individuals, eager to share their broad knowledge and passion for global exploration during your trip.");
        faqMap.put("I'm an experienced traveler and want to become a guide. How can I join?", "If you have extensive travel experience and want to become a guide with us, please email your details to contact@triptoday.com");

        model.addAttribute("faqs", faqMap);

        return "contact-page";
    }
}