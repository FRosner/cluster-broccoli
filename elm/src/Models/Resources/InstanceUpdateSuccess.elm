module Models.Resources.InstanceUpdateSuccess exposing (InstanceUpdateSuccess, decoder)

import Json.Decode as Decode
import Models.Resources.Instance as Instance exposing (Instance)
import Models.Resources.InstanceUpdate as InstanceUpdate exposing (InstanceUpdate)


type alias InstanceUpdateSuccess =
    { instanceUpdate : InstanceUpdate
    , instance : Instance
    }


decoder =
    Decode.map2 InstanceUpdateSuccess
        (Decode.field "instanceUpdate" InstanceUpdate.decoder)
        (Decode.field "instanceWithStatus" Instance.decoder)
