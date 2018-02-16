module Views.PeriodicRunsView exposing (view)

import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.PeriodicRun exposing (PeriodicRun)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.Allocation exposing (shortAllocationId)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Views.JobStatusView as JobStatusView
import Views.LogUrl as LogUrl
import Views.ResourceUsageBar as ResourceUsageBar
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit)
import Dict exposing (..)
import Date
import Date.Extra.Format as DateFormat
import Date.Extra.Config.Config_en_us as Config_en_us


view : InstanceId -> Maybe InstanceTasks -> List PeriodicRun -> Html UpdateBodyViewMsg
view instanceId instanceTasks periodicRuns =
    table
        [ class "table table-condensed"
        ]
        [ thead
            -- Do not wrap table headers
            [ style [ ( "white-space", "nowrap" ) ] ]
            [ tr []
                [ th [ style [ ( "width", "20%" ) ] ] [ icon "fa fa-clock-o" [ title "Run Time" ] ]
                , th [ style [ ( "width", "20%" ) ] ] [ icon "fa fa-cogs" [ title "Job Controls" ] ]
                , th [] [ text "Allocation" ]
                , th [ class "text-center" ] [ text "State" ]
                , th [ style [ ( "width", "60%" ) ] ] [ text "Task" ]
                , th [ class "text-center" ] [ text "CPU" ]
                , th [ class "text-center" ] [ text "Memory" ]
                , th [ class "text-center" ] [ text "Task Logs" ]
                ]
            ]
        , tbody [] <| List.concatMap (row instanceId instanceTasks) periodicRuns
        ]


row : InstanceId -> Maybe InstanceTasks -> PeriodicRun -> List (Html UpdateBodyViewMsg)
row instanceId instanceTasks periodicRun =
    let
        periodicTasks =
            instanceTasks
                |> Maybe.map .allocatedPeriodicTasks
                |> Maybe.map (\tasks -> Maybe.withDefault [] (Dict.get periodicRun.jobName tasks))

        rowSpan =
            Maybe.map List.length periodicTasks
                |> Maybe.withDefault 1

        ( firstPeriodicTask, remainingPeriodicTasks ) =
            Maybe.map splitTasks periodicTasks
                |> Maybe.withDefault ( Nothing, [] )
    in
        (List.concat
            [ [ tr []
                    (List.concat
                        [ [ td [ rowspan rowSpan ]
                                [ formatUtcSeconds "%Y-%m-%d %H:%M:%S" "UTC%z" periodicRun.utcSeconds ]
                          , td [ rowspan rowSpan ]
                                [ JobStatusView.view periodicRun.status
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
                                ]
                          ]
                        , allocationView periodicRun.jobName firstPeriodicTask
                        ]
                    )
              ]
            , List.map (remainingAllocationView periodicRun.jobName) remainingPeriodicTasks
            ]
        )


remainingAllocationView : String -> AllocatedTask -> Html msg
remainingAllocationView jobId periodicTask =
    tr [] <| allocationView jobId (Just periodicTask)


allocationView : String -> Maybe AllocatedTask -> List (Html msg)
allocationView jobId maybePeriodicTask =
    case maybePeriodicTask of
        Nothing ->
            [ td [ colspan 6 ]
                []
            ]

        Just task ->
            let
                ( description, labelKind ) =
                    case task.taskState of
                        TaskDead ->
                            ( "dead", "label-danger" )

                        TaskPending ->
                            ( "pending", "label-warning" )

                        TaskRunning ->
                            ( "running", "label-success" )
            in
                [ td [] [ code [] [ text (shortAllocationId task.allocationId) ] ]
                , td [ class "text-center" ]
                    [ span [ class ("label " ++ labelKind) ] [ text description ]
                    ]
                , td [] [ text task.taskName ]
                , td []
                    [ Maybe.withDefault (ResourceUsageBar.unknown)
                        (Maybe.map2 ResourceUsageBar.cpuUsageBar task.resources.cpuUsedMhz task.resources.cpuRequiredMhz)
                    ]
                , td []
                    [ Maybe.withDefault (ResourceUsageBar.unknown)
                        (Maybe.map2 ResourceUsageBar.memoryUsageBar task.resources.memoryUsedBytes task.resources.memoryRequiredBytes)
                    ]
                , td
                    -- Do not wrap buttons in this cell
                    [ class "text-center", style [ ( "white-space", "nowrap" ) ] ]
                    [ a
                        [ href (LogUrl.view jobId task StdOut)
                        , target "_blank"
                        , class "btn btn-default btn-xs"
                        ]
                        [ text "stdout" ]
                    , text " "
                    , a
                        [ href (LogUrl.view jobId task StdErr)
                        , target "_blank"
                        , class "btn btn-default btn-xs"
                        ]
                        [ text "stderr" ]
                    ]
                ]


formatUtcSeconds : String -> String -> Int -> Html msg
formatUtcSeconds textFormat titleFormat utcSeconds =
    let
        date =
            (utcSeconds * 1000)
                |> toFloat
                |> Date.fromTime

        fText =
            DateFormat.format Config_en_us.config textFormat date

        fTitle =
            DateFormat.format Config_en_us.config titleFormat date
    in
        span [ title fTitle ]
            [ text fText ]


splitTasks : List AllocatedTask -> ( Maybe AllocatedTask, List AllocatedTask )
splitTasks tasks =
    ( List.head tasks, Maybe.withDefault [] <| List.tail tasks )
