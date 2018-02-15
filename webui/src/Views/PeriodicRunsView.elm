module Views.PeriodicRunsView exposing (view)

import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.PeriodicRun exposing (PeriodicRun)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Views.JobStatusView as JobStatusView
import Views.JobTasksView as JobTasksView
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit)
import Dict exposing (..)
import Date
import Date.Extra.Format as DateFormat
import Date.Extra.Config.Config_en_us as Config_en_us


view : InstanceId -> Maybe InstanceTasks -> List PeriodicRun -> Html UpdateBodyViewMsg
view instanceId instanceTasks periodicRuns =
    ul []
        (List.map (row instanceId instanceTasks) periodicRuns)


row : InstanceId -> Maybe InstanceTasks -> PeriodicRun -> Html UpdateBodyViewMsg
row instanceId instanceTasks periodicRun =
    let
        periodicTasks =
            instanceTasks
                |> Maybe.map .allocatedPeriodicTasks
                |> Maybe.map (\tasks -> Maybe.withDefault [] (Dict.get periodicRun.jobName tasks))
    in
        li [ style [ ( "margin", "0 0 3px 0" ) ] ]
            [ code [ style [ ( "margin-right", "12px" ) ] ] [ text periodicRun.jobName ]
            , text " "
            , span
                [ class "hidden-xs"
                , style [ ( "margin-right", "12px" ) ]
                ]
                [ icon "fa fa-clock-o" []
                , text " "
                , (periodicRun.utcSeconds * 1000)
                    |> toFloat
                    |> Date.fromTime
                    |> DateFormat.format Config_en_us.config "%Y-%m-%d %H:%M:%S UTC%z"
                    |> text
                ]
            , text " "
            , JobStatusView.view periodicRun.status
            , text " "
            , iconButton
                "btn btn-default btn-xs"
                "glyphicon glyphicon-stop"
                "Stop Instance"
                (List.append
                    [ onClick (StopPeriodicJobs instanceId [ periodicRun.jobName ])
                    , id <| String.concat [ "stop-instance-", instanceId ]
                    ]
                    (if
                        (periodicRun.status
                            == JobStatus.JobStopped
                            || periodicRun.status
                            == JobStatus.JobUnknown
                        )
                     then
                        [ attribute "disabled" "disabled" ]
                     else
                        []
                    )
                )
            , div []
                (JobTasksView.view periodicRun.jobName periodicTasks True)
            ]
