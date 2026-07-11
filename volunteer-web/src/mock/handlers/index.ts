import { authHandlers } from './auth'
import { activityHandlers } from './activities'
import { mapHandlers } from './map'
import { signupHandlers } from './signups'
import { checkinHandlers } from './checkin'
import { uploadHandlers } from './upload'
import { aiHandlers } from './ai'
import { templateHandlers } from './templates'

export const handlers = [
  ...authHandlers,
  ...activityHandlers,
  ...mapHandlers,
  ...signupHandlers,
  ...checkinHandlers,
  ...uploadHandlers,
  ...aiHandlers,
  ...templateHandlers,
]
