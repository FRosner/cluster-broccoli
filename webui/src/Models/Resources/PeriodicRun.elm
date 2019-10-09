module Models.Resources.PeriodicRun exposing (PeriodicRun, decoder)

import Json.Decode as Decode exposing (field)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)



-- createdBy: String, status: JobStatus, utcSeconds: Long, jobName: String


type alias PeriodicRun =
    { status : JobStatus
    , utcSeconds : Int
    , jobName : String
    }


decoder =
    Decode.map3 PeriodicRun
        (field "status" JobStatus.decoder)
        (field "utcSeconds" Decode.int)
        (field "jobName" Decode.string)
