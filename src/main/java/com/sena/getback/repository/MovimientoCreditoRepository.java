package com.sena.getback.repository;

import com.sena.getback.model.MovimientoCredito;
import com.sena.getback.model.ClienteFrecuente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCreditoRepository extends JpaRepository<MovimientoCredito, Long> {

    List<MovimientoCredito> findByClienteOrderByFechaDesc(ClienteFrecuente cliente);
}
