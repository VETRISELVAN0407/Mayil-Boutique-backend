package com.mayilboutique.address.dto.response;

import com.mayilboutique.address.entity.CustomerAddress;

public record AddressResponse(
        Long id,
        String fullName,
        String mobile,
        String line1,
        String line2,
        String city,
        String state,
        String pincode,
        boolean defaultAddress) {

    public static AddressResponse from(CustomerAddress a) {
        return new AddressResponse(a.getId(), a.getFullName(), a.getMobile(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPincode(), a.isDefaultAddress());
    }
}
