module Models.Resources.InstanceDeletionSuccess exposing (InstanceDeletionSuccess, decoder)

import Json.Decode as Decode exposing (field)
import Dict exposing (Dict)

import Models.Resources.Instance as Instance exposing (Instance, InstanceId)

type alias InstanceDeletionSuccess =
  { instanceId : InstanceId
  , instance : Instance
  }

decoder =
  Decode.map2 InstanceDeletionSuccess
    (field "instanceId" Decode.string)
    (field "instanceWithStatus" Instance.decoder)
