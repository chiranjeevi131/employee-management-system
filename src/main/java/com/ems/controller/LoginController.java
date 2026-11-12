package com.ems.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

import com.ems.entity.User;
import com.ems.service.UserService;

@Controller
public class LoginController {


private final UserService userService;

public LoginController(UserService userService) {
    this.userService = userService;
}

// Display login page
@GetMapping("/login")
public String showLoginPage() {
    return "login";
}

// Display registration page
@GetMapping("/register")
public String showRegistrationPage(Model model) {
    model.addAttribute("user", new User());
    return "register";
}

// Register admin
@PostMapping("/register")
public String registerUser(@ModelAttribute("user") User user,
                           Model model) {

    try {
        userService.registerUser(user);
        return "redirect:/login?registered";

    } catch (RuntimeException e) {
        model.addAttribute("error", e.getMessage());
        return "register";
    }
}

// Display Admin Profile
@GetMapping("/profile")
public String showAdminProfile(Authentication authentication,
                               Model model) {

    String username = authentication.getName();

    User user = userService.findByUsername(username)
            .orElseThrow(() ->
                new RuntimeException("Admin not found"));

    model.addAttribute("user", user);

    return "admin-profile";
}

//Display Edit Profile page
@GetMapping("/profile/edit")
public String showEditProfilePage(Authentication authentication,
                               Model model) {

 User user = userService.findByUsername(authentication.getName())
         .orElseThrow(() ->
             new RuntimeException("User not found"));

 model.addAttribute("user", user);

 return "edit-profile";
}


//Process Edit Profile
@PostMapping("/profile/edit")
public String updateProfile(
     @ModelAttribute("user") User updatedUser,
     Authentication authentication,
     HttpServletRequest request,
     RedirectAttributes redirectAttributes) {

 try {
     User currentUser = userService.findByUsername(authentication.getName())
             .orElseThrow(() ->
                 new RuntimeException("User not found"));

     userService.updateProfile(
             currentUser.getId(),
             updatedUser.getUsername(),
             updatedUser.getEmail());

     // Log out so the user can log in with the updated username
     request.getSession().invalidate();

     redirectAttributes.addFlashAttribute(
             "success", "Profile updated successfully! Please log in again.");

     return "redirect:/login";

 } catch (RuntimeException e) {

     redirectAttributes.addFlashAttribute("error", e.getMessage());

     return "redirect:/profile/edit";
 }
}


// Display Change Password page
@GetMapping("/change-password")
public String showChangePasswordPage() {
    return "change-password";
}

// Process Change Password
@PostMapping("/change-password")
public String changePassword(
        @RequestParam String currentPassword,
        @RequestParam String newPassword,
        @RequestParam String confirmPassword,
        Authentication authentication,
        Model model) {

    // Check whether new passwords match
    if (!newPassword.equals(confirmPassword)) {
        model.addAttribute("error",
                "New passwords do not match.");
        return "change-password";
    }

    // Check minimum password length
    if (newPassword.length() < 8) {
        model.addAttribute("error",
                "Password must contain at least 8 characters.");
        return "change-password";
    }

    // Check whether old and new passwords are different
    if (currentPassword.equals(newPassword)) {
        model.addAttribute("error",
                "New password must be different from current password.");
        return "change-password";
    }

    try {
        userService.changePassword(
                authentication.getName(),
                currentPassword,
                newPassword);

        model.addAttribute("message",
                "Password changed successfully!");

    } catch (RuntimeException e) {
        model.addAttribute("error", e.getMessage());
    }

    return "change-password";
}


}
