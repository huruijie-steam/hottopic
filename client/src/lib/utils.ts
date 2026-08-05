// 轻量 class 合并(替代 clsx + tailwind-merge)
export function cn(...classes: (string | false | null | undefined)[]): string {
  return classes.filter(Boolean).join(' ');
}
