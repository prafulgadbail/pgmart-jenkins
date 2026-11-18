package com.pgmart.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PricingTest {

    @Test
    void deliveryIsFreeFrom499() {
        var totals = Pricing.of(List.of(new CatalogClient.ReservedLine(1, "A", 1, 499_00, 799_00)));
        assertThat(totals.delivery()).isZero();
        assertThat(totals.total()).isEqualTo(499_00);
    }

    @Test
    void smallOrderPaysDelivery() {
        var totals = Pricing.of(List.of(new CatalogClient.ReservedLine(1, "A", 1, 349_00, 599_00)));
        assertThat(totals.delivery()).isEqualTo(40_00);
        assertThat(totals.total()).isEqualTo(389_00);
    }

    @Test
    void quantitiesAreMultiplied() {
        var totals = Pricing.of(List.of(
                new CatalogClient.ReservedLine(1, "A", 2, 549_00, 899_00),
                new CatalogClient.ReservedLine(2, "B", 1, 699_00, 1199_00)));
        assertThat(totals.items()).isEqualTo(1797_00);
        assertThat(totals.mrp()).isEqualTo(2997_00);
    }

    @Test
    void rupeesFormat() {
        assertThat(Pricing.rupees(799_00)).isEqualTo("₹799");
        assertThat(Pricing.rupees(799_50)).isEqualTo("₹799.50");
    }
}
