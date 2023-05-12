import { AppState } from '../api/api'
import { Box, Grid } from '@mui/material'
import { RelativeTime } from './RelativeTime'
import React, { Fragment } from 'react'
import { PrevActionSet } from './PrevActionSet'
import { H2 } from './Base'
import { BalanceView } from './BalanceView'
import { TradeHistory } from './TradeHistory'
import { ActiveOrderFC } from './ActiveOrderFC'

const ActiveOrdersView: React.FC<{ state: AppState }> = ({ state }) => {
  const list = state.activeTrades.activeOrders || []

  return (
    <Grid container spacing={0}>
      {list.map((a) => (
        <ActiveOrderFC key={`${a.id}`} order={a} />
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
          <div style={{ fontWeight: 'bold', textAlign: 'right' }}>%</div>
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
                <div style={{ textAlign: 'right' }}>{p.toFixed(2)}%</div>
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
        <TradeHistory state={state} />
      </div>
    </Box>
  )
}
