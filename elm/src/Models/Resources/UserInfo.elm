module Models.Resources.UserInfo exposing (UserInfo, userInfoDecoder)

import Models.Resources.Role as Role exposing (Role)

import Json.Decode as Decode exposing (field)

type alias UserInfo =
  { name : String
  , role : Role
  , instanceRegex : String
  }

userInfoDecoder : Decode.Decoder UserInfo
userInfoDecoder =
  Decode.map3 UserInfo
    (field "name" Decode.string)
    (field "role" Role.decoder)
    (field "instanceRegex" Decode.string)
