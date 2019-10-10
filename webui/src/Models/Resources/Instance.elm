module Models.Resources.Instance exposing (..)

import Dict exposing (Dict)
import Json.Decode as Decode exposing (field)
import Models.Resources.JobStatus as JobStatus exposing (JobStatus)
import Models.Resources.PeriodicRun as PeriodicRun exposing (PeriodicRun)
import Models.Resources.Service as Service exposing (Service)
import Models.Resources.Template as Template exposing (ParameterValue, Template, decodeMaybeValueFromInfo)
import Utils.DecodeUtils as DecodeUtils


type alias InstanceId =
    String


type alias Instance =
    { id : InstanceId
    , template : Template
    , parameterValues : Dict String (Maybe ParameterValue) -- Nothing as a value here means that you have no right to see the value
    , jobStatus : JobStatus
    , services : List Service
    , periodicRuns : List PeriodicRun
    }


decoder =
    field "template" Template.decoder
        |> Decode.andThen
            (\template ->
                field "parameterValues" (decodeMaybeValueFromInfo template.parameterInfos)
                    |> Decode.andThen
                        (\paramValues ->
                            Decode.map6 Instance
                                (field "id" Decode.string)
                                (Decode.succeed template)
                                (Decode.succeed paramValues)
                                (field "status" JobStatus.decoder)
                                (field "services" (Decode.list Service.decoder))
                                (field "periodicRuns" (Decode.list PeriodicRun.decoder))
                        )
            )
