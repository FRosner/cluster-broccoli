module Views.TemplateView exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.InstanceView
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Models.Ui.InstanceParameterForm exposing (..)
import Set exposing (Set)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)

view instances templates bodyUiModel template =
  let (templateInstances) =
    List.filter (\i -> i.template.id == template.id) instances
  in
    let (selectedTemplateInstances) =
      templateInstances
      |> List.map (\i -> i.id)
      |> Set.fromList
      |> Set.intersect bodyUiModel.selectedInstances
    in
      div
        [ class "panel panel-default" ]
        [ div
          [ class "panel-heading" ]
          [ templatePanelHeadingView template bodyUiModel.expandedTemplates templateInstances ]
        , div
          [ class (if (Set.member template.id bodyUiModel.expandedTemplates) then "show" else "hidden") ]
          [ div
            [ class "panel-body"
            , style [ ( "padding-bottom", "0px" ) ]
            ]
            [ p []
              [ text template.description ]
            , p []
              [ iconButtonText
                  "btn btn-default"
                  "fa fa-plus-circle"
                  "New"
                  []
              , text " "
              , div
                [ class "btn-group"
                , attribute "role" "group"
                , attribute "aria-label" "..."
                ]
                [ iconButtonText
                    "btn btn-default"
                    "fa fa-play-circle"
                    "Start"
                    ( disabledIfNothingSelected selectedTemplateInstances )
                , text " "
                , iconButtonText
                    "btn btn-default"
                    "fa fa-stop-circle"
                    "Stop"
                    ( disabledIfNothingSelected selectedTemplateInstances )
                , text " "
                , iconButtonText
                    "btn btn-default"
                    "fa fa-code-fork"
                    "Upgrade"
                    ( disabledIfNothingSelected selectedTemplateInstances )
                ]
              , text " "
              , iconButtonText
                  "btn btn-default"
                  "fa fa-trash"
                  "Delete"
                  []
              ]
            ]
          , ( Views.InstanceView.view
              templateInstances
              selectedTemplateInstances
              bodyUiModel.expandedInstances
              bodyUiModel.instanceParameterForms
              bodyUiModel.visibleSecrets
              templates
            )
          ]
        ]

disabledIfNothingSelected selectedInstances =
  if (Set.isEmpty selectedInstances) then
    [ attribute "disabled" "disabled" ]
  else
    []

templatePanelHeadingView template expandedTemplates instances =
  span
    []
    [ templateIdView template expandedTemplates
    , text " "
    , templatePanelHeadingInfo "fa fa-list" "Number of Instances" (text (toString (List.length instances)))
    , text " "
    , templatePanelHeadingInfo "fa fa-code-fork" "Template Version" (templateVersion template)
    ]

templatePanelHeadingInfo clazz infoTitle info =
  span
    [ style
      [ ("margin-left", "10px")
      , ("font-weight", "100")
      , ("background-color", "#555")
      , ("color", "#fff")
      , ("margin-top", "4px")
      ]
    , title infoTitle
    , class "badge pull-right hidden-xs"
    ]
    [ icon clazz [ style [ ("margin-right", "4px") ] ]
    , info
    ]

templateIdView template expandedTemplates =
  span
    [ attribute "role" "button"
    , onClick (ToggleTemplate template.id)
    ]
    [ icon
      ( String.concat
        [ "glyphicon glyphicon-chevron-"
        , if (Set.member template.id expandedTemplates) then "down" else "right"
        ]
      )
      [ style [ ("margin-right", "4px") ] ]
    , span
      [ style [ ("font-size", "125%"), ("margin-right", "10px") ] ]
      [ text template.id ]
    ]

templateVersion template =
  span
    [ style [ ("font-family", "monospace") ] ]
    [ text (String.left 8 template.version) ]
