module Views.FooterSuite exposing (tests)

import Views.Footer as Footer

import Test exposing (test, describe, Test)
import Test.Html.Query as Query
import Test.Html.Selector as Selector

tests : Test
tests =
  describe "Footer View"

    [ test "Websocket disconnected" <|
        \() ->
          let ( maybeAboutInfo, wsConnected ) =
            ( Nothing, False )
          in
            Footer.view maybeAboutInfo wsConnected
            |> expectClasses [ "fa", "fa-refresh", "fa-spin" ]

    , test "Websocket connected" <|
        \() ->
          let ( maybeAboutInfo, wsConnected ) =
            ( Nothing, True )
          in
            Footer.view maybeAboutInfo wsConnected
            |> expectClasses [ "fa", "fa-check-circle" ]
    ]

expectClasses classes html =
  html
  |> Query.fromHtml
  |> Query.find [ Selector.id "ws-indicator" ]
  |> Query.has
    [ Selector.classes classes
    -- TODO test style (not possible at the moment): https://github.com/eeue56/elm-html-test/issues/3
    ]
