import type { CloverApiConfig } from './types.js'

/** Sandbox — default for M1 dev (NOT api.clover.com). */
export const CLOVER_SANDBOX_API_BASE = 'https://apisandbox.dev.clover.com'

export const CLOVER_ECOMMERCE_SANDBOX_BASE = 'https://scl-sandbox.dev.clover.com'

export const CLOVER_PRODUCTION_API_BASE = 'https://api.clover.com'

export interface CloverEnvConfig {
  apiBase: string
  accessToken: string
  merchantId?: string
  appId?: string
  appSecret?: string
}

export function loadCloverConfigFromEnv(
  env: Record<string, string | undefined> = process.env,
): CloverEnvConfig {
  const apiBase = (env.CLOVER_API_BASE?.trim() || CLOVER_SANDBOX_API_BASE).replace(/\/$/, '')
  return {
    apiBase,
    accessToken: env.CLOVER_API_TOKEN?.trim() ?? '',
    merchantId: env.CLOVER_MERCHANT_ID?.trim() || undefined,
    appId: env.CLOVER_APP_ID?.trim() || undefined,
    appSecret: env.CLOVER_APP_SECRET?.trim() || undefined,
  }
}

export function toApiConfig(envConfig: CloverEnvConfig): CloverApiConfig {
  const isSandbox = envConfig.apiBase.includes('apisandbox')
  return {
    apiBase: envConfig.apiBase,
    ecommerceBase: isSandbox ? CLOVER_ECOMMERCE_SANDBOX_BASE : 'https://scl.clover.com',
    accessToken: envConfig.accessToken,
  }
}

export function createWorkerOptionsFromEnv(
  env: Record<string, string | undefined> = process.env,
): { api: CloverApiConfig; defaultMerchantId?: string } {
  const config = loadCloverConfigFromEnv(env)
  if (!config.accessToken) {
    throw new Error('CLOVER_API_TOKEN is not set')
  }
  return {
    api: toApiConfig(config),
    defaultMerchantId: config.merchantId,
  }
}
