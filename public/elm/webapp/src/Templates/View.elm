module Templates.View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
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
      [ class "col-lg-12" ]
      [ h2 []
        [ newInstanceButton
        , templateId template
        , templateVersion template
        ]
      ]
    ]

templateId template =
  span
    [ style [ ("margin-left", "12px") ] ]
    [ text template.id ]

newInstanceButton =
  img
    [ src "images/plus.svg"
    , class "img-responsive"
    , alt "Add Instance"
    , style
      [ ("height", "20px")
      , ("display", "inline-block")
      ]
    ]
    []

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
