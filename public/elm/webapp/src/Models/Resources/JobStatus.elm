module Models.Resources.JobStatus exposing (JobStatus(..), decoder, encoder)

import Json.Decode as Decode
import Json.Encode as Encode

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

encoder jobStatus =
  jobStatus
  |> jobStatusToString
  |> Encode.string

stringToJobStatus s =
  case s of
    "running" -> JobRunning
    "pending" -> JobPending
    "stopped" -> JobStopped
    "dead" -> JobDead
    _ -> JobUnknown

jobStatusToString s =
  case s of
    JobRunning -> "running"
    JobPending -> "pending"
    JobStopped -> "stopped"
    JobDead -> "dead"
    JobUnknown -> "unknown"
