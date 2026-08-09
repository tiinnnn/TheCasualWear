package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Employee;
import com.datn.TheCasualWear.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AppUserService     appUserService;

    private static final int    MAX_EMPLOYEE_NUMBER = 9999;
    private static final String CODE_PREFIX         = "NV";

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + id));
    }

    // Tự sinh mã kế tiếp dạng NV0001..NV9999 — không cho admin gõ tay để
    // tránh trùng/sai định dạng. Lấy số lớn nhất hiện có rồi +1.
    private String generateNextEmployeeCode() {
        int maxNum = employeeRepository.findAll().stream()
                .map(Employee::getEmployeeCode)
                .filter(c -> c != null && c.matches(CODE_PREFIX + "\\d{4}"))
                .mapToInt(c -> Integer.parseInt(c.substring(CODE_PREFIX.length())))
                .max()
                .orElse(0);

        int next = maxNum + 1;
        if (next > MAX_EMPLOYEE_NUMBER) {
            throw new IllegalStateException(
                    "Đã dùng hết mã nhân viên khả dụng (" + CODE_PREFIX + "0001–"
                            + CODE_PREFIX + MAX_EMPLOYEE_NUMBER + ")!");
        }
        return String.format(CODE_PREFIX + "%04d", next);
    }

    /**
     * Tạo tài khoản AppUser mới + thông tin nhân viên cùng lúc. Dùng lại
     * AppUserService.createUserWithRole() để đảm bảo cùng 1 bộ validate
     * (username/password/email/phone) với luồng đăng ký khách hàng thường.
     * Mã nhân viên tự sinh, không nhận từ client.
     */
    @Transactional
    public Employee createEmployee(AppUser newUser, String roleName,
                                   LocalDate hireDate, String note) {
        String code = generateNextEmployeeCode();
        if (employeeRepository.existsByEmployeeCode(code)) {
            // Cực hiếm khi xảy ra race condition (2 request tạo cùng lúc) —
            // vẫn check lại cho chắc trước khi lưu.
            throw new IllegalStateException("Trùng mã nhân viên, vui lòng thử lại!");
        }

        // Tạo AppUser trước — nếu lỗi validate (trùng username/email...) sẽ
        // throw ngay tại đây, chưa tạo Employee nào cả.
        AppUser savedUser = appUserService.createUserWithRole(newUser, roleName);

        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setEmployeeCode(code);
        employee.setHireDate(hireDate);
        employee.setIsActive(true);
        employee.setNote(note);
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Integer id, LocalDate hireDate, String note) {
        Employee employee = getEmployeeById(id);
        employee.setHireDate(hireDate);
        employee.setNote(note);
        return employeeRepository.save(employee);
    }

    public void toggleActive(Integer id) {
        Employee employee = getEmployeeById(id);
        setActive(id, !employee.getIsActive());
    }

    // Set tường minh (không đảo ngược) — dùng cho cả admin bấm tay
    // (toggleActive) lẫn đồng bộ tự động theo role (add/removeRole).
    // Đồng thời khóa/mở khóa LUÔN tài khoản đăng nhập (AppUser.enabled) —
    // nhân viên "nghỉ việc" thì không được đăng nhập vào hệ thống nữa.
    public void setActive(Integer id, boolean active) {
        Employee employee = getEmployeeById(id);
        employee.setIsActive(active);
        employeeRepository.save(employee);

        if (active) {
            appUserService.unlockUser(employee.getUser().getId());
        } else {
            appUserService.lockUser(employee.getUser().getId());
        }
    }
}