module Views.Body exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.NewInstanceForm
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon)

view : List Template -> Set TemplateId -> List Instance -> Html UpdateBodyViewMsg
view templates expandedTemplates instances =
  div
    [ class "container" ]
    (List.map (templateView expandedTemplates instances) templates)

templateView expandedTemplates instances template =
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
          [ class "panel-body" ]
          [ text template.description ]
        , table
          [ class "table table-hover"
          , style [ ("margin-bottom", "0px") ]
          ]
          [ thead []
            [ tr []
              [ th []
                [ text "Actions" ]
              , th []
                [ text "ID" ]
              , th []
                [ text "Services" ]
              , th []
                [ text "Job Status" ]
              , th []
                [ text "" ]
              ]
            ]
          , tbody []
            ( List.map instanceRowView templateInstances )
          ]
        ]
      ]

instanceRowView instance =
  tr []
    [ td []
      [ icon "fa fa-pencil-square-o" [] ]
    , td []
      [ text instance.id ]
    , td []
      [ text "..." ] -- TODO services
    , td []
      [ text "..." ] -- TODO job status
    , td []
      [ icon "glyphicon glyphicon-play" []
      , icon "glyphicon glyphicon-stop" []
      ]
    ]

templatePanelHeadingView template expandedTemplates instances =
  span
    []
    [ templateIdView template expandedTemplates
    , text " "
    , templatePanelHeadingInfo "fa fa-list" "Number of Instances" (text (toString (List.length instances)))
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
    [ icon clazz [ ("margin-right", "4px") ]
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
      [ ("margin-right", "4px") ]
    , span
      [ style [ ("font-size", "125%"), ("margin-right", "10px") ] ]
      [ text template.id ]
    ]

templateVersion template =
  span
    [ style [ ("font-family", "monospace") ] ]
    [ text (String.left 8 template.version) ]
