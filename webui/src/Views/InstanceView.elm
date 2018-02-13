module Views.InstanceView exposing (view)

import Models.Resources.ServiceStatus exposing (..)
import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.Role exposing (Role(..))
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.Instance exposing (Instance)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.ClientStatus exposing (ClientStatus(ClientComplete))
import Models.Resources.Allocation exposing (shortAllocationId)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Views.ParameterFormView as ParameterFormView
import Views.Styles exposing (instanceViewElementStyle)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onCheck, onInput, onSubmit)
import Dict exposing (..)
import Set exposing (Set)
import Date
import Filesize
import Round
import Maybe.Extra exposing (unwrap)
import Date.Extra.Format as DateFormat
import Date.Extra.Config.Config_en_us as Config_en_us


checkboxColumnWidth =
    1


chevronColumnWidth =
    30



-- nameColumnWidth = 200


serviceColumnWidth =
    500


templateVersionColumnWidth =
    1


jobControlsColumnWidth =
    200


view instances selectedInstances expandedInstances instanceParameterForms visibleSecrets tasks templates maybeRole attemptedDeleteInstances =
    let
        instancesIds =
            instances
                |> Dict.keys
                |> Set.fromList
    in
        let
            ( allInstancesSelected, allInstancesExpanded ) =
                ( (instancesIds
                    |> Set.intersect selectedInstances
                    |> (==) instancesIds
                    |> (&&) (not (Set.isEmpty instancesIds))
                  )
                , (instancesIds
                    |> Set.intersect expandedInstances
                    |> (==) instancesIds
                    |> (&&) (not (Set.isEmpty instancesIds))
                  )
                )
        in
            table
                [ class "table"
                , style [ ( "margin-bottom", "0px" ) ]
                ]
                [ thead []
                    [ tr []
                        [ th
                            [ width checkboxColumnWidth ]
                            [ input
                                [ type_ "checkbox"
                                , title "Select All"
                                , onCheck (AllInstancesSelected instancesIds)
                                , checked allInstancesSelected
                                ]
                                []
                            ]
                        , th
                            [ width chevronColumnWidth ]
                            [ icon
                                (String.concat
                                    [ "fa fa-chevron-"
                                    , if (allInstancesExpanded) then
                                        "down"
                                      else
                                        "right"
                                    ]
                                )
                                [ attribute "role" "button"
                                , onClick
                                    (AllInstancesExpanded
                                        instancesIds
                                        (not allInstancesExpanded)
                                    )
                                ]
                            ]
                        , th
                            -- [ width nameColumnWidth ]
                            []
                            [ icon "fa fa-hashtag" [ title "Instance ID" ] ]
                        , th
                            [ class "text-left hidden-xs"
                            , width serviceColumnWidth
                            ]
                            [ icon "fa fa-cubes" [ title "Services" ] ]
                        , th
                            [ class "text-center hidden-sm hidden-xs"
                            , width templateVersionColumnWidth
                            ]
                            [ icon "fa fa-code-fork" [ title "Template Version" ] ]
                        , th
                            [ class "text-center"
                            , width jobControlsColumnWidth
                            ]
                            [ icon "fa fa-cogs" [ title "Job Controls" ] ]
                        ]
                    ]
                , tbody []
                    (instances
                        |> Dict.values
                        |> List.concatMap
                            (instanceRow
                                selectedInstances
                                expandedInstances
                                instanceParameterForms
                                visibleSecrets
                                templates
                                tasks
                                maybeRole
                                attemptedDeleteInstances
                            )
                    )
                ]


