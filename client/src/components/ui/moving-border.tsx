import type { ReactNode } from 'react';
import { cn } from '../../lib/utils';

/** 动态渐变边框(hover 时旋转光晕) */
export function MovingBorder({
  children,
  duration = 3000,
  className,
  containerClassName,
}: {
  children: ReactNode;
  duration?: number;
  className?: string;
  containerClassName?: string;
}) {
  return (
    <div
      className={cn(
        'relative h-auto w-auto overflow-hidden bg-transparent p-[1px]',
        containerClassName,
      )}
    >
      <div
        className="absolute inset-0 group-hover:opacity-100 opacity-0 transition-opacity duration-300"
        style={{
          background:
            'conic-gradient(from var(--angle, 0deg), transparent 60%, #6366f1, #ec4899, transparent 40%)',
          animation: `spin ${duration}ms linear infinite`,
        }}
      />
      <div className={cn('relative flex items-center justify-center gap-2 backdrop-blur-xl', className)}>
        {children}
      </div>
    </div>
  );
}
