module Models.Resources.InstanceCreation exposing (InstanceCreation, encoder, decoder)

import Json.Encode as Encode
import Json.Decode as Decode
import Dict exposing (Dict)
import Models.Resources.ServiceStatus as ServiceStatus exposing (ServiceStatus)


type alias InstanceCreation =
    { templateId : String
    , parameters : Dict String String
    }


decoder =
    Decode.map2 InstanceCreation
        (Decode.field "templateId" Decode.string)
        (Decode.field "parameters" (Decode.dict Decode.string))


encoder instanceCreation =
    Encode.object
        [ ( "templateId", Encode.string instanceCreation.templateId )
        , ( "parameters", (parametersToObject instanceCreation.parameters) )
        ]


parametersToObject parameters =
    Encode.object
        (parameters
            |> Dict.toList
            |> List.map (\( k, v ) -> ( k, Encode.string v ))
        )
