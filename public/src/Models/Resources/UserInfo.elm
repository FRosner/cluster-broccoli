module Models.Resources.UserInfo exposing (..)

import Json.Decode as Decode exposing (field)

type alias UserInfo =
  { name : String
  , role : String
  , instanceRegex : String
  }

userInfoDecoder =
  Decode.map3 UserInfo
    (field "name" Decode.string)
    (field "role" Decode.string)
    (field "instanceRegex" Decode.string)
