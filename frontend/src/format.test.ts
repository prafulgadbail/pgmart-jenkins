import { describe, expect, it } from 'vitest';
import { deliveryFor, discount, inr, timeAgo, trackStep } from './format';
import { contentFor, logoSvg, LOGO_KEYS } from './content';

describe('money', () => {
  it('formats paise as rupees with Indian grouping', () => {
    expect(inr(79900)).toBe('₹799');
    expect(inr(18450000)).toBe('₹1,84,500');
    expect(inr(79950)).toBe('₹799.50');
  });

  it('works out the discount from MRP and price', () => {
    expect(discount({ mrp: 119900, price: 69900 })).toBe(42);
  });

  it('charges delivery below ₹499 only', () => {
    expect(deliveryFor(49900)).toBe(0);
    expect(deliveryFor(34900)).toBe(4000);
  });
});

describe('orders', () => {
  it('maps status to the tracking step', () => {
    expect(trackStep('PLACED')).toBe(0);
    expect(trackStep('DELIVERED')).toBe(3);
    expect(trackStep('CANCELLED')).toBe(-1);
  });

  it('shows recent times in words', () => {
    const now = Date.parse('2026-11-17T10:00:00Z');
    expect(timeAgo('2026-11-17T09:59:30Z', now)).toBe('just now');
    expect(timeAgo('2026-11-17T09:30:00Z', now)).toBe('30 min ago');
  });
});

describe('book content', () => {
  it('has logos and chapters for the seeded books', () => {
    expect(LOGO_KEYS).toContain('k8s');
    expect(contentFor(9).flow?.length).toBeGreaterThan(0);
    expect(contentFor(99999)).toEqual({});
  });

  it('gives every logo copy its own SVG ids', () => {
    const a = logoSvg('azure');
    const b = logoSvg('azure');
    expect(a).not.toBe(b);
    expect(logoSvg('no-such-logo')).toContain('<svg');
  });
});
