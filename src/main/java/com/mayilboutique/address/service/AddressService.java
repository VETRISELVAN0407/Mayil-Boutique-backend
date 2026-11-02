package com.mayilboutique.address.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.address.dto.request.AddressRequest;
import com.mayilboutique.address.dto.response.AddressResponse;
import com.mayilboutique.address.entity.CustomerAddress;
import com.mayilboutique.address.repository.CustomerAddressRepository;
import com.mayilboutique.core.exception.BusinessException;

@Service
public class AddressService {

    private final CustomerAddressRepository addressRepository;

    public AddressService(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        CustomerAddress address = new CustomerAddress();
        address.setUserId(userId);
        apply(address, request);
        // the first address is always the default
        boolean makeDefault = request.isDefaultAddress() || addressRepository.countByUserId(userId) == 0;
        if (makeDefault) {
            clearDefault(userId);
        }
        address.setDefaultAddress(makeDefault);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long id, AddressRequest request) {
        CustomerAddress address = find(userId, id);
        apply(address, request);
        if (request.isDefaultAddress() && !address.isDefaultAddress()) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefault(Long userId, Long id) {
        CustomerAddress address = find(userId, id);
        clearDefault(userId);
        address.setDefaultAddress(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        CustomerAddress address = find(userId, id);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        addressRepository.save(next);
                    });
        }
    }

    /** Resolves an address owned by the user; 404 if missing or owned by someone else. */
    @Transactional(readOnly = true)
    public CustomerAddress find(Long userId, Long id) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    private void clearDefault(Long userId) {
        List<CustomerAddress> defaults = addressRepository.findByUserIdAndDefaultAddressTrue(userId);
        defaults.forEach(a -> a.setDefaultAddress(false));
        addressRepository.saveAll(defaults);
        addressRepository.flush();
    }

    private void apply(CustomerAddress address, AddressRequest request) {
        address.setFullName(request.getFullName().trim());
        address.setMobile(request.getMobile().trim());
        address.setLine1(request.getLine1().trim());
        address.setLine2(request.getLine2() == null || request.getLine2().isBlank() ? null : request.getLine2().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPincode(request.getPincode().trim());
    }
}
