module Utils.HtmlUtils exposing (icon)

import Html exposing (..)
import Html.Attributes exposing (..)

icon clazz stylez =
  span
    [ class clazz
    , attribute "aria-hidden" "true"
    , style stylez
    ]
    []
