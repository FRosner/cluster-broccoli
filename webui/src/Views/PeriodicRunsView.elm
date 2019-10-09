module Views.PeriodicRunsView exposing (view)

import Date
import Date.Extra.Config.Config_en_us as Config_en_us
import Date.Extra.Format as DateFormat
import Dict exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onCheck, onClick, onInput, onSubmit)
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.Allocation exposing (shortAllocationId)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.PeriodicRun exposing (PeriodicRun)
import Models.Resources.TaskState exposing (TaskState(..))
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButton, iconButtonText)
import Views.JobStatusView as JobStatusView
import Views.LogUrl as LogUrl
import Views.ResourceUsageBar as ResourceUsageBar


view : InstanceId -> Maybe InstanceTasks -> List PeriodicRun -> Html UpdateBodyViewMsg
view instanceId instanceTasks periodicRuns =
    table
        [ class "table table-condensed"
        ]
        [ thead
            -- Do not wrap table headers
            [ style [ ( "white-space", "nowrap" ) ] ]
            [ tr []
                [ th
                    [ style [ ( "width", "150px" ) ]
                    , class "text-center"
                    ]
                    [ icon "fa fa-clock-o" [ title "Run Time" ] ]
                , th
                    [ style [ ( "width", "130px" ) ]
                    , class "text-center"
                    ]
                    [ icon "fa fa-cogs" [ title "Job Controls" ] ]
                , th
                    [ style [ ( "width", "80px" ) ]
                    ]
                    [ text "Allocation" ]
                , th
                    [ style [ ( "width", "80px" ) ]
                    , class "text-center"
                    ]
                    [ text "State" ]
                , th
                    [ style [ ( "width", "300px" ) ]
                    , class "hidden-xs"
                    ]
                    [ text "Task" ]
                , th
                    [ style [ ( "width", "110px" ) ]
                    , class "text-center hidden-xs hidden-sm"
                    ]
                    [ text "CPU" ]
                , th
                    [ style [ ( "width", "110px" ) ]
                    , class "text-center hidden-xs hidden-sm"
                    ]
                    [ text "Memory" ]
                , th
                    [ class "text-center hidden-xs" ]
                    [ text "Task Logs" ]
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
    List.concat
        [ [ tr []
                (List.concat
                    [ [ td
                            [ class "periodic-run-time"
                            , rowspan rowSpan
                            ]
                            [ formatUtcSeconds "%Y-%m-%d %H:%M:%S" "UTC%z" periodicRun.utcSeconds ]
                      , td
                            [ rowspan rowSpan
                            , style [ ( "white-space", "nowrap" ) ]
                            ]
                            [ JobStatusView.view "" periodicRun.status
                            , text " "
                            , (if
                                periodicRun.status
                                    == JobStatus.JobStopped
                                    || periodicRun.status
                                    == JobStatus.JobDead
                               then
                                iconButton
                                    "btn btn-default btn-xs"
                                    "fa fa-trash"
                                    "Delete Instance"

                               else
                                iconButton
                                    "btn btn-default btn-xs"
                                    "fa fa-stop"
                                    "Stop and Delete Instance"
                              )
                                (List.append
                                    [ onClick (StopPeriodicJobs instanceId [ periodicRun.jobName ])
                                    , id <| String.concat [ "stop-instance-", instanceId ]
                                    ]
                                    (if
                                        periodicRun.status
                                            == JobStatus.JobStopped
                                            || periodicRun.status
                                            == JobStatus.JobUnknown
                                     then
                                        [ attribute "disabled" "disabled" ]

                                     else
                                        []
                                    )
                                )
                            ]
                      ]
                    , allocationView instanceId periodicRun.jobName firstPeriodicTask
                    ]
                )
          ]
        , List.map (remainingAllocationView instanceId periodicRun.jobName) remainingPeriodicTasks
        ]


remainingAllocationView : String -> String -> AllocatedTask -> Html msg
remainingAllocationView instanceId periodicJobId periodicTask =
    tr [] <| allocationView instanceId periodicJobId (Just periodicTask)


allocationView : String -> String -> Maybe AllocatedTask -> List (Html msg)
allocationView instanceId periodicJobId maybePeriodicTask =
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
                            ( "completed", "label-primary" )

                        TaskPending ->
                            ( "pending", "label-warning" )

                        TaskRunning ->
                            ( "running", "label-success" )
            in
            [ td [ class "periodic-run-allocation-id" ]
                [ code [] [ text (shortAllocationId task.allocationId) ] ]
            , td [ class "text-center" ]
                [ span [ class ("label " ++ labelKind) ] [ text description ]
                ]
            , td [ class "hidden-xs" ] [ text task.taskName ]
            , td [ class "hidden-xs hidden-sm" ]
                [ Maybe.withDefault ResourceUsageBar.unknown
                    (Maybe.map2 ResourceUsageBar.cpuUsageBar task.resources.cpuUsedMhz task.resources.cpuRequiredMhz)
                ]
            , td [ class "hidden-xs hidden-sm" ]
                [ Maybe.withDefault ResourceUsageBar.unknown
                    (Maybe.map2 ResourceUsageBar.memoryUsageBar task.resources.memoryUsedBytes task.resources.memoryRequiredBytes)
                ]
            , td
                -- Do not wrap buttons in this cell
                [ class "text-center hidden-xs", style [ ( "white-space", "nowrap" ) ] ]
                [ a
                    [ href (LogUrl.periodicTaskLog instanceId periodicJobId task StdOut)
                    , target "_blank"
                    , class "btn btn-default btn-xs"
                    ]
                    [ text "stdout" ]
                , text " "
                , a
                    [ href (LogUrl.periodicTaskLog instanceId periodicJobId task StdErr)
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
