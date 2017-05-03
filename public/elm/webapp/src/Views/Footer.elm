module Views.Footer exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)

import Model exposing (Model)

import Messages exposing (AnyMsg(..))
import Utils.HtmlUtils exposing (icon)

view : Model -> Html AnyMsg
view model =
  let
    maybeAboutInfo = model.aboutInfo
  in
    footer [ class "footer" ]
      [ div [ class "container" ]
        [ div
          [ class "row" ]
          [ div
            [ class "col-md-12" ]
            [ p [ class "text-muted text-center" ]
              [ text
                ( maybeAboutInfo
                  |> Maybe.map (\i -> i.projectInfo.name)
                  |> Maybe.withDefault "<Project Name>"
                )
              , text ": "
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
              , text "), Websocket: "
              , statusToIcon (if (model.wsConnected) then Just True else Nothing) (\i -> i)
              , text ", Cluster Manager: "
              , statusToIcon maybeAboutInfo (\i -> i.services.clusterManagerInfo.connected)
              , text ", Service Discovery: "
              , statusToIcon maybeAboutInfo (\i -> i.services.serviceDiscoveryInfo.connected)
              ]
            ]
          ]
        ]
      ]

statusToIcon maybeAboutInfo statusFunction =
  let (iconClass, textColor) =
      case (Maybe.map statusFunction maybeAboutInfo) of
        Just True ->
          ("fa fa-check-circle", "#070")
        Just False ->
          ("fa fa-times-circle", "#900")
        Nothing ->
          ("fa fa-refresh fa-spin", "grey")
    in
      span
        [ style [ ("color", textColor) ] ]
        [ icon iconClass [] ]
