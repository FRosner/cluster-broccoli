module Templates.View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Templates.Messages exposing (..)
import Templates.Models exposing (..)
import Dict exposing (..)

view : Model -> Html Msg
view model =
  div
    [ class "container" ]
    ( model
      |> Dict.values
      |> List.map templateRow
    )

templateRow : TemplateWithForms -> Html Msg
templateRow templateWithForm =
  div
    [ class "row" ]
    [ div
      [ class "col-lg-12" ]
      [ h2 []
        [ newInstanceButton templateWithForm.template
        , templateId templateWithForm.template
        , templateVersion templateWithForm.template
        ]
      , newInstanceForm templateWithForm.newInstanceForm
      ]
    ]

templateId template =
  span
    [ style [ ("margin-left", "12px") ] ]
    [ text template.id ]

addTemplateInstanceString template =
    String.concat ["New ", template.id, " instance"]

newInstanceButton template =
  a
    [ onClick (ShowNewInstanceForm template)
    , title (addTemplateInstanceString template)
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

newInstanceForm maybeNewInstanceForm =
  case maybeNewInstanceForm of
    Just form ->
      div
        [ class "panel panel-default" ]
        [ div
          [ class "panel-heading" ]
          [ strong []
            [ text (addTemplateInstanceString form.selectedTemplate) ]
          ]
        , div
          [ class "panel-body" ]
          [ text "bla" ]
        , div
          [ class "modal-footer" ]
          [ button
            [ class "btn btn-warning"
            , onClick (HideNewInstanceForm form.selectedTemplate)
            ]
            [ text "Cancel" ]
          , button
            [ class "btn btn-primary"
            , onClick (HideNewInstanceForm form.selectedTemplate)
            ]
            [ text "Ok" ]
          ]
        ]
    Nothing ->
      div
        [ class "hidden" ]
        []
