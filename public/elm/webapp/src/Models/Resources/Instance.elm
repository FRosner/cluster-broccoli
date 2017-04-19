module Models.Resources.Instance exposing (..)

import Json.Decode as Decode exposing (field)
import Utils.DecodeUtils as DecodeUtils
import Models.Resources.Template as Template exposing (Template)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Resources.PeriodicRun as PeriodicRun exposing (PeriodicRun)
import Models.Resources.Service as Service exposing (Service)
import Dict exposing (Dict)

type alias InstanceId = String

type alias Instance =
  { id : InstanceId
  , template : Template
  , parameterValues : Dict String String
  , jobStatus : JobStatus
  , services : List Service
  , periodicRuns : List PeriodicRun
  }

decoder =
  Decode.map6 Instance
    (field "id" Decode.string)
    (field "template" Template.decoder)
    (field "parameterValues" (Decode.dict Decode.string))
    (field "status" JobStatus.decoder)
    (field "services" (Decode.list Service.decoder))
    (field "periodicRuns" (Decode.list PeriodicRun.decoder))
