import { authHandlers } from './auth'
import { activityHandlers } from './activities'
import { mapHandlers } from './map'
import { signupHandlers } from './signups'
import { checkinHandlers } from './checkin'

export const handlers = [
  ...authHandlers,
  ...activityHandlers,
  ...mapHandlers,
  ...signupHandlers,
  ...checkinHandlers,
]
