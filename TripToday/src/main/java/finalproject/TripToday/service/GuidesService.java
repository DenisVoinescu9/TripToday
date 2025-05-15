package finalproject.TripToday.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GuidesService {

    private static final Logger logger = LoggerFactory.getLogger(GuidesService.class);
    private final Auth0Service auth0Service;

    @Autowired
    public GuidesService(Auth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    public Map<String, String> getStaticPageData() {
        Map<String, String> staticData = new HashMap<>();
        staticData.put("pageTitle", "Meet the guides | TripToday");
        staticData.put("mainHeading", "Meet our team of experienced guides");
        staticData.put("introParagraph", "Get to know the knowledgeable and friendly faces who will bring your destinations to life. Our guides aren't just experts in history and nature; they're passionate storytellers dedicated to making your TripToday experience authentic and unforgettable. Find out more about the individuals ready to lead your next adventure!");
        staticData.put("noGuidesMessage", "There are no guides at the moment.");
        return staticData;
    }

    public Map<String, Object> getPagedGuidesData(int pageRequest) {
        Map<String, Object> resultData = new HashMap<>();
        List<Map<String, String>> pageGuides = new ArrayList<>();
        String loadErrorMessage = null;
        int currentPage = pageRequest;
        int totalPages = 0;

        try {
            Map<String, Object> serviceResult = auth0Service.getPagedGuidesWithDescriptions(currentPage);
            pageGuides = (List<Map<String, String>>) serviceResult.get("guides");
            currentPage = (int) serviceResult.get("page");
            totalPages = (int) serviceResult.get("totalPages");
        } catch (Exception e) {
            logger.error("Error loading guides data from Auth0Service", e);
            loadErrorMessage = "An unexpected error occurred while loading guide information. Please try again later.";
            pageGuides = new ArrayList<>();
            currentPage = 0;
            totalPages = 0;
        }

        resultData.put("guides", pageGuides);
        resultData.put("page", currentPage);
        resultData.put("totalPages", totalPages);
        if (loadErrorMessage != null) {
            resultData.put("errorMessage", loadErrorMessage);
        }

        return resultData;
    }
}