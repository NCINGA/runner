package com.ncinga.runner.dtos;

import lombok.Data;
import org.modelmapper.ModelMapper;

import java.io.Serializable;

@Data
public abstract class BaseClass<T, D> implements Serializable {
    private static final ModelMapper modelMapper = new ModelMapper();

    public static <T extends BaseClass<T, D>, D> T fromEntity(D entity, Class<T> dtoClass) {
        T dto = modelMapper.map(entity, dtoClass);
        return dto;
    }

    public D toEntity(Class<D> type) {
        D entity = modelMapper.map(this, type);
        return entity;
    }
}

