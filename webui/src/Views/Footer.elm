module Views.Footer exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Messages exposing (AnyMsg(..))
import Models.Resources.AboutInfo exposing (AboutInfo)
import Utils.HtmlUtils exposing (icon)


view : Maybe AboutInfo -> Bool -> Html AnyMsg
view maybeAboutInfo wsConnected =
    footer [ class "footer navbar-fixed-bottom" ]
        [ div [ class "container" ]
            [ div
                [ class "row" ]
                [ div
                    [ class "col-md-12" ]
                    [ p [ class "text-muted text-center" ]
                        (List.concat
                            [ maybeAboutInfo
                                |> Maybe.map aboutInfoToProjectText
                                |> Maybe.withDefault []
                            , [ text "Websocket: "
                              , wsToIcon wsConnected
                              , text ", Cluster Manager: "
                              , statusToIcon "footer-cm-indicator" maybeAboutInfo (\i -> i.services.clusterManagerInfo.connected)
                              , text ", Service Discovery: "
                              , statusToIcon "footer-sd-indicator" maybeAboutInfo (\i -> i.services.serviceDiscoveryInfo.connected)
                              ]
                            ]
                        )
                    ]
                ]
            ]
        ]


aboutInfoToProjectText : AboutInfo -> List (Html msg)
aboutInfoToProjectText aboutInfo =
    [ span
        [ id "footer-project-info"
        , class "hidden-xs hidden-sm"
        ]
        [ text <|
            String.concat
                [ aboutInfo.projectInfo.name
                , ": "
                , aboutInfo.projectInfo.version
                , " (built with Scala "
                , aboutInfo.scalaInfo.version
                , ", SBT "
                , aboutInfo.sbtInfo.version
                , "), "
                ]
        ]
    ]


wsToIcon : Bool -> Html msg
wsToIcon connected =
    let
        ( iconClass, textColor ) =
            if connected then
                ( "fa fa-check-circle", "#070" )

            else
                ( "fa fa-refresh fa-spin", "grey" )
    in
    icon
        iconClass
        [ id "footer-ws-indicator"
        , style [ ( "color", textColor ) ]
        ]


statusToIcon : String -> Maybe AboutInfo -> (AboutInfo -> Bool) -> Html msg
statusToIcon elementId maybeAboutInfo statusFunction =
    let
        ( iconClass, textColor ) =
            case Maybe.map statusFunction maybeAboutInfo of
                Just True ->
                    ( "fa fa-check-circle", "#070" )

                Just False ->
                    ( "fa fa-times-circle", "#900" )

                Nothing ->
                    ( "fa fa-question-circle", "grey" )
    in
    icon
        iconClass
        [ style [ ( "color", textColor ) ]
        , id elementId
        ]
