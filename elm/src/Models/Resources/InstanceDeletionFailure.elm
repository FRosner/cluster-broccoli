module Models.Resources.InstanceDeletionFailure exposing (InstanceDeletionFailure, decoder)

import Json.Decode as Decode exposing (field)
import Dict exposing (Dict)
import Models.Resources.Instance exposing (InstanceId)


type alias InstanceDeletionFailure =
    { instanceId : InstanceId
    , reason : String
    }


decoder =
    Decode.map2 InstanceDeletionFailure
        (field "instanceId" Decode.string)
        (field "reason" Decode.string)
