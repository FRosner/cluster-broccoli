module Views.FooterSuite exposing (tests)

import Views.Footer as Footer

import Models.Resources.AboutInfo exposing (AboutInfo)

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

    , test "Shows correct info" <|
        \() ->
          let ( maybeAboutInfo, wsConnected ) =
            ( Just defaultAboutInfo, True )
          in
            Footer.view maybeAboutInfo wsConnected
            |> Query.fromHtml
            |> Query.find [ Selector.id "footer-project-info" ]
            |> Query.has
              [ Selector.text "pname: pversion (built with Scala sversion, SBT sbtversion), "
              ]
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
      , role = "role"
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

expectClasses classes html =
  html
  |> Query.fromHtml
  |> Query.find [ Selector.id "footer-ws-indicator" ]
  |> Query.has
    [ Selector.classes classes
    -- TODO test style (not possible at the moment): https://github.com/eeue56/elm-html-test/issues/3
    ]
