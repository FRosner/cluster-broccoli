module Models.Resources.InstanceDeleted exposing (InstanceDeleted, decoder)

import Json.Decode as Decode exposing (field)
import Models.Resources.Instance as Instance exposing (Instance, InstanceId)


type alias InstanceDeleted =
    { instanceId : InstanceId
    , instance : Instance
    }


decoder : Decode.Decoder InstanceDeleted
decoder =
    Decode.map2 InstanceDeleted
        (field "instanceId" Decode.string)
        (field "instanceWithStatus" Instance.decoder)
