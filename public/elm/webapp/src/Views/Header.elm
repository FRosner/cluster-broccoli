module Views.Header exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput, onSubmit)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Messages exposing (AnyMsg(..))
import Updates.Messages exposing (UpdateLoginFormMsg(..))

view : Maybe AboutInfo -> Html AnyMsg
view maybeAboutInfo =
  nav
    [ class "navbar navbar-default" ]
    [ div
      [ class "container-fluid" ]
      [ navbarHeader
      , navbarCollapse
      ]
    ]
  -- case maybeAboutInfo of
  --     Just aboutInfo ->
  --       div []
  --           [ text aboutInfo.projectInfo.name
  --           , text aboutInfo.projectInfo.version]
  --     Nothing ->
  --       div []
  --           [ text "nothing" ]

navbarHeader =
  div
    [ class "navbar-header" ]
    [ navbarToggleButton
    , navbarBrand
    , navbarBrandDropdown
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

navbarBrandDropdown =
  ul
    [ class "dropdown-menu" ]
    [ li []
      [ a []
        [ text "Cluster Manager" ]
      ]
    , li []
      [ a []
        [ text "Service Discovery" ]
      ]
    , li
      [ attribute "role" "separator"
      , class "divider"
      ]
      []
    , lia "https://github.com/FRosner/cluster-broccoli" "Source Code"
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

navbarCollapse =
  div
    [ class "collapse navbar-collapse"
    , id "navbar-collapse"
    ]
    [ Html.map UpdateLoginFormMsg loginForm
    ]

loginForm =
  Html.form
    [ class "navbar-form navbar-right"
    , onSubmit LoginAttempt
    ]
    [ div [ class "form-group" ]
      [ input
        [ type_ "text"
        , class "form-control"
        , onInput EnterUserName
        , placeholder "User"
        ]
        []
      , text " "
      , input
        [ type_ "password"
        , onInput EnterPassword
        , class "form-control"
        , placeholder "Password"
        ]
        []
      ]
    , text " "
    , button
      [ type_ "submit"
      , class "btn btn-default"
      ]
      [ text "Login" ]
    ]
