module Utils.HtmlUtils exposing (..)

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

iconButton iconClass buttonText =
  button
    [ class "btn btn-default"
    , title buttonText
    ]
    [ icon iconClass [ style [ ("margin-right", "4px")] ]
    , text buttonText
    ]
