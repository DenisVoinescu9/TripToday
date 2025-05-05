package finalproject.TripToday.controller;

import finalproject.TripToday.service.Auth0Service; // Service import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam; // Import @RequestParam

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Controller
public class GuidesController {

    private final Auth0Service auth0Service;

    @Autowired
    public GuidesController(Auth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    @GetMapping("/guides")
    public String guides(@RequestParam(defaultValue = "0") int page, Model model, @AuthenticationPrincipal OidcUser principal) {
        // Adaugat @RequestParam page
        List<Map<String, String>> guidesForView = new ArrayList<>();
        String errorMessage = null; // Variabila pentru erori

        try {
            List<Map<String, String>> initialGuides = auth0Service.getAllGuides();

            for (Map<String, String> guide : initialGuides) {
                String guideId = guide.get("id");
                if (guideId != null && !guideId.isEmpty()) {
                    // Adaugam descrierea - ok
                    String description = auth0Service.getGuideDescription(guideId);
                    guide.put("description", description == null || description.isEmpty() ? "The guide has no description yet." : description);
                    guidesForView.add(guide);
                }
            }

            // --- Logica de Paginare ---
            int pageSize = 4; // Numarul de ghizi pe pagina
            int totalGuides = guidesForView.size();
            int totalPages = (int) Math.ceil((double) totalGuides / pageSize);

            // Validare pagina ceruta
            if (page < 0) {
                page = 0;
            } else if (page >= totalPages && totalPages > 0) {
                page = totalPages - 1;
            }

            int fromIndex = page * pageSize;
            // Asigura-te ca toIndex nu depaseste dimensiunea listei
            int toIndex = Math.min(fromIndex + pageSize, totalGuides);

            List<Map<String, String>> pageGuides;
            // Extrage sublista doar daca indecsii sunt valizi
            if (fromIndex >= 0 && fromIndex < totalGuides) {
                pageGuides = guidesForView.subList(fromIndex, toIndex);
            } else {
                pageGuides = new ArrayList<>(); // Lista goala daca pagina ceruta e invalida
                if (totalGuides > 0) page = 0; // Reseteaza la prima pagina daca exista ghizi
            }
            // --- Sfarsit Logica de Paginare ---


            // Adauga la model DOAR ghizii pentru pagina curenta
            model.addAttribute("guides", pageGuides);
            // Adauga informatiile de paginare la model
            model.addAttribute("page", page);
            model.addAttribute("totalPages", totalPages);


            if (principal != null) {
                // Trimite profilul userului logat - ok
                model.addAttribute("profile", principal.getClaims()); // Foloseste 'profile' in loc de 'user' daca asa e consistent
            }


        } catch (Exception e) {
            errorMessage = "An unexpected error occurred while loading guide information. Please try again later.";
            // Logheaza eroarea e.printStackTrace();
        }

        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("guides", new ArrayList<>()); // Trimite lista goala in caz de eroare
            model.addAttribute("page", 0);
            model.addAttribute("totalPages", 0);
        }
        return "guides-page";


    }
}