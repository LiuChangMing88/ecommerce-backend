package com.github.liuchangming88.ecommerce_backend.service.order;

import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrderItems;
import com.github.liuchangming88.ecommerce_backend.model.order.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.model.order.repository.LocalOrderRepository;
import com.github.liuchangming88.ecommerce_backend.model.product.Inventory;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class OrderRestockServiceTest {

    @Mock LocalOrderRepository localOrderRepository;
    @InjectMocks OrderRestockService restockService;

    private static LocalOrder order(long id,
                                    OrderStatus status,
                                    boolean restocked,
                                    OffsetDateTime expiresAt,
                                    int... qtys) {
        LocalOrder o = new LocalOrder();
        o.setId(id);
        o.setStatus(status);
        o.setRestocked(restocked);
        o.setExpiresAt(expiresAt);
        List<LocalOrderItems> lines = new ArrayList<>();
        int prodId = 1;
        for (int q : qtys) {
            Product p = new Product();
            p.setId((long) prodId++);
            Inventory inv = new Inventory();
            inv.setQuantity(100L);
            p.setInventory(inv);

            LocalOrderItems li = new LocalOrderItems();
            li.setQuantity(q);
            li.setProduct(p);
            lines.add(li);
        }
        o.setItems(lines);
        return o;
    }

    @Test
    void emptyBatch_returns0() {
        when(localOrderRepository.findExpiredPendingOrderIds(any(), any(PageRequest.class))).thenReturn(List.of());
        int r = restockService.failAndRestockExpired(OffsetDateTime.now(), 10);
        assertThat(r).isZero();
        verify(localOrderRepository, never()).findById(anyLong());
    }

    @Test
    void orderVanished_throws() {
        OffsetDateTime now = OffsetDateTime.now();
        when(localOrderRepository.findExpiredPendingOrderIds(eq(now), any(PageRequest.class))).thenReturn(List.of(5L));
        when(localOrderRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> restockService.failAndRestockExpired(now, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order 5");
    }

    @Test
    void skipsNonPendingRestockedNullExpiryAndNotYetExpired() {
        OffsetDateTime now = OffsetDateTime.now();

        LocalOrder nonPending = order(1, OrderStatus.FAILED, false, now.minusMinutes(5), 2);
        LocalOrder alreadyRestocked = order(2, OrderStatus.PENDING, true, now.minusMinutes(5), 3);
        LocalOrder noExpiry = order(3, OrderStatus.PENDING, false, null, 4);
        LocalOrder notYetExpired = order(4, OrderStatus.PENDING, false, now.plusMinutes(1), 5);

        when(localOrderRepository.findExpiredPendingOrderIds(eq(now), any(PageRequest.class)))
                .thenReturn(List.of(1L,2L,3L,4L));
        when(localOrderRepository.findById(1L)).thenReturn(Optional.of(nonPending));
        when(localOrderRepository.findById(2L)).thenReturn(Optional.of(alreadyRestocked));
        when(localOrderRepository.findById(3L)).thenReturn(Optional.of(noExpiry));
        when(localOrderRepository.findById(4L)).thenReturn(Optional.of(notYetExpired));

        int processed = restockService.failAndRestockExpired(now, 10);
        assertThat(processed).isZero();
        assertThat(nonPending.getStatus()).isEqualTo(OrderStatus.FAILED); // unchanged
        assertThat(alreadyRestocked.isRestocked()).isTrue();
        assertThat(noExpiry.isRestocked()).isFalse();
        assertThat(notYetExpired.isRestocked()).isFalse();
    }

    @Test
    void processesExpiredPending() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalOrder expired = order(10, OrderStatus.PENDING, false, now.minusMinutes(1), 2,3);
        when(localOrderRepository.findExpiredPendingOrderIds(eq(now), any(PageRequest.class)))
                .thenReturn(List.of(10L));
        when(localOrderRepository.findById(10L)).thenReturn(Optional.of(expired));

        int processed = restockService.failAndRestockExpired(now, 5);

        assertThat(processed).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(expired.isRestocked()).isTrue();
        // inventory +2 and +3 added
        Long q1 = expired.getItems().get(0).getProduct().getInventory().getQuantity();
        Long q2 = expired.getItems().get(1).getProduct().getInventory().getQuantity();
        assertThat(q1).isEqualTo(102L);
        assertThat(q2).isEqualTo(103L);
    }

    @Test
    void idempotentRestock_notDoubleAppliedOnSecondRun() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalOrder expired = order(50, OrderStatus.PENDING, false, now.minusMinutes(2), 5);
        when(localOrderRepository.findExpiredPendingOrderIds(eq(now), any(PageRequest.class)))
                .thenReturn(List.of(50L));
        when(localOrderRepository.findById(50L)).thenReturn(Optional.of(expired));

        int first = restockService.failAndRestockExpired(now, 1);
        assertThat(first).isEqualTo(1);
        Long qtyAfterFirst = expired.getItems().get(0).getProduct().getInventory().getQuantity();
        assertThat(qtyAfterFirst).isEqualTo(105L);

        // Second pass: restocked=true → skipped
        int second = restockService.failAndRestockExpired(now, 1);
        assertThat(second).isZero();
        Long qtyAfterSecond = expired.getItems().get(0).getProduct().getInventory().getQuantity();
        assertThat(qtyAfterSecond).isEqualTo(105L); // unchanged
    }

    @Test
    void expiresAtEqualNow_notProcessed() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalOrder equalExpiry = order(70, OrderStatus.PENDING, false, now, 4);
        when(localOrderRepository.findExpiredPendingOrderIds(eq(now), any(PageRequest.class)))
                .thenReturn(List.of(70L));
        when(localOrderRepository.findById(70L)).thenReturn(Optional.of(equalExpiry));

        int processed = restockService.failAndRestockExpired(now, 3);
        assertThat(processed).isZero();
        assertThat(equalExpiry.isRestocked()).isFalse();
        assertThat(equalExpiry.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}