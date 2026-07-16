package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void onCreateSetsTimestamps() {
        User user = new User();

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        User user = new User();
        user.onCreate();

        user.onUpdate();

        assertThat(user.getUpdatedAt()).isNotNull();
    }

}
