module Utils.HtmlUtils exposing (icon)

import Html exposing (..)
import Html.Attributes exposing (..)

icon clazz attributes =
  span
    ( List.append
      [ class clazz
      , attribute "aria-hidden" "true"
      ]
      attributes
    )
    []
