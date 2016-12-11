module Views.Body exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButton)

view : List Template -> Set TemplateId -> List Instance -> Dict InstanceId (List Service) -> Html UpdateBodyViewMsg
view templates expandedTemplates instances services =
  div
    [ class "container" ]
    (List.map (templateView expandedTemplates instances services) templates)

templateView expandedTemplates instances services template =
  let (templateInstances) =
    List.filter (\i -> i.template.id == template.id) instances
  in
    div
      [ class "panel panel-default" ]
      [ div
        [ class "panel-heading" ]
        [ templatePanelHeadingView template expandedTemplates templateInstances ]
      , div
        [ class (if (Set.member template.id expandedTemplates) then "show" else "hidden") ]
        [ div
          [ class "panel-body"
          , style [ ( "padding-bottom", "0px" ) ]
          ]
          [ p []
            [ text template.description ]
          , p []
            [ iconButton
                "fa fa-plus-circle"
                "New"
            , text " "
            , iconButton
                "fa fa-code-fork"
                "Upgrade"
            , text " "
            , iconButton
                "fa fa-play-circle"
                "Start"
            , text " "
            , iconButton
                "fa fa-stop-circle"
                "Stop"
            ]
          ]
        , table
          [ class "table table-hover"
          , style [ ("margin-bottom", "0px") ]
          ]
          [ thead []
            [ tr []
              [ th []
                [ input
                  [ type_ "checkbox"
                  , title "Select All"
                  ]
                  []
                ]
              , th []
                [ icon "fa fa-hashtag" [ title "Instance ID" ] ]
              , th []
                [ icon "fa fa-code-fork" [ title "Template Version" ] ]
              , th []
                [ icon "fa fa-cubes" [ title "Services" ] ]
              , th []
                [ icon "fa fa-question-circle-o" [ title "Job Status" ] ]
              , th []
                [ icon "fa fa-cogs" [ title "Job Controls" ] ]
              ]
            ]
          , tbody []
            ( List.map (instanceRowView services) templateInstances )
          ]
        ]
      ]

instanceRowView services instance =
  let (maybeInstanceServices) =
    Dict.get instance.id services
  in
    tr []
      [ td []
        [ input [ type_ "checkbox" ] [] ]
      , td []
        [ span
            [ style [ ("role", "button") ] ]
            [ icon "fa fa-caret-right" []
            , text instance.id
            ]
        ]
      , td []
        [ span
          [ style [ ("font-family", "monospace") ] ]
          [ text (String.left 8 instance.template.version) ]
        ]
      , td []
        ( servicesView maybeInstanceServices )
      , td []
        [ text "running" ] -- TODO job status
      , td []
        [ icon "glyphicon glyphicon-play" []
        , icon "glyphicon glyphicon-stop" []
        ]
      ]

servicesView maybeServices =
  case maybeServices of
    Just services ->
      (List.concatMap serviceView services)
    Nothing ->
      [ text "-" ]

serviceView service =
  let (iconClass, textColor) =
    case service.status of
      Passing ->
        ("fa fa-check-circle", "#070")
      Failing ->
        ("fa fa-times-circle", "#900")
      Unknown ->
        ("fa fa-question-circle", "grey")
  in
    [ a
      [ href
        ( String.concat
          [ service.protocol
          , "://"
          , service.address
          , ":"
          , (toString service.port_)
          ]
        )
      , style
        [ ("margin-right", "8px")
        , ("color", textColor)
        ]
      ]
      [ icon iconClass [ style [ ("margin-right", "4px") ] ]
      , text service.name
      ]
    , text " "
    ]

templatePanelHeadingView template expandedTemplates instances =
  span
    []
    [ templateIdView template expandedTemplates
    , text " "
    , templatePanelHeadingInfo "fa fa-list" "Number of Instances" (text (toString (List.length instances)))
    , text " "
    , templatePanelHeadingInfo "fa fa-code-fork" "Template Version" (templateVersion template)
    ]

templatePanelHeadingInfo clazz infoTitle info =
  span
    [ style
      [ ("margin-left", "10px")
      , ("font-weight", "100")
      , ("background-color", "#555")
      , ("color", "#fff")
      , ("margin-top", "4px")
      ]
    , title infoTitle
    , class "badge pull-right"
    ]
    [ icon clazz [ style [ ("margin-right", "4px") ] ]
    , info
    ]

templateIdView template expandedTemplates =
  span
    [ attribute "role" "button"
    , onClick (ToggleTemplate template.id)
    ]
    [ icon
      ( String.concat
        [ "glyphicon glyphicon-chevron-"
        , if (Set.member template.id expandedTemplates) then "down" else "right"
        ]
      )
      [ style [ ("margin-right", "4px") ] ]
    , span
      [ style [ ("font-size", "125%"), ("margin-right", "10px") ] ]
      [ text template.id ]
    ]

templateVersion template =
  span
    [ style [ ("font-family", "monospace") ] ]
    [ text (String.left 8 template.version) ]
