module Views.Body exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon)

view : List Template -> Set TemplateId -> Html UpdateBodyViewMsg
view templates expandedTemplates =
  div
    [ class "container" ]
    (List.map (templateView expandedTemplates) templates)

templateView expandedTemplates template =
  div
    [ class "panel panel-default" ]
    [ div
      [ class "panel-heading" ]
      [ templatePanelHeadingView template expandedTemplates ]
    , div
      [ class
          ( String.concat
            [ "panel-body "
            , if (Set.member template.id expandedTemplates) then "show" else "hidden"
            ]
          )
      ]
      [ text template.description
      , p [] [ text "instances..."] -- instances view
      ]
    ]

templatePanelHeadingView template expandedTemplates =
  span
    []
    [ templateIdView template expandedTemplates
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
      , ("margin-bottom", "2px")
      ]
    , title infoTitle
    , class "badge"
    ]
    [ icon clazz [ ("margin-right", "2px") ]
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
        , if (Set.member template.id expandedTemplates) then "right" else "down"
        ]
      )
      [ ("margin-right", "4px") ]
    , span
      [ style [ ("font-size", "125%"), ("margin-right", "10px") ] ]
      [ text template.id ]
    ]

templateVersion template =
  span
    [ style [ ("font-family", "monospace") ] ]
    [ text (String.left 8 template.version) ]
