module Views.JobTasksView exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.Allocation exposing (shortAllocationId)
import Models.Resources.ClientStatus exposing (ClientStatus(ClientComplete))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.TaskState exposing (TaskState(..))
import Views.LogUrl as LogUrl
import Views.ResourceUsageBar as ResourceUsageBar
import Views.Styles as Styles


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
                                    [ th [] [ text "Allocation" ]
                                    , th [ class "text-center" ] [ text "State" ]
                                    , th [ style [ ( "width", "100%" ) ] ] [ text "Task" ]
                                    , th [ class "text-center" ] [ text "CPU" ]
                                    , th [ class "text-center" ] [ text "Memory" ]
                                    , th [ class "text-center" ] [ text "Task Logs" ]
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
                    ( "completed", "label-primary" )

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
                (Maybe.map2 ResourceUsageBar.cpuUsageBar task.resources.cpuUsedMhz task.resources.cpuRequiredMhz)
            ]
        , td []
            [ Maybe.withDefault (text "Unknown")
                (Maybe.map2 ResourceUsageBar.memoryUsageBar task.resources.memoryUsedBytes task.resources.memoryRequiredBytes)
            ]
        , td
            -- Do not wrap buttons in this cell
            [ class "text-center", style [ ( "white-space", "nowrap" ) ] ]
            [ a
                [ href (LogUrl.taskLog jobId task StdOut)
                , target "_blank"
                , class "btn btn-default btn-xs"
                ]
                [ text "stdout" ]
            , text " "
            , a
                [ href (LogUrl.taskLog jobId task StdErr)
                , target "_blank"
                , class "btn btn-default btn-xs"
                ]
                [ text "stderr" ]
            ]
        ]
