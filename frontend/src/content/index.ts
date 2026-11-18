import logos from './logos.json';
import books from './books.json';
import type { BookContent } from '../types';

const LOGOS = logos as Record<string, string>;
const CONTENT = books as unknown as Record<string, BookContent>;

/** Tool logos available for covers (the admin picks one when adding a book). */
export const LOGO_KEYS = Object.keys(LOGOS).filter((k) => k !== 'book').sort();

let counter = 0;

/** SVG markup of a tool logo. ids inside the SVG get a unique suffix so two copies on one page do not clash. */
export function logoSvg(key: string): string {
  const svg = LOGOS[key] ?? LOGOS.book;
  const n = ++counter;
  return svg.replace(/(id="|url\(#|href="#)([^")]+)/g, (_m, a: string, b: string) => `${a}${b}_${n}`);
}

/** Chapters, sample file and key concepts for the "look inside" pages. Books added by the admin have none. */
export function contentFor(id: number): BookContent {
  return CONTENT[String(id)] ?? {};
}
