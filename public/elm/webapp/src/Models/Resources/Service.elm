module Models.Resources.Service exposing (..)

import Json.Decode as Decode exposing (field, andThen)

type alias Service =
  { name : String
  , protocol : String
  , address : String
  , port_ : Int
  , status : ServiceStatus
  }

type ServiceStatus
  = Passing
  | Failing
  | Unknown

instanceDecoder =
  Decode.map5 Service
    ( field "name" Decode.string )
    ( field "protocol" Decode.string )
    ( field "address" Decode.string )
    ( field "port" Decode.int )
    ( field "status"
      ( Decode.andThen
          (\statusString -> Decode.succeed (stringToServiceStatus statusString))
          Decode.string
      )
    )

stringToServiceStatus s =
  case s of
    "passing" -> Passing
    "failing" -> Failing
    _ -> Unknown
