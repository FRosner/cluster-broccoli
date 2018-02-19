module Views.TemplateView exposing (view)

import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)
import Views.InstancesView as InstancesView
import Views.ParameterFormView as ParameterFormView
import Updates.Messages exposing (..)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.Role as Role exposing (Role(..))
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Dict exposing (Dict)
import Set exposing (Set)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)


view : Dict InstanceId Instance -> Dict InstanceId InstanceTasks -> Dict TemplateId Template -> BodyUiModel -> Maybe Role -> Template -> Html UpdateBodyViewMsg
view instances tasks templates bodyUiModel maybeRole template =
    let
        templateInstances =
            Dict.filter (\k i -> i.template.id == template.id) instances

        attemptedDeleteInstances =
            bodyUiModel.attemptedDeleteInstances
                |> Maybe.andThen
                    (\( templateId, instanceIds ) ->
                        if (templateId == template.id) then
                            Just instanceIds
                        else
                            Nothing
                    )
    in
        let
            selectedTemplateInstances =
                templateInstances
                    |> Dict.keys
                    |> Set.fromList
                    |> Set.intersect bodyUiModel.selectedInstances
        in
            div
                [ id (String.concat [ "template-", template.id ])
                , class "panel panel-default template"
                ]
                [ div
                    [ class "panel-heading" ]
                    [ templatePanelHeadingView template bodyUiModel.expandedTemplates templateInstances ]
                , div
                    [ class
                        (if (Set.member template.id bodyUiModel.expandedTemplates) then
                            "show"
                         else
                            "hidden"
                        )
                    ]
                    [ div
                        [ class "panel-body"
                        , style [ ( "padding-bottom", "0px" ) ]
                        ]
                        [ p []
                            [ text template.description ]
                        , p []
                            (List.concat
                                [ if (maybeRole /= Just Administrator) then
                                    []
                                  else
                                    [ iconButtonText
                                        "btn btn-default"
                                        "fa fa-plus-circle"
                                        "New"
                                        [ onClick (ExpandNewInstanceForm True template.id)
                                        , id <| String.concat [ "expand-new-instance-", template.id ]
                                        ]
                                    , text " "
                                    ]
                                , if (maybeRole /= Just Administrator && maybeRole /= Just Operator) then
                                    []
                                  else
                                    [ div
                                        [ class "btn-group"
                                        , attribute "role" "group"
                                        , attribute "aria-label" "..."
                                        ]
                                        [ iconButtonText
                                            "btn btn-default"
                                            "fa fa-play-circle"
                                            "Start"
                                            (List.concat
                                                [ (if (Set.isEmpty selectedTemplateInstances) then
                                                    [ attribute "disabled" "disabled" ]
                                                   else
                                                    []
                                                  )
                                                , [ onClick (StartSelectedInstances selectedTemplateInstances)
                                                  , id <| String.concat [ "start-selected-instances-", template.id ]
                                                  ]
                                                ]
                                            )
                                        , text " "
                                        , iconButtonText
                                            "btn btn-default"
                                            "fa fa-stop-circle"
                                            "Stop"
                                            (List.concat
                                                [ (if (Set.isEmpty selectedTemplateInstances) then
                                                    [ attribute "disabled" "disabled" ]
                                                   else
                                                    []
                                                  )
                                                , [ onClick (StopSelectedInstances selectedTemplateInstances)
                                                  , id <| String.concat [ "stop-selected-instances-", template.id ]
                                                  ]
                                                ]
                                            )

                                        -- , text " "
                                        -- , iconButtonText
                                        --     "btn btn-default"
                                        --     "fa fa-code-fork"
                                        --     "Upgrade"
                                        --     ( disabledIfNothingSelected selectedTemplateInstances )
                                        ]
                                    , text " "
                                    ]
                                , if (maybeRole /= Just Administrator) then
                                    []
                                  else
                                    case attemptedDeleteInstances of
                                        Nothing ->
                                            [ iconButtonText
                                                "btn btn-default"
                                                "fa fa-trash"
                                                "Delete"
                                                (List.concat
                                                    [ (if (Set.isEmpty selectedTemplateInstances) then
                                                        [ attribute "disabled" "disabled" ]
                                                       else
                                                        []
                                                      )
                                                    , [ onClick (AttemptDeleteSelectedInstances template.id selectedTemplateInstances)
                                                      , id <| String.concat [ "delete-selected-instances-", template.id ]
                                                      ]
                                                    ]
                                                )
                                            ]

                                        Just toDelete ->
                                            [ iconButtonText
                                                "btn btn-danger"
                                                "fa fa-trash"
                                                (String.concat [ "Delete?" ])
                                                (List.concat
                                                    [ (if (Set.isEmpty selectedTemplateInstances) then
                                                        [ attribute "disabled" "disabled" ]
                                                       else
                                                        []
                                                      )
                                                    , [ onClick (DeleteSelectedInstances template.id selectedTemplateInstances)
                                                      , id <| String.concat [ "confirm-delete-selected-instances-", template.id ]
                                                      ]
                                                    ]
                                                )
                                            ]
                                ]
                            )
                        ]
                    , div
                        [ class
                            (if (Dict.member template.id bodyUiModel.expandedNewInstanceForms) then
                                "show"
                             else
                                "hidden"
                            )
                        , id <| String.concat [ "new-instance-form-container-", template.id ]
                        ]
                        [ (ParameterFormView.newView
                            template
                            (Dict.get template.id bodyUiModel.expandedNewInstanceForms)
                            bodyUiModel.visibleNewInstanceSecrets
                          )
                        ]
                    , (InstancesView.view
                        templateInstances
                        selectedTemplateInstances
                        bodyUiModel.expandedInstances
                        bodyUiModel.instanceParameterForms
                        bodyUiModel.visibleEditInstanceSecrets
                        tasks
                        templates
                        maybeRole
                        attemptedDeleteInstances
                      )
                    ]
                ]


templatePanelHeadingView template expandedTemplates instances =
    span
        []
        [ templateIdView template expandedTemplates
        , text " "
        , templatePanelHeadingInfo "fa fa-list" "Number of Instances" (text (toString (Dict.size instances)))
        , text " "
        , templatePanelHeadingInfo "fa fa-code-fork" "Template Version" (templateVersion template)
        ]


templatePanelHeadingInfo clazz infoTitle info =
    span
        [ style
            [ ( "margin-left", "10px" )
            , ( "font-weight", "100" )
            , ( "background-color", "#555" )
            , ( "color", "#fff" )
            , ( "margin-top", "4px" )
            ]
        , title infoTitle
        , class "badge pull-right hidden-xs"
        ]
        [ icon clazz [ style [ ( "margin-right", "4px" ) ] ]
        , info
        ]


templateIdView template expandedTemplates =
    span
        [ id (String.concat [ "expand-template-", template.id ])
        , attribute "role" "button"
        , onClick (ToggleTemplate template.id)
        ]
        [ icon
            (String.concat
                [ "glyphicon glyphicon-chevron-"
                , if (Set.member template.id expandedTemplates) then
                    "down"
                  else
                    "right"
                ]
            )
            [ style [ ( "margin-right", "4px" ) ] ]
        , span
            [ style [ ( "font-size", "125%" ), ( "margin-right", "10px" ) ] ]
            [ text template.id ]
        ]


templateVersion template =
    span
        [ style [ ( "font-family", "monospace" ) ] ]
        [ text (String.left 8 template.version) ]
