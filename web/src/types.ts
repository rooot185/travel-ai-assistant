export type Pace = 'RELAXED' | 'BALANCED' | 'FULL'

export interface Request {
  city: string
  startDate: string
  days: number
  travelers: number
  budget: number
  pace: Pace
  preferences: string[]
  mustVisit: string[]
  notes: string
}

export interface Point { lng: number; lat: number }
export interface Place { id: string; name: string; address: string; point: Point }
export interface Route {
  mode: 'WALK' | 'DRIVE'
  minutes: number
  meters: number
  line: Point[]
  checkedAt: string
}
export interface Stop {
  place: Place
  start: string
  end: string
  note: string
  estimatedCost: number
  locked: boolean
  placeCheckedAt: string
  inbound: Route | null
}
export interface Day { number: number; date: string; title: string; stops: Stop[] }
export interface Budget { activities: number; food: number; lodging: number; localTransport: number }
export interface Plan {
  request: Request
  city: { name: string; adcode: string }
  title: string
  summary: string
  days: Day[]
  budget: Budget
  mustVisitIds: string[]
  evidence: { step: string; calls: number }[]
  changeSummary: string
  generatedAt: string
}
export interface Trip { id: string; revision: number; updatedAt: string; plan: Plan }
export interface TripSummary {
  id: string
  revision: number
  updatedAt: string
  city: string
  title: string
  startDate: string
  days: number
}
export interface Account { id: string; email: string; displayName: string }
export interface Status { plannerConfigured: boolean; registrationOpen: boolean }
export interface ErrorBody { code: string; message: string; retryable: boolean }
export interface Job {
  id: string
  state: 'QUEUED' | 'RUNNING' | 'COMPLETE' | 'FAILED'
  step: string
  trip: Trip | null
  error: ErrorBody | null
}
