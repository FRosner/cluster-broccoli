module Models.Resources.InstanceUpdateFailure exposing (InstanceUpdateFailure, decoder)

import Json.Decode as Decode

import Models.Resources.InstanceUpdate as InstanceUpdate exposing (InstanceUpdate)

type alias InstanceUpdateFailure =
  { instanceUpdate : InstanceUpdate
  , reason : String
  }

decoder =
  Decode.map2 InstanceUpdateFailure
    (Decode.field "instanceUpdate" InstanceUpdate.decoder)
    (Decode.field "reason" Decode.string)
