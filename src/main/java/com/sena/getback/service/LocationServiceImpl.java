package com.sena.getback.service;

import com.sena.getback.model.Location;
import com.sena.getback.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Override
    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    @Override
    public Optional<Location> findById(Integer id) {
        return locationRepository.findById(id);
    }

    @Override
    public Location save(Location location) {
        return locationRepository.save(location);
    }

    @Override
    public Location update(Location location) {
        return locationRepository.save(location);
    }

    @Override
    public void deleteById(Integer id) {
        locationRepository.deleteById(id);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return locationRepository.existsByNombre(nombre);
    }

    @Override
    public boolean existsByNombreAndIdNot(String nombre, Integer id) {
        return locationRepository.existsByNombreAndIdNot(nombre, id);
    }
}