package com.mayilboutique.order.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderNoteRequest {

    /** Blank or missing clears the note. */
    @Size(max = 1000, message = "Note must be at most 1000 characters")
    private String note;
}
