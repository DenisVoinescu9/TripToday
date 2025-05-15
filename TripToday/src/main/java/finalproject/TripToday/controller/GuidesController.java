package finalproject.TripToday.controller;

import finalproject.TripToday.service.GuidesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;


@Controller
@RequestMapping("/guides")
public class GuidesController {

    private final GuidesService guidesService;

    @Autowired
    public GuidesController(GuidesService guidesService) {
        this.guidesService = guidesService;
    }

    @GetMapping
    public String guides(@RequestParam(defaultValue = "0") int page, Model model, @AuthenticationPrincipal OidcUser principal) {

        Map<String, String> staticData = guidesService.getStaticPageData();
        staticData.forEach(model::addAttribute);

        Map<String, Object> pagedData = guidesService.getPagedGuidesData(page);
        model.addAttribute("guides", pagedData.get("guides"));
        model.addAttribute("page", pagedData.get("page"));
        model.addAttribute("totalPages", pagedData.get("totalPages"));

        if (pagedData.containsKey("errorMessage")) {
            model.addAttribute("errorMessage", pagedData.get("errorMessage"));
        }

        if (principal != null) {
            model.addAttribute("profile", principal.getClaims());
        }

        return "guides-page";
    }
}