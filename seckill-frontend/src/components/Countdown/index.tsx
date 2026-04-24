import { useEffect, useState, useCallback } from 'react';
import { formatCountdown } from '../../utils/format';
import './index.css';

interface CountdownProps {
  targetTime: string | Date;
  onEnd?: () => void;
  className?: string;
}

export function Countdown({ targetTime, onEnd, className = '' }: CountdownProps) {
  const [remaining, setRemaining] = useState(0);

  const calculateRemaining = useCallback(() => {
    const target = new Date(targetTime).getTime();
    const now = Date.now();
    const diff = Math.max(0, Math.floor((target - now) / 1000));
    return diff;
  }, [targetTime]);

  useEffect(() => {
    setRemaining(calculateRemaining());

    const timer = setInterval(() => {
      const diff = calculateRemaining();
      setRemaining(diff);

      if (diff === 0) {
        clearInterval(timer);
        onEnd?.();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [calculateRemaining, onEnd]);

  if (remaining === 0) {
    return <span className={`countdown ${className}`}>00:00:00</span>;
  }

  return (
    <span className={`countdown ${className}`}>
      {formatCountdown(remaining)}
    </span>
  );
}
