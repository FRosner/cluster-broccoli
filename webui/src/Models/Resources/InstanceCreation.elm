module Models.Resources.InstanceCreation exposing (InstanceCreation, decoder, encoder)

import Dict exposing (Dict)
import Json.Decode as Decode
import Json.Encode as Encode
import Models.Resources.ServiceStatus as ServiceStatus exposing (ServiceStatus)
import Models.Resources.Template as Template exposing (ParameterValue, decodeValueFromInfo, encodeParamValue)


type alias InstanceCreation =
    { templateId : String
    , parameters : Dict String ParameterValue
    }


decoder parameterInfos =
    Decode.map2 InstanceCreation
        (Decode.field "templateId" Decode.string)
        (Decode.field "parameters" (decodeValueFromInfo parameterInfos))


encoder instanceCreation =
    Encode.object
        [ ( "templateId", Encode.string instanceCreation.templateId )
        , ( "parameters", parametersToObject instanceCreation.parameters )
        ]


parametersToObject parameters =
    Encode.object
        (parameters
            |> Dict.toList
            |> List.map (\( k, v ) -> ( k, encodeParamValue v ))
        )
