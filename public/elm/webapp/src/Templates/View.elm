module Templates.View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (class, style)
import Templates.Messages exposing (..)
import Templates.Models exposing (..)
import Color

templateList : Templates -> Html Msg
templateList templates =
  div
    [ class "container" ]
    (List.map templateRow templates)

templateRow template =
  div
    [ class "row" ]
    [ div
      [ class "col-lg-8" ]
      [ h2 []
        [ span
          [ style [ ("padding-right", "12px") ] ]
          [ text template.id ]
        , templateVersion template
        ]
      ]
    , div
      [ class "col-lg-4" ]
      [ h2 [ class "text-right" ]
        [ newInstanceButton
        ]
      ]
    ]

newInstanceButton =
  button
    [ class "btn btn-default" ]
    [ span
      [ class "fa fa-plus-circle" ]
      []
    , span
      [ style [ ("padding-left", "6px") ] ]
      [ text "New Instance" ]
    ]

templateVersion template =
  span
    [ class "badge"
    , style [ ("font-family", "monospace") ]
    ]
    [ text (String.left 8 template.version) ]
