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

iconButtonText btnClass iconClass buttonText attributes =
  button
    ( List.append
        attributes
        [ class btnClass
        , title buttonText
        ]
    )
    [ icon iconClass []
    , span
      [ class "hidden-xs"
      , style [ ("margin-left", "4px")]
      ]
      [ text buttonText ]
    ]

iconButton btnClass iconClass buttonTitle =
  button
    [ class btnClass
    , title buttonTitle
    ]
    [ icon iconClass [] ]
