package com.sena.getback.repository;

import com.sena.getback.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Integer> {

    // 🔹 Verifica si ya existe una mesa con el mismo número en una ubicación específica
    boolean existsByNumeroAndUbicacion_Id(String numero, Integer ubicacionId);

    // 🔹 Verifica si ya existe una mesa con el mismo número y ubicación, excluyendo una mesa por ID
    boolean existsByNumeroAndUbicacion_IdAndIdNot(String numero, Integer ubicacionId, Integer id);

    // 🔹 En caso de querer validar mesas sin ubicación asignada (opcional)
    boolean existsByNumeroAndUbicacionIsNull(String numero);
}
