module Views.PeriodicRunView exposing (view)

import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.Role exposing (Role(..))
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.PeriodicRun exposing (PeriodicRun)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Template exposing (Template)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.ClientStatus exposing (ClientStatus(ClientComplete))
import Models.Resources.Allocation exposing (shortAllocationId)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Views.ParameterFormView as ParameterFormView
import Views.JobStatusView as JobStatusView
import Views.JobTasksView as JobTasksView
import Views.Styles as Styles
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit)
import Dict exposing (..)
import Set exposing (Set)
import Date
import Filesize
import Round
import Date.Extra.Format as DateFormat
import Date.Extra.Config.Config_en_us as Config_en_us


view : InstanceId -> Maybe InstanceTasks -> PeriodicRun -> Html UpdateBodyViewMsg
view instanceId instanceTasks periodicRun =
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
