package finalproject.TripToday.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class ContactController {

    @GetMapping("/contact")
    public String contact(Model model) { // Renamed from 'about' to 'contact' for clarity

        // Page structure texts
        String pageTitle = "Contact us | TripToday"; // For <title> tag
        String pageMainHeader = "Contact us";       // For the main H1

        // Section: "We value your feedback" (previously mainHeading)
        String feedbackSectionHeading = "We value your feedback";
        String feedbackSectionParagraph = "At TripToday, your satisfaction is paramount. To ensure the best possible experience, we encourage you to contact us with any inquiries or requests. We're dedicated to making your adventure seamless and enjoyable.";
        String feedbackImageAltText = "Feedback concept image"; // More generic alt
        String feedbackImageUrl = "/images/your-experience-text.png"; // Assuming you have this image

        // Contact details data
        String contactEmail = "support@triptoday.com";
        String contactPhone = "+40 770 123 987";
        String contactAddress = "Nicolae Balcescu Street, No. 114, Brasov, 500019, Romania";
        String contactHours = "09:00 - 17:00 (Monday - Friday)"; // Adjusted format slightly

        // FAQ section texts
        String faqSectionTitle = "Frequently Asked Questions";
        String noFaqsText = "No FAQs available at the moment.";

        // Add to model
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageMainHeader", pageMainHeader); // New attribute for H1

        model.addAttribute("heading", feedbackSectionHeading); // Kept 'heading' for existing section
        model.addAttribute("paragraph", feedbackSectionParagraph);
        model.addAttribute("imageAlt", feedbackImageAltText);
        model.addAttribute("imageUrl", feedbackImageUrl);

        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("contactPhone", contactPhone);
        model.addAttribute("contactAddress", contactAddress);
        model.addAttribute("contactHours", contactHours);

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