package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Employee;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.EmployeeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/employees")
public class AdminEmployeeController {

    private final EmployeeService employeeService;
    private final AppUserService  appUserService;

    public AdminEmployeeController(EmployeeService employeeService, AppUserService appUserService) {
        this.employeeService = employeeService;
        this.appUserService  = appUserService;
    }

    private boolean isOwner(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("view", "admin/employee/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/add")
    public String addForm(Authentication auth, Model model) {
        model.addAttribute("isOwner", isOwner(auth));
        model.addAttribute("newUser", new AppUser());
        model.addAttribute("view", "admin/employee/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newUser") AppUser newUser,
                       @RequestParam String confirmPassword,
                       @RequestParam String roleName,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDate,
                       @RequestParam(required = false) String note,
                       Authentication auth,
                       RedirectAttributes ra) {

        // Chỉ OWNER được cấp role ADMIN/OWNER — cùng quy tắc với
        // AdminUserController.addRole() đang áp dụng cho user thường.
        if (!isOwner(auth) && (roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_OWNER"))) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền tạo nhân viên với role này!");
            return "redirect:/admin/employees/add";
        }

        try {
            if (newUser.getPassword() == null || !newUser.getPassword().equals(confirmPassword)) {
                throw new IllegalArgumentException("Mật khẩu nhập lại không khớp!");
            }
            employeeService.createEmployee(newUser, roleName, hireDate, note);
            ra.addFlashAttribute("successMessage", "Đã thêm nhân viên mới!");
            return "redirect:/admin/employees";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/employees/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Authentication auth, Model model) {
        model.addAttribute("employee", employeeService.getEmployeeById(id));
        model.addAttribute("allRoles", appUserService.getAllRoles());
        model.addAttribute("isOwner", isOwner(auth));
        model.addAttribute("view", "admin/employee/edit");
        return "layouts/admin-layout";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Integer id,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDate,
                         @RequestParam(required = false) String note,
                         RedirectAttributes ra) {
        employeeService.updateEmployee(id, hireDate, note);
        ra.addFlashAttribute("successMessage", "Đã cập nhật thông tin nhân viên!");
        return "redirect:/admin/employees";
    }

    @GetMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
        // Trước đây endpoint này không check quyền gì cả — 1 ADMIN thường có
        // thể gọi thẳng URL này để cho ADMIN khác hoặc OWNER "nghỉ việc"
        // (kéo theo khóa luôn tài khoản đăng nhập của họ, xem
        // EmployeeService.setActive). Chỉ OWNER mới được đổi trạng thái của
        // nhân viên đang có role ADMIN/OWNER — cùng quy tắc với addRole/removeRole.
        Employee employee = employeeService.getEmployeeById(id);
        boolean targetIsAdminOrOwner = employee.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_OWNER"));
        if (!isOwner(auth) && targetIsAdminOrOwner) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền đổi trạng thái nhân viên này!");
            return "redirect:/admin/employees";
        }

        // Không cho tự "cho mình nghỉ việc" — hành động này kéo theo khóa
        // luôn tài khoản đăng nhập (xem EmployeeService.setActive), nên phải
        // do người khác chủ động thực hiện, không tự thao tác lên chính mình.
        if (employee.getIsActive() && employee.getUser().getUsername().equals(auth.getName())) {
            ra.addFlashAttribute("errorMessage", "Bạn không thể tự đổi trạng thái làm việc của chính mình!");
            return "redirect:/admin/employees";
        }

        employeeService.toggleActive(id);
        ra.addFlashAttribute("successMessage", "Đã đổi trạng thái làm việc!");
        return "redirect:/admin/employees";
    }

    // ── QUẢN LÝ ROLE — chuyển hẳn từ AdminUserController sang đây ────────

    private static final java.util.Set<String> STAFF_ROLES =
            java.util.Set.of("ROLE_CASHIER", "ROLE_ADMIN", "ROLE_OWNER");

    @PostMapping("/{id}/role/add")
    public String addRole(@PathVariable Integer id,
                          @RequestParam String roleName,
                          Authentication auth,
                          RedirectAttributes ra) {
        if (!isOwner(auth) && (roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_OWNER"))) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền cấp role này!");
            return "redirect:/admin/employees/edit/" + id;
        }

        try {
            Employee employee = employeeService.getEmployeeById(id);
            appUserService.addRole(employee.getUser().getId(), roleName);

            // Vừa thêm lại role nhân viên nhưng hồ sơ đang bị đánh dấu nghỉ
            // việc -> tự kích hoạt lại, tránh admin phải bấm thêm 1 bước.
            if (STAFF_ROLES.contains(roleName) && !employee.getIsActive()) {
                employeeService.setActive(id, true);
            }
            ra.addFlashAttribute("successMessage", "Đã thêm role!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/employees/edit/" + id;
    }

    @PostMapping("/{id}/role/remove")
    public String removeRole(@PathVariable Integer id,
                             @RequestParam String roleName,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (!isOwner(auth) && (roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_OWNER"))) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền xóa role này!");
            return "redirect:/admin/employees/edit/" + id;
        }

        try {
            Employee employee = employeeService.getEmployeeById(id);

            // Không cho Owner tự xóa role OWNER của chính mình — kể cả khi hệ
            // thống vẫn còn Owner khác. Việc tước quyền quản trị cao nhất của
            // 1 tài khoản nên luôn do MỘT Owner KHÁC chủ động thực hiện, tránh
            // trường hợp tự thao tác nhầm hoặc bị lừa (social engineering) tự
            // hạ quyền chính mình.
            if (roleName.equals("ROLE_OWNER") && employee.getUser().getUsername().equals(auth.getName())) {
                ra.addFlashAttribute("errorMessage", "Bạn không thể tự xóa role Owner của chính mình!");
                return "redirect:/admin/employees/edit/" + id;
            }

            // Chặn thêm ở đây trong AppUserService.removeRole() nếu đang có
            // ca OPEN — bắt IllegalStateException cùng nhánh với các lỗi khác.
            appUserService.removeRole(employee.getUser().getId(), roleName);

            // Re-fetch để lấy đúng danh sách role SAU khi đã xóa (tránh đọc
            // collection cũ còn cache trong object employee ở trên).
            Employee refreshed = employeeService.getEmployeeById(id);
            boolean stillStaff = refreshed.getUser().getRoles().stream()
                    .anyMatch(r -> STAFF_ROLES.contains(r.getName()));
            if (!stillStaff && refreshed.getIsActive()) {
                employeeService.setActive(id, false);
            }
            ra.addFlashAttribute("successMessage", "Đã xóa role!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/employees/edit/" + id;
    }
}