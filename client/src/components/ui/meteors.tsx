import { useState } from 'react';
import { cn } from '../../lib/utils';

/** 流星雨特效 */
export function Meteors({ number = 12, className }: { number?: number; className?: string }) {
  // 随机参数用 useState 惰性初始化,仅在组件挂载时生成一次(保持 render 纯净)
  const [meteors] = useState(() =>
    Array.from({ length: number }).map(() => ({
      left: Math.floor(Math.random() * 800 - 400) + 'px',
      delay: Math.random() * 0.6 + 0.2 + 's',
      duration: Math.floor(Math.random() * 8 + 2) + 's',
    })),
  );

  return (
    <>
      {meteors.map((m, idx) => (
        <span
          key={'meteor' + idx}
          className={cn(
            'animate-meteor-effect absolute h-0.5 w-0.5 rounded-full bg-slate-400 shadow-[0_0_0_1px_#ffffff10] rotate-[215deg]',
            "before:content-[''] before:absolute before:top-1/2 before:transform before:-translate-y-[50%] before:w-[50px] before:h-[1px] before:bg-gradient-to-r before:from-[#64748b] before:to-transparent",
            className,
          )}
          style={{
            top: 0,
            left: m.left,
            animationDelay: m.delay,
            animationDuration: m.duration,
          }}
        />
      ))}
    </>
  );
}
