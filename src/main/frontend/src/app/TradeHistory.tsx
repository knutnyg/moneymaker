import React from 'react'
import { AppState } from '../api/api'
import { Grid } from '@mui/material'
import { TradeHistoryView } from './TradeHistoryView'

export const TradeHistory: React.FC<{ state: AppState }> = ({ state }) => {
  const list = state.filledOrders.filledOrders || []

  return (
    <Grid container spacing={0}>
      {list.map((a) => (
        <TradeHistoryView key={`${a.id}`} order={a} />
      ))}
    </Grid>
  )
}