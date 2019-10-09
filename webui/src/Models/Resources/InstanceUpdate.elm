module Models.Resources.InstanceUpdate exposing (InstanceUpdate, decoder, encoder)

import Dict exposing (Dict)
import Json.Decode as Decode
import Json.Encode as Encode
import Maybe exposing (Maybe)
import Models.Resources.Instance exposing (InstanceId)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Resources.Template exposing (ParameterInfo, ParameterValue, TemplateId, decodeValueFromInfo, encodeParamValue)
import Set exposing (Set)


type alias InstanceUpdate =
    { instanceId : InstanceId
    , status : Maybe JobStatus
    , parameterValues : Maybe (Dict String ParameterValue)
    , selectedTemplate : Maybe TemplateId
    , periodicJobsToStop : Maybe (List String)
    }


decoder : Dict String ParameterInfo -> Decode.Decoder InstanceUpdate
decoder parameterInfos =
    Decode.map5 InstanceUpdate
        (Decode.field "instanceId" Decode.string)
        (Decode.maybe (Decode.field "status" JobStatus.decoder))
        (Decode.maybe (Decode.field "parameterValues" (decodeValueFromInfo parameterInfos)))
        (Decode.maybe (Decode.field "selectedTemplate" Decode.string))
        (Decode.maybe (Decode.field "periodicJobsToStop" (Decode.list Decode.string)))


encoder instanceUpdate =
    Encode.object
        (List.concat
            [ [ ( "instanceId", Encode.string instanceUpdate.instanceId ) ]
            , instanceUpdate.status
                |> Maybe.map (\s -> [ ( "status", JobStatus.encoder s ) ])
                |> Maybe.withDefault []
            , instanceUpdate.parameterValues
                |> Maybe.map (\p -> [ ( "parameterValues", parametersToObject p ) ])
                |> Maybe.withDefault []
            , instanceUpdate.selectedTemplate
                |> Maybe.map (\t -> [ ( "selectedTemplate", Encode.string t ) ])
                |> Maybe.withDefault []
            , instanceUpdate.periodicJobsToStop
                |> Maybe.map (\t -> [ ( "periodicJobsToStop", periodicJobsToList t ) ])
                |> Maybe.withDefault []
            ]
        )


parametersToObject parameters =
    Encode.object
        (parameters
            |> Dict.toList
            |> List.map (\( k, v ) -> ( k, encodeParamValue v ))
        )


periodicJobsToList jobs =
    jobs
        |> List.map (\j -> Encode.string j)
        |> Encode.list
