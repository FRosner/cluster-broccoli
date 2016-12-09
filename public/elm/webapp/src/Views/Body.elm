module Views.Body exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Messages exposing (AnyMsg(..))

view : List Template -> Html AnyMsg
view templates =
  div
    [ class "container" ]
    (List.map templateView templates)

templateView : Template -> Html AnyMsg
templateView template =
  div
    [ class "panel panel-default" ]
    [ div
      [ class "panel-heading" ]
      [ templatePanelHeadingView template ]
    , div
      [ class "panel-body" ]
      [ text template.description ]
    , p [] [ text "instances..."] -- instances view
    ]

templatePanelHeadingView template =
  span
    []
    [ templateIdView template
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
      ]
    , title infoTitle
    , class "badge"
    ]
    [ span
      [ class clazz
      , attribute "aria-hidden" "true"
      , style [ ("margin-right", "2px") ]
      ]
      []
    , info
    ]

templateIdView template =
  span
    [ style [ ("font-size", "125%"), ("margin-right", "10px") ] ]
    [ text template.id ]

templateVersion template =
  span
    [ style [ ("font-family", "monospace") ] ]
    [ text (String.left 8 template.version) ]
