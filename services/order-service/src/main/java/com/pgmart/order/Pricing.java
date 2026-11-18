package com.pgmart.order;

import java.util.List;

/** Order totals. All amounts in paise (₹1 = 100 paise), so there is no rounding. */
public final class Pricing {

    public static final long FREE_DELIVERY_FROM = 499_00;
    public static final long DELIVERY_FEE = 40_00;

    public record Totals(long items, long mrp, long delivery, long total) {
    }

    private Pricing() {
    }

    public static Totals of(List<CatalogClient.ReservedLine> lines) {
        long items = 0;
        long mrp = 0;
        for (var l : lines) {
            items += l.price() * l.qty();
            mrp += l.mrp() * l.qty();
        }
        long delivery = items >= FREE_DELIVERY_FROM ? 0 : DELIVERY_FEE;
        return new Totals(items, mrp, delivery, items + delivery);
    }

    public static String rupees(long paise) {
        return paise % 100 == 0 ? "₹" + (paise / 100) : String.format("₹%d.%02d", paise / 100, paise % 100);
    }
}
