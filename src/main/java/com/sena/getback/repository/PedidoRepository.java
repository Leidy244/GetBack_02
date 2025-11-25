// PedidoRepository.java
package com.sena.getback.repository;

import com.sena.getback.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para la entidad Pedido.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /**
     * Busca pedidos por el ID de la mesa.
     * 
     * @param mesaId ID de la mesa.
     * @return Lista de pedidos asociados a la mesa.
     */
    List<Pedido> findByMesaId(Integer mesaId);

    /**
     * Busca pedidos con filtros y paginación para el historial de pedidos.
     * 
     * @param mesa      Número de la mesa (opcional).
     * @param estado    Estado del pedido (opcional).
     * @param desde     Fecha de creación desde (opcional).
     * @param hasta     Fecha de creación hasta (opcional).
     * @param usuarioId ID del usuario (mesero) que atendió el pedido (opcional).
     * @param pageable  Configuración de paginación.
     * @return Página de pedidos que cumplen con los filtros.
     */
    @Query("SELECT p FROM Pedido p WHERE " +
           "(:mesa IS NULL OR :mesa = '' OR LOWER(p.mesa.numero) LIKE LOWER(CONCAT('%', :mesa, '%'))) AND " +
           "(:estado IS NULL OR :estado = '' OR UPPER(p.estado.nombreEstado) = UPPER(:estado)) AND " +
           "(:desde IS NULL OR p.fechaCreacion >= :desde) AND " +
           "(:hasta IS NULL OR p.fechaCreacion <= :hasta) AND " +
           "(:usuarioId IS NULL OR p.usuario.id = :usuarioId)")
    Page<Pedido> buscarHistorial(@Param("mesa") String mesa,
                                 @Param("estado") String estado,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta,
                                 @Param("usuarioId") Long usuarioId,
                                 Pageable pageable);
}