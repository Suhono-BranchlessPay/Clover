export interface CloverWebhookObjectRef {
  objectId?: string
  id?: string
  type?: string
}

export interface CloverWebhookMerchantPayload {
  employees?: CloverWebhookObjectRef[]
  orders?: CloverWebhookObjectRef[]
  payments?: CloverWebhookObjectRef[]
}

export interface CloverWebhookBody {
  merchants?: Record<string, CloverWebhookMerchantPayload>
  verificationCode?: string
}

export interface CloverOrderRef {
  id?: string
}

export interface CloverEmployeeRef {
  id?: string
}

export interface CloverCardTransaction {
  last4?: string
  type?: string
  cardType?: string
}

export interface CloverPayment {
  id: string
  amount?: number
  tipAmount?: number
  taxAmount?: number
  currency?: string
  order?: CloverOrderRef
  employee?: CloverEmployeeRef
  createdTime?: number
  modifiedTime?: number
  cardTransaction?: CloverCardTransaction
  externalPaymentId?: string
  result?: string
}

export interface CloverOrder {
  id: string
  total?: number
  taxRemoved?: boolean
  currency?: string
  title?: string
  note?: string
  createdTime?: number
  modifiedTime?: number
  employee?: CloverEmployeeRef
}

export interface BpAnchorMetadata {
  erp: string
  erp_system: string
  merchant_id: string
  order_id?: string
  tip_amount?: number
  tax_amount?: number
  payment_method?: string
  last4?: string
  employee_id?: string
  clover_payment_id?: string
  clover_order_id?: string
  amount_cents?: number
}

export interface BpAnchorPayload {
  event_type: string
  reference_id: string
  amount: number
  currency: string
  timestamp: string
  vendor: string
  merchant_id: string
  metadata: BpAnchorMetadata
}

export interface StoredAnchorRecord {
  anchor_id?: string
  event_type?: string
  reference_id?: string
  amount?: number
  currency?: string
  timestamp?: string
  merchant_id?: string
  metadata?: Partial<BpAnchorMetadata> & Record<string, unknown>
}

export interface EnrichmentInput {
  merchantId: string
  paymentId?: string
  orderId?: string
  eventType?: string
  existing?: StoredAnchorRecord
}

export interface EnrichmentResult {
  enriched: boolean
  skipped?: string
  error?: string
  paymentId?: string
  orderId?: string
  payload?: BpAnchorPayload
  patch?: Partial<StoredAnchorRecord>
}

export interface CloverApiConfig {
  apiBase?: string
  accessToken: string
  fetchImpl?: typeof fetch
}
