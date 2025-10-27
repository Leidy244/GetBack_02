package com.sena.getback.service;

import com.sena.getback.model.Location;
import java.util.List;
import java.util.Optional;

public interface LocationService {
    List<Location> findAll();
    Optional<Location> findById(Integer id);
    Location save(Location location);
    Location update(Location location);
    void deleteById(Integer id);
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Integer id);
}