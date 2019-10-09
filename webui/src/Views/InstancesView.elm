module Views.InstancesView exposing (view)

import Dict exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onCheck, onClick, onInput, onSubmit)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Role exposing (Role(..))
import Models.Resources.Template exposing (Template)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)
import Set exposing (Set)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButton, iconButtonText)
import Views.InstanceView as InstanceView
import Views.Styles as Styles


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
            ( instancesIds
                |> Set.intersect selectedInstances
                |> (==) instancesIds
                |> (&&) (not (Set.isEmpty instancesIds))
            , instancesIds
                |> Set.intersect expandedInstances
                |> (==) instancesIds
                |> (&&) (not (Set.isEmpty instancesIds))
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
                            , if allInstancesExpanded then
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
