module Utils.DecodeUtils exposing (maybe)

import Json.Decode as Decode

maybe decoder =
  Decode.oneOf
    [ Decode.null Nothing
    , Decode.map Just decoder
    ]
