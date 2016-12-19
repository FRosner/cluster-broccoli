module Models.Resources.Instance exposing (..)

import Json.Decode as Decode exposing (field)
import Utils.DecodeUtils as DecodeUtils
import Models.Resources.Template as Template exposing (Template)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Dict exposing (Dict)

type alias InstanceId = String

-- TODO make services, jobstatus and servicestatus etc. part of this (impossible state impossible)
type alias Instance =
  { id : InstanceId
  , template : Template
  , parameterValues : Dict String String
  , jobStatus : JobStatus
  }

instanceDecoder =
  Decode.map4 Instance
    (field "id" Decode.string)
    (field "template" Template.decoder)
    (field "parameterValues" (Decode.dict Decode.string))
    (field "status" JobStatus.decoder)
