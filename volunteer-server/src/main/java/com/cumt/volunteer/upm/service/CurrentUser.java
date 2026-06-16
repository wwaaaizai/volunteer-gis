package com.cumt.volunteer.upm.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentUser {
    private Long userId;
    private String studentId;
    private String role;
}
