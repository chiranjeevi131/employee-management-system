
package com.ems.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ems.entity.User;
import com.ems.service.UserService;

@Controller
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // Display HR registration form - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/create-hr")
    public String showCreateHRForm(Model model) {

        model.addAttribute("user", new User());

        model.addAttribute("hrAccounts",
                userService.getAllHRAccounts());
        
        return "create-hr";
    }

    // Create HR account - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create-hr")
    public String createHR(
            @ModelAttribute("user") User user,
            Model model) {

        try {
            userService.registerHR(user);

            model.addAttribute("success",
                    "HR account created successfully!");

            model.addAttribute("user", new User());

        } catch (RuntimeException e) {

            model.addAttribute("error", e.getMessage());
        }
        
        
        model.addAttribute("hrAccounts",
                userService.getAllHRAccounts());

        return "create-hr";
    }
 // Delete HR account - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/delete-hr/{id}")
    public String deleteHR(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User loggedInUser = userService
                    .findByUsername(authentication.getName())
                    .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

            if (loggedInUser.getId().equals(id)) {
                throw new RuntimeException(
                        "You cannot delete your own account");
            }

            userService.deleteHRAccount(id);

            redirectAttributes.addFlashAttribute(
                    "success", "HR account deleted successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/admin/create-hr";
    }
}

