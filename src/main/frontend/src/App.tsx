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

function createWebSocketUrl(path: string): string {
  const loc = window.location
  const protocolPrefix = loc.protocol === 'https:' ? 'wss:' : 'ws:'
  return protocolPrefix + '//' + loc.host + path
}

const Sidebar: React.FC<{ appState?: AppState }> = ({ appState }) => {
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

type MessageHandler = (ev: MessageEvent) => any
type ErrorHandler = (ev: Event) => any

class ReconnectingWebSocket {
  private readonly wsUrl: string
  private conn?: WebSocket
  private isClosing = false
  onmessage: MessageHandler
  onerror: ErrorHandler

  constructor(
    wsUrl: string,
    conn: WebSocket,
    callbackFn: MessageHandler,
    onerror: ErrorHandler,
  ) {
    this.wsUrl = wsUrl
    this.conn = conn
    this.onmessage = callbackFn
    this.onerror = onerror
    this.setConn(conn)
  }

  private onClose(evt: CloseEvent) {
    console.log('ws: onclose', evt)
    if (this.isClosing) {
      console.log('ws: onclose isClosing', evt)
      return
    }
    console.log('ws: onclose attempt reconnect', evt)

    setTimeout(() => {
      console.log('ws: attempting reconnect')
      this.setConn(makeWebsocket(this.wsUrl))
    }, 5000)
  }

  private setConn(conn?: WebSocket) {
    this.conn = conn
    if (this.conn) {
      this.conn.onmessage = (ev) => this.onmessage(ev)
      this.conn.onerror = (ev) => this.onerror(ev)
      this.conn.onclose = (ev) => this.onClose(ev)
    }
  }

  close() {
    this.isClosing = true
    if (this.conn) {
      this.conn.close()
    }
  }
}

function makeWebsocket(url: string) {
  const wsSource = new WebSocket(url)
  //const wsSource = new WebSocket('ws://localhost:8020/api/app/state/ws');
  return wsSource
}

function startWebsocket(
  url: string,
  onmessage: MessageHandler,
  onerror: (ev: Event) => any,
): ReconnectingWebSocket {
  const wsSource = makeWebsocket(url)
  const sock = new ReconnectingWebSocket(url, wsSource, onmessage, onerror)
  return sock
}

const DataSource: React.FC = () => {
  const [error, setError] = useState<string | undefined>()
  const [appState, setAppState] = useState<AppState | undefined>()

  useEffect(() => {
    const url = createWebSocketUrl('/api/app/state/ws')
    console.log('subscribe to url=' + url)

    const wsSource = startWebsocket(
      url,
      (evt) => {
        const jsonData = evt.data
        try {
          const nextState = parseAppState(jsonData)
          setAppState(nextState)
          // clear any error:
          setError(undefined)
        } catch (err) {
          console.log('ws: error parsing msg: ', err)
          console.log('ws: error parsing msg data=', jsonData)
        }
      },
      (err) => {
        console.log('ws: onerror', err)
        const ts = new Date()
        setError(`ws: error at ${ts.toISOString()}: ${err}`)
      },
    )

    return () => {
      console.log('unsubscribe')
      wsSource.close()
    }
  }, [setAppState, setError])

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
