module Models.Resources.InstanceError exposing (InstanceError, decoder)

import Json.Decode as Decode exposing (Decoder, field)
import Models.Resources.Instance exposing (InstanceId)


{-| An error occurred while performing an operation on an instance.
-}
type alias InstanceError =
    { reason : String
    }


{-| Decode an instance error from JSON.
-}
decoder : Decoder InstanceError
decoder =
    Decode.map InstanceError
        (field "reason" Decode.string)
