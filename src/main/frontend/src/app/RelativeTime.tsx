import React, { useState } from 'react'
import useInterval from '../hooks/useInterval'
import { getRelativeTime } from '../util/time'

export const RelativeTime: React.FC<{ ts: Date }> = ({ ts }) => {
  const [time, setTime] = useState(new Date())
  useInterval(() => {
    setTime(new Date())
  }, 1000)
  return <span>{getRelativeTime(ts, time)}</span>
}
