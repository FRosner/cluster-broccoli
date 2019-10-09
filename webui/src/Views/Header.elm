module Views.Header exposing (view)

import Bootstrap.Button as Button
import Bootstrap.Form.Input as Input
import Bootstrap.Utilities.Spacing as Spacing
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput, onSubmit)
import Messages exposing (AnyMsg(..))
import Model exposing (TabState(Instances, Resources))
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Ui.LoginForm exposing (LoginForm)
import Regex exposing (Regex)
import Updates.Messages exposing (UpdateLoginFormMsg(..))
import Utils.HtmlUtils exposing (icon)


view : Maybe AboutInfo -> LoginForm -> Maybe Bool -> String -> String -> String -> TabState -> Html AnyMsg
view maybeAboutInfo loginFormModel maybeAuthRequired templateFilterString instanceFilterString nodeFilterString tabState =
    let
        ( maybeUserInfo, maybeAuthEnabled ) =
            ( Maybe.map (\i -> i.authInfo.userInfo) maybeAboutInfo
            , Maybe.map (\i -> i.authInfo.enabled) maybeAboutInfo
            )
    in
    nav
        [ class "navbar navbar-expand-md navbar-fixed-top navbar-light bg-light border" ]
        [ div [ class "dropdown ml-3" ] [ navbarBrand, navbarBrandDropdown maybeAboutInfo ]
        , navbarToggleButton
        , navbarCollapse maybeAboutInfo maybeUserInfo maybeAuthEnabled maybeAuthRequired loginFormModel templateFilterString instanceFilterString nodeFilterString tabState
        ]


navbarToggleButton =
    button
        [ type_ "button"
        , class "navbar-toggler collapsed"
        , attribute "data-toggle" "collapse"
        , attribute "data-target" "#navbar-collapse"
        , attribute "aria-expanded" "false"
        ]
        [ span [ class "sr-only navbar-toggler-icon" ] [ text "Toggle menu" ]
        ]


navbarBrand =
    a
        [ class "navbar-brand nav-link dropdown-toggle"
        , id "brandDropdown"
        , attribute "data-toggle" "dropdown"
        , attribute "role" "button"
        , attribute "aria-haspopup" "true"
        , attribute "aria-expanded" "false"
        , href "#"
        ]
        [ text "Cluster Broccoli"
        , span [ class "caret" ] []
        ]


navbarBrandDropdown maybeAboutInfo =
    div
        [ class "dropdown-menu"
        , attribute "aria-labelledby" "brandDropdown"
        ]
        [ lia "https://github.com/FRosner/cluster-broccoli" "Source Code"
        , lia "https://github.com/FRosner/cluster-broccoli/wiki" "Documentation"
        , lia "https://github.com/FRosner/cluster-broccoli/issues/new" "Report a Bug"
        , lia "https://gitter.im/FRosner/cluster-broccoli" "Get Help"
        ]


lia aHref content =
    a [ class "dropdown-item", href aHref ] [ text content ]


navbarCollapse : Maybe AboutInfo -> Maybe UserInfo -> Maybe Bool -> Maybe Bool -> LoginForm -> String -> String -> String -> TabState -> Html AnyMsg
navbarCollapse maybeAboutInfo maybeUserInfo maybeAuthEnabled maybeAuthRequired loginFormModel templateFilterString instanceFilterString nodeFilterString tabState =
    div
        [ class "collapse navbar-collapse"
        , id "navbar-collapse"
        ]
        (List.concat
            [ if maybeAuthRequired == Just True || maybeAuthRequired == Nothing || (maybeAuthRequired == Just False && maybeAuthEnabled == Nothing) then
                []

              else
                List.append
                    [ tabGen "Instances" Instances (tabState == Instances)
                    , tabGen "Resources" Resources (tabState == Resources)
                    ]
                    (case tabState of
                        Instances ->
                            [ templateFilter templateFilterString
                            , instanceFilter instanceFilterString
                            ]

                        Resources ->
                            [ nodeFilter nodeFilterString ]
                    )
            , [ userInfoView maybeUserInfo
              , Html.map UpdateLoginFormMsg (loginLogoutView loginFormModel maybeAuthEnabled maybeAuthRequired)
              ]
            ]
        )


tabGen : String -> TabState -> Bool -> Html AnyMsg
tabGen navString tabState isActive =
    ul
        [ class "nav navbar-nav mr-3 ml-3" ]
        [ li
            [ classList [ ( "nav-item", True ), ( "active", isActive ) ] ]
            [ a
                [ class "nav-link"
                , href "#"
                , onClick (TabMsg tabState)
                ]
                [ text navString ]
            ]
        ]


