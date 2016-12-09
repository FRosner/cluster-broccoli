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

-- <div class="panel panel-default">
--   <!-- Default panel contents -->
--   <div class="panel-heading">Panel heading</div>
--   <div class="panel-body">
--     <p>...</p>
--   </div>
--
--   <!-- Table -->
--   <table class="table">
--     ...
--   </table>
-- </div>

templateView : Template -> Html AnyMsg
templateView template =
  div
    [ class "panel panel-default" ]
    [ div
      [ class "panel-heading" ]
      [ text template.id ]
    , div
      [ class "panel-body" ]
      [ text template.description ]
    , p [] [ text "instances..."] -- instances view
    ]

templateId template =
  span
    [ style [ ("margin-left", "12px") ] ]
    [ text template.id ]

newInstanceButton template =
  a
    [ title (addTemplateInstanceString template)
    -- , onClick (ShowNewInstanceForm template.id)
    , attribute "role" "button"
    ]
    [ img
      [ src "images/plus.svg"
      , class "img-responsive"
      , alt (addTemplateInstanceString template)
      , style
        [ ("height", "20px")
        , ("display", "inline-block")
        ]
      ]
      []
    ]

templateVersion template =
  span
    [ class "badge"
    , style
      [ ("font-family", "monospace")
      , ("margin-left", "10px")
      ]
    , title "Template Version"
    ]
    [ text (String.left 8 template.version) ]
