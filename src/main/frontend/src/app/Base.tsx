import React from 'react'

export const H1: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <h1 style={{ paddingTop: '10px' }}>{children}</h1>
)
export const H2: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <h2 style={{ paddingTop: '10px' }}>{children}</h2>
)
