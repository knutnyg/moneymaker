import { AppState } from '../api/api'
import { Box } from '@mui/material'
import { RelativeTime } from './RelativeTime'
import React, { Fragment } from 'react'
import { PrevActionSet } from './PrevActionSet'
import { formatDate } from '../util/time'
import { H2 } from './Base'

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
                    backgroundColor: '#333',
                  },
                  '&:focus > div': {
                    backgroundColor: '#333',
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
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(155px, 2fr) 1fr 1fr 1fr 2fr 1fr',
            gridRowGap: '4px',
          }}
        >
          {state.activeTrades.activeOrders.map((a) => (
            <Fragment key={`${a.id}`}>
              <Box
                sx={{
                  display: 'contents',
                  '&:hover > div': {
                    backgroundColor: '#333',
                  },
                  '&:focus > div': {
                    backgroundColor: '#333',
                  },
                }}
              >
                <Box>{formatDate(a.created_at)}</Box>
                <Box sx={{ textAlign: 'center' }}>{a.market}</Box>
                <Box sx={{ textAlign: 'center' }}>{a.type}</Box>
                <Box sx={{ textAlign: 'right' }}>{a.amount}</Box>
                <Box sx={{ textAlign: 'right' }}>{a.price.toFixed(2)}</Box>
                <Box sx={{ textAlign: 'right' }}>
                  {(a.price * a.amount).toFixed(2)} kr
                </Box>
              </Box>
            </Fragment>
          ))}
        </div>
      </div>
      <H2>Trade History</H2>
      <div>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(155px, 2fr) 1fr 1fr 1fr 2fr 1fr',
            gridRowGap: '4px',
          }}
        >
          {state.filledOrders.filledOrders.map((a) => (
            <Fragment key={`${a.id}`}>
              <Box
                sx={{
                  display: 'contents',
                  '&:hover > div': {
                    backgroundColor: '#333',
                  },
                  '&:focus > div': {
                    backgroundColor: '#333',
                  },
                }}
              >
                <Box>{formatDate(a.created_at)}</Box>
                <Box sx={{ textAlign: 'center' }}>{a.market}</Box>
                <Box sx={{ textAlign: 'center' }}>{a.type}</Box>
                <Box sx={{ textAlign: 'right' }}>{a.amount}</Box>
                <Box sx={{ textAlign: 'right' }}>{a.price.toFixed(2)}</Box>
                <Box sx={{ textAlign: 'right' }}>
                  {(a.price * a.amount).toFixed(2)} kr
                </Box>
              </Box>
            </Fragment>
          ))}
        </div>
      </div>
    </Box>
  )
}
