module Views.InstanceView exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)

view services instance =
  let (maybeInstanceServices) =
    Dict.get instance.id services
  in
    tr []
      [ td []
        [ input [ type_ "checkbox" ] [] ]
      , td []
        [ span
            [ style [ ("role", "button") ] ]
            [ text instance.id ]
        ]
      , td [ class "text-center" ]
        [ span
          [ style [ ("font-family", "monospace") ] ]
          [ text (String.left 8 instance.template.version) ]
        ]
      , td [ class "text-center" ]
        ( servicesView maybeInstanceServices )
      , td [ class "text-center" ]
        [ span
          [ class "label label-success"
          , style
            [ ("font-size", "90%")
            , ("width", "80px")
            , ("margin-right", "8px")
            ]
          ]
          [ text "running" ]
        , text " "
        , iconButton "btn btn-default btn-xs" "glyphicon glyphicon-play" "Start Instance"
        , text " "
        , iconButton "btn btn-default btn-xs" "glyphicon glyphicon-stop" "Stop Instance"
        ]
      ]

servicesView maybeServices =
  case maybeServices of
    Just services ->
      (List.concatMap serviceView services)
    Nothing ->
      [ text "-" ]

serviceView service =
  let (iconClass, textColor) =
    case service.status of
      Passing ->
        ("fa fa-check-circle", "#070")
      Failing ->
        ("fa fa-times-circle", "#900")
      Unknown ->
        ("fa fa-question-circle", "grey")
  in
    [ a
      [ href
        ( String.concat
          [ service.protocol
          , "://"
          , service.address
          , ":"
          , (toString service.port_)
          ]
        )
      , style
        [ ("margin-right", "8px")
        , ("color", textColor)
        ]
      ]
      [ icon iconClass [ style [ ("margin-right", "4px") ] ]
      , text service.name
      ]
    , text " "
    ]