templateFilter filterString =
    filterView "header-template-filter" "Template Filter" TemplateFilter filterString


instanceFilter filterString =
    filterView "header-instance-filter" "Instance Filter" InstanceFilter filterString


nodeFilter filterString =
    filterView "header-node-filter" "Node Filter" NodeFilter filterString


filterView inputId titleString onInputMessage filterString =
    ul
        [ class "nav navbar-nav mr-3 ml-3" ]
        [ li []
            [ div [ class "form-inline" ]
                [ div
                    [ class "input-group" ]
                    [ div
                        [ class "input-group-prepend" ]
                        [ div [ class "input-group-text" ]
                            [ i [ class "fa fa-filter", title titleString ] [] ]
                        ]
                    , input
                        [ type_ "text"
                        , id inputId
                        , class "form-control"
                        , onInput onInputMessage
                        , placeholder titleString
                        , value filterString
                        ]
                        []
                    ]
                ]
            ]
        ]


userInfoView : Maybe UserInfo -> Html a
userInfoView maybeUserInfo =
    case maybeUserInfo of
        Just userInfo ->
            ul
                [ class "nav navbar-nav ml-auto" ]
                [ li
                    [ class "nav-item dropdown" ]
                    [ a
                        [ class "nav-link dropdown-toggle"
                        , id "userDropdown"
                        , attribute "data-toggle" "dropdown"
                        , attribute "role" "button"
                        , attribute "aria-haspopup" "true"
                        , attribute "aria-expanded" "false"
                        , href "#"
                        ]
                        [ text userInfo.name
                        , span [ class "caret" ] []
                        ]
                    , div
                        [ class "dropdown-menu dropdown-menu-right"
                        , attribute "aria-labelledby" "userDropdown"
                        ]
                        [ a [ class "dropdown-item", href "#" ]
                            [ text "Role: "
                            , code [] [ text (toString userInfo.role) ]
                            ]
                        , a [ class "dropdown-item", href "#" ]
                            [ text "Instances: "
                            , code [] [ text userInfo.instanceRegex ]
                            ]
                        ]
                    ]
                ]

        _ ->
            span [] []


redIfLoginFailed loginFailed =
    if loginFailed then
        "#fee"

    else
        "#fff"


attentionIfLoginFailed loginFailed =
    if loginFailed then
        "animated shake"

    else
        ""


loginLogoutView loginFormModel maybeAuthEnabled maybeAuthRequired =
    case maybeAuthRequired of
        Nothing ->
            span [] []

        Just True ->
            loginFormView loginFormModel

        Just False ->
            case maybeAuthEnabled of
                Nothing ->
                    span [] []

                Just True ->
                    logoutFormView

                Just False ->
                    span [] []


loginFormView loginFormModel =
    Html.form
        [ id "header-login-form"
        , class
            (String.concat
                [ "form-inline ml-auto mr-3 "
                , attentionIfLoginFailed loginFormModel.loginIncorrect
                ]
            )
        , onSubmit <| LoginAttempt loginFormModel.username loginFormModel.password
        ]
        [ input
            [ type_ "text"
            , id "header-login-username"
            , class "form-control mr-sm-2"
            , style [ ( "background-color", redIfLoginFailed loginFormModel.loginIncorrect ) ]
            , onInput EnterUserName
            , placeholder "User"
            , value loginFormModel.username
            ]
            []
        , text " "
        , input
            [ type_ "password"
            , id "header-login-password"
            , onInput EnterPassword
            , class "form-control mr-sm-2"
            , style [ ( "background-color", redIfLoginFailed loginFormModel.loginIncorrect ) ]
            , placeholder "Password"
            , value loginFormModel.password
            ]
            []
        , text " " -- otherwise Bootstrap layout breaks, doh
        , button
            [ type_ "submit"
            , class "btn btn-outline-secondary"
            , title "Login"
            ]
            [ i [ class "fa fa-sign-in" ] [] ]
        ]


logoutFormView =
    Html.form
        [ id "header-logout-form"
        , class "navbar-form ml-5 mr-3"
        , onSubmit LogoutAttempt
        ]
        [ button
            [ type_ "submit"
            , class "btn btn-outline-secondary"
            , title "Logout"
            ]
            [ i [ class "fa fa-sign-out" ] [] ]
        ]
