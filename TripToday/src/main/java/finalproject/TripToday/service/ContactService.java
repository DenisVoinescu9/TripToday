package finalproject.TripToday.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ContactService {

    public String getPageTitle() {
        return "Contact us | TripToday";
    }

    public String getPageMainHeader() {
        return "Contact us";
    }

    public String getFeedbackSectionHeading() {
        return "We value your feedback";
    }

    public String getFeedbackSectionParagraph() {
        return "At TripToday, your satisfaction is paramount. To ensure the best possible experience, we encourage you to contact us with any inquiries or requests. We're dedicated to making your adventure seamless and enjoyable.";
    }

    public String getFeedbackImageAltText() {
        return "Feedback concept image";
    }

    public String getFeedbackImageUrl() {
        return "/images/your-experience-text.png";
    }

    public String getContactEmail() {
        return "support@triptoday.com";
    }

    public String getContactPhone() {
        return "+40 770 123 987";
    }

    public String getContactAddress() {
        return "Nicolae Balcescu Street, No. 114, Brasov, 500019, Romania";
    }

    public String getContactHours() {
        return "09:00 - 17:00 (Monday - Friday)";
    }

    public String getFaqSectionTitle() {
        return "Frequently Asked Questions";
    }

    public String getNoFaqsText() {
        return "No FAQs available at the moment.";
    }

    public Map<String, String> getFaqs() {
        Map<String, String> faqMap = new LinkedHashMap<>();
        faqMap.put("What is TripToday about?", "TripToday connects you with experienced, world-traveled guides for unique travel experiences and tours.");
        faqMap.put("How can I book a trip I see on TripToday?", "Find a trip you like on our platform, then follow the booking steps shown on the trip's page to reserve your place.");
        faqMap.put("What kind of trips can I find here?", "We offer diverse trips, often led by guides with extensive global travel backgrounds, offering unique insights into various destinations.");
        faqMap.put("Who are the guides for the trips?", "Our guides are generally well-traveled individuals, eager to share their broad knowledge and passion for global exploration during your trip.");
        faqMap.put("I'm an experienced traveler and want to become a guide. How can I join?", "If you have extensive travel experience and want to become a guide with us, please email your details to contact@triptoday.com");
        return faqMap;
    }
}