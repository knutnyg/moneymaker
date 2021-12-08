import { ActiveOrder, AppState } from '../api/api'
import { Box, Grid, Paper } from '@mui/material'
import { RelativeTime } from './RelativeTime'
import React, { Fragment } from 'react'
import { PrevActionSet } from './PrevActionSet'
import { formatDate } from '../util/time'
import { H2 } from './Base'
import { BalanceView } from './BalanceView'

const FilledOrderView: React.FC<{ order: ActiveOrder }> = ({ order }) => {
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
            mb: [1, 1],
          }}
        >
          {a.type}
        </Grid>
        <Grid item xs={9} sm={2} sx={{ textAlign: 'right', mb: [1, 1] }}>
          {(a.price * a.amount).toFixed(2)} kr
        </Grid>
      </Box>
    </Fragment>
  )
}

const FilledOrdersView: React.FC<{ state: AppState }> = ({ state }) => {
  const list = state.filledOrders.filledOrders || []

  return (
    <Grid container spacing={0}>
      {list.map((a) => (
        <FilledOrderView key={`${a.id}`} order={a} />
      ))}
    </Grid>
  )
}

const ActiveOrdersView: React.FC<{ state: AppState }> = ({ state }) => {
  const list = state.activeTrades.activeOrders || []

  return (
    <Grid container spacing={0}>
      {list.map((a) => (
        <FilledOrderView key={`${a.id}`} order={a} />
      ))}
    </Grid>
  )
}

export const AppStateView: React.FC<{ state?: AppState }> = ({ state }) => {
  if (!state) {
    return <div>Loading...</div>
  }
  const marketMap = state.market.markets || {}
  const markets = Object.keys(marketMap).map((marketId) => ({
    ...marketMap[marketId],
    id: marketId,
  }))

  return (
    <Box>
      <div>
        <div>Last updated:</div>
        <div>
          <RelativeTime ts={state.lastUpdatedAt} />
        </div>
      </div>
      <div>
        <BalanceView state={state} />
      </div>
      <H2>Market</H2>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr',
        }}
      >
        <Box
          sx={{
            display: 'contents',
          }}
        >
          <div style={{ fontWeight: 'bold' }} />
          <div style={{ fontWeight: 'bold' }}>Ask</div>
          <div style={{ fontWeight: 'bold' }}>Bid</div>
          <div style={{ fontWeight: 'bold' }}>Spread</div>
          <div style={{ fontWeight: 'bold' }}>%</div>
        </Box>
        {markets.map((m) => {
          const p = (m.spread / ((m.ask + m.bid) / 2)) * 100
          return (
            <Fragment key={m.id}>
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
                <div>{m.id}</div>
                <div>{m.ask.toFixed(0)}</div>
                <div>{m.bid.toFixed(0)}</div>
                <div>{m.spread}</div>
                <div>{p.toFixed(2)}%</div>
              </Box>
            </Fragment>
          )
        })}
      </div>
      <Box
        sx={{
          display: ['grid', 'grid', 'none'],
          justifyItems: ['center', 'center', 'unset'],
        }}
      >
        <PrevActionSet appState={state} header={<H2>Latest Actions</H2>} />
      </Box>
      <H2>Active orders</H2>
      <div>
        <ActiveOrdersView state={state} />
      </div>
      <H2>Trade History</H2>
      <div>
        <FilledOrdersView state={state} />
      </div>
    </Box>
  )
}
