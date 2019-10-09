module Views.HeaderSuite exposing (tests)

import Messages exposing (AnyMsg(InstanceFilter, TemplateFilter, UpdateLoginFormMsg))
import Model exposing (TabState(Instances))
import Models.Resources.AboutInfo as AboutInfo exposing (AboutInfo)
import Models.Resources.Role as Role exposing (Role(Administrator))
import Models.Ui.LoginForm as LoginForm exposing (LoginForm)
import Test exposing (Test, describe, test)
import Test.Html.Events as Events
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Updates.Messages exposing (UpdateLoginFormMsg(EnterPassword, EnterUserName, LoginAttempt, LogoutAttempt))
import Views.Header as Header


tests : Test
tests =
    describe "Header View"
        [ describe "Login Form"
            [ test "Should render username in input field" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-username" ]
                        |> Query.has [ Selector.attribute "value" defaultLoginForm.username ]
            , test "Should render password in input field" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-password" ]
                        |> Query.has [ Selector.attribute "value" defaultLoginForm.password ]
            , test "Should look normal if the login is correct" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            { defaultLoginForm | loginIncorrect = False }

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-form" ]
                        |> Query.has [ Selector.classes [ "form-inline", "ml-auto" ] ]
            , test "Should shake if the login is correct" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            { defaultLoginForm | loginIncorrect = True }

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-form" ]
                        |> Query.has [ Selector.classes [ "form-inline", "ml-auto", "animated", "shake" ] ]
            , test "Should render if auth is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "header-login-form" ]
            , test "Should not render if auth is not required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "header-login-form" ]
            , test "Should not render if we don't know if auth is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Nothing

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "header-login-form" ]
            , test "Should update the username when the user name input field changes" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-username" ]
                        |> Events.simulate (Events.Input "admin")
                        |> Events.expectEvent (UpdateLoginFormMsg (EnterUserName "admin"))
            , test "Should update the password when the password input field changes" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-password" ]
                        |> Events.simulate (Events.Input "secret")
                        |> Events.expectEvent (UpdateLoginFormMsg (EnterPassword "secret"))
            , test "Should attempt to login with the entered credentials on form sumbission" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just True

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-login-form" ]
                        |> Events.simulate Events.Submit
                        |> Events.expectEvent (UpdateLoginFormMsg (LoginAttempt defaultLoginForm.username defaultLoginForm.password))
            ]
        , describe "Logout Form"
            [ test "Should render if auth is enabled but not required (which means you are logged in)" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo True

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "header-logout-form" ]
            , test "Should not render if auth is disabled and not required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo False

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "header-logout-form" ]
            , test "Should not render if it is unknown whether auth is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "header-logout-form" ]
            , test "Should attempt to login with the entered credentials on form sumbission" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo True

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-logout-form" ]
                        |> Events.simulate Events.Submit
                        |> Events.expectEvent (UpdateLoginFormMsg LogoutAttempt)
            ]
        , describe "Template Filter"
            [ test "Should update the template filter when the user types values" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just defaultAboutInfo

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""

                        input =
                            "zeppelin"
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-template-filter" ]
                        |> Events.simulate (Events.Input input)
                        |> Events.expectEvent (TemplateFilter input)
            , test "Should not render if it is unknown whether auth is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "header-template-filter" ]
            , test "Should render if it when logged in" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo True

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "header-template-filter" ]
            , test "Should render if no login is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo False

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "header-template-filter" ]
            ]
        , describe "Instance Filter"
            [ test "Should update the instance filter when the user types values" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just defaultAboutInfo

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""

                        input =
                            "zeppelin"
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.find [ Selector.id "header-instance-filter" ]
                        |> Events.simulate (Events.Input input)
                        |> Events.expectEvent (InstanceFilter input)
            , test "Should not render if it is unknown whether auth is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Nothing

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.hasNot [ Selector.id "header-instance-filter" ]
            , test "Should render if it when logged in" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo True

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "header-instance-filter" ]
            , test "Should render if no login is required" <|
                \() ->
                    let
                        maybeAboutInfo =
                            Just <| withAuthEnabled defaultAboutInfo False

                        loginForm =
                            defaultLoginForm

                        maybeAuthRequired =
                            Just False

                        templateFilter =
                            ""

                        instanceFilter =
                            ""

                        nodeFilter =
                            ""
                    in
                    Header.view maybeAboutInfo loginForm maybeAuthRequired templateFilter instanceFilter nodeFilter Instances
                        |> Query.fromHtml
                        |> Query.has [ Selector.id "header-instance-filter" ]
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
    }
        |> AboutInfo.asAuthInfoOf aboutInfo
