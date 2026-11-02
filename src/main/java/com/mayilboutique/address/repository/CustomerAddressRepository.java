package com.mayilboutique.address.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.address.entity.CustomerAddress;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    Optional<CustomerAddress> findByIdAndUserId(Long id, Long userId);

    List<CustomerAddress> findByUserIdAndDefaultAddressTrue(Long userId);

    long countByUserId(Long userId);
}
