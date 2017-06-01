module Views.HeaderSuite exposing (tests)

import Views.Header as Header

import Models.Resources.AboutInfo as AboutInfo exposing (AboutInfo)
import Models.Resources.Role as Role exposing (Role(Administrator))
import Models.Ui.LoginForm as LoginForm exposing (LoginForm)

import Updates.Messages exposing (UpdateLoginFormMsg(EnterUserName, EnterPassword, LoginAttempt, LogoutAttempt))

import Messages exposing (AnyMsg(UpdateLoginFormMsg))

import Test exposing (test, describe, Test)
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Test.Html.Events as Events

tests : Test
tests =
  describe "Header View"

    [ describe "Login Form"

      [ test "Should render username in input field" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-username" ]
              |> Query.has [ Selector.attribute "value" defaultLoginForm.username ]

      , test "Should render password in input field" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-password" ]
              |> Query.has [ Selector.attribute "value" defaultLoginForm.password ]

      , test "Should look normal if the login is correct" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, { defaultLoginForm | loginIncorrect = False }, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-form" ]
              |> Query.has [ Selector.classes [ "navbar-form", "navbar-right" ] ]

      , test "Should shake if the login is correct" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, { defaultLoginForm | loginIncorrect = True }, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-form" ]
              |> Query.has [ Selector.classes [ "navbar-form", "navbar-right", "animated", "shake" ] ]

      , test "Should render if auth is required" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.has [ Selector.id "header-login-form" ]

      , test "Should not render if auth is not required" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just False )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "header-login-form" ]

      , test "Should not render if we don't know if auth is required" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Nothing )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "header-login-form" ]

      , test "Should update the username when the user name input field changes" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-username" ]
              |> Events.simulate (Events.Input "admin")
              |> Events.expectEvent (UpdateLoginFormMsg (EnterUserName "admin"))

      , test "Should update the password when the password input field changes" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-password" ]
              |> Events.simulate (Events.Input "secret")
              |> Events.expectEvent (UpdateLoginFormMsg (EnterPassword "secret"))

      , test "Should attempt to login with the entered credentials on form sumbission" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing, defaultLoginForm, Just True )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-login-form" ]
              |> Events.simulate Events.Submit
              |> Events.expectEvent (UpdateLoginFormMsg (LoginAttempt defaultLoginForm.username defaultLoginForm.password))
      ]

    , describe "Logout Form"

      [ test "Should render if auth is enabled but not required (which means you are logged in)" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Just <| withAuthEnabled defaultAboutInfo True
              , defaultLoginForm
              , Just False
              )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.has [ Selector.id "header-logout-form" ]

      , test "Should not render if auth is disabled and not required" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Just <| withAuthEnabled defaultAboutInfo False
              , defaultLoginForm
              , Just False
              )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "header-logout-form" ]

      , test "Should not render if it is unknown whether auth is required" <|
          \() ->
            let ( maybeAboutInfo, loginForm, maybeAuthRequired ) =
              ( Nothing
              , defaultLoginForm
              , Just False
              )
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.hasNot [ Selector.id "header-logout-form" ]


      , test "Should attempt to login with the entered credentials on form sumbission" <|
          \() ->
            let
              maybeAboutInfo = Just <| withAuthEnabled defaultAboutInfo True
              loginForm = defaultLoginForm
              maybeAuthRequired = Just False
            in
              Header.view maybeAboutInfo loginForm maybeAuthRequired "" ""
              |> Query.fromHtml
              |> Query.find [ Selector.id "header-logout-form" ]
              |> Events.simulate Events.Submit
              |> Events.expectEvent (UpdateLoginFormMsg LogoutAttempt)

      ]

    ]


defaultLoginForm : LoginForm
defaultLoginForm =
  { username = "username"
  , password = "password"
  , loginIncorrect = False
  }

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

withAuthEnabled aboutInfo authEnabled =
  { enabled = authEnabled
  , userInfo = aboutInfo.authInfo.userInfo
  } |> AboutInfo.asAuthInfoOf aboutInfo
