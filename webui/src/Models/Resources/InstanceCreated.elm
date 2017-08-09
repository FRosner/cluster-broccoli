module Models.Resources.InstanceCreated exposing (InstanceCreated, decoder)

import Json.Decode as Decode exposing (field)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Models.Resources.Instance as Instance exposing (Instance)


type alias InstanceCreated =
    { instanceCreation : InstanceCreation
    , instance : Instance
    }


decoder : Decode.Decoder InstanceCreated
decoder =
    Decode.map2 InstanceCreated
        (field "instanceCreation" InstanceCreation.decoder)
        (field "instanceWithStatus" Instance.decoder)
