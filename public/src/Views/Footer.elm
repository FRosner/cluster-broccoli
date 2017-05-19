module Views.Footer exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)

import Models.Resources.AboutInfo exposing (AboutInfo)

import Messages exposing (AnyMsg(..))
import Utils.HtmlUtils exposing (icon)

view : Maybe AboutInfo -> Bool -> Html AnyMsg
view maybeAboutInfo wsConnected =
  footer [ class "footer" ]
    [ div [ class "container" ]
      [ div
        [ class "row" ]
        [ div
          [ class "col-md-12" ]
          [ p [ class "text-muted text-center" ]
            ( List.concat
              [ ( maybeAboutInfo
                  |> Maybe.map aboutInfoToProjectText
                  |> Maybe.withDefault []
                )
              , [ text "Websocket: "
                , wsToIcon wsConnected
                , text ", Cluster Manager: "
                , statusToIcon maybeAboutInfo (\i -> i.services.clusterManagerInfo.connected)
                , text ", Service Discovery: "
                , statusToIcon maybeAboutInfo (\i -> i.services.serviceDiscoveryInfo.connected)
                ]
              ]
            )
          ]
        ]
      ]
    ]

aboutInfoToProjectText : AboutInfo -> List (Html msg)
aboutInfoToProjectText aboutInfo =
  [ text aboutInfo.projectInfo.name
  , text ": "
  , text aboutInfo.projectInfo.version
  , text " (built with Scala "
  , text aboutInfo.scalaInfo.version
  , text ", SBT "
  , text aboutInfo.sbtInfo.version
  , text "), "
  ]

wsToIcon : Bool -> Html msg
wsToIcon connected =
  let (iconClass, textColor) =
      if (connected) then
        ("fa fa-check-circle", "#070")
      else
        ("fa fa-refresh fa-spin", "grey")
    in
      icon
        iconClass
        [ id "ws-indicator"
        , style [ ("color", textColor) ]
        ]

statusToIcon : Maybe AboutInfo -> (AboutInfo -> Bool) -> Html msg
statusToIcon maybeAboutInfo statusFunction =
  let (iconClass, textColor) =
      case (Maybe.map statusFunction maybeAboutInfo) of
        Just True ->
          ("fa fa-check-circle", "#070")
        Just False ->
          ("fa fa-times-circle", "#900")
        Nothing ->
          ("fa fa-question-circle", "grey")
    in
      span
        [ style [ ("color", textColor) ] ]
        [ icon iconClass [] ]
