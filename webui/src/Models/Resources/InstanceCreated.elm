module Models.Resources.InstanceCreated exposing (InstanceCreated, decoder)

import Dict exposing (Dict)
import Json.Decode as Decode exposing (field)
import Models.Resources.Instance as Instance exposing (Instance)
import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Models.Resources.Template exposing (ParameterInfo)


type alias InstanceCreated =
    { instanceCreation : InstanceCreation
    , instance : Instance
    }


decoder : Decode.Decoder InstanceCreated
decoder =
    field "instanceWithStatus" Instance.decoder
        |> Decode.andThen
            (\instanceWithStatus ->
                Decode.map2 InstanceCreated
                    (field "instanceCreation" (InstanceCreation.decoder instanceWithStatus.template.parameterInfos))
                    (Decode.succeed instanceWithStatus)
            )
