module Models.Resources.Instance exposing (..)

import Json.Decode as Decode exposing (field)
import Utils.DecodeUtils as DecodeUtils
import Models.Resources.Template exposing (Template, templateDecoder)
import Dict exposing (Dict)

type alias InstanceId = String

type alias Instance =
  { id : InstanceId
  , template : Template
  , parameterValues : Dict String String
  }

instanceDecoder =
  Decode.map3 Instance
    (field "id" Decode.string)
    (field "template" templateDecoder)
    (field "parameterValues" (Decode.dict Decode.string))
