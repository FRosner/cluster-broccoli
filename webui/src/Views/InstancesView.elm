module Views.InstancesView exposing (view)

import Models.Resources.ServiceStatus exposing (..)
import Models.Resources.JobStatus as JobStatus exposing (..)
import Models.Resources.Role exposing (Role(..))
import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.PeriodicRun exposing (PeriodicRun)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Template exposing (Template)
import Models.Resources.Service exposing (Service)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.TaskState exposing (TaskState(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.ClientStatus exposing (ClientStatus(ClientComplete))
import Models.Resources.Allocation exposing (shortAllocationId)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Views.ParameterFormView as ParameterFormView
import Views.Styles as Styles
import Views.InstanceView as InstanceView
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


view : Dict String Instance -> Set InstanceId -> Set InstanceId -> Dict String InstanceParameterForm -> Set ( InstanceId, String ) -> Dict String InstanceTasks -> Dict String Template -> Maybe Role -> Maybe (Set String) -> Html UpdateBodyViewMsg
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
                            [ width Styles.checkboxColumnWidth ]
                            [ input
                                [ type_ "checkbox"
                                , title "Select All"
                                , onCheck (AllInstancesSelected instancesIds)
                                , checked allInstancesSelected
                                ]
                                []
                            ]
                        , th
                            [ width Styles.chevronColumnWidth ]
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
                            , width Styles.serviceColumnWidth
                            ]
                            [ icon "fa fa-cubes" [ title "Services" ] ]
                        , th
                            [ class "text-center hidden-sm hidden-xs"
                            , width Styles.templateVersionColumnWidth
                            ]
                            [ icon "fa fa-code-fork" [ title "Template Version" ] ]
                        , th
                            [ class "text-center"
                            , width Styles.jobControlsColumnWidth
                            ]
                            [ icon "fa fa-cogs" [ title "Job Controls" ] ]
                        ]
                    ]
                , tbody []
                    (instances
                        |> Dict.values
                        |> List.concatMap
                            (InstanceView.view
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
