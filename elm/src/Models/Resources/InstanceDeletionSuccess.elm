module Models.Resources.InstanceDeletionSuccess exposing (InstanceDeletionSuccess, decoder)

import Json.Decode as Decode exposing (field)
import Models.Resources.Instance as Instance exposing (Instance, InstanceId)


type alias InstanceDeletionSuccess =
    { instanceId : InstanceId
    , instance : Instance
    }


decoder : Decode.Decoder InstanceDeletionSuccess
decoder =
    Decode.map2 InstanceDeletionSuccess
        (field "instanceId" Decode.string)
        (field "instanceWithStatus" Instance.decoder)
