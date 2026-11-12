package com.ems.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ems.entity.Department;
import com.ems.service.DepartmentService;

@Controller
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // Display all departments - ADMIN and HR
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/departments")
    public String listDepartments(Model model) {

        model.addAttribute("departments",
                departmentService.getAllDepartments());

        return "departments";
    }

    // Display Add Department Form - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/departments/add")
    public String showAddDepartmentForm(Model model) {

        model.addAttribute("department", new Department());

        return "add-department";
    }

    // Save Department - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/departments/save")
    public String saveDepartment(
            @ModelAttribute("department") Department department,
            RedirectAttributes redirectAttributes) {

        try {
            departmentService.saveDepartment(department);

            redirectAttributes.addFlashAttribute(
                    "success", "Department added successfully!");

            return "redirect:/departments";

        } catch (RuntimeException e) {

            modelError(redirectAttributes, e.getMessage());

            return "redirect:/departments/add";
        }
    }

    // Display Edit Department Form - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/departments/edit/{id}")
    public String showEditDepartmentForm(
            @PathVariable Long id, Model model) {

        model.addAttribute("department",
                departmentService.getDepartmentById(id));

        return "edit-department";
    }

    // Update Department - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/departments/update/{id}")
    public String updateDepartment(
            @PathVariable Long id,
            @ModelAttribute("department") Department department,
            RedirectAttributes redirectAttributes) {

        try {
            departmentService.updateDepartment(id, department);

            redirectAttributes.addFlashAttribute(
                    "success", "Department updated successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/departments";
    }

    // Delete Department - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/departments/delete/{id}")
    public String deleteDepartment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            departmentService.deleteDepartment(id);

            redirectAttributes.addFlashAttribute(
                    "success", "Department deleted successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/departments";
    }

    private void modelError(
            RedirectAttributes redirectAttributes, String message) {

        redirectAttributes.addFlashAttribute("error", message);
    }
}

