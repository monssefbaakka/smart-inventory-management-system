package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MovementTypeTest {

    @ParameterizedTest
    @EnumSource(value = MovementType.class, names = {"TRANSFER_OUT", "TRANSFER_IN"})
    void transferLegsAreRecognised(MovementType type) {
        assertThat(type.isTransferLeg()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = MovementType.class, names = {"IN", "OUT", "ADJUSTMENT"})
    void plainMovementsAreNotTransferLegs(MovementType type) {
        assertThat(type.isTransferLeg()).isFalse();
    }

}
