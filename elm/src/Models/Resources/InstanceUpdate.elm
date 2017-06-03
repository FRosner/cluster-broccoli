module Models.Resources.InstanceUpdate exposing (InstanceUpdate, encoder, decoder)

import Json.Encode as Encode
import Json.Decode as Decode

import Dict exposing (Dict)
import Maybe exposing (Maybe)

import Models.Resources.ServiceStatus as ServiceStatus exposing (ServiceStatus)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)

type alias InstanceUpdate =
  { instanceId : InstanceId
  , status : Maybe JobStatus
  , parameterValues : Maybe (Dict String String)
  , selectedTemplate : Maybe TemplateId
  }

decoder =
  Decode.map4 InstanceUpdate
    (Decode.field "instanceId" Decode.string)
    (Decode.maybe (Decode.field "status" JobStatus.decoder))
    (Decode.maybe (Decode.field "parameterValues" (Decode.dict Decode.string)))
    (Decode.maybe (Decode.field "selectedTemplate" Decode.string))

encoder instanceUpdate =
  Encode.object
    ( List.concat
      [ [("instanceId", Encode.string instanceUpdate.instanceId)]
      , instanceUpdate.status
        |> Maybe.map (\s -> [("status", JobStatus.encoder s)])
        |> Maybe.withDefault []
      , instanceUpdate.parameterValues
        |> Maybe.map (\p -> [("parameterValues", parametersToObject p)])
        |> Maybe.withDefault []
      , instanceUpdate.selectedTemplate
        |> Maybe.map (\t -> [("selectedTemplate", Encode.string t)])
        |> Maybe.withDefault []
      ]
    )

parametersToObject parameters =
  Encode.object
    ( parameters
      |> Dict.toList
      |> List.map (\(k, v) -> (k, Encode.string v))
    )
