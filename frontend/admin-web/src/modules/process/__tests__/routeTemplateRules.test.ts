import { describe, expect, it } from 'vitest'

import { hasActiveEnabledStep } from '../utils/routeTemplateRules'
import type { StepTemplate } from '../types'

describe('hasActiveEnabledStep', () => {
  it('returns false when a route has no enabled non-deleted step', () => {
    const steps: StepTemplate[] = [
      {
        id: 1,
        routeTemplateId: 10,
        stepCode: 'CUT',
        stepName: '下料',
        stepOrder: 1,
        assignedRole: 'WORKER',
        photoRequired: false,
        remarkRequired: false,
        mobileEnabled: true,
        estimatedHours: null,
        operationInstruction: '',
        enabled: false,
        deleted: false,
      },
    ]

    expect(hasActiveEnabledStep(steps)).toBe(false)
  })

  it('returns true when a route has at least one enabled non-deleted step', () => {
    const steps: StepTemplate[] = [
      {
        id: 1,
        routeTemplateId: 10,
        stepCode: 'CUT',
        stepName: '下料',
        stepOrder: 1,
        assignedRole: 'WORKER',
        photoRequired: false,
        remarkRequired: false,
        mobileEnabled: true,
        estimatedHours: null,
        operationInstruction: '',
        enabled: true,
        deleted: false,
      },
    ]

    expect(hasActiveEnabledStep(steps)).toBe(true)
  })
})
