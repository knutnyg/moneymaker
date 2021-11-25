import React from 'react'
import { AppState, CurrencyBalance } from '../api/api'
import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material'
import { H2 } from './Base'

export const CurrencyRow: React.FC<{ c: CurrencyBalance }> = ({ c }) => (
  <TableRow key={c.currency} hover>
    <TableCell>{c.currency}</TableCell>
    <TableCell align={'right'}>{c.balance}</TableCell>
    <TableCell align={'right'}>{c.available}</TableCell>
    <TableCell align={'right'}>{c.hold}</TableCell>
  </TableRow>
)

export const BalanceView: React.FC<{ state?: AppState }> = ({ state }) => {
  if (!state) {
    return <div></div>
  }

  const currencies = Object.values(state.accountBalance.account.currencies)

  return (
    <Box>
      <H2>Balance</H2>
      <Table size={'small'}>
        <TableHead>
          <TableRow>
            <TableCell>Currency</TableCell>
            <TableCell align={'right'}>Balance</TableCell>
            <TableCell align={'right'}>Available</TableCell>
            <TableCell align={'right'}>Hold</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {currencies.map((c) => (
            <CurrencyRow c={c} />
          ))}
        </TableBody>
      </Table>
      {/*<Grid*/}
      {/*  sx={{*/}
      {/*    display: 'grid',*/}
      {/*    gridTemplateColumns: '1fr 1fr 1fr 1fr',*/}
      {/*    gridRowGap: '4px',*/}
      {/*  }}*/}
      {/*>*/}
      {/*  {currencies.map((c) => (*/}
      {/*    <Fragment key={c.currency}>*/}
      {/*      <Box>{c.currency}</Box>*/}
      {/*      <Box>{c.balance}</Box>*/}
      {/*      <Box>{c.available}</Box>*/}
      {/*      <Box>{c.hold}</Box>*/}
      {/*    </Fragment>*/}
      {/*  ))}*/}
      {/*</Grid>*/}
    </Box>
  )
}
