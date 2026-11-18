import { useMemo } from 'react';
import { logoSvg } from '../content';

/** A tool logo. The SVG files ship with the frontend, so covers need no image storage. */
export function Logo({ name }: { name: string }) {
  const html = useMemo(() => logoSvg(name), [name]);
  return <span style={{ display: 'contents' }} dangerouslySetInnerHTML={{ __html: html }} />;
}
