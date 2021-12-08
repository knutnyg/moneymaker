import { useEffect, useRef } from 'react'

function useInterval<T extends Function>(callback: T, delayMillis: number) {
  const savedCallback = useRef(callback)

  // Remember the latest callback if it changes.
  useEffect(() => {
    savedCallback.current = callback
  }, [callback])

  // Set up the interval.
  useEffect(() => {
    // Don't schedule if no delay is specified.
    if (delayMillis === null) {
      return
    }

    const id = setInterval(() => savedCallback.current(), delayMillis)

    return () => clearInterval(id)
  }, [delayMillis])
}

export default useInterval
