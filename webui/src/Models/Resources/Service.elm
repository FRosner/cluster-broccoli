module Models.Resources.Service exposing (Service, decoder)

import Json.Decode as Decode exposing (field)
import Models.Resources.ServiceStatus as ServiceStatus exposing (ServiceStatus)


type alias Service =
    { name : String
    , protocol : String
    , address : String
    , port_ : Int
    , status : ServiceStatus
    }


decoder =
    Decode.map5 Service
        (field "name" Decode.string)
        (field "protocol" Decode.string)
        (field "address" Decode.string)
        (field "port" Decode.int)
        (field "status" ServiceStatus.serviceStatusDecoder)
