package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    boolean existsByEmployeeCode(String employeeCode);

    Optional<Employee> findByUserId(Integer userId);

    @Query("SELECT DISTINCT e FROM Employee e JOIN e.user u WHERE " +
            "(:keyword IS NULL OR " +
            " LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " u.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Employee> searchEmployees(@Param("keyword") String keyword, Pageable pageable);
}