module Views.InstanceView exposing (view)

import Models.Resources.ServiceStatus exposing (..)
import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.Role exposing (Role(..))
import Models.Resources.Task exposing (Task)
import Models.Resources.Instance exposing (Instance)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.ClientStatus exposing (ClientStatus(ClientComplete))
import Models.Resources.Allocation as Allocation exposing (Allocation, shortAllocationId)
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
                            (List.map periodicRunView periodicRuns)
                        ]
                      )
                    , [ instanceTasksView instance instanceTasks ]
                    ]
                )
            ]


{-| Get active, ie, non-complete tasks of an allocation.

Filter all complete allocations and attach the task name to every allocation.

We remove complete allocations because Nomad 0.5.x (possibly other versions as
well) returns all complete and thus dead allocations for stopped jobs, whereas
it only returns non-complete allocations for running jobs.

If we did not filter complete allocations the user would see dead allocations
suddenly popping up in the UI when they stopped the task—which would be
somewhat confusing.

-}
getActiveAllocations : Task -> List ( String, Allocation )
getActiveAllocations task =
    task.allocations
        |> List.filter (.clientStatus >> (/=) ClientComplete)
        |> List.map ((,) task.name)


instanceTasksView : Instance -> Maybe (List Task) -> Html msg
instanceTasksView instance instanceTasks =
    let
        allocatedTasks =
            case Maybe.map (List.concatMap getActiveAllocations) instanceTasks of
                Nothing ->
                    [ i [ class "fa fa-spinner fa-spin" ] [] ]

                Just [] ->
                    [ text "No tasks have been allocated, yet." ]

                Just allocations ->
                    [ table [ class "table table-condensed table-hover" ]
                        [ thead []
                            [ tr []
                                [ th [] [ text "Task" ]
                                , th [] [ text "Allocation ID" ]
                                , th [ class "text-center" ] [ text "State" ]
                                , th [ class "text-center" ] [ text "Log files" ]
                                ]
                            ]
                        , tbody [] <| List.indexedMap (instanceAllocationRow instance) allocations
                        ]
                    ]
    in
        div
            [ style instanceViewElementStyle ]
            (List.concat
                [ [ h5 [] [ text "Allocated Tasks" ] ]
                , allocatedTasks
                ]
            )


{-| Get the URL to a task log of an instance
-}
logUrl : Instance -> String -> Allocation -> LogKind -> String
logUrl instance taskName allocation kind =
    String.concat
        [ "/downloads/instances/"
        , instance.id
        , "/allocations/"
        , allocation.id
        , "/tasks/"
        , taskName
        , "/logs/"
        , case kind of
            StdOut ->
                "stdout"

            StdErr ->
                "stderr"

        -- Only fetch the last 500 KiB of the log, to avoid large requests and download times
        , "?offset=500KiB"
        ]


instanceAllocationRow : Instance -> Int -> ( String, Allocation ) -> Html msg
instanceAllocationRow instance index ( taskName, allocation ) =
    let
        ( description, labelKind ) =
            case allocation.taskState of
                TaskDead ->
                    ( "dead", "label-danger" )

                TaskPending ->
                    ( "pending", "label-warning" )

                TaskRunning ->
                    ( "running", "label-success" )
    in
        tr []
            [ th [ scope <| toString (index + 1) ] [ text taskName ]
            , td [] [ code [] [ text (shortAllocationId allocation.id) ] ]
            , td [ class "text-center" ]
                [ span [ class ("label " ++ labelKind) ] [ text description ]
                ]
            , td [ class "text-center" ]
                [ a
                    [ href (logUrl instance taskName allocation StdOut)
                    , target "_blank"
                    , class "btn btn-info btn-xs"
                    ]
                    [ text "stdout" ]
                , text " "
                , a
                    [ href (logUrl instance taskName allocation StdErr)
                    , target "_blank"
                    , class "btn btn-info btn-xs"
                    ]
                    [ text "stderr" ]
                ]
            ]


periodicRunView periodicRun =
    li []
        [ code [ style [ ( "margin-right", "12px" ) ] ] [ text periodicRun.jobName ]
        , text " "
        , span
            [ class "hidden-xs"
            , style [ ( "margin-right", "12px" ) ]
            ]
            [ icon "fa fa-clock-o" []
            , text " "
            , text (periodicRunDateView (Date.fromTime (toFloat periodicRun.utcSeconds)))
            ]
        , text " "
        , jobStatusView periodicRun.status
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
