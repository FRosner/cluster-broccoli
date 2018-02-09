module Models.Resources.InstanceUpdate exposing (InstanceUpdate, encoder, decoder)

import Json.Encode as Encode
import Json.Decode as Decode
import Dict exposing (Dict)
import Set exposing (Set)
import Maybe exposing (Maybe)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)


type alias InstanceUpdate =
    { instanceId : InstanceId
    , status : Maybe JobStatus
    , parameterValues : Maybe (Dict String String)
    , selectedTemplate : Maybe TemplateId
    , periodicJobsToStop : Maybe (List String)
    }


decoder =
    Decode.map5 InstanceUpdate
        (Decode.field "instanceId" Decode.string)
        (Decode.maybe (Decode.field "status" JobStatus.decoder))
        (Decode.maybe (Decode.field "parameterValues" (Decode.dict Decode.string)))
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
            |> List.map (\( k, v ) -> ( k, Encode.string v ))
        )


periodicJobsToList jobs =
    jobs
        |> List.map (\j -> Encode.string j)
        |> Encode.list
