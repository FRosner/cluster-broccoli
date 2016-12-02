module Views.NewInstanceForm exposing (view, Msg)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Models.Resources.Template exposing (Template, addTemplateInstanceString)

type Msg
  = ShowNewInstanceForm Template
  | HideNewInstanceForm Template

view : Template -> Bool -> Html Msg
view template expanded =
  div
    [ class
      ( String.concat
        [ "panel panel-default "
        , if (expanded) then "show" else "hidden"
        ]
      )
    ]
    [ div
      [ class "panel-heading" ]
      [ strong []
        [ text (addTemplateInstanceString template) ]
      ]
    , div
      [ class "panel-body" ]
      [ text "bla" ]
    , div
      [ class "modal-footer" ]
      [ button
        [ class "btn btn-warning"
        -- , onClick (HideNewInstanceForm template)
        ]
        [ text "Cancel" ]
      , button
        [ class "btn btn-primary"
        -- , onClick (HideNewInstanceForm template)
        ]
        [ text "Ok" ]
      ]
    ]
