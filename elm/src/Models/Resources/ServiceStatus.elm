module Models.Resources.ServiceStatus exposing (ServiceStatus(..), serviceStatusDecoder)

import Json.Decode as Decode

type ServiceStatus
  = ServicePassing
  | ServiceFailing
  | ServiceUnknown

serviceStatusDecoder =
  Decode.andThen
    (\statusString -> Decode.succeed (stringToServiceStatus statusString))
    Decode.string

stringToServiceStatus s =
  case s of
    "passing" -> ServicePassing
    "failing" -> ServiceFailing
    _ -> ServiceUnknown
