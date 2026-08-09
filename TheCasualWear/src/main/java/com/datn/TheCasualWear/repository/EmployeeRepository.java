package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    boolean existsByEmployeeCode(String employeeCode);

    Optional<Employee> findByUserId(Integer userId);
}
