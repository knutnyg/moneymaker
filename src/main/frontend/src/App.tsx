import './App.scss'
import React, { Fragment, useEffect, useState } from 'react'
import CssBaseline from '@mui/material/CssBaseline'
import { AppState, Currency, parseAppState } from './api/api'
import { Box, Container, createTheme, ThemeProvider } from '@mui/material'
import { PrevActionSet } from './app/PrevActionSet'
import { AppStateView } from './app/AppStateView'

const darkTheme = createTheme({
  palette: {
    mode: 'dark',
  },
})

function createWebSocket(path: string): string {
  const loc = window.location
  const protocolPrefix = loc.protocol === 'https:' ? 'wss:' : 'ws:'
  return protocolPrefix + '//' + loc.host + path
}

const Sidebar: React.FC<{ appState?: AppState }> = ({ appState }) => {
  const nok = appState?.accountBalance.account.currencies[Currency.NOK]
  return (
    <Box
      sx={{
        justifyContent: 'center',
        alignContent: 'center',
        maxWidth: ['100%', '280px'],
        width: ['100%', '280px'],
      }}
    >
      <aside>
        <PrevActionSet appState={appState} />
      </aside>
    </Box>
  )
}

const DataSource: React.FC = () => {
  const [error, setError] = useState<string | undefined>()
  const [appState, setAppState] = useState<AppState | undefined>()

  useEffect(() => {
    const url = createWebSocket('/api/app/state/ws')
    console.log('subscribe to url=' + url)
    const wsSource = new WebSocket(url)
    //const wsSource = new WebSocket('ws://localhost:8020/api/app/state/ws');

    wsSource.onerror = (err) => {
      console.log('onerror', err)
      const ts = new Date(err.timeStamp)
      setError(`error at ${ts.toISOString()}: ${err}`)
    }
    wsSource.onmessage = (evt) => {
      const jsonData = evt.data
      try {
        const nextState = parseAppState(jsonData)
        setAppState(nextState)
      } catch (err) {
        console.log('error parsing msg: ', err)
        console.log('error parsing msg data=', jsonData)
      }
    }

    wsSource.onclose = (evt) => {
      console.log('onclose', evt)
      const ts = new Date(evt.timeStamp)
      setError(`connection closed at: ${ts.toISOString()}`)
    }

    return () => {
      console.log('unsubscribe')
      wsSource.close()
    }
  }, [setAppState])

  return (
    <Container
      sx={{
        display: ['flex', 'flex', 'grid'],
        flexDirection: ['column', 'column', 'unset'],
        gridTemplateColumns: ['unset', 'unset', '1fr 280px'],
        gridTemplateRows: ['unset', 'unset', 'auto'],
        gridTemplateAreas: ['unset', 'unset', '"content sidebar"'],
        gridGap: '16px',
      }}
    >
      <Box
        sx={{
          gridArea: ['unset', 'unset', 'content'],
        }}
      >
        <Box
          sx={{
            display: 'grid',
            justifyItems: 'center',
          }}
        >
          <h1 className="logo">Moneymaker 🤑</h1>
        </Box>
        <Box sx={{ gridArea: ['unset', 'unset', 'content'] }}>
          {error && <span>{error}</span>}
        </Box>
        <Box
          sx={{
            gridArea: ['unset', 'unset', 'content'],
          }}
        >
          <AppStateView state={appState} />
        </Box>
      </Box>
      <Container
        sx={{
          gridArea: ['unset', 'unset', 'sidebar'],
          display: ['none', 'none', 'grid'],
          justifyItems: ['unset', 'unset', 'center'],
          alignItems: ['unset', 'unset', 'center'],
        }}
      >
        <Sidebar appState={appState} />
      </Container>
    </Container>
  )
}

function App() {
  return (
    <ThemeProvider theme={darkTheme}>
      <Fragment>
        <CssBaseline />
        <DataSource />
      </Fragment>
    </ThemeProvider>
  )
}

export default App
