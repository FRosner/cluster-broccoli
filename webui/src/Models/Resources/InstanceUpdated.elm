module Models.Resources.InstanceUpdated exposing (InstanceUpdated, decoder)

import Json.Decode as Decode
import Models.Resources.Instance as Instance exposing (Instance)
import Models.Resources.InstanceUpdate as InstanceUpdate exposing (InstanceUpdate)


type alias InstanceUpdated =
    { instanceUpdate : InstanceUpdate
    , instance : Instance
    }


decoder : Decode.Decoder InstanceUpdated
decoder =
    Decode.field "instanceWithStatus" Instance.decoder
        |> Decode.andThen
            (\instanceWithStatus ->
                Decode.map2 InstanceUpdated
                    (Decode.field "instanceUpdate" (InstanceUpdate.decoder instanceWithStatus.template.parameterInfos))
                    (Decode.succeed instanceWithStatus)
            )
