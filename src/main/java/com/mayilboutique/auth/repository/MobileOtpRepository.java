package com.mayilboutique.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.auth.entity.MobileOtp;

public interface MobileOtpRepository extends JpaRepository<MobileOtp, Long> {

    Optional<MobileOtp> findTopByMobileOrderByIdDesc(String mobile);
}
