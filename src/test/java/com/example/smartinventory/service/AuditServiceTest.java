package com.example.smartinventory.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.AuditLog;
import com.example.smartinventory.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordUsesAuthenticatedUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "pw", List.of()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record("Product", 7L, AuditAction.CREATE);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("Product");
        assertThat(saved.getEntityId()).isEqualTo(7L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getUsername()).isEqualTo("alice");
    }

    @Test
    void recordFallsBackToAnonymousWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record("Product", 7L, AuditAction.DELETE);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("anonymous");
    }

    @Test
    void findAllReturnsThePageTheRepositoryFinds() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLog entry = AuditLog.builder().id(1L).entityType("Product").entityId(1L)
                .action(AuditAction.UPDATE).username("bob").build();
        when(auditLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        Page<AuditLog> result = auditService.findAll(pageable);

        assertThat(result.getContent()).containsExactly(entry);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

}
