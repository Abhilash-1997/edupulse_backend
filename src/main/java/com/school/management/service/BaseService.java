package com.school.management.service;

import java.util.UUID;

public interface BaseService<T, ID> {
    T findById(ID id);
    void delete(ID id);
}