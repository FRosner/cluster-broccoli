module Views.Header exposing (view)

import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Ui.LoginForm exposing (LoginForm)
import Messages exposing (AnyMsg(..))
import Updates.Messages exposing (UpdateLoginFormMsg(..))
import Utils.HtmlUtils exposing (icon)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput, onSubmit)
import Regex exposing (Regex)


view : Maybe AboutInfo -> LoginForm -> Maybe Bool -> String -> String -> Html AnyMsg
view maybeAboutInfo loginFormModel maybeAuthRequired templateFilterString instanceFilterString =
    let
        ( maybeUserInfo, maybeAuthEnabled ) =
            ( Maybe.map (\i -> i.authInfo.userInfo) maybeAboutInfo
            , Maybe.map (\i -> i.authInfo.enabled) maybeAboutInfo
            )
    in
        div
            [ class "container" ]
            [ nav
                [ class "navbar navbar-default navbar-fixed-top" ]
                [ div
                    [ class "container-fluid"
                    , style
                        [ ( "padding-right", "35px" )
                        ]
                    ]
                    [ navbarHeader maybeAboutInfo
                    , navbarCollapse maybeAboutInfo maybeUserInfo maybeAuthEnabled maybeAuthRequired loginFormModel templateFilterString instanceFilterString
                    ]
                ]
            ]


navbarHeader maybeAboutInfo =
    div
        [ class "navbar-header"
        , style
            [ ( "margin-left", "35px" )
            ]
        ]
        [ navbarToggleButton
        , navbarBrand
        , navbarBrandDropdown maybeAboutInfo
        ]


navbarBrand =
    a
        [ class "navbar-brand dropdown-toggle"
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
    ul
        [ class "dropdown-menu"
        , style
            [ ( "margin-left", "35px" )
            ]
        ]
        [ lia "https://github.com/FRosner/cluster-broccoli" "Source Code"
        , lia "https://github.com/FRosner/cluster-broccoli/wiki" "Documentation"
        , lia "https://github.com/FRosner/cluster-broccoli/issues/new" "Report a Bug"
        , lia "https://gitter.im/FRosner/cluster-broccoli" "Get Help"
        ]


lia aHref content =
    li []
        [ a
            [ href aHref ]
            [ text content ]
        ]


navbarToggleButton =
    button
        [ type_ "button"
        , class "navbar-toggle collapsed"
        , attribute "data-toggle" "collapse"
        , attribute "data-target" "#navbar-collapse"
        , attribute "aria-expanded" "false"
        ]
        [ span [ class "sr-only" ] [ text "Toggle menu" ]
        , span [ class "icon-bar" ] []
        , span [ class "icon-bar" ] []
        , span [ class "icon-bar" ] []
        ]


navbarCollapse : Maybe AboutInfo -> Maybe UserInfo -> Maybe Bool -> Maybe Bool -> LoginForm -> String -> String -> Html AnyMsg
navbarCollapse maybeAboutInfo maybeUserInfo maybeAuthEnabled maybeAuthRequired loginFormModel templateFilterString instanceFilterString =
    div
        [ class "collapse navbar-collapse"
        , id "navbar-collapse"
        ]
        (List.concat
            [ [ Html.map UpdateLoginFormMsg (loginLogoutView loginFormModel maybeAuthEnabled maybeAuthRequired)
              , userInfoView maybeUserInfo
              ]
            , if (maybeAuthRequired == Just True || maybeAuthRequired == Nothing || (maybeAuthRequired == Just False && maybeAuthEnabled == Nothing)) then
                []
              else
                [ templateFilter templateFilterString
                , instanceFilter instanceFilterString
                ]
            ]
        )


templateFilter filterString =
    ul
        [ class "nav navbar-nav navbar-left" ]
        [ li []
            [ div [ class "form-group navbar-form" ]
                [ div
                    [ class "input-group" ]
                    [ span
                        [ class "input-group-addon" ]
                        [ icon "fa fa-filter" [ title "Template Filter" ] ]
                    , input
                        [ type_ "text"
                        , id "header-template-filter"
                        , class "form-control"
                        , onInput TemplateFilter
                        , placeholder "Template Filter"
                        , value filterString
                        ]
                        []
                    ]
                ]
            ]
        ]


instanceFilter filterString =
    ul
        [ class "nav navbar-nav navbar-left" ]
        [ li []
            [ div [ class "form-group navbar-form" ]
                [ div
                    [ class "input-group" ]
                    [ span
                        [ class "input-group-addon" ]
                        [ icon "fa fa-filter" [ title "Instance Filter" ] ]
                    , input
                        [ type_ "text"
                        , id "header-instance-filter"
                        , class "form-control"
                        , onInput InstanceFilter
                        , placeholder "Instance Filter"
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
                [ class "nav navbar-nav navbar-right" ]
                [ li
                    [ class "dropdown" ]
                    [ a
                        [ class "dropdown-toggle"
                        , attribute "data-toggle" "dropdown"
                        , attribute "role" "button"
                        , attribute "aria-haspopup" "true"
                        , attribute "aria-expanded" "false"
                        ]
                        [ text userInfo.name
                        , span [ class "caret" ] []
                        ]
                    , ul
                        [ class "dropdown-menu" ]
                        [ li []
                            [ a []
                                [ text "Role: "
                                , code [] [ text (toString userInfo.role) ]
                                ]
                            ]
                        , li []
                            [ a []
                                [ text "Instances: "
                                , code [] [ text userInfo.instanceRegex ]
                                ]
                            ]
                        ]
                    ]
                ]

        _ ->
            span [] []


redIfLoginFailed loginFailed =
    if (loginFailed) then
        "#fee"
    else
        "#fff"


attentionIfLoginFailed loginFailed =
    if (loginFailed) then
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
                [ "navbar-form navbar-right "
                , (attentionIfLoginFailed loginFormModel.loginIncorrect)
                ]
            )
        , onSubmit <| LoginAttempt loginFormModel.username loginFormModel.password
        ]
        [ div [ class "form-group" ]
            [ input
                [ type_ "text"
                , id "header-login-username"
                , class "form-control"
                , style [ ( "background-color", (redIfLoginFailed loginFormModel.loginIncorrect) ) ]
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
                , class "form-control"
                , style [ ( "background-color", (redIfLoginFailed loginFormModel.loginIncorrect) ) ]
                , placeholder "Password"
                , value loginFormModel.password
                ]
                []
            ]
        , text " " -- otherwise Bootstrap layout breaks, doh
        , button
            [ type_ "submit"
            , class "btn btn-default"
            , title "Login"
            ]
            [ icon "glyphicon glyphicon-arrow-right" [] ]
        ]


logoutFormView =
    Html.form
        [ id "header-logout-form"
        , class "navbar-form navbar-right"
        , onSubmit LogoutAttempt
        ]
        [ div [ class "form-group" ]
            [ button
                [ type_ "submit"
                , class "btn btn-default"
                , title "Logout"
                ]
                [ icon "glyphicon glyphicon-log-out" [] ]
            ]
        ]
