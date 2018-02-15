module Views.JobTasksView exposing (view)

import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.ClientStatus exposing (ClientStatus(ClientComplete))
import Models.Resources.Allocation exposing (shortAllocationId)
import Views.Styles as Styles
import Html exposing (..)
import Html.Attributes exposing (..)
import Filesize
import Round


view : String -> Maybe (List AllocatedTask) -> Bool -> List (Html msg)
view jobId allocatedTasks showDead =
    -- Possibility to filter all complete allocations and attach the task name to every
    -- allocation.
    --
    -- We remove complete allocations because Nomad 0.5.x (possibly
    -- other versions as well) returns all complete and thus dead
    -- allocations for stopped jobs, whereas it only returns
    -- non-complete allocations for running jobs.
    --
    -- If we did not filter complete allocations the user would see dead
    -- allocations suddenly popping up in the UI when they stopped the
    -- task—which would be somewhat confusing.
    case Maybe.map (List.filter (.clientStatus >> (/=) ClientComplete >> (||) showDead)) allocatedTasks of
        Nothing ->
            [ div
                [ style Styles.instanceViewElementStyle ]
                [ h5 [] [ text "Allocated Tasks" ]
                , i [ class "fa fa-spinner fa-spin" ] []
                ]
            ]

        Just [] ->
            []

        Just allocations ->
            [ div
                [ style Styles.instanceViewElementStyle ]
                (List.concat
                    [ [ h5 [] [ text "Allocated Tasks" ] ]
                    , [ table
                            [ class "table table-condensed table-hover"
                            ]
                            [ thead
                                -- Do not wrap table headers
                                [ style [ ( "white-space", "nowrap" ) ] ]
                                [ tr []
                                    [ th [] [ text "Allocation ID" ]
                                    , th [ class "text-center" ] [ text "State" ]
                                    , th [ style [ ( "width", "100%" ) ] ] [ text "Task" ]
                                    , th [ class "text-center" ] [ text "CPU" ]
                                    , th [ class "text-center" ] [ text "Memory" ]
                                    , th [ class "text-center" ] [ text "Task logs" ]
                                    ]
                                ]
                            , tbody [] <| List.indexedMap (jobAllocationRow jobId) allocations
                            ]
                      ]
                    ]
                )
            ]


jobAllocationRow : String -> Int -> AllocatedTask -> Html msg
jobAllocationRow jobId index task =
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
        tr []
            [ td [] [ code [] [ text (shortAllocationId task.allocationId) ] ]
            , td [ class "text-center" ]
                [ span [ class ("label " ++ labelKind) ] [ text description ]
                ]
            , td [] [ text task.taskName ]
            , td []
                [ Maybe.withDefault (text "Unknown")
                    (Maybe.map2 cpuUsageBar task.resources.cpuUsedMhz task.resources.cpuRequiredMhz)
                ]
            , td []
                [ Maybe.withDefault (text "Unknown")
                    (Maybe.map2 memoryUsageBar task.resources.memoryUsedBytes task.resources.memoryRequiredBytes)
                ]
            , td
                -- Do not wrap buttons in this cell
                [ class "text-center", style [ ( "white-space", "nowrap" ) ] ]
                [ a
                    [ href (logUrl jobId task StdOut)
                    , target "_blank"
                    , class "btn btn-default btn-xs"
                    ]
                    [ text "stdout" ]
                , text " "
                , a
                    [ href (logUrl jobId task StdErr)
                    , target "_blank"
                    , class "btn btn-default btn-xs"
                    ]
                    [ text "stderr" ]
                ]
            ]


cpuUsageBar : Float -> Float -> Html msg
cpuUsageBar current required =
    resourceUsageBar
        ((Round.round 0 current) ++ " MHz / " ++ (Round.round 0 required) ++ " MHz CPU used")
        current
        required


memoryUsageBar : Int -> Int -> Html msg
memoryUsageBar current required =
    resourceUsageBar
        ((Filesize.format current) ++ " of " ++ (Filesize.format required) ++ " memory used")
        (toFloat current)
        (toFloat required)


resourceUsageBar : String -> Float -> Float -> Html msg
resourceUsageBar tooltip current required =
    let
        ratio =
            current / required

        context =
            if ratio > 1.0 then
                "progress-bar-danger"
            else if ratio >= 0.8 then
                "progress-bar-warning"
            else
                "progress-bar-success"
    in
        div
            [ class "progress"
            , style
                [ ( "width", "100px" )
                , ( "position", "relative" )
                ]
            , title tooltip
            ]
            [ div
                [ class "progress-bar"
                , class context
                , attribute "role" "progressbar"
                , attribute "aria-valuemin" "0"
                , attribute "aria-valuenow" (Round.round 2 current)
                , attribute "aria-valuemax" (Round.round 2 current)
                , style
                    [ ( "text-align", "center" )
                    , ( "width", (Round.round 0 (100 * (Basics.min 1.0 ratio))) ++ "%" )
                    ]
                ]
                []
            , span
                [ style
                    [ ( "position", "absolute" )
                    , ( "left", "0" )
                    , ( "width", "100%" )
                    , ( "text-align", "center" )
                    , ( "z-index", "2" )
                    ]
                ]
                [ text (Round.round 0 (ratio * 100)), text "%" ]
            ]


{-| Get the URL to a task log of an instance
-}
logUrl : String -> AllocatedTask -> LogKind -> String
logUrl jobId task kind =
    String.concat
        [ "/downloads/instances/"
        , jobId
        , "/allocations/"
        , task.allocationId
        , "/tasks/"
        , task.taskName
        , "/logs/"
        , case kind of
            StdOut ->
                "stdout"

            StdErr ->
                "stderr"

        -- Only fetch the last 500 KiB of the log, to avoid large requests and download times
        , "?offset=500KiB"
        ]
