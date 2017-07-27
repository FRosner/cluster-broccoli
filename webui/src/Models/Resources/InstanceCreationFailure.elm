module Models.Resources.InstanceCreationFailure exposing (InstanceCreationFailure, decoder)

import Json.Decode as Decode exposing (field)
import Dict exposing (Dict)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)


type alias InstanceCreationFailure =
    { instanceCreation : InstanceCreation
    , reason : String
    }


decoder =
    Decode.map2 InstanceCreationFailure
        (field "instanceCreation" InstanceCreation.decoder)
        (field "reason" Decode.string)