instanceRow selectedInstances expandedInstances instanceParameterForms visibleSecrets templates tasks maybeRole attemptedDeleteInstances instance =
    let
        instanceExpanded =
            Set.member instance.id expandedInstances

        instanceParameterForm =
            Dict.get instance.id instanceParameterForms

        instanceTasks =
            Dict.get instance.id tasks

        toDelete =
            attemptedDeleteInstances
                |> Maybe.map (Set.member instance.id)
                |> Maybe.withDefault False
    in
        List.append
            [ tr
                (List.concat
                    [ [ class "instance-row"
                      , id (String.concat [ "instance-row-", instance.id ])
                      ]
                    , (if (toDelete) then
                        [ style [ ( "background-color", "#c9302c" ) ] ]
                       else
                        []
                      )
                    ]
                )
                [ td
                    [ width checkboxColumnWidth ]
                    [ input
                        [ type_ "checkbox"
                        , onCheck (InstanceSelected instance.id)
                        , checked (Set.member instance.id selectedInstances)
                        , id <| String.concat [ "select-instance-", instance.id ]
                        ]
                        []
                    ]
                , td
                    [ width chevronColumnWidth ]
                    [ icon
                        (String.concat
                            [ "fa fa-chevron-"
                            , if (Set.member instance.id expandedInstances) then
                                "down"
                              else
                                "right"
                            ]
                        )
                        [ attribute "role" "button"
                        , onClick (InstanceExpanded instance.id (not instanceExpanded))
                        , id <| String.concat [ "expand-instance-chevron-", instance.id ]
                        ]
                    ]
                , td
                    -- [ width nameColumnWidth ]
                    []
                    [ span
                        [ attribute "role" "button"
                        , onClick (InstanceExpanded instance.id (not instanceExpanded))
                        , id <| String.concat [ "expand-instance-name-", instance.id ]
                        ]
                        [ text instance.id ]
                    ]
                , td
                    [ class "text-left hidden-xs"
                    , width serviceColumnWidth
                    ]
                    (servicesView instance.services)
                , td
                    [ class "text-center hidden-sm hidden-xs"
                    , width templateVersionColumnWidth
                    ]
                    [ span
                        [ style [ ( "font-family", "monospace" ) ] ]
                        [ text (String.left 8 instance.template.version) ]
                    ]
                , td
                    [ class "text-center"
                    , width jobControlsColumnWidth
                    ]
                    (List.concat
                        [ [ jobStatusView instance.jobStatus
                          , text " "
                          ]
                        , if (maybeRole /= Just Operator && maybeRole /= Just Administrator) then
                            []
                          else
                            [ iconButton
                                "btn btn-default btn-xs"
                                (String.append
                                    "glyphicon glyphicon-"
                                    (if
                                        (instance.jobStatus
                                            == JobStatus.JobRunning
                                            || instance.jobStatus
                                            == JobStatus.JobPending
                                            || instance.jobStatus
                                            == JobStatus.JobDead
                                        )
                                     then
                                        "refresh"
                                     else
                                        "play"
                                    )
                                )
                                "Start Instance"
                                (List.append
                                    [ onClick (StartInstance instance.id)
                                    , id <| String.concat [ "start-instance-", instance.id ]
                                    ]
                                    (if (instance.jobStatus == JobStatus.JobUnknown) then
                                        [ attribute "disabled" "disabled" ]
                                     else
                                        []
                                    )
                                )
                            , text " "
                            , iconButton
                                "btn btn-default btn-xs"
                                "glyphicon glyphicon-stop"
                                "Stop Instance"
                                (List.append
                                    [ onClick (StopInstance instance.id)
                                    , id <| String.concat [ "stop-instance-", instance.id ]
                                    ]
                                    (if
                                        (instance.jobStatus
                                            == JobStatus.JobStopped
                                            || instance.jobStatus
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
                    )
                ]
            ]
            (if (instanceExpanded) then
                [ instanceDetailView
                    instance
                    instanceTasks
                    instanceParameterForm
                    visibleSecrets
                    templates
                    maybeRole
                ]
             else
                []
            )


expandedTdStyle =
    [ ( "border-top", "0px" )
    , ( "padding-top", "0px" )
    ]



-- TODO as "id" is special we should treat it also special


instanceDetailView instance instanceTasks maybeInstanceParameterForm visibleSecrets templates maybeRole =
    let
        periodicRuns =
            List.reverse (List.sortBy .utcSeconds instance.periodicRuns)
    in
        tr []
            [ td
                [ style expandedTdStyle
                , width checkboxColumnWidth
                ]
                []
            , td
                [ colspan 5
                , style
                    (List.append
                        expandedTdStyle
                        [ ( "padding-right", "40px" ) ]
                    )
                ]
                (List.concat
                    [ [ ParameterFormView.editView instance templates maybeInstanceParameterForm visibleSecrets maybeRole
                      ]
                    , (if (List.isEmpty periodicRuns) then
                        []
                       else
                        [ h5 [] [ text "Periodic Runs" ]
                        , ul []
                            (List.map (periodicRunView instance.id) periodicRuns)
                        ]
                      )
                    , instanceTasksView instance instanceTasks
                    ]
                )
            ]


instanceTasksView : Instance -> Maybe (List AllocatedTask) -> List (Html msg)
instanceTasksView instance instanceTasks =
    case Maybe.map (List.filter (.clientStatus >> (/=) ClientComplete)) instanceTasks of
        Nothing ->
            [ h5 [] [ text "Allocated Tasks" ]
            , i [ class "fa fa-spinner fa-spin" ] []
            ]

        Just [] ->
            []

        -- Filter all complete allocations and attach the task name to every
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
        Just allocations ->
            [ div
                [ style instanceViewElementStyle ]
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
                            , tbody [] <| List.indexedMap (instanceAllocationRow instance) allocations
                            ]
                      ]
                    ]
                )
            ]


{-| Get the URL to a task log of an instance
-}
logUrl : Instance -> AllocatedTask -> LogKind -> String
logUrl instance task kind =
    String.concat
        [ "/downloads/instances/"
        , instance.id
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


instanceAllocationRow : Instance -> Int -> AllocatedTask -> Html msg
instanceAllocationRow instance index task =
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
                    [ href (logUrl instance task StdOut)
                    , target "_blank"
                    , class "btn btn-default btn-xs"
                    ]
                    [ text "stdout" ]
                , text " "
                , a
                    [ href (logUrl instance task StdErr)
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


periodicRunView instanceId periodicRun =
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
        , jobStatusView periodicRun.status
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


periodicRunDateView date =
    String.concat
        [ toString (Date.hour date)
        , ":"
        , toString (Date.minute date)
        , ":"
        , toString (Date.second date)
        , ":"
        , toString (Date.millisecond date)
        , ", "
        , toString (Date.day date)
        , ". "
        , toString (Date.month date)
        , " "
        , toString (Date.year date)
        ]


jobStatusView jobStatus =
    let
        ( statusLabel, statusText ) =
            case jobStatus of
                JobRunning ->
                    ( "success", "running" )

                JobPending ->
                    ( "warning", "pending" )

                JobStopped ->
                    ( "default", "stopped" )

                JobDead ->
                    ( "primary", "completed" )

                JobUnknown ->
                    ( "warning", "unknown" )
    in
        span
            [ class (String.concat [ "hidden-xs label label-", statusLabel ])
            , style
                [ ( "font-size", "90%" )
                , ( "width", "80px" )
                , ( "display", "inline-block" )
                , ( "margin-right", "8px" )
                ]
            ]
            [ text statusText ]


servicesView services =
    if (List.isEmpty services) then
        [ text "-" ]
    else
        List.concatMap serviceView services


serviceView service =
    let
        ( iconClass, textColor ) =
            case service.status of
                ServicePassing ->
                    ( "fa fa-check-circle", "#070" )

                ServiceFailing ->
                    ( "fa fa-times-circle", "#900" )

                ServiceUnknown ->
                    ( "fa fa-question-circle", "grey" )
    in
        [ a
            (List.append
                (if (service.status /= ServiceUnknown) then
                    [ href
                        (String.concat
                            [ service.protocol
                            , "://"
                            , service.address
                            , ":"
                            , (toString service.port_)
                            ]
                        )
                    ]
                 else
                    []
                )
                [ style
                    [ ( "margin-right", "8px" )
                    , ( "color", textColor )
                    ]
                , target "_blank"
                ]
            )
            [ icon iconClass [ style [ ( "margin-right", "4px" ) ] ]
            , text service.name
            ]
        , text " "
        ]
