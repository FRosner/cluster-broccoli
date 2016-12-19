module Models.Resources.JobStatus exposing (JobStatus(..), decoder)

import Json.Decode as Decode exposing (field, andThen)

type JobStatus
  = JobRunning
  | JobPending
  | JobStopped
  | JobDead
  | JobUnknown

decoder =
  Decode.andThen
    (\statusString -> Decode.succeed (stringToJobStatus statusString))
    Decode.string

stringToJobStatus s =
  case s of
    "running" -> JobRunning
    "pending" -> JobPending
    "stopped" -> JobStopped
    "dead" -> JobDead
    _ -> JobUnknown
