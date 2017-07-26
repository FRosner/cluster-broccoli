module Utils.DecodeUtils exposing (maybeNull)

import Json.Decode as Decode


maybeNull decoder =
    Decode.oneOf
        [ Decode.null Nothing
        , Decode.map Just decoder
        ]
