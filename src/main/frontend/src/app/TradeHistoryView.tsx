import React, { Fragment } from 'react'
import { ActiveOrder } from '../api/api'
import { Box, Grid } from '@mui/material'
import { formatDate } from '../util/time'

export const TradeHistoryView: React.FC<{ order: ActiveOrder }> = ({
  order,
}) => {
  const a = order
  return (
    <Fragment>
      <Box
        sx={{
          display: 'contents',
          '&:hover > div': {
            backgroundColor: 'rgba(255, 255, 255, 0.08)',
          },
          '&:focus > div': {
            backgroundColor: 'rgba(255, 255, 255, 0.08)',
          },
        }}
      >
        <Grid item xs={7} sm={3}>
          {formatDate(a.created_at)}
        </Grid>
        <Grid item xs={5} sm={2} sx={{ textAlign: 'right' }}>
          {a.amount}
        </Grid>
        <Grid
          item
          xs={3}
          sm={2}
          sx={{ textAlign: ['left', 'center', 'center', 'center'] }}
        >
          {a.market}
        </Grid>
        <Grid item xs={9} sm={2} sx={{ textAlign: 'right' }}>
          {a.price.toFixed(2)} kr
        </Grid>
        <Grid
          item
          xs={3}
          sm={1}
          sx={{
            textAlign: ['left', 'center', 'center', 'center'],
          }}
        >
          {a.type}
        </Grid>
        <Grid item xs={9} sm={2} sx={{ textAlign: 'right' }}>
          {(a.price * a.amount).toFixed(2)} kr
        </Grid>
      </Box>
    </Fragment>
  )
}
