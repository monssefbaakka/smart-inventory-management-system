package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockTransfer;

/** Repository for {@link StockTransfer} persistence operations. */
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    /**
     * Returns every recorded transfer, most recent first.
     *
     * @return the full transfer history
     */
    List<StockTransfer> findAllByOrderByCreatedAtDesc();

    /**
     * Returns the transfers that moved one product, most recent first.
     *
     * @param productId identifier of the product
     * @return that product's transfer history
     */
    List<StockTransfer> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Returns the transfers a warehouse took part in on either side, most recent first.
     *
     * @param sourceWarehouseId      identifier matched against the source side
     * @param destinationWarehouseId identifier matched against the destination side
     * @return transfers into or out of that warehouse
     */
    List<StockTransfer> findBySourceWarehouseIdOrDestinationWarehouseIdOrderByCreatedAtDesc(
            Long sourceWarehouseId, Long destinationWarehouseId);

}
