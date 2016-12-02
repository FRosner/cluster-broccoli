module Views.Body exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)

type Msg
  = NoOp

type alias ExpandedNewInstanceForms = Set TemplateId
type alias NewInstanceFormExpanded = Bool

view : List Template -> ExpandedNewInstanceForms -> Html Msg
view templates expandedNewInstanceForms =
  div
    [ class "container" ]
    (List.map (templateRow expandedNewInstanceForms) templates)

templateRow : ExpandedNewInstanceForms -> Template -> Html Msg
templateRow expandedNewInstanceForms template =
  div
    [ class "row" ]
    [ div
      [ class "col-lg-12" ]
      [ h2 []
        [ newInstanceButton template
        , templateId template
        , templateVersion template
        ]
      -- , Views.NewInstanceForm.view template (Set.member template.id expandedNewInstanceForms)
      ]
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
