// PedidoRepository.java
package com.sena.getback.repository;

import com.sena.getback.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByMesaId(Integer mesaId);
}