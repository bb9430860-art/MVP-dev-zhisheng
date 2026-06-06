import { describe, expect, it } from 'vitest'

import { httpClient } from '../httpClient'

describe('httpClient', () => {
  it('uses relative API paths so Vite dev proxy handles backend requests', () => {
    expect(httpClient.defaults.baseURL).toBe('')
  })
})
