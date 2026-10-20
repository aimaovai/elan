"use client";

import Image from "next/image";
import { useState } from "react";

interface EditorialImageProps {
  src: string | null | undefined;
  alt: string;
  className?: string;
  priority?: boolean;
  sizes?: string;
}

export function EditorialImage({
  src,
  alt,
  className = "",
  priority = false,
  sizes = "100vw",
}: EditorialImageProps) {
  const [failed, setFailed] = useState(false);
  if (!src || failed) {
    return <div className={`image-fallback ${className}`} role="img" aria-label={alt}>{alt}</div>;
  }
  return (
    <Image
      src={src}
      alt={alt}
      fill
      priority={priority}
      sizes={sizes}
      className={className}
      onError={() => setFailed(true)}
    />
  );
}
