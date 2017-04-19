module Views.Footer exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Messages exposing (AnyMsg(..))
import Utils.HtmlUtils exposing (icon)

view : Maybe AboutInfo -> Html AnyMsg
view maybeAboutInfo =
  footer [ class "footer" ]
    [ div [ class "container" ]
      [ p [ class "text-muted" ]
        [ text
          ( maybeAboutInfo
            |> Maybe.map (\i -> i.projectInfo.name)
            |> Maybe.withDefault "<Project Name>"
          )
        , text " "
        , text
          ( maybeAboutInfo
            |> Maybe.map (\i -> i.projectInfo.version)
            |> Maybe.withDefault "<Project Version>"
          )
        , text " (built with Scala "
        , text
          ( maybeAboutInfo
            |> Maybe.map (\i -> i.scalaInfo.version)
            |> Maybe.withDefault "<Scala Version>"
          )
        , text ", SBT "
        , text
          ( maybeAboutInfo
            |> Maybe.map (\i -> i.sbtInfo.version)
            |> Maybe.withDefault "<SBT Version>"
          )
        , text ")"
        ]
      ]
    ]
