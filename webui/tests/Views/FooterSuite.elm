module Views.FooterSuite exposing (tests)

import Models.Resources.AboutInfo as AboutInfo exposing (AboutInfo)
import Models.Resources.Role as Role exposing (Role(Administrator))
import Test exposing (Test, describe, test)
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Views.Footer as Footer


tests : Test
tests =
    describe "Footer View"
        [ test "Websocket disconnected" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Nothing, False )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-ws-indicator" [ "fa", "fa-refresh", "fa-spin" ]
        , test "Websocket connected" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Nothing, True )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-ws-indicator" [ "fa", "fa-check-circle" ]
        , test "Shows correct info if available" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Just defaultAboutInfo, True )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> Query.fromHtml
                    |> Query.find [ Selector.id "footer-project-info" ]
                    |> Query.has
                        [ Selector.text "pname: pversion (built with Scala sversion, SBT sbtversion), "
                        ]
        , test "Shows correct info if available" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Nothing, True )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> Query.fromHtml
                    |> Query.hasNot [ Selector.id "footer-project-info" ]
        , test "Cluster manager disconnected" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Just <| withClusterManagerConnected defaultAboutInfo False
                        , False
                        )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-cm-indicator" [ "fa", "fa-times-circle" ]
        , test "Cluster manager connected" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Just <| withClusterManagerConnected defaultAboutInfo True
                        , False
                        )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-cm-indicator" [ "fa", "fa-check-circle" ]
        , test "Cluster manager unknown" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Nothing
                        , False
                        )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-cm-indicator" [ "fa", "fa-question-circle" ]
        , test "Service discovery disconnected" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Just <| withServiceDiscoveryConnected defaultAboutInfo False
                        , False
                        )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-sd-indicator" [ "fa", "fa-times-circle" ]
        , test "Service discovery connected" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Just <| withServiceDiscoveryConnected defaultAboutInfo True
                        , False
                        )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-sd-indicator" [ "fa", "fa-check-circle" ]
        , test "Service discovery unknown" <|
            \() ->
                let
                    ( maybeAboutInfo, wsConnected ) =
                        ( Nothing
                        , False
                        )
                in
                Footer.view maybeAboutInfo wsConnected
                    |> expectClasses "footer-sd-indicator" [ "fa", "fa-question-circle" ]
        ]


defaultAboutInfo : AboutInfo
defaultAboutInfo =
    { projectInfo =
        { name = "pname"
        , version = "pversion"
        }
    , scalaInfo =
        { version = "sversion"
        }
    , sbtInfo =
        { version = "sbtversion"
        }
    , authInfo =
        { enabled = True
        , userInfo =
            { name = "user"
            , role = Administrator
            , instanceRegex = ".*"
            }
        }
    , services =
        { clusterManagerInfo =
            { connected = True
            }
        , serviceDiscoveryInfo =
            { connected = True
            }
        }
    }


withClusterManagerConnected aboutInfo connected =
    { connected = connected }
        |> AboutInfo.asClusterManagerInfoOf aboutInfo.services
        |> AboutInfo.asServicesInfoOf aboutInfo


withServiceDiscoveryConnected aboutInfo connected =
    { connected = connected }
        |> AboutInfo.asServiceDiscoveryInfoOf aboutInfo.services
        |> AboutInfo.asServicesInfoOf aboutInfo


expectClasses id classes html =
    html
        |> Query.fromHtml
        |> Query.find [ Selector.id id ]
        |> Query.has
            [ Selector.classes classes

            -- TODO test style (not possible at the moment): https://github.com/eeue56/elm-html-test/issues/3
            ]
