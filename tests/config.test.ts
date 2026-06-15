import { describe, expect, it } from 'vitest'
import {
  CLOVER_PRODUCTION_API_BASE,
  CLOVER_SANDBOX_API_BASE,
  loadCloverConfigFromEnv,
  toApiConfig,
} from '../src/config.js'

describe('config', () => {
  it('defaults to sandbox API base when CLOVER_API_BASE unset', () => {
    const config = loadCloverConfigFromEnv({})
    expect(config.apiBase).toBe(CLOVER_SANDBOX_API_BASE)
  })

  it('uses production base only when explicitly set', () => {
    const config = loadCloverConfigFromEnv({
      CLOVER_API_BASE: CLOVER_PRODUCTION_API_BASE,
      CLOVER_API_TOKEN: 'tok',
    })
    expect(config.apiBase).toBe(CLOVER_PRODUCTION_API_BASE)
  })

  it('loads token and merchant id from env', () => {
    const config = loadCloverConfigFromEnv({
      CLOVER_API_TOKEN: 'abc',
      CLOVER_MERCHANT_ID: '26V6BKSEAX311',
      CLOVER_APP_ID: 'APP1',
    })
    expect(config.accessToken).toBe('abc')
    expect(config.merchantId).toBe('26V6BKSEAX311')
    expect(toApiConfig(config).accessToken).toBe('abc')
  })
})
