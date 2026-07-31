package com.example.smartinventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockTransfer;

/** Repository for {@link StockTransfer} persistence operations. */
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    /**
     * Returns one page of the transfers that moved one product.
     *
     * @param productId identifier of the product
     * @param pageable  the page to return and the order to return it in
     * @return the requested page of that product's transfer history
     */
    Page<StockTransfer> findByProductId(Long productId, Pageable pageable);

    /**
     * Returns one page of the transfers a warehouse took part in on either side.
     *
     * @param sourceWarehouseId      identifier matched against the source side
     * @param destinationWarehouseId identifier matched against the destination side
     * @param pageable               the page to return and the order to return it in
     * @return the requested page of transfers into or out of that warehouse
     */
    Page<StockTransfer> findBySourceWarehouseIdOrDestinationWarehouseId(
            Long sourceWarehouseId, Long destinationWarehouseId, Pageable pageable);

}
