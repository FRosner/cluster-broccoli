module Views.InstanceView exposing (view)

import Dict exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onCheck, onClick, onInput, onSubmit)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.Role exposing (Role(..))
import Models.Resources.Template exposing (Template)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)
import Set exposing (Set)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButton, iconButtonText)
import Views.InstanceDetailView as InstanceDetailView
import Views.JobStatusView as JobStatusView
import Views.ServicesView as ServicesView
import Views.Styles as Styles


view : Set InstanceId -> Set InstanceId -> Dict String InstanceParameterForm -> Set ( InstanceId, String ) -> Dict String Template -> Dict String InstanceTasks -> Maybe Role -> Maybe (Set String) -> Instance -> List (Html UpdateBodyViewMsg)
view selectedInstances expandedInstances instanceParameterForms visibleSecrets templates tasks maybeRole attemptedDeleteInstances instance =
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
                , if toDelete then
                    [ style [ ( "background-color", "#c9302c" ) ] ]

                  else
                    []
                ]
            )
            [ td
                [ width Styles.checkboxColumnWidth ]
                [ input
                    [ type_ "checkbox"
                    , onCheck (InstanceSelected instance.id)
                    , checked (Set.member instance.id selectedInstances)
                    , id <| String.concat [ "select-instance-", instance.id ]
                    ]
                    []
                ]
            , td
                [ width Styles.chevronColumnWidth ]
                [ icon
                    (String.concat
                        [ "fa fa-chevron-"
                        , if Set.member instance.id expandedInstances then
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
                , width Styles.serviceColumnWidth
                ]
                (ServicesView.view instance.services)
            , td
                [ class "text-center hidden-sm hidden-xs"
                , width Styles.templateVersionColumnWidth
                ]
                [ span
                    [ style [ ( "font-family", "monospace" ) ] ]
                    [ text (String.left 8 instance.template.version) ]
                ]
            , td
                [ class "text-center"
                , width Styles.jobControlsColumnWidth
                ]
                (List.concat
                    [ [ JobStatusView.view "hidden-xs" instance.jobStatus
                      , text " "
                      ]
                    , if maybeRole /= Just Operator && maybeRole /= Just Administrator then
                        []

                      else
                        [ iconButton
                            "btn btn-outline-secondary btn-xs"
                            (String.append
                                "fa fa-"
                                (if
                                    instance.jobStatus
                                        == JobStatus.JobRunning
                                        || instance.jobStatus
                                        == JobStatus.JobPending
                                        || instance.jobStatus
                                        == JobStatus.JobDead
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
                                (if instance.jobStatus == JobStatus.JobUnknown then
                                    [ attribute "disabled" "disabled" ]

                                 else
                                    []
                                )
                            )
                        , text " "
                        , iconButton
                            "btn btn-outline-secondary btn-xs"
                            "fa fa-stop"
                            "Stop Instance"
                            (List.append
                                [ onClick (StopInstance instance.id)
                                , id <| String.concat [ "stop-instance-", instance.id ]
                                ]
                                (if
                                    instance.jobStatus
                                        == JobStatus.JobStopped
                                        || instance.jobStatus
                                        == JobStatus.JobUnknown
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
        (if instanceExpanded then
            [ InstanceDetailView.view
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
